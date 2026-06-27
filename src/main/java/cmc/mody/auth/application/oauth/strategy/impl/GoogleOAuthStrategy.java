package cmc.mody.auth.application.oauth.strategy.impl;

import cmc.mody.auth.application.oauth.dto.OAuthProfile;
import cmc.mody.auth.application.oauth.strategy.OAuthStrategy;
import cmc.mody.auth.infrastructure.oauth.GoogleOAuthClient;
import cmc.mody.auth.infrastructure.oauth.dto.GoogleOAuthTokenResponse;
import cmc.mody.auth.infrastructure.oauth.dto.GoogleUserResponse;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.member.domain.LoginType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GoogleOAuthStrategy implements OAuthStrategy {
    private final GoogleOAuthClient googleOAuthClient;

    @Override
    public LoginType getType() {
        return LoginType.GOOGLE;
    }

    @Override
    public String getRedirectUrl() {
        return googleOAuthClient.getRedirectUrl();
    }

    @Override
    public OAuthProfile getProfileByAuthorizationCode(String code) {
        GoogleOAuthTokenResponse token = googleOAuthClient.requestAccessToken(code);
        return getProfileByProviderToken(token.accessToken());
    }

    @Override
    public OAuthProfile getProfileByProviderToken(String providerToken) {
        GoogleUserResponse user = googleOAuthClient.requestUserInfo(providerToken);
        if (user == null || user.id() == null || user.id().isBlank()) {
            throw new GeneralException(ErrorStatus.INVALID_OAUTH_PROFILE);
        }
        return new OAuthProfile(getType(), user.id(), user.email(), user.name(), user.picture());
    }
}
