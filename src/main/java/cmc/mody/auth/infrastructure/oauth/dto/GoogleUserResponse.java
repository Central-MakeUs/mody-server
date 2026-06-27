package cmc.mody.auth.infrastructure.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleUserResponse(
    String id,
    String email,
    @JsonProperty("verified_email")
    Boolean verifiedEmail,
    String name,
    String picture
) {
}
