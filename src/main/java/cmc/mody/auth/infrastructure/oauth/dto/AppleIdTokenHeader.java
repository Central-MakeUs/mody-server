package cmc.mody.auth.infrastructure.oauth.dto;

public record AppleIdTokenHeader(
    String kid,
    String alg
) {
}
