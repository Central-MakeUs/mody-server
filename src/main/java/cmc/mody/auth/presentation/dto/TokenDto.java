package cmc.mody.auth.presentation.dto;

public record TokenDto(
    Long id,
    String accessToken,
    String refreshToken,
    boolean personalInfoCompleted,
    boolean mainAccessible,
    boolean groupOnboardingCompleted
) {
    public static TokenDto of(Long id, String accessToken, String refreshToken) {
        return of(id, accessToken, refreshToken, false, false, false);
    }

    public static TokenDto of(
        Long id,
        String accessToken,
        String refreshToken,
        boolean personalInfoCompleted
    ) {
        return of(id, accessToken, refreshToken, personalInfoCompleted, false, false);
    }

    public static TokenDto of(
        Long id,
        String accessToken,
        String refreshToken,
        boolean personalInfoCompleted,
        boolean mainAccessible
    ) {
        return of(id, accessToken, refreshToken, personalInfoCompleted, mainAccessible, false);
    }

    public static TokenDto of(
        Long id,
        String accessToken,
        String refreshToken,
        boolean personalInfoCompleted,
        boolean mainAccessible,
        boolean groupOnboardingCompleted
    ) {
        return new TokenDto(
            id,
            accessToken,
            refreshToken,
            personalInfoCompleted,
            mainAccessible,
            groupOnboardingCompleted
        );
    }
}
