package cmc.mody.notification.application;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.id.IdGenerator;
import cmc.mody.member.domain.Member;
import cmc.mody.member.infrastructure.repository.MemberRepository;
import cmc.mody.notification.domain.MemberPushToken;
import cmc.mody.notification.domain.PushPlatform;
import cmc.mody.notification.infrastructure.repository.MemberPushTokenRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationPushTokenService {
    private final IdGenerator idGenerator;
    private final MemberRepository memberRepository;
    private final MemberPushTokenRepository memberPushTokenRepository;

    @Transactional
    public void register(Long memberId, RegisterPushTokenCommand command) {
        getMember(memberId);
        LocalDateTime now = LocalDateTime.now();

        memberPushTokenRepository.findByFcmTokenAndDeletedAtIsNull(command.fcmToken())
            .filter(found -> !found.belongsTo(memberId, command.deviceId()))
            .ifPresent(MemberPushToken::disable);

        MemberPushToken memberPushToken = memberPushTokenRepository
            .findByMemberIdAndDeviceIdAndDeletedAtIsNull(memberId, command.deviceId())
            .orElseGet(() -> new MemberPushToken(
                idGenerator.nextId(),
                memberId,
                command.deviceId(),
                command.platform(),
                command.fcmToken(),
                now
            ));

        memberPushToken.updateToken(command.fcmToken(), command.platform(), now);
        memberPushTokenRepository.save(memberPushToken);
    }

    @Transactional
    public void disable(Long memberId, DisablePushTokenCommand command) {
        getMember(memberId);
        memberPushTokenRepository.findByMemberIdAndDeviceIdAndDeletedAtIsNull(memberId, command.deviceId())
            .ifPresent(MemberPushToken::disable);
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
            .filter(Member::isActive)
            .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
    }

    public record RegisterPushTokenCommand(
        String deviceId,
        PushPlatform platform,
        String fcmToken
    ) {
    }

    public record DisablePushTokenCommand(String deviceId) {
    }
}
