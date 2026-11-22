package cmc.mody.common.image;

import cmc.mody.common.id.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;

/**
 * 축소본 생성 작업을 만든다.
 * 기록 저장이 커밋된 뒤 별도 트랜잭션에서 만들고, 실패해도 예외를 밖으로 던지지 않는다.
 * 축소본은 없어도 앱이 원본으로 보여 줄 수 있으므로 기록 저장을 막을 이유가 없다. 놓친 작업은 backfill이 채운다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageVariantRequestService {
    private final IdGenerator idGenerator;
    private final ImageVariantRepository imageVariantRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(ImageVariantRequestedEvent event) {
        try {
            request(event.imageKey());
        } catch (DataIntegrityViolationException e) {
            log.info("image variant already requested imageKey={}", event.imageKey());
        } catch (RuntimeException e) {
            log.warn("image variant request failed imageKey={} reason={}", event.imageKey(), e.getMessage());
        }
    }

    @Transactional
    public boolean request(String imageKey) {
        if (!isStoredImageKey(imageKey) || imageVariantRepository.existsByImageKey(imageKey)) {
            return false;
        }
        imageVariantRepository.saveAndFlush(new ImageVariant(idGenerator.nextId(), imageKey));
        return true;
    }

    static boolean isStoredImageKey(String imageKey) {
        return StringUtils.hasText(imageKey) && !imageKey.startsWith("http://") && !imageKey.startsWith("https://");
    }
}
