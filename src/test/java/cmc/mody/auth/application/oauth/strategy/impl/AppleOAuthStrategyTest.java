package cmc.mody.auth.application.oauth.strategy.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import cmc.mody.auth.application.oauth.dto.OAuthProfile;
import cmc.mody.auth.infrastructure.oauth.AppleIdentityTokenVerifier;
import cmc.mody.auth.infrastructure.oauth.AppleOAuthClient;
import cmc.mody.auth.infrastructure.oauth.dto.AppleIdTokenPayload;
import cmc.mody.member.domain.LoginType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppleOAuthStrategyTest {
    @Mock
    private AppleOAuthClient appleOAuthClient;

    @Mock
    private AppleIdentityTokenVerifier appleIdentityTokenVerifier;

    @Test
    @DisplayName("클라이언트가 전달한 Apple identity token을 검증해 OAuth 프로필로 변환한다.")
    void getProfileByProviderToken() {
        AppleOAuthStrategy strategy = new AppleOAuthStrategy(appleOAuthClient, appleIdentityTokenVerifier);
        given(appleIdentityTokenVerifier.verify("identity-token"))
            .willReturn(new AppleIdTokenPayload(
                "apple-sub",
                "mody@example.com",
                "true",
                "https://appleid.apple.com",
                "com.jagsim.mody-dev",
                1L,
                2L
            ));

        OAuthProfile result = strategy.getProfileByProviderToken("identity-token");

        assertThat(result.loginType()).isEqualTo(LoginType.APPLE);
        assertThat(result.providerUserId()).isEqualTo("apple-sub");
        assertThat(result.email()).isEqualTo("mody@example.com");
    }
}
