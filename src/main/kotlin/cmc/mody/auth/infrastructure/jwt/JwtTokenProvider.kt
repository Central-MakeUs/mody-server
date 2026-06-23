package cmc.mody.auth.infrastructure.jwt

import cmc.mody.auth.application.token.TokenProvider
import cmc.mody.auth.presentation.dto.TokenDto
import cmc.mody.common.api.exception.GeneralException
import cmc.mody.common.api.status.ErrorStatus
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    private val properties: JwtProperties
) : TokenProvider {
    private val signingKey: SecretKey
        get() = Keys.hmacShaKeyFor(properties.accessSecret.toByteArray(StandardCharsets.UTF_8))

    override fun createToken(memberId: Long): TokenDto {
        val now = Date()
        val accessToken = generateToken(memberId, now, properties.accessTokenExpirationTime, ACCESS)
        val refreshToken = generateToken(memberId, now, properties.refreshTokenExpirationTime, REFRESH)

        return TokenDto.of(memberId, accessToken, refreshToken)
    }

    override fun validateToken(token: String) {
        parseClaims(token)
    }

    override fun getMemberIdByToken(token: String): Long {
        val memberId = parseClaims(token)[MEMBER_ID] ?: throw GeneralException(ErrorStatus.INVALID_JWT)
        return when (memberId) {
            is Number -> memberId.toLong()
            is String -> memberId.toLongOrNull() ?: throw GeneralException(ErrorStatus.INVALID_JWT)
            else -> throw GeneralException(ErrorStatus.INVALID_JWT)
        }
    }

    fun createTestToken(memberId: Long): TokenDto {
        val now = Date()
        val oneYearInMillis = 365L * 24 * 60 * 60 * 1000
        val accessToken = generateToken(memberId, now, oneYearInMillis, ACCESS)
        val refreshToken = generateToken(memberId, now, oneYearInMillis, REFRESH)

        return TokenDto.of(memberId, accessToken, refreshToken)
    }

    private fun generateToken(
        memberId: Long,
        now: Date,
        expirationTime: Long,
        tokenType: String
    ): String =
        Jwts
            .builder()
            .claim(MEMBER_ID, memberId)
            .claim(TOKEN_TYPE, tokenType)
            .setIssuedAt(now)
            .setExpiration(Date(now.time + expirationTime))
            .signWith(signingKey, SignatureAlgorithm.HS256)
            .compact()

    private fun parseClaims(token: String): Claims {
        if (token.isBlank()) {
            throwJwtException(ErrorStatus.EMPTY_JWT)
        }

        try {
            return Jwts
                .parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .body
        } catch (e: ExpiredJwtException) {
            throwJwtException(ErrorStatus.EXPIRED_JWT)
        } catch (e: UnsupportedJwtException) {
            throwJwtException(ErrorStatus.UNSUPPORTED_JWT)
        } catch (e: SignatureException) {
            throwJwtException(ErrorStatus.UNSUPPORTED_JWT)
        } catch (e: MalformedJwtException) {
            throwJwtException(ErrorStatus.INVALID_JWT)
        } catch (e: IllegalArgumentException) {
            throwJwtException(ErrorStatus.EMPTY_JWT)
        } catch (e: JwtException) {
            throwJwtException(ErrorStatus.INVALID_JWT)
        }
    }

    private fun throwJwtException(status: ErrorStatus): Nothing = throw GeneralException(status)

    companion object {
        const val MEMBER_ID = "memberId"
        const val TOKEN_TYPE = "tokenType"
        const val ACCESS = "access"
        const val REFRESH = "refresh"
    }
}
