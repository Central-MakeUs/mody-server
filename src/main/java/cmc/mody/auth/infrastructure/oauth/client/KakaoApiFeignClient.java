package cmc.mody.auth.infrastructure.oauth.client;

import cmc.mody.auth.infrastructure.oauth.dto.KakaoUserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "kakaoApiFeignClient", url = "${oauth.kakao.api-base-uri:https://kapi.kakao.com}")
public interface KakaoApiFeignClient {
    @GetMapping("/v2/user/me")
    KakaoUserResponse requestUserInfo(
        @RequestHeader("Authorization") String authorization
    );
}
