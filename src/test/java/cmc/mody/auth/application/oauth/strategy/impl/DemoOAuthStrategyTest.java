package cmc.mody.auth.application.oauth.strategy.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cmc.mody.auth.application.oauth.DemoLoginProperties;
import cmc.mody.auth.application.oauth.dto.OAuthProfile;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.member.domain.LoginType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DemoOAuthStrategyTest {
    @Test
    @DisplayName("데모 로그인이 켜져 있으면 Android 심사용 OAuth 프로필을 반환한다.")
    void getAndroidProfile() {
        DemoLoginProperties properties = enabledProperties();
        AndroidTestOAuthStrategy strategy = new AndroidTestOAuthStrategy(properties);

        OAuthProfile result = strategy.getProfileByProviderToken(null);

        assertThat(result.loginType()).isEqualTo(LoginType.ANDROIDTEST);
        assertThat(result.providerUserId()).isEqualTo("android-reviewer");
        assertThat(result.nickname()).isEqualTo("AndroidDemo");
    }

    @Test
    @DisplayName("데모 로그인이 켜져 있으면 iOS 심사용 OAuth 프로필을 반환한다.")
    void getIosProfile() {
        DemoLoginProperties properties = enabledProperties();
        IosTestOAuthStrategy strategy = new IosTestOAuthStrategy(properties);

        OAuthProfile result = strategy.getProfileByProviderToken(null);

        assertThat(result.loginType()).isEqualTo(LoginType.IOSTEST);
        assertThat(result.providerUserId()).isEqualTo("ios-reviewer");
        assertThat(result.nickname()).isEqualTo("IosDemo");
    }

    @Test
    @DisplayName("데모 로그인이 꺼져 있으면 차단한다.")
    void disabled() {
        DemoLoginProperties properties = enabledProperties();
        properties.setEnabled(false);
        AndroidTestOAuthStrategy strategy = new AndroidTestOAuthStrategy(properties);

        assertThatThrownBy(() -> strategy.getProfileByProviderToken(null))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getStatus()).isEqualTo(ErrorStatus.DEMO_LOGIN_DISABLED));
    }

    @Test
    @DisplayName("데모 provider user id가 비어 있으면 OAuth 프로필 오류로 처리한다.")
    void blankProviderUserId() {
        DemoLoginProperties properties = enabledProperties();
        properties.setAndroidProviderUserId(" ");
        AndroidTestOAuthStrategy strategy = new AndroidTestOAuthStrategy(properties);

        assertThatThrownBy(() -> strategy.getProfileByProviderToken(null))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getStatus()).isEqualTo(ErrorStatus.INVALID_OAUTH_PROFILE));
    }

    private DemoLoginProperties enabledProperties() {
        DemoLoginProperties properties = new DemoLoginProperties();
        properties.setEnabled(true);
        properties.setAndroidProviderUserId("android-reviewer");
        properties.setIosProviderUserId("ios-reviewer");
        properties.setAndroidNickname("AndroidDemo");
        properties.setIosNickname("IosDemo");
        return properties;
    }
}
