package cmc.mody.auth.infrastructure.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cmc.mody.auth.presentation.dto.TokenDto;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {
    private static final String SECRET = "test-jwt-secret-key-must-be-at-least-32-bytes";
    private static final String OTHER_SECRET = "other-jwt-secret-key-must-be-at-least-32-bytes";

    @Test
    @DisplayName("memberId로 access/refresh 토큰을 만들고 memberId를 다시 읽는다.")
    void createTokenAndReadMemberId() {
        JwtTokenProvider provider = provider(SECRET);

        TokenDto token = provider.createToken(1L);

        assertThat(token.id()).isEqualTo(1L);
        assertThat(token.accessToken()).isNotBlank();
        assertThat(token.refreshToken()).isNotBlank();
        assertThat(provider.getMemberIdByToken(token.accessToken())).isEqualTo(1L);
        assertThat(provider.getMemberIdByToken(token.refreshToken())).isEqualTo(1L);
    }

    @Test
    @DisplayName("만료된 토큰은 EXPIRED_JWT 예외를 던진다.")
    void throwExpiredJwtWhenTokenExpired() {
        JwtTokenProvider provider = provider(SECRET, -1L, -1L);
        TokenDto token = provider.createToken(1L);

        assertThatThrownBy(() -> provider.validateToken(token.accessToken()))
            .isInstanceOf(GeneralException.class)
            .extracting("status")
            .isEqualTo(ErrorStatus.EXPIRED_JWT);
        assertThatThrownBy(() -> provider.validateToken(token.refreshToken()))
            .isInstanceOf(GeneralException.class)
            .extracting("status")
            .isEqualTo(ErrorStatus.EXPIRED_JWT);
    }

    @Test
    @DisplayName("서명이 다른 토큰은 UNSUPPORTED_JWT 예외를 던진다.")
    void throwUnsupportedJwtWhenSignatureDiffers() {
        TokenDto token = provider(SECRET).createToken(1L);
        JwtTokenProvider otherProvider = provider(OTHER_SECRET);

        assertThatThrownBy(() -> otherProvider.validateToken(token.accessToken()))
            .isInstanceOf(GeneralException.class)
            .extracting("status")
            .isEqualTo(ErrorStatus.UNSUPPORTED_JWT);
        assertThatThrownBy(() -> otherProvider.validateToken(token.refreshToken()))
            .isInstanceOf(GeneralException.class)
            .extracting("status")
            .isEqualTo(ErrorStatus.UNSUPPORTED_JWT);
    }

    @Test
    @DisplayName("빈 문자열 토큰은 EMPTY_JWT 예외를 던진다.")
    void throwEmptyJwtWhenTokenIsBlank() {
        JwtTokenProvider provider = provider(SECRET);

        assertThatThrownBy(() -> provider.validateToken(" "))
            .isInstanceOf(GeneralException.class)
            .extracting("status")
            .isEqualTo(ErrorStatus.EMPTY_JWT);
    }

    private JwtTokenProvider provider(String secret) {
        return provider(secret, 3_600_000L, 1_209_600_000L);
    }

    private JwtTokenProvider provider(
        String secret,
        long accessExpirationTime,
        long refreshExpirationTime
    ) {
        return new JwtTokenProvider(new JwtProperties(secret, accessExpirationTime, refreshExpirationTime));
    }
}
