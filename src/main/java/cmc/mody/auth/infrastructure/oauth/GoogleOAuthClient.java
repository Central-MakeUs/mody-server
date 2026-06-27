package cmc.mody.auth.infrastructure.oauth;

import cmc.mody.auth.infrastructure.oauth.dto.GoogleOAuthTokenResponse;
import cmc.mody.auth.infrastructure.oauth.dto.GoogleUserResponse;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GoogleOAuthClient extends OAuthProviderClient {
    private final OAuthProperties.Provider properties;

    public GoogleOAuthClient(RestClient.Builder restClientBuilder, OAuthProperties properties) {
        super(restClientBuilder);
        this.properties = properties.getGoogle();
    }

    public String getRedirectUrl() {
        return buildRedirectUrl(properties);
    }

    public GoogleOAuthTokenResponse requestAccessToken(String code) {
        return exchangeAuthorizationCode(properties, Map.of("code", code), GoogleOAuthTokenResponse.class);
    }

    public GoogleUserResponse requestUserInfo(String accessToken) {
        validateToken(accessToken);
        return getUserInfo(properties.userInfoUri(), accessToken, GoogleUserResponse.class);
    }
}
