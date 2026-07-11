package cmc.mody.auth.presentation;

import static cmc.mody.docs.ApiDocumentUtils.commonResponseFields;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cmc.mody.auth.application.AuthService;
import cmc.mody.auth.application.oauth.OAuthService;
import cmc.mody.auth.presentation.dto.TokenDto;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.member.domain.LoginType;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({AuthController.class, OAuthController.class})
@AutoConfigureRestDocs
class OAuthControllerDocsTest {
    private static final String CLIENT_LOGIN_DESCRIPTION = """
        클라이언트가 provider token으로 로그인하고 서비스 JWT를 발급받는다.
        Kakao/Google은 provider access token, Apple은 identity token을 `accessToken` 파라미터로 전달한다.
        회원탈퇴 후 동일 소셜 계정으로 다시 로그인하면 신규 회원으로 생성되며,
        personalInfoCompleted=false, groupOnboardingCompleted=false, mainAccessible=false를 반환한다.

        발생 가능한 예외:
        - `COMMON4000`: accessToken query parameter가 누락됨
        - `AUTH407`: 지원하지 않는 소셜 로그인 타입
        - `AUTH408`: provider token이 비어있거나 유효하지 않음. Apple은 서명, issuer, Bundle ID audience, 만료 검증 실패 포함
        - `AUTH409`: OAuth 프로필 조회 실패
        - `AUTH410`: OAuth 프로필 정보가 올바르지 않음
        """;
    private static final String MAIN_ACCESSIBLE_DESCRIPTION =
        "메인 화면 진입 가능 여부. 개인 정보 입력 완료 및 참여 그룹 1개 이상이면 true";
    private static final String GROUP_ONBOARDING_COMPLETED_DESCRIPTION =
        "그룹 생성 또는 참여를 한 번이라도 완료했는지 여부. 현재 참여 그룹이 없어도 과거 완료 이력이 있으면 true";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OAuthService oAuthService;

    @MockitoBean
    private AuthService authService;

    @Test
    void getRedirectUrl() throws Exception {
        given(oAuthService.getRedirectUrl(LoginType.KAKAO))
            .willReturn("https://kauth.kakao.com/oauth/authorize?client_id=client-id");

        mockMvc.perform(get("/api/v1/oauth/{loginType}/redirect-url", "kakao"))
            .andExpect(status().isOk())
            .andDo(document("oauth-redirect-url",
                resource(ResourceSnippetParameters.builder()
                    .tag("OAuth")
                    .summary("OAuth redirect URL 조회")
                    .description("""
                        서버 callback 흐름에서 사용할 provider authorization URL을 조회한다.
                        네이티브 앱 로그인은 클라이언트 소셜 로그인 API를 우선 사용한다.
                        """)
                    .pathParameters(
                        parameterWithName("loginType").description("로그인 타입: kakao, apple, google")
                    )
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.redirectUrl")
                            .type(JsonFieldType.STRING)
                            .description("provider authorization URL")
                    ))
                    .build())
            ));
    }

    @Test
    void callback() throws Exception {
        given(oAuthService.loginByAuthorizationCode(LoginType.KAKAO, "authorization-code"))
            .willReturn(TokenDto.of(1L, "access-token", "refresh-token", true, true, true));

        mockMvc.perform(get("/api/v1/oauth/{loginType}/callback", "kakao")
                .queryParam("code", "authorization-code"))
            .andExpect(status().isOk())
            .andDo(document("oauth-callback",
                resource(ResourceSnippetParameters.builder()
                    .tag("OAuth")
                    .summary("OAuth callback 로그인")
                    .description("""
                        authorization code로 로그인하고 서비스 JWT를 발급한다.
                        서버 callback 흐름용 API이며, 네이티브 앱은 provider token을 전달하는 클라이언트 소셜 로그인 API를 사용한다.
                        """)
                    .pathParameters(
                        parameterWithName("loginType").description("로그인 타입: kakao, apple, google")
                    )
                    .queryParameters(
                        parameterWithName("code").description("provider authorization code")
                    )
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.id").type(JsonFieldType.NUMBER).description("회원 id"),
                        fieldWithPath("result.accessToken").type(JsonFieldType.STRING).description("access token"),
                        fieldWithPath("result.refreshToken").type(JsonFieldType.STRING).description("refresh token"),
                        fieldWithPath("result.personalInfoCompleted")
                            .type(JsonFieldType.BOOLEAN)
                            .description("개인 정보 입력 완료 여부"),
                        fieldWithPath("result.mainAccessible")
                            .type(JsonFieldType.BOOLEAN)
                            .description(MAIN_ACCESSIBLE_DESCRIPTION),
                        fieldWithPath("result.groupOnboardingCompleted")
                            .type(JsonFieldType.BOOLEAN)
                            .description(GROUP_ONBOARDING_COMPLETED_DESCRIPTION)
                    ))
                    .build())
            ));
    }

    @Test
    void callbackByFormPost() throws Exception {
        given(oAuthService.loginByAuthorizationCode(LoginType.APPLE, "authorization-code"))
            .willReturn(TokenDto.of(1L, "access-token", "refresh-token", true, true, true));

        mockMvc.perform(post("/api/v1/oauth/{loginType}/callback", "apple")
                .queryParam("code", "authorization-code"))
            .andExpect(status().isOk())
            .andDo(document("oauth-callback-form-post",
                resource(ResourceSnippetParameters.builder()
                    .tag("OAuth")
                    .summary("OAuth callback 로그인(Form POST)")
                    .description("""
                        form_post 방식 authorization code로 로그인하고 서비스 JWT를 발급한다.
                        """)
                    .pathParameters(
                        parameterWithName("loginType").description("로그인 타입: apple")
                    )
                    .queryParameters(
                        parameterWithName("code").description("provider authorization code")
                    )
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.id").type(JsonFieldType.NUMBER).description("회원 id"),
                        fieldWithPath("result.accessToken").type(JsonFieldType.STRING).description("access token"),
                        fieldWithPath("result.refreshToken").type(JsonFieldType.STRING).description("refresh token"),
                        fieldWithPath("result.personalInfoCompleted")
                            .type(JsonFieldType.BOOLEAN)
                            .description("개인 정보 입력 완료 여부"),
                        fieldWithPath("result.mainAccessible")
                            .type(JsonFieldType.BOOLEAN)
                            .description(MAIN_ACCESSIBLE_DESCRIPTION),
                        fieldWithPath("result.groupOnboardingCompleted")
                            .type(JsonFieldType.BOOLEAN)
                            .description(GROUP_ONBOARDING_COMPLETED_DESCRIPTION)
                    ))
                    .build())
            ));
    }

    @Test
    void clientLogin() throws Exception {
        given(oAuthService.loginByProviderToken(LoginType.KAKAO, "provider-access-token"))
            .willReturn(TokenDto.of(1L, "access-token", "refresh-token", false));

        mockMvc.perform(get("/api/v1/oauth/client/{loginType}", "kakao")
                .queryParam("accessToken", "provider-access-token"))
            .andExpect(status().isOk())
            .andDo(document("oauth-client-login",
                resource(ResourceSnippetParameters.builder()
                    .tag("OAuth")
                    .summary("클라이언트 소셜 로그인")
                    .description(CLIENT_LOGIN_DESCRIPTION)
                    .pathParameters(
                        parameterWithName("loginType").description("로그인 타입: kakao, apple, google")
                    )
                    .queryParameters(
                        parameterWithName("accessToken")
                            .description("provider token. Kakao/Google은 access token, Apple은 identity token")
                    )
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.id").type(JsonFieldType.NUMBER).description("회원 id"),
                        fieldWithPath("result.accessToken").type(JsonFieldType.STRING).description("access token"),
                        fieldWithPath("result.refreshToken").type(JsonFieldType.STRING).description("refresh token"),
                        fieldWithPath("result.personalInfoCompleted")
                            .type(JsonFieldType.BOOLEAN)
                            .description("개인 정보 입력 완료 여부"),
                        fieldWithPath("result.mainAccessible")
                            .type(JsonFieldType.BOOLEAN)
                            .description(MAIN_ACCESSIBLE_DESCRIPTION),
                        fieldWithPath("result.groupOnboardingCompleted")
                            .type(JsonFieldType.BOOLEAN)
                            .description(GROUP_ONBOARDING_COMPLETED_DESCRIPTION)
                    ))
                    .build())
            ));
    }

    @Test
    void clientLoginUnsupportedType() throws Exception {
        mockMvc.perform(get("/api/v1/oauth/client/{loginType}", "naver")
                .queryParam("accessToken", "provider-access-token"))
            .andExpect(status().isBadRequest())
            .andDo(document("oauth-client-login-unsupported-type",
                resource(ResourceSnippetParameters.builder()
                    .tag("OAuth")
                    .summary("클라이언트 소셜 로그인")
                    .description(CLIENT_LOGIN_DESCRIPTION)
                    .responseFields(commonResponseFields())
                    .build())
            ));
    }

    @Test
    void clientLoginMissingAccessToken() throws Exception {
        mockMvc.perform(get("/api/v1/oauth/client/{loginType}", "kakao"))
            .andExpect(status().isBadRequest())
            .andDo(document("oauth-client-login-missing-access-token",
                resource(ResourceSnippetParameters.builder()
                    .tag("OAuth")
                    .summary("클라이언트 소셜 로그인")
                    .description(CLIENT_LOGIN_DESCRIPTION)
                    .responseFields(commonResponseFields())
                    .build())
            ));
    }

    @Test
    void clientLoginInvalidOAuthToken() throws Exception {
        given(oAuthService.loginByProviderToken(LoginType.KAKAO, "bad-token"))
            .willThrow(new GeneralException(ErrorStatus.INVALID_OAUTH_TOKEN));

        mockMvc.perform(get("/api/v1/oauth/client/{loginType}", "kakao")
                .queryParam("accessToken", "bad-token"))
            .andExpect(status().isBadRequest())
            .andDo(document("oauth-client-login-invalid-oauth-token",
                resource(ResourceSnippetParameters.builder()
                    .tag("OAuth")
                    .summary("클라이언트 소셜 로그인")
                    .description(CLIENT_LOGIN_DESCRIPTION)
                    .responseFields(commonResponseFields())
                    .build())
            ));
    }

    @Test
    void clientLoginOAuthProfileRequestFailed() throws Exception {
        given(oAuthService.loginByProviderToken(LoginType.GOOGLE, "provider-access-token"))
            .willThrow(new GeneralException(ErrorStatus.OAUTH_PROFILE_REQUEST_FAILED));

        mockMvc.perform(get("/api/v1/oauth/client/{loginType}", "google")
                .queryParam("accessToken", "provider-access-token"))
            .andExpect(status().isBadRequest())
            .andDo(document("oauth-client-login-profile-request-failed",
                resource(ResourceSnippetParameters.builder()
                    .tag("OAuth")
                    .summary("클라이언트 소셜 로그인")
                    .description(CLIENT_LOGIN_DESCRIPTION)
                    .responseFields(commonResponseFields())
                    .build())
            ));
    }

    @Test
    void clientLoginInvalidOAuthProfile() throws Exception {
        given(oAuthService.loginByProviderToken(LoginType.APPLE, "identity-token"))
            .willThrow(new GeneralException(ErrorStatus.INVALID_OAUTH_PROFILE));

        mockMvc.perform(get("/api/v1/oauth/client/{loginType}", "apple")
                .queryParam("accessToken", "identity-token"))
            .andExpect(status().isBadRequest())
            .andDo(document("oauth-client-login-invalid-oauth-profile",
                resource(ResourceSnippetParameters.builder()
                    .tag("OAuth")
                    .summary("클라이언트 소셜 로그인")
                    .description(CLIENT_LOGIN_DESCRIPTION)
                    .responseFields(commonResponseFields())
                    .build())
            ));
    }
}
