package cmc.mody.auth.infrastructure.oauth;

import cmc.mody.auth.infrastructure.oauth.dto.AppleOAuthTokenResponse;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class AppleOAuthClient extends OAuthProviderClient {
    private final OAuthProperties.Provider properties;

    public AppleOAuthClient(RestClient.Builder restClientBuilder, OAuthProperties properties) {
        super(restClientBuilder);
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
        return exchangeAuthorizationCode(properties, Map.of("code", code), AppleOAuthTokenResponse.class);
    }
}
