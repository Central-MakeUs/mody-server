package cmc.mody.auth.infrastructure.oauth;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import feign.FeignException;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
public abstract class OAuthProviderClient {
    private static final String BEARER_PREFIX = "Bearer ";

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

    protected <T> T executeFeign(Supplier<T> supplier, ErrorStatus status) {
        try {
            return supplier.get();
        } catch (FeignException e) {
            log.warn(
                "OAuth provider request failed. status={}, responseBody={}",
                e.status(),
                sanitizeResponseBody(e.contentUTF8())
            );
            throw new GeneralException(status);
        }
    }

    protected String bearer(String accessToken) {
        return BEARER_PREFIX + accessToken;
    }

    protected void validateToken(String providerToken) {
        if (providerToken == null || providerToken.isBlank()) {
            throw new GeneralException(ErrorStatus.INVALID_OAUTH_TOKEN);
        }
    }

    private String sanitizeResponseBody(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "";
        }
        return responseBody.replaceAll("[\\r\\n\\t]+", " ");
    }
}
