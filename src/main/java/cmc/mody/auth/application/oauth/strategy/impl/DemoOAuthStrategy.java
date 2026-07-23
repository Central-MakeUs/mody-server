package cmc.mody.auth.application.oauth.strategy.impl;

import cmc.mody.auth.application.oauth.DemoLoginProperties;
import cmc.mody.auth.application.oauth.dto.OAuthProfile;
import cmc.mody.auth.application.oauth.strategy.OAuthStrategy;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.member.domain.LoginType;

public abstract class DemoOAuthStrategy implements OAuthStrategy {
    private final DemoLoginProperties properties;
    private final LoginType loginType;

    protected DemoOAuthStrategy(DemoLoginProperties properties, LoginType loginType) {
        this.properties = properties;
        this.loginType = loginType;
    }

    @Override
    public LoginType getType() {
        return loginType;
    }

    @Override
    public String getRedirectUrl() {
        throw new GeneralException(ErrorStatus.UNSUPPORTED_LOGIN_TYPE);
    }

    @Override
    public OAuthProfile getProfileByAuthorizationCode(String code) {
        throw new GeneralException(ErrorStatus.UNSUPPORTED_LOGIN_TYPE);
    }

    @Override
    public OAuthProfile getProfileByProviderToken(String providerToken) {
        if (!properties.isEnabled()) {
            throw new GeneralException(ErrorStatus.DEMO_LOGIN_DISABLED);
        }
        String providerUserId = properties.providerUserId(loginType);
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new GeneralException(ErrorStatus.INVALID_OAUTH_PROFILE);
        }
        return new OAuthProfile(
            loginType,
            providerUserId,
            null,
            properties.nickname(loginType),
            null
        );
    }
}
