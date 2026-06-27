package cmc.mody.auth.presentation;

import static cmc.mody.docs.ApiDocumentUtils.commonResponseFields;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureRestDocs
class AuthControllerDocsTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void socialLogin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/social-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "loginType": "KAKAO",
                      "providerAccessToken": "provider-token"
                    }
                    """))
            .andExpect(status().isOk())
            .andDo(document("auth-social-login",
                resource(ResourceSnippetParameters.builder()
                    .tag("Auth")
                    .summary("소셜 로그인")
                    .description("소셜 provider 토큰으로 로그인하고 서비스 JWT를 발급한다.")
                    .requestFields(
                        fieldWithPath("loginType")
                            .type(JsonFieldType.STRING)
                            .description("로그인 타입: KAKAO, APPLE, GOOGLE"),
                        fieldWithPath("providerAccessToken")
                            .type(JsonFieldType.STRING)
                            .description("소셜 provider access token")
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
    void reissue() throws Exception {
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
                    .description("refresh token으로 access token과 refresh token을 재발급한다.")
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
        mockMvc.perform(post("/api/v1/auth/logout"))
            .andExpect(status().isOk())
            .andDo(document("auth-logout",
                resource(ResourceSnippetParameters.builder()
                    .tag("Auth")
                    .summary("로그아웃")
                    .description("현재 로그인 세션을 로그아웃 처리한다.")
                    .responseFields(commonResponseFields())
                    .build())
            ));
    }
}
