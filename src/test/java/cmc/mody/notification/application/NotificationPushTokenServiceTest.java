package cmc.mody.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.id.IdGenerator;
import cmc.mody.member.domain.Member;
import cmc.mody.member.infrastructure.repository.MemberRepository;
import cmc.mody.notification.application.NotificationPushTokenService.DisablePushTokenCommand;
import cmc.mody.notification.application.NotificationPushTokenService.RegisterPushTokenCommand;
import cmc.mody.notification.domain.MemberPushToken;
import cmc.mody.notification.domain.PushPlatform;
import cmc.mody.notification.infrastructure.repository.MemberPushTokenRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationPushTokenServiceTest {
    @Mock
    private IdGenerator idGenerator;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberPushTokenRepository memberPushTokenRepository;

    @Test
    @DisplayName("회원 디바이스의 푸시 토큰을 새로 등록한다.")
    void registerPushToken() {
        NotificationPushTokenService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(memberPushTokenRepository.findByFcmTokenAndDeletedAtIsNull("fcm-token")).willReturn(Optional.empty());
        given(memberPushTokenRepository.findByMemberIdAndDeviceIdAndDeletedAtIsNull(1L, "ios-device"))
            .willReturn(Optional.empty());
        given(idGenerator.nextId()).willReturn(10L);

        service.register(1L, new RegisterPushTokenCommand("ios-device", PushPlatform.IOS, "fcm-token"));

        ArgumentCaptor<MemberPushToken> captor = ArgumentCaptor.forClass(MemberPushToken.class);
        verify(memberPushTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(10L);
        assertThat(captor.getValue().getMemberId()).isEqualTo(1L);
        assertThat(captor.getValue().getDeviceId()).isEqualTo("ios-device");
        assertThat(captor.getValue().getPlatform()).isEqualTo(PushPlatform.IOS);
        assertThat(captor.getValue().getFcmToken()).isEqualTo("fcm-token");
        assertThat(captor.getValue().isEnabled()).isTrue();
        assertThat(captor.getValue().getLastSeenAt()).isNotNull();
    }

    @Test
    @DisplayName("기존 디바이스 토큰은 새 FCM 토큰으로 갱신한다.")
    void updatePushToken() {
        NotificationPushTokenService service = service();
        MemberPushToken pushToken = new MemberPushToken(
            10L,
            1L,
            "ios-device",
            PushPlatform.IOS,
            "old-token",
            LocalDateTime.of(2026, 7, 4, 10, 0)
        );
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(memberPushTokenRepository.findByFcmTokenAndDeletedAtIsNull("new-token")).willReturn(Optional.empty());
        given(memberPushTokenRepository.findByMemberIdAndDeviceIdAndDeletedAtIsNull(1L, "ios-device"))
            .willReturn(Optional.of(pushToken));

        service.register(1L, new RegisterPushTokenCommand("ios-device", PushPlatform.IOS, "new-token"));

        assertThat(pushToken.getFcmToken()).isEqualTo("new-token");
        assertThat(pushToken.isEnabled()).isTrue();
        verify(memberPushTokenRepository).save(pushToken);
    }

    @Test
    @DisplayName("동일 FCM 토큰이 다른 디바이스에 있으면 이전 토큰을 비활성화한다.")
    void disableDuplicatedFcmToken() {
        NotificationPushTokenService service = service();
        MemberPushToken duplicated = new MemberPushToken(
            9L,
            2L,
            "android-device",
            PushPlatform.ANDROID,
            "fcm-token",
            LocalDateTime.of(2026, 7, 4, 10, 0)
        );
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(memberPushTokenRepository.findByFcmTokenAndDeletedAtIsNull("fcm-token"))
            .willReturn(Optional.of(duplicated));
        given(memberPushTokenRepository.findByMemberIdAndDeviceIdAndDeletedAtIsNull(1L, "ios-device"))
            .willReturn(Optional.empty());
        given(idGenerator.nextId()).willReturn(10L);

        service.register(1L, new RegisterPushTokenCommand("ios-device", PushPlatform.IOS, "fcm-token"));

        assertThat(duplicated.isEnabled()).isFalse();
        verify(memberPushTokenRepository).save(any(MemberPushToken.class));
    }

    @Test
    @DisplayName("푸시 토큰을 비활성화한다.")
    void disablePushToken() {
        NotificationPushTokenService service = service();
        MemberPushToken pushToken = new MemberPushToken(
            10L,
            1L,
            "ios-device",
            PushPlatform.IOS,
            "fcm-token",
            LocalDateTime.of(2026, 7, 4, 10, 0)
        );
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(memberPushTokenRepository.findByMemberIdAndDeviceIdAndDeletedAtIsNull(1L, "ios-device"))
            .willReturn(Optional.of(pushToken));

        service.disable(1L, new DisablePushTokenCommand("ios-device"));

        assertThat(pushToken.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("회원이 없으면 푸시 토큰을 등록할 수 없다.")
    void registerPushTokenMemberNotFound() {
        NotificationPushTokenService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
            service.register(1L, new RegisterPushTokenCommand("ios-device", PushPlatform.IOS, "fcm-token")))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getStatus()).isEqualTo(ErrorStatus.MEMBER_NOT_FOUND));
    }

    private NotificationPushTokenService service() {
        return new NotificationPushTokenService(idGenerator, memberRepository, memberPushTokenRepository);
    }

    private Member member() {
        return new Member(1L, "민석", LocalDate.of(2000, 1, 1), BigDecimal.valueOf(68.0));
    }
}
