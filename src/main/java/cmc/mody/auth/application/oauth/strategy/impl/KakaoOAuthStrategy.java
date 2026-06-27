package cmc.mody.auth.application.oauth.strategy.impl;

import cmc.mody.auth.application.oauth.dto.OAuthProfile;
import cmc.mody.auth.application.oauth.strategy.OAuthStrategy;
import cmc.mody.auth.infrastructure.oauth.KakaoOAuthClient;
import cmc.mody.auth.infrastructure.oauth.dto.KakaoOAuthTokenResponse;
import cmc.mody.auth.infrastructure.oauth.dto.KakaoUserResponse;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.member.domain.LoginType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KakaoOAuthStrategy implements OAuthStrategy {
    private final KakaoOAuthClient kakaoOAuthClient;

    @Override
    public LoginType getType() {
        return LoginType.KAKAO;
    }

    @Override
    public String getRedirectUrl() {
        return kakaoOAuthClient.getRedirectUrl();
    }

    @Override
    public OAuthProfile getProfileByAuthorizationCode(String code) {
        KakaoOAuthTokenResponse token = kakaoOAuthClient.requestAccessToken(code);
        return getProfileByProviderToken(token.accessToken());
    }

    @Override
    public OAuthProfile getProfileByProviderToken(String providerToken) {
        KakaoUserResponse user = kakaoOAuthClient.requestUserInfo(providerToken);
        if (user == null || user.id() == null) {
            throw new GeneralException(ErrorStatus.INVALID_OAUTH_PROFILE);
        }
        return new OAuthProfile(
            getType(),
            String.valueOf(user.id()),
            user.account() == null ? null : user.account().email(),
            user.properties() == null ? null : user.properties().nickname(),
            user.properties() == null ? null : user.properties().profileImage()
        );
    }
}
