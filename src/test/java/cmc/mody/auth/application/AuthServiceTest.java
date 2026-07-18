package cmc.mody.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import cmc.mody.auth.application.oauth.RefreshTokenService;
import cmc.mody.auth.application.token.TokenProvider;
import cmc.mody.auth.presentation.dto.TokenDto;
import cmc.mody.notification.application.NotificationPushTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private NotificationPushTokenService notificationPushTokenService;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(new NoOpTransactionManager());
    }

    @Test
    @DisplayName("refresh token을 검증하고 새 토큰으로 교체한다.")
    void reissue() {
        AuthService service = service();
        given(tokenProvider.getMemberIdByRefreshToken("refresh")).willReturn(1L);
        given(tokenProvider.createToken(1L)).willReturn(TokenDto.of(1L, "new-access", "new-refresh"));

        TokenDto result = service.reissue("refresh");

        assertThat(result.accessToken()).isEqualTo("new-access");
        assertThat(result.refreshToken()).isEqualTo("new-refresh");
        then(refreshTokenService).should().validate(1L, "refresh");
        then(refreshTokenService).should().replace(1L, "new-refresh");
    }

    @Test
    @DisplayName("토큰 재발급 중 DB deadlock이 발생하면 한 번 재시도한다.")
    void reissueWithDeadlockRetry() {
        AuthService service = service();
        given(tokenProvider.getMemberIdByRefreshToken("refresh")).willReturn(1L);
        given(tokenProvider.createToken(1L)).willReturn(TokenDto.of(1L, "new-access", "new-refresh"));
        org.mockito.BDDMockito.willThrow(new CannotAcquireLockException("deadlock"))
            .willDoNothing()
            .given(refreshTokenService)
            .replace(1L, "new-refresh");

        TokenDto result = service.reissue("refresh");

        assertThat(result.refreshToken()).isEqualTo("new-refresh");
        then(refreshTokenService).should(times(2)).validate(1L, "refresh");
        then(refreshTokenService).should(times(2)).replace(1L, "new-refresh");
    }

    @Test
    @DisplayName("로그아웃 시 refresh token을 비활성화한다.")
    void logout() {
        AuthService service = service();
        given(tokenProvider.getMemberIdByRefreshToken("refresh")).willReturn(1L);

        service.logout("refresh");

        then(refreshTokenService).should().delete(1L, "refresh");
        then(notificationPushTokenService).should().disableAll(1L);
    }

    private AuthService service() {
        return new AuthService(tokenProvider, refreshTokenService, notificationPushTokenService, transactionTemplate);
    }

    private static class NoOpTransactionManager implements PlatformTransactionManager {
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    }
}
