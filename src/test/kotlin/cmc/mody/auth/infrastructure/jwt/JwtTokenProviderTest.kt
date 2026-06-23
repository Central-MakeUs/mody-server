package cmc.mody.auth.infrastructure.jwt

import cmc.mody.common.api.exception.GeneralException
import cmc.mody.common.api.status.ErrorStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class JwtTokenProviderTest {
    private val secret = "test-jwt-secret-key-must-be-at-least-32-bytes"
    private val otherSecret = "other-jwt-secret-key-must-be-at-least-32-bytes"

    @Test
    @DisplayName("memberId로 access/refresh 토큰을 만들고 memberId를 다시 읽는다.")
    fun createTokenAndReadMemberId() {
        val provider = provider(secret)

        val token = provider.createToken(1L)

        assertThat(token.id).isEqualTo(1L)
        assertThat(token.accessToken).isNotBlank()
        assertThat(token.refreshToken).isNotBlank()
        assertThat(provider.getMemberIdByToken(token.accessToken)).isEqualTo(1L)
        assertThat(provider.getMemberIdByToken(token.refreshToken)).isEqualTo(1L)
    }

    @Test
    @DisplayName("만료된 토큰은 EXPIRED_JWT 예외를 던진다.")
    fun throwExpiredJwtWhenTokenExpired() {
        val provider = provider(secret, accessExpirationTime = -1L, refreshExpirationTime = -1L)
        val token = provider.createToken(1L)

        assertThatThrownBy { provider.validateToken(token.accessToken) }
            .isInstanceOf(GeneralException::class.java)
            .extracting("status")
            .isEqualTo(ErrorStatus.EXPIRED_JWT)
        assertThatThrownBy { provider.validateToken(token.refreshToken) }
            .isInstanceOf(GeneralException::class.java)
            .extracting("status")
            .isEqualTo(ErrorStatus.EXPIRED_JWT)
    }

    @Test
    @DisplayName("서명이 다른 토큰은 UNSUPPORTED_JWT 예외를 던진다.")
    fun throwUnsupportedJwtWhenSignatureDiffers() {
        val token = provider(secret).createToken(1L)
        val otherProvider = provider(otherSecret)

        assertThatThrownBy { otherProvider.validateToken(token.accessToken) }
            .isInstanceOf(GeneralException::class.java)
            .extracting("status")
            .isEqualTo(ErrorStatus.UNSUPPORTED_JWT)
        assertThatThrownBy { otherProvider.validateToken(token.refreshToken) }
            .isInstanceOf(GeneralException::class.java)
            .extracting("status")
            .isEqualTo(ErrorStatus.UNSUPPORTED_JWT)
    }

    @Test
    @DisplayName("빈 문자열 토큰은 EMPTY_JWT 예외를 던진다.")
    fun throwEmptyJwtWhenTokenIsBlank() {
        val provider = provider(secret)

        assertThatThrownBy { provider.validateToken(" ") }
            .isInstanceOf(GeneralException::class.java)
            .extracting("status")
            .isEqualTo(ErrorStatus.EMPTY_JWT)
    }

    private fun provider(
        secret: String,
        accessExpirationTime: Long = 3_600_000L,
        refreshExpirationTime: Long = 1_209_600_000L
    ): JwtTokenProvider =
        JwtTokenProvider(
            JwtProperties(
                accessSecret = secret,
                accessTokenExpirationTime = accessExpirationTime,
                refreshTokenExpirationTime = refreshExpirationTime
            )
        )
}
