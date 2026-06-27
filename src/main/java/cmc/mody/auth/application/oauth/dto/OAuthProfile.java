package cmc.mody.auth.application.oauth.dto;

import cmc.mody.member.domain.LoginType;

public record OAuthProfile(
    LoginType loginType,
    String providerUserId,
    String email,
    String nickname,
    String profileImageUrl
) {
}
