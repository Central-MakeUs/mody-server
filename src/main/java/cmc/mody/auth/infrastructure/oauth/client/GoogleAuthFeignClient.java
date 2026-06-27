package cmc.mody.auth.infrastructure.oauth.client;

import cmc.mody.auth.infrastructure.oauth.dto.GoogleOAuthTokenResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "googleAuthFeignClient", url = "${oauth.google.auth-base-uri:https://oauth2.googleapis.com}")
public interface GoogleAuthFeignClient {
    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    GoogleOAuthTokenResponse requestAccessToken(
        @RequestParam("code") String code,
        @RequestParam("client_id") String clientId,
        @RequestParam("client_secret") String clientSecret,
        @RequestParam("redirect_uri") String redirectUri,
        @RequestParam("grant_type") String grantType
    );
}
