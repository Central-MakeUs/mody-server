package cmc.mody.auth.presentation;

import cmc.mody.common.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    @PostMapping("/social-login")
    public ApiResponse<SocialLoginResponse> socialLogin(@RequestBody SocialLoginRequest request) {
        return ApiResponse.ok(new SocialLoginResponse(
            1L,
            true,
            "access-token",
            "refresh-token",
            List.of("PROFILE_REQUIRED", "GROUP_REQUIRED")
        ));
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
        String loginType,
        String providerAccessToken
    ) {
    }

    public record SocialLoginResponse(
        Long memberId,
        boolean newMember,
        String accessToken,
        String refreshToken,
        List<String> requiredSteps
    ) {
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
