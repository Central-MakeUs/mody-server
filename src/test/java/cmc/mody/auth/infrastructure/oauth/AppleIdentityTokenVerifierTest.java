package cmc.mody.auth.infrastructure.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import cmc.mody.auth.infrastructure.oauth.client.AppleAuthFeignClient;
import cmc.mody.auth.infrastructure.oauth.dto.AppleIdTokenPayload;
import cmc.mody.auth.infrastructure.oauth.dto.ApplePublicKey;
import cmc.mody.auth.infrastructure.oauth.dto.ApplePublicKeyResponse;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppleIdentityTokenVerifierTest {
    private static final String KID = "apple-key-id";
    private static final String ISSUER = "https://appleid.apple.com";
    private static final String AUDIENCE = "com.jagsim.mody-dev";

    @Mock
    private AppleAuthFeignClient appleAuthFeignClient;

    private KeyPair keyPair;
    private AppleIdentityTokenProperties properties;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
        properties = new AppleIdentityTokenProperties();
        properties.setIssuer(ISSUER);
        properties.setAudience(AUDIENCE);
        properties.setPublicKeyCacheTtlSeconds(3600);
    }

    @Test
    @DisplayName("Apple identity token의 서명과 issuer/audience/만료를 검증하고 프로필 payload를 반환한다.")
    void verify() {
        given(appleAuthFeignClient.getPublicKeys()).willReturn(new ApplePublicKeyResponse(List.of(publicKey(KID))));
        AppleIdentityTokenVerifier verifier = verifier();
        String idToken = createIdToken(KID, AUDIENCE, "apple-sub");

        AppleIdTokenPayload result = verifier.verify(idToken);

        assertThat(result.sub()).isEqualTo("apple-sub");
        assertThat(result.email()).isEqualTo("mody@example.com");
        assertThat(result.iss()).isEqualTo(ISSUER);
        assertThat(result.aud()).isEqualTo(AUDIENCE);
    }

    @Test
    @DisplayName("Bundle ID와 audience가 다르면 유효하지 않은 OAuth 토큰으로 처리한다.")
    void throwInvalidTokenWhenAudienceDiffers() {
        given(appleAuthFeignClient.getPublicKeys()).willReturn(new ApplePublicKeyResponse(List.of(publicKey(KID))));
        AppleIdentityTokenVerifier verifier = verifier();
        String idToken = createIdToken(KID, "com.jagsim.mody", "apple-sub");

        assertThatThrownBy(() -> verifier.verify(idToken))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getStatus()).isEqualTo(ErrorStatus.INVALID_OAUTH_TOKEN));
    }

    @Test
    @DisplayName("kid에 대응하는 Apple 공개키가 없으면 유효하지 않은 OAuth 토큰으로 처리한다.")
    void throwInvalidTokenWhenPublicKeyNotMatched() {
        given(appleAuthFeignClient.getPublicKeys()).willReturn(new ApplePublicKeyResponse(List.of(publicKey("other-kid"))));
        AppleIdentityTokenVerifier verifier = verifier();
        String idToken = createIdToken(KID, AUDIENCE, "apple-sub");

        assertThatThrownBy(() -> verifier.verify(idToken))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getStatus()).isEqualTo(ErrorStatus.INVALID_OAUTH_TOKEN));
    }

    @Test
    @DisplayName("토큰 형식이 JWT가 아니면 유효하지 않은 OAuth 토큰으로 처리한다.")
    void throwInvalidTokenWhenMalformed() {
        AppleIdentityTokenVerifier verifier = verifier();

        assertThatThrownBy(() -> verifier.verify("not-jwt"))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getStatus()).isEqualTo(ErrorStatus.INVALID_OAUTH_TOKEN));
    }

    private AppleIdentityTokenVerifier verifier() {
        return new AppleIdentityTokenVerifier(
            appleAuthFeignClient,
            properties,
            new ObjectMapper()
        );
    }

    private String createIdToken(String kid, String audience, String subject) {
        Instant now = Instant.now();
        return Jwts.builder()
            .setHeaderParam("kid", kid)
            .setIssuer(ISSUER)
            .setAudience(audience)
            .setSubject(subject)
            .claim("email", "mody@example.com")
            .claim("email_verified", "true")
            .setIssuedAt(Date.from(now.minusSeconds(60)))
            .setExpiration(Date.from(now.plusSeconds(3600)))
            .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
            .compact();
    }

    private ApplePublicKey publicKey(String kid) {
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        return new ApplePublicKey(
            "RSA",
            kid,
            "sig",
            "RS256",
            base64Url(publicKey.getModulus().toByteArray()),
            base64Url(publicKey.getPublicExponent().toByteArray())
        );
    }

    private String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
