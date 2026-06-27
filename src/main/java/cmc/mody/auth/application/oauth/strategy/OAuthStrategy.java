package cmc.mody.auth.application.oauth.strategy;

import cmc.mody.auth.application.oauth.dto.OAuthProfile;
import cmc.mody.member.domain.LoginType;

public interface OAuthStrategy {
    LoginType getType();

    String getRedirectUrl();

    OAuthProfile getProfileByAuthorizationCode(String code);

    OAuthProfile getProfileByProviderToken(String providerToken);
}
