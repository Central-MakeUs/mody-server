package cmc.mody.auth.infrastructure.oauth;

import cmc.mody.auth.infrastructure.oauth.dto.KakaoOAuthTokenResponse;
import cmc.mody.auth.infrastructure.oauth.dto.KakaoUserResponse;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KakaoOAuthClient extends OAuthProviderClient {
    private final OAuthProperties.Provider properties;

    public KakaoOAuthClient(RestClient.Builder restClientBuilder, OAuthProperties properties) {
        super(restClientBuilder);
        this.properties = properties.getKakao();
    }

    public String getRedirectUrl() {
        return buildRedirectUrl(properties);
    }

    public KakaoOAuthTokenResponse requestAccessToken(String code) {
        return exchangeAuthorizationCode(properties, Map.of("code", code), KakaoOAuthTokenResponse.class);
    }

    public KakaoUserResponse requestUserInfo(String accessToken) {
        validateToken(accessToken);
        return getUserInfo(properties.userInfoUri(), accessToken, KakaoUserResponse.class);
    }
}
