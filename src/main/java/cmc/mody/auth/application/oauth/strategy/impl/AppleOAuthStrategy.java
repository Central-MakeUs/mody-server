package cmc.mody.auth.application.oauth.strategy.impl;

import cmc.mody.auth.application.oauth.dto.OAuthProfile;
import cmc.mody.auth.application.oauth.strategy.OAuthStrategy;
import cmc.mody.auth.infrastructure.oauth.AppleOAuthClient;
import cmc.mody.auth.infrastructure.oauth.dto.AppleIdTokenPayload;
import cmc.mody.auth.infrastructure.oauth.dto.AppleOAuthTokenResponse;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.member.domain.LoginType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppleOAuthStrategy implements OAuthStrategy {
    private static final int JWT_PARTS_LENGTH = 3;
    private static final int PAYLOAD_INDEX = 1;

    private final AppleOAuthClient appleOAuthClient;
    private final ObjectMapper objectMapper;

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
        AppleIdTokenPayload payload = parseIdToken(providerToken);
        if (payload.sub() == null || payload.sub().isBlank()) {
            throw new GeneralException(ErrorStatus.INVALID_OAUTH_PROFILE);
        }
        return new OAuthProfile(getType(), payload.sub(), payload.email(), null, null);
    }

    private AppleIdTokenPayload parseIdToken(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new GeneralException(ErrorStatus.INVALID_OAUTH_TOKEN);
        }
        String[] parts = idToken.split("\\.");
        if (parts.length != JWT_PARTS_LENGTH) {
            throw new GeneralException(ErrorStatus.INVALID_OAUTH_TOKEN);
        }
        try {
            byte[] decodedPayload = Base64.getUrlDecoder().decode(parts[PAYLOAD_INDEX]);
            return objectMapper.readValue(new String(decodedPayload, StandardCharsets.UTF_8), AppleIdTokenPayload.class);
        } catch (IllegalArgumentException | JsonProcessingException e) {
            throw new GeneralException(ErrorStatus.INVALID_OAUTH_TOKEN);
        }
    }
}
