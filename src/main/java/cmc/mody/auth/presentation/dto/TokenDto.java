package cmc.mody.auth.presentation.dto;

public record TokenDto(
    Long id,
    String accessToken,
    String refreshToken,
    boolean isNewMember
) {
    public static TokenDto of(Long id, String accessToken, String refreshToken) {
        return of(id, accessToken, refreshToken, false);
    }

    public static TokenDto of(
        Long id,
        String accessToken,
        String refreshToken,
        boolean isNewMember
    ) {
        return new TokenDto(id, accessToken, refreshToken, isNewMember);
    }
}
