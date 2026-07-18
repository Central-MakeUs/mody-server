package cmc.mody.auth.application;

import cmc.mody.auth.application.oauth.RefreshTokenService;
import cmc.mody.auth.application.token.TokenProvider;
import cmc.mody.auth.presentation.dto.TokenDto;
import cmc.mody.notification.application.NotificationPushTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final TokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final NotificationPushTokenService notificationPushTokenService;

    @Transactional
    public TokenDto reissue(String refreshToken) {
        Long memberId = tokenProvider.getMemberIdByRefreshToken(refreshToken);
        refreshTokenService.validate(memberId, refreshToken);

        TokenDto token = tokenProvider.createToken(memberId);
        refreshTokenService.replace(memberId, token.refreshToken());
        return token;
    }

    @Transactional
    public void logout(String refreshToken) {
        Long memberId = tokenProvider.getMemberIdByRefreshToken(refreshToken);
        refreshTokenService.delete(memberId, refreshToken);
        notificationPushTokenService.disableAll(memberId);
    }
}
