package cmc.mody.notification.infrastructure.repository;

import cmc.mody.notification.domain.MemberPushToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberPushTokenRepository extends JpaRepository<MemberPushToken, Long> {
    List<MemberPushToken> findByMemberIdAndDeletedAtIsNull(Long memberId);

    List<MemberPushToken> findByMemberIdAndEnabledTrueAndDeletedAtIsNull(Long memberId);

    Optional<MemberPushToken> findByMemberIdAndDeviceIdAndDeletedAtIsNull(Long memberId, String deviceId);

    Optional<MemberPushToken> findByFcmTokenAndDeletedAtIsNull(String fcmToken);

    List<MemberPushToken> findByFcmTokenInAndDeletedAtIsNull(List<String> fcmTokens);
}
