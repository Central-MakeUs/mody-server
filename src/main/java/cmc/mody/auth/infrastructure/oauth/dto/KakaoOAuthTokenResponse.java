package cmc.mody.auth.infrastructure.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoOAuthTokenResponse(
    @JsonProperty("access_token")
    String accessToken,
    @JsonProperty("token_type")
    String tokenType,
    @JsonProperty("refresh_token")
    String refreshToken,
    @JsonProperty("expires_in")
    Integer expiresIn
) {
}
