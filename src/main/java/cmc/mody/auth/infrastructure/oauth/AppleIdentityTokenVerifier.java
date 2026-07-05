package cmc.mody.auth.infrastructure.oauth;

import cmc.mody.auth.infrastructure.oauth.client.AppleAuthFeignClient;
import cmc.mody.auth.infrastructure.oauth.dto.AppleIdTokenHeader;
import cmc.mody.auth.infrastructure.oauth.dto.AppleIdTokenPayload;
import cmc.mody.auth.infrastructure.oauth.dto.ApplePublicKey;
import cmc.mody.auth.infrastructure.oauth.dto.ApplePublicKeyResponse;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AppleIdentityTokenVerifier {
    private static final int JWT_PARTS_LENGTH = 3;
    private static final int HEADER_INDEX = 0;
    private static final String RSA_KEY_TYPE = "RSA";
    private static final String RS256_ALGORITHM = "RS256";

    private final AppleAuthFeignClient appleAuthFeignClient;
    private final AppleIdentityTokenProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    private volatile ApplePublicKeyResponse cachedPublicKeys;
    private volatile Instant cachedAt = Instant.EPOCH;

    @Autowired
    public AppleIdentityTokenVerifier(
        AppleAuthFeignClient appleAuthFeignClient,
        AppleIdentityTokenProperties properties,
        ObjectMapper objectMapper
    ) {
        this(appleAuthFeignClient, properties, objectMapper, Clock.systemUTC());
    }

    AppleIdentityTokenVerifier(
        AppleAuthFeignClient appleAuthFeignClient,
        AppleIdentityTokenProperties properties,
        ObjectMapper objectMapper,
        Clock clock
    ) {
        this.appleAuthFeignClient = appleAuthFeignClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public AppleIdTokenPayload verify(String idToken) {
        validateToken(idToken);
        AppleIdTokenHeader header = parseHeader(idToken);
        RSAPublicKey publicKey = resolvePublicKey(header);
        try {
            Claims claims = Jwts.parserBuilder()
                .requireIssuer(properties.getIssuer())
                .requireAudience(properties.getAudience())
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(idToken)
                .getBody();

            return new AppleIdTokenPayload(
                claims.getSubject(),
                claims.get("email", String.class),
                stringClaim(claims, "email_verified"),
                claims.getIssuer(),
                claims.getAudience(),
                toEpochSecond(claims.getIssuedAt()),
                toEpochSecond(claims.getExpiration())
            );
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Apple identity token validation failed. reason={}", e.getClass().getSimpleName());
            throw new GeneralException(ErrorStatus.INVALID_OAUTH_TOKEN);
        }
    }

    private void validateToken(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new GeneralException(ErrorStatus.INVALID_OAUTH_TOKEN);
        }
        String[] parts = idToken.split("\\.");
        if (parts.length != JWT_PARTS_LENGTH) {
            throw new GeneralException(ErrorStatus.INVALID_OAUTH_TOKEN);
        }
    }

    private AppleIdTokenHeader parseHeader(String idToken) {
        try {
            String[] parts = idToken.split("\\.");
            byte[] decodedHeader = Base64.getUrlDecoder().decode(parts[HEADER_INDEX]);
            AppleIdTokenHeader header = objectMapper.readValue(
                new String(decodedHeader, StandardCharsets.UTF_8),
                AppleIdTokenHeader.class
            );
            if (header.kid() == null || header.kid().isBlank() || !RS256_ALGORITHM.equals(header.alg())) {
                throw new GeneralException(ErrorStatus.INVALID_OAUTH_TOKEN);
            }
            return header;
        } catch (IllegalArgumentException | JsonProcessingException e) {
            throw new GeneralException(ErrorStatus.INVALID_OAUTH_TOKEN);
        }
    }

    private RSAPublicKey resolvePublicKey(AppleIdTokenHeader header) {
        return getPublicKeys().stream()
            .filter(key -> header.kid().equals(key.kid()))
            .filter(key -> RSA_KEY_TYPE.equals(key.kty()))
            .filter(key -> key.alg() == null || RS256_ALGORITHM.equals(key.alg()))
            .findFirst()
            .map(this::toRsaPublicKey)
            .orElseThrow(() -> new GeneralException(ErrorStatus.INVALID_OAUTH_TOKEN));
    }

    private List<ApplePublicKey> getPublicKeys() {
        ApplePublicKeyResponse cached = cachedPublicKeys;
        if (cached != null && !isCacheExpired()) {
            return cached.keys();
        }
        synchronized (this) {
            if (cachedPublicKeys == null || isCacheExpired()) {
                cachedPublicKeys = fetchPublicKeys();
                cachedAt = Instant.now(clock);
            }
            return cachedPublicKeys.keys();
        }
    }

    private boolean isCacheExpired() {
        return cachedAt.plusSeconds(properties.getPublicKeyCacheTtlSeconds()).isBefore(Instant.now(clock));
    }

    private ApplePublicKeyResponse fetchPublicKeys() {
        try {
            ApplePublicKeyResponse response = appleAuthFeignClient.getPublicKeys();
            if (response.keys() == null || response.keys().isEmpty()) {
                throw new GeneralException(ErrorStatus.OAUTH_PROFILE_REQUEST_FAILED);
            }
            return response;
        } catch (FeignException e) {
            log.warn("Apple public key request failed. status={}", e.status());
            throw new GeneralException(ErrorStatus.OAUTH_PROFILE_REQUEST_FAILED);
        }
    }

    private RSAPublicKey toRsaPublicKey(ApplePublicKey key) {
        try {
            BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(key.n()));
            BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(key.e()));
            return (RSAPublicKey) KeyFactory.getInstance(RSA_KEY_TYPE)
                .generatePublic(new RSAPublicKeySpec(modulus, exponent));
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            throw new GeneralException(ErrorStatus.INVALID_OAUTH_TOKEN);
        }
    }

    private String stringClaim(Claims claims, String key) {
        Object value = claims.get(key);
        return value == null ? null : value.toString();
    }

    private Long toEpochSecond(java.util.Date date) {
        return date == null ? null : date.toInstant().getEpochSecond();
    }
}
