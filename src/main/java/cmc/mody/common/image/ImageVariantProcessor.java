package cmc.mody.common.image;

import cmc.mody.common.image.ImageVariantGenerator.GeneratedVariants;
import cmc.mody.common.image.ImageVariantGenerator.UnsupportedImageFormatException;
import cmc.mody.common.upload.ImageObjectStorage;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 선점한 작업 하나를 처리한다: 원본 읽기 → 축소 → 저장소에 쓰기 → 결과 기록.
 * 이미지 처리와 저장소 입출력은 트랜잭션 밖에서 하고, 상태 변경만 짧은 트랜잭션으로 남긴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageVariantProcessor {
    private static final String JPEG = "image/jpeg";

    private final ImageVariantRepository imageVariantRepository;
    private final ImageObjectStorage imageObjectStorage;
    private final ImageVariantGenerator imageVariantGenerator;
    private final ImageVariantProperties properties;
    private final TransactionTemplate transactionTemplate;

    public void process(Long variantId) {
        ImageVariant variant = imageVariantRepository.findById(variantId).orElse(null);
        if (variant == null || variant.getVariantStatus() != ImageVariantStatus.PROCESSING) {
            return;
        }
        String imageKey = variant.getImageKey();
        long started = System.currentTimeMillis();
        try {
            byte[] original = imageObjectStorage.read(imageKey);
            GeneratedVariants generated = imageVariantGenerator.generate(original);
            String thumbnailKey = variantKey(imageKey, properties.getThumbnailMaxSize());
            String displayKey = variantKey(imageKey, properties.getDisplayMaxSize());
            imageObjectStorage.write(thumbnailKey, generated.thumbnail(), JPEG);
            imageObjectStorage.write(displayKey, generated.display(), JPEG);
            complete(variantId, v -> v.markReady(
                thumbnailKey,
                displayKey,
                original.length,
                generated.thumbnail().length,
                generated.display().length,
                LocalDateTime.now()
            ));
            log.info("image variant ready imageKey={} originalBytes={} thumbnailBytes={} displayBytes={} elapsedMs={}",
                imageKey, original.length, generated.thumbnail().length, generated.display().length,
                System.currentTimeMillis() - started);
        } catch (UnsupportedImageFormatException e) {
            fail(variantId, imageKey, e, false);
        } catch (Exception | OutOfMemoryError e) {
            fail(variantId, imageKey, e, true);
        }
    }

    /** 원본 키에서 결정적으로 만든다. 같은 원본을 다시 처리하면 같은 위치를 덮어쓴다. */
    String variantKey(String imageKey, int size) {
        int dot = imageKey.lastIndexOf('.');
        int slash = imageKey.lastIndexOf('/');
        String withoutExtension = dot > slash ? imageKey.substring(0, dot) : imageKey;
        return "%s/w%d/%s.jpg".formatted(properties.getKeyPrefix(), size, withoutExtension);
    }

    private void fail(Long variantId, String imageKey, Throwable error, boolean retryable) {
        String message = error.getClass().getSimpleName() + ": " + error.getMessage();
        log.warn("image variant failed imageKey={} retryable={} reason={}", imageKey, retryable, message);
        complete(variantId, v -> v.markAttemptFailed(
            message,
            retryable,
            properties.getMaxAttempts(),
            LocalDateTime.now().plusSeconds(properties.getRetryDelaySeconds() * v.getAttemptCount())
        ));
    }

    private void complete(Long variantId, java.util.function.Consumer<ImageVariant> change) {
        transactionTemplate.executeWithoutResult(status ->
            imageVariantRepository.findById(variantId).ifPresent(change));
    }
}
