package cmc.mody.auth.application.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import cmc.mody.auth.application.oauth.dto.OAuthMemberResult;
import cmc.mody.auth.application.oauth.dto.OAuthProfile;
import cmc.mody.auth.application.oauth.strategy.OAuthStrategy;
import cmc.mody.auth.application.token.TokenProvider;
import cmc.mody.auth.presentation.dto.TokenDto;
import cmc.mody.member.domain.LoginType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OAuthServiceTest {
    @Mock
    private OAuthMemberProcessor memberProcessor;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("loginType에 맞는 전략으로 프로필을 조회하고 서비스 토큰을 발급한다.")
    void loginByProviderToken() {
        OAuthProfile profile = new OAuthProfile(LoginType.KAKAO, "123", "a@b.com", "민석", "image");
        OAuthStrategy strategy = new StubOAuthStrategy(LoginType.KAKAO, profile);
        OAuthService service = new OAuthService(
            new OAuthStrategyFactory(List.of(strategy)),
            memberProcessor,
            tokenProvider,
            refreshTokenService
        );

        given(memberProcessor.ensure(profile)).willReturn(new OAuthMemberResult(1L, true, true));
        given(tokenProvider.createToken(1L)).willReturn(TokenDto.of(1L, "access", "refresh"));

        TokenDto result = service.loginByProviderToken(LoginType.KAKAO, "provider-token");

        assertThat(result).isEqualTo(TokenDto.of(1L, "access", "refresh", true, true));
        then(refreshTokenService).should().replace(1L, "refresh");
    }

    private record StubOAuthStrategy(LoginType type, OAuthProfile profile) implements OAuthStrategy {
        @Override
        public LoginType getType() {
            return type;
        }

        @Override
        public String getRedirectUrl() {
            return "https://provider.example.com/oauth/authorize";
        }

        @Override
        public OAuthProfile getProfileByAuthorizationCode(String code) {
            return profile;
        }

        @Override
        public OAuthProfile getProfileByProviderToken(String providerToken) {
            return profile;
        }
    }
}
