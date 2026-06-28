package cmc.mody.auth.application.oauth;

import cmc.mody.auth.application.oauth.dto.OAuthProfile;
import cmc.mody.auth.application.oauth.dto.OAuthMemberResult;
import cmc.mody.auth.application.oauth.strategy.OAuthStrategy;
import cmc.mody.auth.application.token.TokenProvider;
import cmc.mody.auth.presentation.dto.TokenDto;
import cmc.mody.member.domain.LoginType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OAuthService {
    private final OAuthStrategyFactory strategyFactory;
    private final OAuthMemberProcessor memberProcessor;
    private final TokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;

    public String getRedirectUrl(LoginType loginType) {
        return strategyFactory.getStrategy(loginType).getRedirectUrl();
    }

    public TokenDto loginByProviderToken(LoginType loginType, String providerToken) {
        OAuthStrategy strategy = strategyFactory.getStrategy(loginType);
        return issueToken(strategy.getProfileByProviderToken(providerToken));
    }

    public TokenDto loginByAuthorizationCode(LoginType loginType, String code) {
        OAuthStrategy strategy = strategyFactory.getStrategy(loginType);
        return issueToken(strategy.getProfileByAuthorizationCode(code));
    }

    private TokenDto issueToken(OAuthProfile profile) {
        OAuthMemberResult result = memberProcessor.ensure(profile);
        TokenDto token = tokenProvider.createToken(result.memberId());
        refreshTokenService.replace(result.memberId(), token.refreshToken());
        return TokenDto.of(result.memberId(), token.accessToken(), token.refreshToken(), result.personalInfoCompleted());
    }
}
