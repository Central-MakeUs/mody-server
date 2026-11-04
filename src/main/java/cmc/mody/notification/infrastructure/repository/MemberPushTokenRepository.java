package cmc.mody.notification.infrastructure.repository;

import cmc.mody.notification.domain.MemberPushToken;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberPushTokenRepository extends JpaRepository<MemberPushToken, Long> {
    List<MemberPushToken> findByMemberIdAndDeletedAtIsNull(Long memberId);

    List<MemberPushToken> findByMemberIdAndEnabledTrueAndDeletedAtIsNull(Long memberId);

    Optional<MemberPushToken> findByMemberIdAndDeviceIdAndDeletedAtIsNull(Long memberId, String deviceId);

    List<MemberPushToken> findByFcmTokenInAndDeletedAtIsNull(List<String> fcmTokens);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        delete from MemberPushToken token
         where token.fcmToken = :fcmToken
           and (token.memberId <> :memberId or token.deviceId <> :deviceId)
        """)
    int deleteOtherDevicesUsingToken(
        @Param("fcmToken") String fcmToken,
        @Param("memberId") Long memberId,
        @Param("deviceId") String deviceId
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
        insert into member_push_token (
            id, member_id, device_id, platform, fcm_token, enabled, last_seen_at, status, created_at, updated_at
        ) values (
            :id, :memberId, :deviceId, :platform, :fcmToken, true, :now, 'ACTIVE', :now, :now
        )
        on duplicate key update
            platform = values(platform),
            fcm_token = values(fcm_token),
            enabled = true,
            last_seen_at = values(last_seen_at),
            updated_at = values(updated_at)
        """, nativeQuery = true)
    int upsert(
        @Param("id") Long id,
        @Param("memberId") Long memberId,
        @Param("deviceId") String deviceId,
        @Param("platform") String platform,
        @Param("fcmToken") String fcmToken,
        @Param("now") LocalDateTime now
    );
}
