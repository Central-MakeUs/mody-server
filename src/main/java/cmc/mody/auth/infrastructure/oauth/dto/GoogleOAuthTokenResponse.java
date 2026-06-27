package cmc.mody.auth.infrastructure.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleOAuthTokenResponse(
    @JsonProperty("access_token")
    String accessToken,
    @JsonProperty("expires_in")
    Integer expiresIn,
    String scope,
    @JsonProperty("token_type")
    String tokenType,
    @JsonProperty("id_token")
    String idToken
) {
}
