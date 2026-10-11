package cmc.mody.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cmc.mody.common.api.exception.GeneralException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GooglePublicKeysManager;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.webtoken.JsonWebSignature;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdminGoogleIdentityVerifierTest {
    private static KeyPair trusted, untrusted;
    private AdminGoogleIdentityVerifier verifier;

    @BeforeAll
    static void keys() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        trusted = generator.generateKeyPair();
        untrusted = generator.generateKeyPair();
    }

    @BeforeEach
    void setup() throws Exception {
        GooglePublicKeysManager keys = mock(GooglePublicKeysManager.class);
        when(keys.getPublicKeys()).thenReturn(List.of(trusted.getPublic()));
        when(keys.getJsonFactory()).thenReturn(GsonFactory.getDefaultInstance());
        when(keys.getTransport()).thenReturn(new NetHttpTransport());
        GoogleIdTokenVerifier google = new GoogleIdTokenVerifier.Builder(keys)
            .setAudience(List.of("admin-web-client")).setAcceptableTimeSkewSeconds(0).build();
        verifier = new AdminGoogleIdentityVerifier("admin-web-client", google);
    }

    @Test
    void verifiesRealSignatureAndNonceWithoutExternalNetwork() throws Exception {
        var identity = verifier.verify(sign(payload(), trusted), "login-nonce");
        assertThat(identity.subject()).isEqualTo("stable-google-sub");
        assertThat(identity.authoritativeEmail()).isTrue();
    }

    @Test
    void rejectsWrongSignatureAudienceIssuerExpiryAndNonce() throws Exception {
        assertRejected(sign(payload(), untrusted), "login-nonce");
        assertRejected(sign(payload().setAudience("mobile-app-client"), trusted), "login-nonce");
        assertRejected(sign(payload().setIssuer("https://untrusted.example"), trusted), "login-nonce");
        var expired = payload().setExpirationTimeSeconds(Instant.now().minusSeconds(1).getEpochSecond());
        assertRejected(sign(expired, trusted),
            "login-nonce");
        assertRejected(sign(payload(), trusted), "other-nonce");
        assertRejected("not-a-token", "login-nonce");
    }

    @Test
    void thirdPartyEmailOrUnverifiedEmailDoesNotBootstrapAdmin() throws Exception {
        assertThat(verifier.verify(sign(payload().setEmail("member@example.com"), trusted), "login-nonce")
            .authoritativeEmail()).isFalse();
        assertThat(verifier.verify(sign(payload().setEmailVerified(false), trusted), "login-nonce")
            .authoritativeEmail()).isFalse();
    }

    @Test
    void missingConfigurationAlwaysFailsClosed() throws Exception {
        var disabled = new AdminGoogleIdentityVerifier("");
        assertThatThrownBy(() -> disabled.verify(sign(payload(), trusted), "login-nonce"))
            .isInstanceOf(GeneralException.class);
    }

    private void assertRejected(String token, String nonce) {
        assertThatThrownBy(() -> verifier.verify(token, nonce)).isInstanceOf(GeneralException.class);
    }

    private GoogleIdToken.Payload payload() {
        long now = Instant.now().getEpochSecond();
        var payload = new GoogleIdToken.Payload().setSubject("stable-google-sub")
            .setIssuer("https://accounts.google.com").setAudience("admin-web-client")
            .setIssuedAtTimeSeconds(now).setExpirationTimeSeconds(now + 3600)
            .setEmail("allowed@gmail.com").setEmailVerified(true);
        payload.set("nonce", "login-nonce");
        return payload;
    }

    private String sign(GoogleIdToken.Payload payload, KeyPair pair) throws Exception {
        var header = new JsonWebSignature.Header().setAlgorithm("RS256").setType("JWT");
        return JsonWebSignature.signUsingRsaSha256((RSAPrivateKey) pair.getPrivate(),
            GsonFactory.getDefaultInstance(), header, payload);
    }
}
