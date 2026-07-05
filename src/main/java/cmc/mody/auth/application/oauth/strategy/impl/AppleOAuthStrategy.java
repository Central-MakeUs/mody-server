package cmc.mody.auth.application.oauth.strategy.impl;

import cmc.mody.auth.application.oauth.dto.OAuthProfile;
import cmc.mody.auth.application.oauth.strategy.OAuthStrategy;
import cmc.mody.auth.infrastructure.oauth.AppleIdentityTokenVerifier;
import cmc.mody.auth.infrastructure.oauth.AppleOAuthClient;
import cmc.mody.auth.infrastructure.oauth.dto.AppleIdTokenPayload;
import cmc.mody.auth.infrastructure.oauth.dto.AppleOAuthTokenResponse;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.member.domain.LoginType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppleOAuthStrategy implements OAuthStrategy {
    private final AppleOAuthClient appleOAuthClient;
    private final AppleIdentityTokenVerifier appleIdentityTokenVerifier;

    @Override
    public LoginType getType() {
        return LoginType.APPLE;
    }

    @Override
    public String getRedirectUrl() {
        return appleOAuthClient.getRedirectUrl();
    }

    @Override
    public OAuthProfile getProfileByAuthorizationCode(String code) {
        AppleOAuthTokenResponse token = appleOAuthClient.requestAccessToken(code);
        return getProfileByProviderToken(token.idToken());
    }

    @Override
    public OAuthProfile getProfileByProviderToken(String providerToken) {
        AppleIdTokenPayload payload = appleIdentityTokenVerifier.verify(providerToken);
        if (payload.sub() == null || payload.sub().isBlank()) {
            throw new GeneralException(ErrorStatus.INVALID_OAUTH_PROFILE);
        }
        return new OAuthProfile(getType(), payload.sub(), payload.email(), null, null);
    }
}
