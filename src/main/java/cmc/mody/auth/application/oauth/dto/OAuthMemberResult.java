package cmc.mody.auth.application.oauth.dto;

public record OAuthMemberResult(
    Long memberId,
    boolean personalInfoCompleted
) {
}
