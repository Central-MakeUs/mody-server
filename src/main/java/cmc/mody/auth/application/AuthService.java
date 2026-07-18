package cmc.mody.auth.application;

import cmc.mody.auth.application.oauth.RefreshTokenService;
import cmc.mody.auth.application.token.TokenProvider;
import cmc.mody.auth.presentation.dto.TokenDto;
import cmc.mody.notification.application.NotificationPushTokenService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final int DEADLOCK_RETRY_MAX_ATTEMPTS = 2;

    private final TokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final NotificationPushTokenService notificationPushTokenService;
    private final TransactionTemplate transactionTemplate;

    public TokenDto reissue(String refreshToken) {
        return Objects.requireNonNull(executeWithDeadlockRetry(() -> transactionTemplate.execute(status -> {
            Long memberId = tokenProvider.getMemberIdByRefreshToken(refreshToken);
            refreshTokenService.validate(memberId, refreshToken);

            TokenDto token = tokenProvider.createToken(memberId);
            refreshTokenService.replace(memberId, token.refreshToken());
            return token;
        })));
    }

    public void logout(String refreshToken) {
        executeWithDeadlockRetry(() -> {
            transactionTemplate.executeWithoutResult(status -> {
                Long memberId = tokenProvider.getMemberIdByRefreshToken(refreshToken);
                refreshTokenService.delete(memberId, refreshToken);
                notificationPushTokenService.disableAll(memberId);
            });
            return null;
        });
    }

    private <T> T executeWithDeadlockRetry(TransactionCallback<T> callback) {
        CannotAcquireLockException lastException = null;
        for (int attempt = 1; attempt <= DEADLOCK_RETRY_MAX_ATTEMPTS; attempt++) {
            try {
                return callback.doInTransaction();
            } catch (CannotAcquireLockException exception) {
                lastException = exception;
                if (attempt == DEADLOCK_RETRY_MAX_ATTEMPTS) {
                    throw exception;
                }
            }
        }
        throw lastException;
    }

    @FunctionalInterface
    private interface TransactionCallback<T> {
        T doInTransaction();
    }
}
