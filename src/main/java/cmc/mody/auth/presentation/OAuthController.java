package cmc.mody.auth.presentation;

import cmc.mody.auth.presentation.AuthController.SocialLoginResponse;
import cmc.mody.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/oauth")
public class OAuthController {
    @GetMapping("/{loginType}/redirect-url")
    public ApiResponse<OAuthRedirectUrlResponse> getRedirectUrl(@PathVariable String loginType) {
        String redirectUrl = "https://provider.example.com/oauth/authorize"
            + "?client_id=client-id"
            + "&redirect_uri=https://api.mody.dev/api/v1/oauth/"
            + loginType.toLowerCase()
            + "/callback";
        return ApiResponse.ok(new OAuthRedirectUrlResponse(redirectUrl));
    }

    @GetMapping("/{loginType}/callback")
    public ApiResponse<SocialLoginResponse> callback(
        @PathVariable String loginType,
        @RequestParam String code
    ) {
        return ApiResponse.ok(new SocialLoginResponse(
            1L,
            "access-token",
            "refresh-token",
            true
        ));
    }

    public record OAuthRedirectUrlResponse(String redirectUrl) {
    }
}
