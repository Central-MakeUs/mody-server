package cmc.mody.report.presentation;

import static cmc.mody.docs.ApiDocumentDescriptions.AUTHENTICATED_API;
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
import cmc.mody.report.application.RecordReportService;
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

@WebMvcTest(RecordReportController.class)
@AutoConfigureRestDocs
@Import(WebConfig.class)
class RecordReportControllerDocsTest {
    private static final String REPORT_DESCRIPTION = """
        피드 기록을 신고한다.

        %s

        신고 사유는 받지 않는다.

        발생 가능한 예외 코드:
        - AUTH401: Authorization 헤더가 없거나 비어있음
        - AUTH402: Bearer 뒤 JWT 값이 비어있음
        - AUTH403: JWT 형식이 올바르지 않거나 refresh token을 사용함
        - AUTH404: 만료된 JWT
        - AUTH405: 지원하지 않는 JWT
        - MEMBER302: 토큰의 회원 id에 해당하는 회원 없음
        - GROUP302: 그룹 없음
        - GROUP306: 그룹 참여 정보 없음
        - RECORD302: 기록 없음 또는 접근할 수 없는 기록
        """.formatted(AUTHENTICATED_API);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecordReportService recordReportService;

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
    void reportRecord() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(recordReportService.reportRecord(1L, 10L, 100L))
            .willReturn(new RecordReportService.RecordReportResult(300L, 100L));

        mockMvc.perform(post("/api/v1/groups/{groupId}/records/{recordId}/report", 10L, 100L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isCreated())
            .andDo(document("record-report",
                resource(ResourceSnippetParameters.builder()
                    .tag("Feed")
                    .summary("피드 기록 신고")
                    .description(REPORT_DESCRIPTION)
                    .pathParameters(
                        parameterWithName("groupId").description("신고할 기록이 노출된 그룹 id"),
                        parameterWithName("recordId").description("신고할 피드 기록 id")
                    )
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.reportId").type(JsonFieldType.NUMBER).description("신고 id"),
                        fieldWithPath("result.recordId").type(JsonFieldType.NUMBER).description("신고된 기록 id")
                    ))
                    .build())
            ));
    }

    @Test
    void reportRecordNotFound() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.RECORD_NOT_FOUND))
            .given(recordReportService)
            .reportRecord(1L, 10L, 100L);

        mockMvc.perform(post("/api/v1/groups/{groupId}/records/{recordId}/report", 10L, 100L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isNotFound())
            .andDo(documentError("record-report-record-not-found"));
    }

    @Test
    void reportRecordWithoutAuthorization() throws Exception {
        mockMvc.perform(post("/api/v1/groups/{groupId}/records/{recordId}/report", 10L, 100L))
            .andExpect(status().isUnauthorized())
            .andDo(documentError("record-report-auth-missing"));
    }

    private RestDocumentationResultHandler documentError(String identifier) {
        return document(identifier,
            resource(ResourceSnippetParameters.builder()
                .tag("Feed")
                .summary("피드 기록 신고")
                .description(REPORT_DESCRIPTION)
                .responseFields(commonResponseFields())
                .build())
        );
    }
}
