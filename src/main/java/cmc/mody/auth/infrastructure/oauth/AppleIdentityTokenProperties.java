package cmc.mody.auth.infrastructure.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oauth.apple.identity-token")
public class AppleIdentityTokenProperties {
    private String issuer = "https://appleid.apple.com";
    private String audience = "com.jagsim.mody-dev";
    private String teamId = "BLRYMXGV5K";
    private long publicKeyCacheTtlSeconds = 3600L;

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public long getPublicKeyCacheTtlSeconds() {
        return publicKeyCacheTtlSeconds;
    }

    public void setPublicKeyCacheTtlSeconds(long publicKeyCacheTtlSeconds) {
        this.publicKeyCacheTtlSeconds = publicKeyCacheTtlSeconds;
    }
}
