package cmc.mody.auth.infrastructure.oauth;

import cmc.mody.auth.infrastructure.oauth.client.GoogleApiFeignClient;
import cmc.mody.auth.infrastructure.oauth.client.GoogleAuthFeignClient;
import cmc.mody.auth.infrastructure.oauth.dto.GoogleOAuthTokenResponse;
import cmc.mody.auth.infrastructure.oauth.dto.GoogleUserResponse;
import cmc.mody.common.api.status.ErrorStatus;
import org.springframework.stereotype.Component;

@Component
public class GoogleOAuthClient extends OAuthProviderClient {
    private static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";

    private final GoogleAuthFeignClient googleAuthFeignClient;
    private final GoogleApiFeignClient googleApiFeignClient;
    private final OAuthProperties.Provider properties;

    public GoogleOAuthClient(
        GoogleAuthFeignClient googleAuthFeignClient,
        GoogleApiFeignClient googleApiFeignClient,
        OAuthProperties properties
    ) {
        this.googleAuthFeignClient = googleAuthFeignClient;
        this.googleApiFeignClient = googleApiFeignClient;
        this.properties = properties.getGoogle();
    }

    public String getRedirectUrl() {
        return buildRedirectUrl(properties);
    }

    public GoogleOAuthTokenResponse requestAccessToken(String code) {
        return executeFeign(
            () -> googleAuthFeignClient.requestAccessToken(
                code,
                properties.clientId(),
                properties.clientSecret(),
                properties.redirectUri(),
                GRANT_TYPE_AUTHORIZATION_CODE
            ),
            ErrorStatus.INVALID_OAUTH_TOKEN
        );
    }

    public GoogleUserResponse requestUserInfo(String accessToken) {
        validateToken(accessToken);
        return executeFeign(
            () -> googleApiFeignClient.requestUserInfo(bearer(accessToken)),
            ErrorStatus.OAUTH_PROFILE_REQUEST_FAILED
        );
    }
}
