package cmc.mody.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자 공지 푸시 발송 이력. 저장은 {@code AdminAnnouncementService}가 JDBC로 하며, 엔티티는 스키마 검증과 테스트용이다.
 */
@Getter
@Entity
@Table(name = "admin_announcement",
    uniqueConstraints = @UniqueConstraint(name = "uk_admin_announcement_idempotency", columnNames = "idempotency_key"),
    indexes = @Index(name = "idx_admin_announcement_created_id", columnList = "created_at, id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAnnouncement {
    @Id
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType;

    @Column(name = "target_detail", length = 2000)
    private String targetDetail;

    @Column(name = "recipient_count", nullable = false)
    private int recipientCount;

    @Column(name = "token_count", nullable = false)
    private int tokenCount;

    @Column(name = "invalid_token_count", nullable = false)
    private int invalidTokenCount;

    @Column(name = "send_status", nullable = false, length = 20)
    private String sendStatus;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @Column(name = "created_by", nullable = false, length = 320)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
