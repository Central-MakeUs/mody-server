package cmc.mody.auth.infrastructure.oauth;

import cmc.mody.auth.infrastructure.oauth.client.AppleAuthFeignClient;
import cmc.mody.auth.infrastructure.oauth.dto.AppleOAuthTokenResponse;
import cmc.mody.common.api.status.ErrorStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class AppleOAuthClient extends OAuthProviderClient {
    private static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";

    private final AppleAuthFeignClient appleAuthFeignClient;
    private final OAuthProperties.Provider properties;

    public AppleOAuthClient(AppleAuthFeignClient appleAuthFeignClient, OAuthProperties properties) {
        this.appleAuthFeignClient = appleAuthFeignClient;
        this.properties = properties.getApple();
    }

    @Override
    public String buildRedirectUrl(OAuthProperties.Provider provider) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(provider.authorizationUri())
            .queryParam("client_id", provider.clientId())
            .queryParam("redirect_uri", provider.redirectUri())
            .queryParam("response_type", "code id_token")
            .queryParam("response_mode", "form_post");
        if (provider.scope() != null && !provider.scope().isEmpty()) {
            builder.queryParam("scope", String.join(" ", provider.scope()));
        }
        return builder.build().toUriString();
    }

    public String getRedirectUrl() {
        return buildRedirectUrl(properties);
    }

    public AppleOAuthTokenResponse requestAccessToken(String code) {
        return executeFeign(
            () -> appleAuthFeignClient.requestAccessToken(
                properties.clientId(),
                properties.clientSecret(),
                code,
                GRANT_TYPE_AUTHORIZATION_CODE,
                properties.redirectUri()
            ),
            ErrorStatus.INVALID_OAUTH_TOKEN
        );
    }
}
