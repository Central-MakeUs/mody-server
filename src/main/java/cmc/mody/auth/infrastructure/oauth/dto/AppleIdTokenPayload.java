package cmc.mody.auth.infrastructure.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AppleIdTokenPayload(
    String sub,
    String email,
    @JsonProperty("email_verified")
    String emailVerified,
    String iss,
    String aud,
    Long iat,
    Long exp
) {
}
