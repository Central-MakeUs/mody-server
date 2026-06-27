package cmc.mody.auth.presentation;

import static cmc.mody.docs.ApiDocumentUtils.commonResponseFields;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({AuthController.class, OAuthController.class})
@AutoConfigureRestDocs
class OAuthControllerDocsTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void getRedirectUrl() throws Exception {
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
}
