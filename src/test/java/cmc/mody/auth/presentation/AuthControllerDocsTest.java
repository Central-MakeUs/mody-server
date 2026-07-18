package cmc.mody.auth.presentation;

import static cmc.mody.docs.ApiDocumentUtils.commonResponseFields;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cmc.mody.auth.application.AuthService;
import cmc.mody.auth.presentation.dto.TokenDto;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureRestDocs
class AuthControllerDocsTest {
    private static final String AUTH_DESCRIPTION = """
        refresh token 기반 세션 관리 API다.
        로그아웃 시 요청 refresh token을 비활성화하고, 해당 회원의 FCM push token도 모두 비활성화한다.

        발생 가능한 예외 코드:
        - AUTH404: 만료된 refresh token
        - AUTH405: 지원하지 않는 JWT
        - AUTH406: 비어 있거나 저장되지 않았거나 access token이 전달된 refresh token
        """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void reissue() throws Exception {
        given(authService.reissue("refresh-token"))
            .willReturn(TokenDto.of(1L, "new-access-token", "new-refresh-token"));

        mockMvc.perform(post("/api/v1/auth/reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "refreshToken": "refresh-token"
                    }
                    """))
            .andExpect(status().isOk())
            .andDo(document("auth-reissue",
                resource(ResourceSnippetParameters.builder()
                    .tag("Auth")
                    .summary("토큰 재발급")
                    .description(AUTH_DESCRIPTION)
                    .requestFields(
                        fieldWithPath("refreshToken").type(JsonFieldType.STRING).description("refresh token")
                    )
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.accessToken").type(JsonFieldType.STRING).description("새 access token"),
                        fieldWithPath("result.refreshToken").type(JsonFieldType.STRING).description("새 refresh token")
                    ))
                    .build())
            ));
    }

    @Test
    void logout() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "refreshToken": "refresh-token"
                    }
                    """))
            .andExpect(status().isOk())
            .andDo(document("auth-logout",
                resource(ResourceSnippetParameters.builder()
                    .tag("Auth")
                    .summary("로그아웃")
                    .description(AUTH_DESCRIPTION)
                    .requestFields(
                        fieldWithPath("refreshToken").type(JsonFieldType.STRING).description("refresh token")
                    )
                    .responseFields(commonResponseFields())
                    .build())
            ));
    }

    @Test
    void reissueInvalidRefreshToken() throws Exception {
        willThrow(new GeneralException(ErrorStatus.INVALID_REFRESH_TOKEN))
            .given(authService)
            .reissue(anyString());

        mockMvc.perform(post("/api/v1/auth/reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshTokenRequest()))
            .andExpect(status().isUnauthorized())
            .andDo(documentError("auth-reissue-invalid-refresh-token", "토큰 재발급"));
    }

    @Test
    void reissueExpiredRefreshToken() throws Exception {
        willThrow(new GeneralException(ErrorStatus.EXPIRED_JWT))
            .given(authService)
            .reissue(anyString());

        mockMvc.perform(post("/api/v1/auth/reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshTokenRequest()))
            .andExpect(status().isUnauthorized())
            .andDo(documentError("auth-reissue-expired-refresh-token", "토큰 재발급"));
    }

    @Test
    void reissueUnsupportedRefreshToken() throws Exception {
        willThrow(new GeneralException(ErrorStatus.UNSUPPORTED_JWT))
            .given(authService)
            .reissue(anyString());

        mockMvc.perform(post("/api/v1/auth/reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshTokenRequest()))
            .andExpect(status().isUnauthorized())
            .andDo(documentError("auth-reissue-unsupported-refresh-token", "토큰 재발급"));
    }

    @Test
    void logoutInvalidRefreshToken() throws Exception {
        willThrow(new GeneralException(ErrorStatus.INVALID_REFRESH_TOKEN))
            .given(authService)
            .logout(anyString());

        mockMvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshTokenRequest()))
            .andExpect(status().isUnauthorized())
            .andDo(documentError("auth-logout-invalid-refresh-token", "로그아웃"));
    }

    @Test
    void logoutExpiredRefreshToken() throws Exception {
        willThrow(new GeneralException(ErrorStatus.EXPIRED_JWT))
            .given(authService)
            .logout(anyString());

        mockMvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshTokenRequest()))
            .andExpect(status().isUnauthorized())
            .andDo(documentError("auth-logout-expired-refresh-token", "로그아웃"));
    }

    @Test
    void logoutUnsupportedRefreshToken() throws Exception {
        willThrow(new GeneralException(ErrorStatus.UNSUPPORTED_JWT))
            .given(authService)
            .logout(anyString());

        mockMvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshTokenRequest()))
            .andExpect(status().isUnauthorized())
            .andDo(documentError("auth-logout-unsupported-refresh-token", "로그아웃"));
    }

    private String refreshTokenRequest() {
        return """
            {
              "refreshToken": "refresh-token"
            }
            """;
    }

    private RestDocumentationResultHandler documentError(
        String identifier,
        String summary
    ) {
        return document(identifier,
            resource(ResourceSnippetParameters.builder()
                .tag("Auth")
                .summary(summary)
                .description(AUTH_DESCRIPTION)
                .responseFields(commonResponseFields())
                .build())
        );
    }
}
