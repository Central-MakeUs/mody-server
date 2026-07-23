package cmc.mody.auth.application.oauth;

import cmc.mody.member.domain.LoginType;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demo-login")
public class DemoLoginProperties {
    private boolean enabled = false;
    private String androidProviderUserId = "mody-android-reviewer";
    private String iosProviderUserId = "mody-ios-reviewer";
    private String androidNickname = "AndroidReviewer";
    private String iosNickname = "iOSReviewer";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAndroidProviderUserId() {
        return androidProviderUserId;
    }

    public void setAndroidProviderUserId(String androidProviderUserId) {
        this.androidProviderUserId = androidProviderUserId;
    }

    public String getIosProviderUserId() {
        return iosProviderUserId;
    }

    public void setIosProviderUserId(String iosProviderUserId) {
        this.iosProviderUserId = iosProviderUserId;
    }

    public String getAndroidNickname() {
        return androidNickname;
    }

    public void setAndroidNickname(String androidNickname) {
        this.androidNickname = androidNickname;
    }

    public String getIosNickname() {
        return iosNickname;
    }

    public void setIosNickname(String iosNickname) {
        this.iosNickname = iosNickname;
    }

    public String providerUserId(LoginType loginType) {
        return switch (loginType) {
            case ANDROIDTEST -> androidProviderUserId;
            case IOSTEST -> iosProviderUserId;
            default -> throw new IllegalArgumentException("Unsupported demo login type: " + loginType);
        };
    }

    public String nickname(LoginType loginType) {
        return switch (loginType) {
            case ANDROIDTEST -> androidNickname;
            case IOSTEST -> iosNickname;
            default -> throw new IllegalArgumentException("Unsupported demo login type: " + loginType);
        };
    }
}
