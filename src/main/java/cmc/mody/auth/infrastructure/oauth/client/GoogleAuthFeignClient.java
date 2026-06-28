package cmc.mody.auth.infrastructure.oauth.client;

import cmc.mody.auth.infrastructure.oauth.dto.GoogleOAuthTokenResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.util.MultiValueMap;

@FeignClient(name = "googleAuthFeignClient", url = "${oauth.google.auth-base-uri:https://oauth2.googleapis.com}")
public interface GoogleAuthFeignClient {
    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    GoogleOAuthTokenResponse requestAccessToken(@RequestBody MultiValueMap<String, String> request);
}
