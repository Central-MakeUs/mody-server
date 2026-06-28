package cmc.mody.auth.infrastructure.oauth.client;

import cmc.mody.auth.infrastructure.oauth.dto.KakaoOAuthTokenResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "kakaoAuthFeignClient", url = "${oauth.kakao.auth-base-uri:https://kauth.kakao.com}")
public interface KakaoAuthFeignClient {
    @PostMapping(value = "/oauth/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    KakaoOAuthTokenResponse requestAccessToken(
        @RequestParam("grant_type") String grantType,
        @RequestParam("redirect_uri") String redirectUri,
        @RequestParam("client_id") String clientId,
        @RequestParam("code") String code,
        @RequestParam(value = "client_secret", required = false) String clientSecret
    );
}
