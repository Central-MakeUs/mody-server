package cmc.mody.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
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
    @DisplayName("다른 기기가 쓰던 같은 토큰을 지우고 회원 디바이스 토큰을 upsert한다.")
    void registerPushToken() {
        NotificationPushTokenService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(idGenerator.nextId()).willReturn(10L);

        service.register(1L, new RegisterPushTokenCommand("ios-device", PushPlatform.IOS, "fcm-token"));

        InOrder inOrder = inOrder(memberPushTokenRepository);
        inOrder.verify(memberPushTokenRepository).deleteOtherDevicesUsingToken("fcm-token", 1L, "ios-device");
        inOrder.verify(memberPushTokenRepository).upsert(
            eq(10L), eq(1L), eq("ios-device"), eq("IOS"), eq("fcm-token"), any(LocalDateTime.class)
        );
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
    @DisplayName("회원의 모든 푸시 토큰을 비활성화한다.")
    void disableAllPushTokens() {
        NotificationPushTokenService service = service();
        MemberPushToken iosToken = new MemberPushToken(
            10L,
            1L,
            "ios-device",
            PushPlatform.IOS,
            "ios-token",
            LocalDateTime.of(2026, 7, 4, 10, 0)
        );
        MemberPushToken androidToken = new MemberPushToken(
            11L,
            1L,
            "android-device",
            PushPlatform.ANDROID,
            "android-token",
            LocalDateTime.of(2026, 7, 4, 10, 0)
        );
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(memberPushTokenRepository.findByMemberIdAndDeletedAtIsNull(1L))
            .willReturn(List.of(iosToken, androidToken));

        service.disableAll(1L);

        assertThat(iosToken.isEnabled()).isFalse();
        assertThat(androidToken.isEnabled()).isFalse();
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
