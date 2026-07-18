package cmc.mody.auth.presentation;

import cmc.mody.auth.application.AuthService;
import cmc.mody.auth.presentation.dto.TokenDto;
import cmc.mody.common.api.ApiResponse;
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
    private final AuthService authService;

    @PostMapping("/reissue")
    public ApiResponse<TokenReissueResponse> reissue(@RequestBody TokenReissueRequest request) {
        TokenDto token = authService.reissue(request.refreshToken());
        return ApiResponse.ok(TokenReissueResponse.from(token));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody TokenLogoutRequest request) {
        authService.logout(request.refreshToken());
        return ApiResponse.ok();
    }

    public record TokenReissueRequest(
        String refreshToken
    ) {
    }

    public record TokenReissueResponse(
        String accessToken,
        String refreshToken
    ) {
        public static TokenReissueResponse from(TokenDto token) {
            return new TokenReissueResponse(token.accessToken(), token.refreshToken());
        }
    }

    public record TokenLogoutRequest(
        @NotBlank(message = "refresh token은 필수입니다.")
        String refreshToken
    ) {
    }
}
