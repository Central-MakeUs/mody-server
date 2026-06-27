package cmc.mody.auth.infrastructure.oauth;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

public abstract class OAuthProviderClient {
    private static final String BEARER_PREFIX = "Bearer ";

    protected final RestClient restClient;

    protected OAuthProviderClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    protected String buildRedirectUrl(OAuthProperties.Provider provider) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(provider.authorizationUri())
            .queryParam("client_id", provider.clientId())
            .queryParam("redirect_uri", provider.redirectUri())
            .queryParam("response_type", "code");

        if (provider.scope() != null && !provider.scope().isEmpty()) {
            builder.queryParam("scope", String.join(" ", provider.scope()));
        }
        return builder.build().toUriString();
    }

    protected <T> T getUserInfo(String userInfoUri, String accessToken, Class<T> responseType) {
        try {
            return restClient.get()
                .uri(userInfoUri)
                .header("Authorization", BEARER_PREFIX + accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(responseType);
        } catch (RestClientException e) {
            throw new GeneralException(ErrorStatus.OAUTH_PROFILE_REQUEST_FAILED);
        }
    }

    protected <T> T exchangeAuthorizationCode(
        OAuthProperties.Provider provider,
        Map<String, String> additionalParams,
        Class<T> responseType
    ) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", provider.clientId());
        body.add("redirect_uri", provider.redirectUri());
        body.add("code", additionalParams.get("code"));

        if (provider.clientSecret() != null && !provider.clientSecret().isBlank()) {
            body.add("client_secret", provider.clientSecret());
        }
        additionalParams.forEach((key, value) -> {
            if (!"code".equals(key)) {
                body.add(key, value);
            }
        });

        try {
            return restClient.post()
                .uri(provider.tokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(responseType);
        } catch (RestClientException e) {
            throw new GeneralException(ErrorStatus.INVALID_OAUTH_TOKEN);
        }
    }

    protected void validateToken(String providerToken) {
        if (providerToken == null || providerToken.isBlank()) {
            throw new GeneralException(ErrorStatus.INVALID_OAUTH_TOKEN);
        }
    }
}
