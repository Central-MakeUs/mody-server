package cmc.mody.common.image;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "image.variant", name = "enabled", havingValue = "true")
public class ImageVariantScheduler {
    private final ImageVariantProperties properties;
    private final ImageVariantClaimService claimService;
    private final ImageVariantProcessor processor;
    private final ImageVariantRequestService requestService;
    private final ImageVariantRepository imageVariantRepository;
    private final Executor executor;
    private final String serverId;

    public ImageVariantScheduler(
        ImageVariantProperties properties,
        ImageVariantClaimService claimService,
        ImageVariantProcessor processor,
        ImageVariantRequestService requestService,
        ImageVariantRepository imageVariantRepository,
        @Qualifier("imageVariantExecutor") Executor executor
    ) {
        this.properties = properties;
        this.claimService = claimService;
        this.processor = processor;
        this.requestService = requestService;
        this.imageVariantRepository = imageVariantRepository;
        this.executor = executor;
        this.serverId = resolveServerId();
    }

    @Scheduled(fixedDelayString = "${image.variant.fixed-delay-ms:5000}")
    public void run() {
        List<Long> claimed = claimService.claimDue(serverId, Math.max(1, properties.getBatchSize()));
        if (claimed.isEmpty()) {
            backfill();
            return;
        }
        for (Long id : claimed) {
            try {
                executor.execute(() -> processor.process(id));
            } catch (RejectedExecutionException e) {
                // 선점한 작업은 PROCESSING으로 남고, staleProcessingMinutes 뒤 다시 선점된다.
                log.warn("image variant executor full variantId={}", id);
            }
        }
    }

    /** 신규 작업이 없을 때만 기존 사진의 생성 요청을 조금씩 만든다. 사용자 요청 처리와 경쟁하지 않게 하기 위해서다. */
    void backfill() {
        if (!properties.isBackfillEnabled()) {
            return;
        }
        List<String> imageKeys = imageVariantRepository.findImageKeysWithoutVariant(
            Math.max(1, properties.getBackfillBatchSize())
        );
        int requested = 0;
        for (String imageKey : imageKeys) {
            try {
                if (requestService.request(imageKey)) {
                    requested++;
                }
            } catch (RuntimeException e) {
                log.debug("image variant backfill skipped imageKey={} reason={}", imageKey, e.getMessage());
            }
        }
        if (requested > 0) {
            log.info("image variant backfill requested count={}", requested);
        }
    }

    private String resolveServerId() {
        try {
            return InetAddress.getLocalHost().getHostName() + "-" + UUID.randomUUID();
        } catch (UnknownHostException exception) {
            return "unknown-" + UUID.randomUUID();
        }
    }
}
