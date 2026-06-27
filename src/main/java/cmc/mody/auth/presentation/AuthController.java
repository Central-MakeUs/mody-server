package cmc.mody.auth.presentation;

import cmc.mody.auth.application.oauth.OAuthService;
import cmc.mody.auth.presentation.dto.TokenDto;
import cmc.mody.common.api.ApiResponse;
import cmc.mody.member.domain.LoginType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final OAuthService oAuthService;

    @PostMapping("/social-login")
    public ApiResponse<SocialLoginResponse> socialLogin(@Valid @RequestBody SocialLoginRequest request) {
        TokenDto token = oAuthService.loginByProviderToken(
            LoginType.from(request.loginType()),
            request.providerAccessToken()
        );
        return ApiResponse.ok(SocialLoginResponse.from(token));
    }

    @PostMapping("/reissue")
    public ApiResponse<TokenReissueResponse> reissue(@RequestBody TokenReissueRequest request) {
        return ApiResponse.ok(new TokenReissueResponse("new-access-token", "new-refresh-token"));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.ok();
    }

    public record SocialLoginRequest(
        @NotBlank
        String loginType,
        @NotBlank
        String providerAccessToken
    ) {
    }

    public record SocialLoginResponse(
        Long id,
        String accessToken,
        String refreshToken,
        boolean isNewMember
    ) {
        public static SocialLoginResponse from(TokenDto token) {
            return new SocialLoginResponse(
                token.id(),
                token.accessToken(),
                token.refreshToken(),
                token.isNewMember()
            );
        }
    }

    public record TokenReissueRequest(
        String refreshToken
    ) {
    }

    public record TokenReissueResponse(
        String accessToken,
        String refreshToken
    ) {
    }
}
