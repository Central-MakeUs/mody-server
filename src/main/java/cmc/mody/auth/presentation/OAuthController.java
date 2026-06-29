package cmc.mody.auth.presentation;

import cmc.mody.auth.application.oauth.OAuthService;
import cmc.mody.auth.presentation.dto.SocialLoginResponse;
import cmc.mody.auth.presentation.dto.TokenDto;
import cmc.mody.common.api.ApiResponse;
import cmc.mody.member.domain.LoginType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/oauth")
public class OAuthController {
    private final OAuthService oAuthService;

    @GetMapping("/{loginType}/redirect-url")
    public ApiResponse<OAuthRedirectUrlResponse> getRedirectUrl(@PathVariable String loginType) {
        return ApiResponse.ok(new OAuthRedirectUrlResponse(
            oAuthService.getRedirectUrl(LoginType.from(loginType))
        ));
    }

    @GetMapping("/{loginType}/callback")
    public ApiResponse<SocialLoginResponse> callback(
        @PathVariable String loginType,
        @RequestParam String code
    ) {
        TokenDto token = oAuthService.loginByAuthorizationCode(LoginType.from(loginType), code);
        return ApiResponse.ok(SocialLoginResponse.from(token));
    }

    @PostMapping("/{loginType}/callback")
    public ApiResponse<SocialLoginResponse> callbackByFormPost(
        @PathVariable String loginType,
        @RequestParam String code
    ) {
        TokenDto token = oAuthService.loginByAuthorizationCode(LoginType.from(loginType), code);
        return ApiResponse.ok(SocialLoginResponse.from(token));
    }

    @GetMapping("/client/{loginType}")
    public ApiResponse<SocialLoginResponse> clientLogin(
        @PathVariable String loginType,
        @RequestParam("accessToken") String providerToken
    ) {
        TokenDto token = oAuthService.loginByProviderToken(LoginType.from(loginType), providerToken);
        return ApiResponse.ok(SocialLoginResponse.from(token));
    }

    public record OAuthRedirectUrlResponse(String redirectUrl) {
    }
}
