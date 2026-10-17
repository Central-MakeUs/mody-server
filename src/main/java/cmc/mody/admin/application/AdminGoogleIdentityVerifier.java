package cmc.mody.admin.application;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AdminGoogleIdentityVerifier {
    private final GoogleIdTokenVerifier verifier;
    private final String clientId;

    @Autowired
    public AdminGoogleIdentityVerifier(@Value("${admin.google-client-id:}") String clientId) {
        this.clientId = clientId;
        this.verifier = new GoogleIdTokenVerifier.Builder(
            new NetHttpTransport(), GsonFactory.getDefaultInstance())
            .setAudience(List.of(clientId)).build();
    }

    AdminGoogleIdentityVerifier(String clientId, GoogleIdTokenVerifier verifier) {
        this.clientId = clientId;
        this.verifier = verifier;
    }

    public Identity verify(String credential, String nonce) {
        if (clientId.isBlank() || credential == null || credential.length() > 8192 || nonce == null) {
            throw new GeneralException(ErrorStatus.UNAUTHORIZED);
        }
        try {
            GoogleIdToken token = verifier.verify(credential);
            if (token == null || !Objects.equals(nonce, token.getPayload().get("nonce"))) {
                throw new GeneralException(ErrorStatus.UNAUTHORIZED);
            }
            GoogleIdToken.Payload payload = token.getPayload();
            String email = payload.getEmail();
            boolean authoritative = Boolean.TRUE.equals(payload.getEmailVerified()) && email != null
                && (email.endsWith("@gmail.com") || StringUtils.hasText(payload.getHostedDomain()));
            return new Identity(payload.getSubject(), email, authoritative);
        } catch (GeneralException exception) {
            throw exception;
        } catch (Exception exception) {
            // Credentials and upstream exception messages must never enter logs or responses.
            throw new GeneralException(ErrorStatus.UNAUTHORIZED);
        }
    }

    public record Identity(String subject, String email, boolean authoritativeEmail) { }
}
