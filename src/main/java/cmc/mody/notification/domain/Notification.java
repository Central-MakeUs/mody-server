package cmc.mody.notification.domain;

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

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "notification",
        indexes = {
                @Index(
                        name = "idx_notification_receiver_status",
                        columnList = "receiver_member_id, notification_status"
                ),
                @Index(name = "idx_notification_receiver_created", columnList = "receiver_member_id, created_at"),
                @Index(
                        name = "idx_notification_dispatch",
                        columnList = "delivery_status, scheduled_at, next_retry_at, picked_by"
                ),
                @Index(name = "uk_notification_dedupe", columnList = "dedupe_key", unique = true)
        }
)
public class Notification extends BaseEntity {
    @Column(name = "receiver_member_id", nullable = false)
    private Long receiverMemberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 30)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_status", nullable = false, length = 20)
    private NotificationStatus notificationStatus = NotificationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 20)
    private NotificationDeliveryStatus deliveryStatus = NotificationDeliveryStatus.PENDING;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(name = "image_key", length = 500)
    private String imageKey;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "link", length = 300)
    private String link;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "picked_by", length = 100)
    private String pickedBy;

    @Column(name = "picked_at")
    private LocalDateTime pickedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "max_retry", nullable = false)
    private int maxRetry = 3;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "dedupe_key", length = 200)
    private String dedupeKey;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public Notification(
        Long id,
        Long receiverMemberId,
        NotificationType notificationType,
        String title,
        String content
    ) {
        super(id);
        this.receiverMemberId = receiverMemberId;
        this.notificationType = notificationType;
        this.title = title;
        this.content = content;
        this.scheduledAt = LocalDateTime.now();
    }

    public Notification(
        Long id,
        Long receiverMemberId,
        NotificationType notificationType,
        String title,
        String content,
        String imageKey,
        String referenceType,
        Long referenceId,
        String link,
        LocalDateTime scheduledAt,
        int maxRetry,
        String dedupeKey
    ) {
        this(id, receiverMemberId, notificationType, title, content);
        this.imageKey = imageKey;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.link = link;
        this.scheduledAt = scheduledAt;
        this.maxRetry = maxRetry;
        this.dedupeKey = dedupeKey;
    }

    public Notification(
        Long id,
        Long receiverMemberId,
        NotificationType notificationType,
        String title,
        String content,
        String imageKey,
        String referenceType,
        Long referenceId,
        LocalDateTime scheduledAt,
        int maxRetry,
        String dedupeKey
    ) {
        this(
            id,
            receiverMemberId,
            notificationType,
            title,
            content,
            imageKey,
            referenceType,
            referenceId,
            null,
            scheduledAt,
            maxRetry,
            dedupeKey
        );
    }

    public void markAsRead(LocalDateTime readAt) {
        if (notificationStatus == NotificationStatus.READ) {
            return;
        }
        this.notificationStatus = NotificationStatus.READ;
        this.readAt = readAt;
    }

    public boolean isRead() {
        return readAt != null || notificationStatus == NotificationStatus.READ;
    }

    public void markProcessing(String pickedBy, LocalDateTime pickedAt) {
        this.deliveryStatus = NotificationDeliveryStatus.PROCESSING;
        this.pickedBy = pickedBy;
        this.pickedAt = pickedAt;
        this.lastError = null;
    }

    public void markSent(LocalDateTime sentAt) {
        this.deliveryStatus = NotificationDeliveryStatus.SENT;
        this.sentAt = sentAt;
        this.nextRetryAt = null;
        this.lastError = null;
    }

    public void reschedule(LocalDateTime nextRetryAt, String lastError) {
        this.deliveryStatus = NotificationDeliveryStatus.PENDING;
        this.retryCount++;
        this.nextRetryAt = nextRetryAt;
        this.lastError = truncate(lastError);
    }

    public void markFailed(String lastError) {
        this.deliveryStatus = NotificationDeliveryStatus.FAILED;
        this.lastError = truncate(lastError);
    }

    public boolean canRetry() {
        return retryCount < maxRetry;
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 1000) {
            return value;
        }
        return value.substring(0, 1000);
    }
}
