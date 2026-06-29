package cmc.mody.auth.infrastructure.jwt;

import cmc.mody.auth.application.token.TokenProvider;
import cmc.mody.auth.presentation.dto.TokenDto;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider implements TokenProvider {
    public static final String MEMBER_ID = "memberId";
    public static final String TOKEN_TYPE = "tokenType";
    public static final String ACCESS = "access";
    public static final String REFRESH = "refresh";

    private final JwtProperties properties;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
    }

    @Override
    public TokenDto createToken(Long memberId) {
        Date now = new Date();
        String accessToken = generateToken(memberId, now, properties.getAccessTokenExpirationTime(), ACCESS);
        String refreshToken = generateToken(memberId, now, properties.getRefreshTokenExpirationTime(), REFRESH);

        return TokenDto.of(memberId, accessToken, refreshToken);
    }

    @Override
    public void validateToken(String token) {
        parseClaims(token);
    }

    @Override
    public Long getMemberIdByToken(String token) {
        return extractMemberId(parseClaims(token));
    }

    @Override
    public Long getMemberIdByAccessToken(String token) {
        Claims claims = parseClaims(token);
        if (!ACCESS.equals(claims.get(TOKEN_TYPE))) {
            throw new GeneralException(ErrorStatus.INVALID_JWT);
        }
        return extractMemberId(claims);
    }

    @Override
    public Long getMemberIdByRefreshToken(String token) {
        Claims claims = parseRefreshTokenClaims(token);
        if (!REFRESH.equals(claims.get(TOKEN_TYPE))) {
            throw new GeneralException(ErrorStatus.INVALID_REFRESH_TOKEN);
        }
        return extractMemberId(claims);
    }

    private Long extractMemberId(Claims claims) {
        Object memberId = claims.get(MEMBER_ID);
        if (memberId instanceof Number number) {
            return number.longValue();
        }
        if (memberId instanceof String value) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                throw new GeneralException(ErrorStatus.INVALID_JWT);
            }
        }
        throw new GeneralException(ErrorStatus.INVALID_JWT);
    }

    public TokenDto createTestToken(Long memberId) {
        Date now = new Date();
        long oneYearInMillis = 365L * 24 * 60 * 60 * 1000;
        String accessToken = generateToken(memberId, now, oneYearInMillis, ACCESS);
        String refreshToken = generateToken(memberId, now, oneYearInMillis, REFRESH);

        return TokenDto.of(memberId, accessToken, refreshToken);
    }

    private String generateToken(Long memberId, Date now, long expirationTime, String tokenType) {
        return Jwts.builder()
            .claim(MEMBER_ID, memberId)
            .claim(TOKEN_TYPE, tokenType)
            .setIssuedAt(now)
            .setExpiration(new Date(now.getTime() + expirationTime))
            .signWith(signingKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    private Claims parseClaims(String token) {
        if (token == null || token.isBlank()) {
            throwJwtException(ErrorStatus.EMPTY_JWT);
        }

        try {
            return Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        } catch (ExpiredJwtException e) {
            throwJwtException(ErrorStatus.EXPIRED_JWT);
        } catch (UnsupportedJwtException e) {
            throwJwtException(ErrorStatus.UNSUPPORTED_JWT);
        } catch (SignatureException e) {
            throwJwtException(ErrorStatus.UNSUPPORTED_JWT);
        } catch (MalformedJwtException e) {
            throwJwtException(ErrorStatus.INVALID_JWT);
        } catch (IllegalArgumentException e) {
            throwJwtException(ErrorStatus.EMPTY_JWT);
        } catch (JwtException e) {
            throwJwtException(ErrorStatus.INVALID_JWT);
        }
        throw new GeneralException(ErrorStatus.INVALID_JWT);
    }

    private Claims parseRefreshTokenClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new GeneralException(ErrorStatus.INVALID_REFRESH_TOKEN);
        }
        try {
            return parseClaims(token);
        } catch (GeneralException e) {
            if (e.getStatus() == ErrorStatus.EXPIRED_JWT || e.getStatus() == ErrorStatus.UNSUPPORTED_JWT) {
                throw e;
            }
            throw new GeneralException(ErrorStatus.INVALID_REFRESH_TOKEN);
        }
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(properties.getAccessSecret().getBytes(StandardCharsets.UTF_8));
    }

    private void throwJwtException(ErrorStatus status) {
        throw new GeneralException(status);
    }
}
