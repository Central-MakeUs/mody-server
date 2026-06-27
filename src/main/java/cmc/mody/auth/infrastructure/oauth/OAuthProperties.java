package cmc.mody.auth.infrastructure.oauth;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oauth")
public class OAuthProperties {
    private Provider kakao = new Provider(
        "https://kauth.kakao.com/oauth/authorize",
        "https://kauth.kakao.com",
        "https://kapi.kakao.com",
        "",
        "",
        "",
        List.of()
    );
    private Provider google = new Provider(
        "https://accounts.google.com/o/oauth2/v2/auth",
        "https://oauth2.googleapis.com",
        "https://www.googleapis.com",
        "",
        "",
        "",
        List.of("profile", "email")
    );
    private Provider apple = new Provider(
        "https://appleid.apple.com/auth/authorize",
        "https://appleid.apple.com",
        "",
        "",
        "",
        "",
        List.of("email", "name")
    );

    public Provider getKakao() {
        return kakao;
    }

    public void setKakao(Provider kakao) {
        this.kakao = kakao;
    }

    public Provider getGoogle() {
        return google;
    }

    public void setGoogle(Provider google) {
        this.google = google;
    }

    public Provider getApple() {
        return apple;
    }

    public void setApple(Provider apple) {
        this.apple = apple;
    }

    public record Provider(
        String authorizationUri,
        String authBaseUri,
        String apiBaseUri,
        String clientId,
        String clientSecret,
        String redirectUri,
        List<String> scope
    ) {
    }
}
