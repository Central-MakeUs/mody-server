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

import cmc.mody.auth.application.oauth.OAuthService;
import cmc.mody.auth.presentation.dto.TokenDto;
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
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OAuthService oAuthService;

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
                    .description("서버 callback 흐름에서 사용할 provider authorization URL을 조회한다.")
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
            .willReturn(TokenDto.of(1L, "access-token", "refresh-token", true));

        mockMvc.perform(get("/api/v1/oauth/{loginType}/callback", "kakao")
                .queryParam("code", "authorization-code"))
            .andExpect(status().isOk())
            .andDo(document("oauth-callback",
                resource(ResourceSnippetParameters.builder()
                    .tag("OAuth")
                    .summary("OAuth callback 로그인")
                    .description("authorization code로 로그인하고 서비스 JWT를 발급한다.")
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
                        fieldWithPath("result.isNewMember")
                            .type(JsonFieldType.BOOLEAN)
                            .description("신규 회원 여부")
                    ))
                    .build())
            ));
    }

    @Test
    void callbackByFormPost() throws Exception {
        given(oAuthService.loginByAuthorizationCode(LoginType.APPLE, "authorization-code"))
            .willReturn(TokenDto.of(1L, "access-token", "refresh-token", true));

        mockMvc.perform(post("/api/v1/oauth/{loginType}/callback", "apple")
                .queryParam("code", "authorization-code"))
            .andExpect(status().isOk())
            .andDo(document("oauth-callback-form-post",
                resource(ResourceSnippetParameters.builder()
                    .tag("OAuth")
                    .summary("OAuth callback 로그인(Form POST)")
                    .description("form_post 방식 authorization code로 로그인하고 서비스 JWT를 발급한다.")
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
                        fieldWithPath("result.isNewMember")
                            .type(JsonFieldType.BOOLEAN)
                            .description("신규 회원 여부")
                    ))
                    .build())
            ));
    }
}
