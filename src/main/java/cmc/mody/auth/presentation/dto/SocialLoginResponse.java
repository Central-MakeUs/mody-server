package cmc.mody.auth.presentation.dto;

public record SocialLoginResponse(
    Long id,
    String accessToken,
    String refreshToken,
    boolean personalInfoCompleted,
    boolean mainAccessible
) {
    public static SocialLoginResponse from(TokenDto token) {
        return new SocialLoginResponse(
            token.id(),
            token.accessToken(),
            token.refreshToken(),
            token.personalInfoCompleted(),
            token.mainAccessible()
        );
    }
}
