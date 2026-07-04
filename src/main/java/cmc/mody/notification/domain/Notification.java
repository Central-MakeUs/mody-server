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
                @Index(name = "idx_notification_receiver_status", columnList = "receiver_member_id, notification_status"),
                @Index(name = "idx_notification_receiver_created", columnList = "receiver_member_id, created_at")
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

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public Notification(Long id, Long receiverMemberId, NotificationType notificationType, String title, String content) {
        super(id);
        this.receiverMemberId = receiverMemberId;
        this.notificationType = notificationType;
        this.title = title;
        this.content = content;
    }

    public void markAsRead(LocalDateTime readAt) {
        if (notificationStatus == NotificationStatus.READ) {
            return;
        }
        this.notificationStatus = NotificationStatus.READ;
        this.readAt = readAt;
    }

    public boolean isRead() {
        return notificationStatus == NotificationStatus.READ;
    }
}
