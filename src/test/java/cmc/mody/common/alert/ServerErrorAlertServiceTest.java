package cmc.mody.common.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import cmc.mody.auth.application.token.TokenProvider;
import cmc.mody.member.domain.Member;
import cmc.mody.member.infrastructure.repository.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServerErrorAlertServiceTest {
    @Mock
    private ServerErrorAlertSender serverErrorAlertSender;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private HttpServletRequest request;

    @Captor
    private ArgumentCaptor<String> textCaptor;

    @Test
    void sendSlackAlertWithMemberContext() {
        ServerErrorAlertService service = service(enabledProperties());
        given(request.getHeader("Authorization")).willReturn("Bearer access-token");
        given(request.getHeader("User-Agent")).willReturn("Mody/1.0.0");
        given(request.getHeader("X-Forwarded-For")).willReturn("203.0.113.10, 10.0.0.1");
        given(request.getMethod()).willReturn("POST");
        given(request.getRequestURI()).willReturn("/api/v1/records");
        given(request.getQueryString()).willReturn("groupId=10&code=secret-code");
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(memberRepository.findById(1L))
            .willReturn(Optional.of(new Member(1L, "민석", LocalDate.of(2000, 1, 1), BigDecimal.valueOf(68))));

        service.notify(new IllegalStateException("boom"), request, 500, "COMMON500");

        then(serverErrorAlertSender).should().send(org.mockito.ArgumentMatchers.eq("https://hooks.slack.test/error"),
            textCaptor.capture());
        String text = textCaptor.getValue();
        assertThat(text).contains("1 / 민석");
        assertThat(text).contains("POST /api/v1/records?groupId=10&code=REDACTED");
        assertThat(text).contains("203.0.113.10");
        assertThat(text).contains("java.lang.IllegalStateException");
        assertThat(text).doesNotContain("access-token");
        assertThat(text).doesNotContain("secret-code");
    }

    @Test
    void skipWhenDisabled() {
        ServerErrorAlertProperties properties = enabledProperties();
        properties.setEnabled(false);
        ServerErrorAlertService service = service(properties);

        service.notify(new RuntimeException("boom"), request, 500, "COMMON500");

        then(serverErrorAlertSender).should(never()).send(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void ignoreAlertBuildFailure() {
        ServerErrorAlertService service = service(enabledProperties());
        given(request.getHeader("Authorization")).willReturn(null);
        willThrow(new RuntimeException("request closed")).given(request).getMethod();

        assertThatCode(() -> service.notify(new RuntimeException("boom"), request, 500, "COMMON500"))
            .doesNotThrowAnyException();
    }

    private ServerErrorAlertService service(ServerErrorAlertProperties properties) {
        return new ServerErrorAlertService(properties, serverErrorAlertSender, tokenProvider, memberRepository);
    }

    private ServerErrorAlertProperties enabledProperties() {
        ServerErrorAlertProperties properties = new ServerErrorAlertProperties();
        properties.setEnabled(true);
        properties.setWebhookUrl("https://hooks.slack.test/error");
        properties.setMaxMessageLength(3500);
        properties.setStackTraceDepth(4);
        return properties;
    }
}
