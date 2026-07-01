package cmc.mody.common.presentation;

import static cmc.mody.docs.ApiDocumentUtils.commonResponseFields;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cmc.mody.auth.application.token.TokenProvider;
import cmc.mody.auth.presentation.support.CurrentMemberArgumentResolver;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.config.WebConfig;
import cmc.mody.common.upload.UploadService;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UploadController.class)
@AutoConfigureRestDocs
@Import(WebConfig.class)
class UploadControllerDocsTest {
    private static final String UPLOAD_DESCRIPTION = """
        인증 회원이 이미지 업로드용 presigned URL과 서버 API에 전달할 imageKey를 발급받는다.

        발생 가능한 예외 코드:
        - AUTH401: Authorization 헤더가 없거나 비어있음
        - AUTH402: Bearer 뒤 JWT 값이 비어있음
        - AUTH403: JWT 형식이 올바르지 않거나 refresh token을 사용함
        - AUTH404: 만료된 JWT
        - AUTH405: 지원하지 않는 JWT
        - COMMON4000: domain 또는 fileName query parameter 누락
        - UPLOAD301: 지원하지 않는 업로드 도메인
        - UPLOAD302: 지원하지 않는 파일 확장자
        - UPLOAD303: 업로드 스토리지 설정 누락
        - UPLOAD304: 업로드 URL 발급 실패
        """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UploadService uploadService;

    @MockitoBean
    private TokenProvider tokenProvider;

    @TestConfiguration
    static class CurrentMemberTestConfig {
        @Bean
        CurrentMemberArgumentResolver currentMemberArgumentResolver(TokenProvider tokenProvider) {
            return new CurrentMemberArgumentResolver(tokenProvider);
        }
    }

    @Test
    void createPresignedUrl() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(uploadService.createPresignedUrl(1L, "record", "meal.jpg"))
            .willReturn(new UploadService.PresignedUrlResult(
                "https://storage.example.com/records/1/2026/06/123.jpg?expiresIn=300",
                "records/1/2026/06/123.jpg",
                300L
            ));

        mockMvc.perform(post("/api/v1/uploads/presigned-url?domain=record&fileName=meal.jpg")
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isOk())
            .andDo(document("upload-presigned-url",
                resource(ResourceSnippetParameters.builder()
                    .tag("Upload")
                    .summary("Presigned URL 생성")
                    .description(UPLOAD_DESCRIPTION)
                    .queryParameters(
                        parameterWithName("domain")
                            .description("업로드 도메인: record, profile, weekly-challenge"),
                        parameterWithName("fileName").description("원본 파일명")
                    )
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.presignedUrl").type(JsonFieldType.STRING).description("업로드 URL"),
                        fieldWithPath("result.imageKey")
                            .type(JsonFieldType.STRING)
                            .description("서버 API에 전달할 이미지 key"),
                        fieldWithPath("result.expiresInSeconds")
                            .type(JsonFieldType.NUMBER)
                            .description("만료 시간 초")
                    ))
                    .build())
            ));
    }

    @Test
    void createPresignedUrlWithoutAuthorization() throws Exception {
        mockMvc.perform(post("/api/v1/uploads/presigned-url?domain=record&fileName=meal.jpg"))
            .andExpect(status().isUnauthorized())
            .andDo(documentError("upload-presigned-url-auth-missing"));
    }

    @Test
    void createPresignedUrlWithEmptyToken() throws Exception {
        mockMvc.perform(post("/api/v1/uploads/presigned-url?domain=record&fileName=meal.jpg")
                .header("Authorization", "Bearer "))
            .andExpect(status().isUnauthorized())
            .andDo(documentError("upload-presigned-url-auth-empty-token"));
    }

    @Test
    void createPresignedUrlWithInvalidAuthorizationHeader() throws Exception {
        mockMvc.perform(post("/api/v1/uploads/presigned-url?domain=record&fileName=meal.jpg")
                .header("Authorization", "access-token"))
            .andExpect(status().isUnauthorized())
            .andDo(documentError("upload-presigned-url-auth-invalid-header"));
    }

    @Test
    void createPresignedUrlWithExpiredToken() throws Exception {
        willThrow(new GeneralException(ErrorStatus.EXPIRED_JWT))
            .given(tokenProvider)
            .getMemberIdByAccessToken("expired-token");

        mockMvc.perform(post("/api/v1/uploads/presigned-url?domain=record&fileName=meal.jpg")
                .header("Authorization", "Bearer expired-token"))
            .andExpect(status().isUnauthorized())
            .andDo(documentError("upload-presigned-url-auth-expired-token"));
    }

    @Test
    void createPresignedUrlWithUnsupportedToken() throws Exception {
        willThrow(new GeneralException(ErrorStatus.UNSUPPORTED_JWT))
            .given(tokenProvider)
            .getMemberIdByAccessToken("unsupported-token");

        mockMvc.perform(post("/api/v1/uploads/presigned-url?domain=record&fileName=meal.jpg")
                .header("Authorization", "Bearer unsupported-token"))
            .andExpect(status().isUnauthorized())
            .andDo(documentError("upload-presigned-url-auth-unsupported-token"));
    }

    @Test
    void createPresignedUrlMissingParameter() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);

        mockMvc.perform(post("/api/v1/uploads/presigned-url?domain=record")
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isBadRequest())
            .andDo(documentError("upload-presigned-url-missing-parameter"));
    }

    @Test
    void createPresignedUrlUnsupportedDomain() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.UPLOAD_UNSUPPORTED_DOMAIN))
            .given(uploadService)
            .createPresignedUrl(1L, "unknown", "meal.jpg");

        mockMvc.perform(post("/api/v1/uploads/presigned-url?domain=unknown&fileName=meal.jpg")
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isBadRequest())
            .andDo(documentError("upload-presigned-url-unsupported-domain"));
    }

    @Test
    void createPresignedUrlUnsupportedExtension() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.UPLOAD_UNSUPPORTED_EXTENSION))
            .given(uploadService)
            .createPresignedUrl(1L, "record", "meal.gif");

        mockMvc.perform(post("/api/v1/uploads/presigned-url?domain=record&fileName=meal.gif")
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isBadRequest())
            .andDo(documentError("upload-presigned-url-unsupported-extension"));
    }

    @Test
    void createPresignedUrlStorageConfigInvalid() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.UPLOAD_STORAGE_CONFIG_INVALID))
            .given(uploadService)
            .createPresignedUrl(1L, "record", "meal.jpg");

        mockMvc.perform(post("/api/v1/uploads/presigned-url?domain=record&fileName=meal.jpg")
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isInternalServerError())
            .andDo(documentError("upload-presigned-url-storage-config-invalid"));
    }

    @Test
    void createPresignedUrlIssueFailed() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.UPLOAD_PRESIGNED_URL_ISSUE_FAILED))
            .given(uploadService)
            .createPresignedUrl(1L, "record", "meal.jpg");

        mockMvc.perform(post("/api/v1/uploads/presigned-url?domain=record&fileName=meal.jpg")
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isInternalServerError())
            .andDo(documentError("upload-presigned-url-issue-failed"));
    }

    private RestDocumentationResultHandler documentError(String identifier) {
        return document(identifier,
            resource(ResourceSnippetParameters.builder()
                .tag("Upload")
                .summary("Presigned URL 생성")
                .description(UPLOAD_DESCRIPTION)
                .responseFields(commonResponseFields())
                .build())
        );
    }
}
