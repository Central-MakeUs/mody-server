package cmc.mody.auth.infrastructure.oauth.dto;

import java.util.List;

public record ApplePublicKeyResponse(
    List<ApplePublicKey> keys
) {
}
