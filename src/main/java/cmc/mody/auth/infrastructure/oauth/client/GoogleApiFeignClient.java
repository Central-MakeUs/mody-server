package cmc.mody.auth.infrastructure.oauth.client;

import cmc.mody.auth.infrastructure.oauth.dto.GoogleUserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "googleApiFeignClient", url = "${oauth.google.api-base-uri:https://www.googleapis.com}")
public interface GoogleApiFeignClient {
    @GetMapping("/oauth2/v2/userinfo")
    GoogleUserResponse requestUserInfo(
        @RequestHeader("Authorization") String authorization
    );
}
