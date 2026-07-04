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
        name = "member_push_token",
        indexes = {
                @Index(name = "idx_member_push_token_member", columnList = "member_id, enabled"),
                @Index(name = "idx_member_push_token_device", columnList = "member_id, device_id")
        }
)
public class MemberPushToken extends BaseEntity {
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PushPlatform platform;

    @Column(name = "fcm_token", nullable = false, length = 500)
    private String fcmToken;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    public MemberPushToken(
        Long id,
        Long memberId,
        String deviceId,
        PushPlatform platform,
        String fcmToken,
        LocalDateTime lastSeenAt
    ) {
        super(id);
        this.memberId = memberId;
        this.deviceId = deviceId;
        this.platform = platform;
        this.fcmToken = fcmToken;
        this.lastSeenAt = lastSeenAt;
    }

    public void updateToken(String fcmToken, PushPlatform platform, LocalDateTime lastSeenAt) {
        this.fcmToken = fcmToken;
        this.platform = platform;
        this.enabled = true;
        this.lastSeenAt = lastSeenAt;
    }

    public void disable() {
        this.enabled = false;
    }

    public boolean belongsTo(Long memberId, String deviceId) {
        return this.memberId.equals(memberId) && this.deviceId.equals(deviceId);
    }
}
