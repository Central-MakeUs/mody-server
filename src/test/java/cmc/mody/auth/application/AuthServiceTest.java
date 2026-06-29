package cmc.mody.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import cmc.mody.auth.application.oauth.RefreshTokenService;
import cmc.mody.auth.application.token.TokenProvider;
import cmc.mody.auth.presentation.dto.TokenDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("refresh token을 검증하고 새 토큰으로 교체한다.")
    void reissue() {
        AuthService service = new AuthService(tokenProvider, refreshTokenService);
        given(tokenProvider.getMemberIdByRefreshToken("refresh")).willReturn(1L);
        given(tokenProvider.createToken(1L)).willReturn(TokenDto.of(1L, "new-access", "new-refresh"));

        TokenDto result = service.reissue("refresh");

        assertThat(result.accessToken()).isEqualTo("new-access");
        assertThat(result.refreshToken()).isEqualTo("new-refresh");
        then(refreshTokenService).should().validate(1L, "refresh");
        then(refreshTokenService).should().replace(1L, "new-refresh");
    }

    @Test
    @DisplayName("로그아웃 시 refresh token을 비활성화한다.")
    void logout() {
        AuthService service = new AuthService(tokenProvider, refreshTokenService);
        given(tokenProvider.getMemberIdByRefreshToken("refresh")).willReturn(1L);

        service.logout("refresh");

        then(refreshTokenService).should().delete(1L, "refresh");
    }
}
