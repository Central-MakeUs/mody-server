package cmc.mody.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자 변경 요청 이력. 저장은 {@code AdminAuditService}가 JDBC로 하며, 엔티티는 스키마 검증과 테스트용이다.
 */
@Getter
@Entity
@Table(name = "admin_audit_log", indexes = @Index(name = "idx_admin_audit_log_created_id", columnList = "created_at, id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAuditLog {
    @Id
    private Long id;

    @Column(name = "actor_admin_id")
    private Long actorAdminId;

    @Column(name = "actor_label", nullable = false, length = 320)
    private String actorLabel;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "request_path", nullable = false, length = 500)
    private String requestPath;

    @Column(name = "status_code", nullable = false)
    private int statusCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
