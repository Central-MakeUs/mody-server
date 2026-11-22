package cmc.mody.common.image;

import cmc.mody.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 원본 사진 하나에 대한 축소본 생성 작업과 결과.
 * 이 행 자체가 작업 큐다. 기록 저장과 분리해 두어, 축소본 생성이 늦거나 실패해도 기록 저장은 영향받지 않는다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "image_variant",
    indexes = {
        @Index(name = "uk_image_variant_image_key", columnList = "image_key", unique = true),
        @Index(name = "idx_image_variant_dispatch", columnList = "variant_status, next_attempt_at")
    }
)
public class ImageVariant extends BaseEntity {
    private static final int MAX_ERROR_LENGTH = 500;

    @Column(name = "image_key", nullable = false, length = 500)
    private String imageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "variant_status", nullable = false, length = 20)
    private ImageVariantStatus variantStatus = ImageVariantStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    @Column(name = "picked_by", length = 100)
    private String pickedBy;

    @Column(name = "picked_at")
    private LocalDateTime pickedAt;

    @Column(name = "thumbnail_key", length = 500)
    private String thumbnailKey;

    @Column(name = "display_key", length = 500)
    private String displayKey;

    @Column(name = "original_bytes")
    private Long originalBytes;

    @Column(name = "thumbnail_bytes")
    private Long thumbnailBytes;

    @Column(name = "display_bytes")
    private Long displayBytes;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "last_error", length = MAX_ERROR_LENGTH)
    private String lastError;

    public ImageVariant(Long id, String imageKey) {
        super(id);
        this.imageKey = imageKey;
    }

    public void markProcessing(String serverId, LocalDateTime pickedAt) {
        this.variantStatus = ImageVariantStatus.PROCESSING;
        this.pickedBy = serverId;
        this.pickedAt = pickedAt;
        this.attemptCount++;
    }

    public void markReady(
        String thumbnailKey,
        String displayKey,
        long originalBytes,
        long thumbnailBytes,
        long displayBytes,
        LocalDateTime processedAt
    ) {
        this.variantStatus = ImageVariantStatus.READY;
        this.thumbnailKey = thumbnailKey;
        this.displayKey = displayKey;
        this.originalBytes = originalBytes;
        this.thumbnailBytes = thumbnailBytes;
        this.displayBytes = displayBytes;
        this.processedAt = processedAt;
        this.nextAttemptAt = null;
        this.lastError = null;
    }

    /** 재시도 가능하면 PENDING으로 되돌리고, 횟수를 다 썼거나 재시도해도 같은 결과면 FAILED로 둔다. */
    public void markAttemptFailed(String error, boolean retryable, int maxAttempts, LocalDateTime nextAttemptAt) {
        this.lastError = truncate(error);
        if (!retryable || attemptCount >= maxAttempts) {
            this.variantStatus = ImageVariantStatus.FAILED;
            this.nextAttemptAt = null;
            return;
        }
        this.variantStatus = ImageVariantStatus.PENDING;
        this.nextAttemptAt = nextAttemptAt;
    }

    public boolean isReady() {
        return variantStatus == ImageVariantStatus.READY;
    }

    private static String truncate(String error) {
        if (error == null) {
            return null;
        }
        return error.length() <= MAX_ERROR_LENGTH ? error : error.substring(0, MAX_ERROR_LENGTH);
    }
}
