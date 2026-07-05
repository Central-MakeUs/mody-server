package cmc.mody.auth.infrastructure.oauth.client;

import cmc.mody.auth.infrastructure.oauth.dto.AppleOAuthTokenResponse;
import cmc.mody.auth.infrastructure.oauth.dto.ApplePublicKeyResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "appleAuthFeignClient", url = "${oauth.apple.auth-base-uri:https://appleid.apple.com}")
public interface AppleAuthFeignClient {
    @GetMapping("/auth/keys")
    ApplePublicKeyResponse getPublicKeys();

    @PostMapping(value = "/auth/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    AppleOAuthTokenResponse requestAccessToken(
        @RequestParam("client_id") String clientId,
        @RequestParam("client_secret") String clientSecret,
        @RequestParam("code") String code,
        @RequestParam("grant_type") String grantType,
        @RequestParam("redirect_uri") String redirectUri
    );
}
