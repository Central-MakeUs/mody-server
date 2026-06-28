package cmc.mody.auth.infrastructure.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserResponse(
    Long id,
    Properties properties,
    @JsonProperty("kakao_account")
    Account account
) {
    public record Properties(
        String nickname,
        @JsonProperty("profile_image")
        String profileImage
    ) {
    }

    public record Account(
        String email
    ) {
    }
}
