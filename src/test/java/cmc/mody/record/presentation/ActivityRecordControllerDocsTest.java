package cmc.mody.record.presentation;

import static cmc.mody.docs.ApiDocumentUtils.commonResponseFields;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cmc.mody.auth.application.token.TokenProvider;
import cmc.mody.auth.presentation.support.CurrentMemberArgumentResolver;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.config.WebConfig;
import cmc.mody.record.application.ActivityRecordService;
import cmc.mody.record.application.ActivityRecordService.RecordCreateCommand;
import cmc.mody.record.application.ActivityRecordService.RecordCreateResult;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(ActivityRecordController.class)
@AutoConfigureRestDocs
@Import(WebConfig.class)
class ActivityRecordControllerDocsTest {
    private static final String RECORD_DESCRIPTION = """
        구현된 기록 API는 access token의 회원 id 기준으로 처리한다.
        이미지 파일은 먼저 Presigned URL로 직접 업로드한다.
        이 API에는 발급받은 imageKey만 전달한다.

        발생 가능한 예외 코드:
        - AUTH401: Authorization 헤더가 없거나 비어있음
        - AUTH402: Bearer 뒤 JWT 값이 비어있음
        - AUTH403: JWT 형식이 올바르지 않거나 refresh token을 사용함
        - AUTH404: 만료된 JWT
        - AUTH405: 지원하지 않는 JWT
        - MEMBER302: 토큰의 회원 id에 해당하는 회원 없음
        - GROUP302: 그룹 없음
        - GROUP306: 그룹 참여 정보 없음
        - RECORD301: 기록 입력값 검증 실패
        """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActivityRecordService activityRecordService;

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
    void getActivityCalendar() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(activityRecordService.getActivityCalendar(eq(1L), eq(1L), any()))
            .willReturn(new ActivityRecordService.ActivityCalendarResult(List.of(
                new ActivityRecordService.ActivityDayResult(java.time.LocalDate.of(2026, 6, 1), false, false),
                new ActivityRecordService.ActivityDayResult(java.time.LocalDate.of(2026, 6, 27), true, true)
            )));

        mockMvc.perform(get("/api/v1/groups/{groupId}/activities/calendar", 1L)
                .header("Authorization", "Bearer access-token")
                .param("yearMonth", "2026-06"))
            .andExpect(status().isOk())
            .andDo(document("activity-calendar",
                resource(ResourceSnippetParameters.builder()
                    .tag("Feed")
                    .summary("월/주차별 활동 유무 조회")
                    .description("월 단위로 식사/운동 기록 존재 여부를 조회한다.")
                    .queryParameters(parameterWithName("yearMonth").description("조회 월, yyyy-MM"))
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.days[].date").type(JsonFieldType.STRING).description("날짜"),
                        fieldWithPath("result.days[].mealRecorded")
                            .type(JsonFieldType.BOOLEAN)
                            .description("식사 기록 여부"),
                        fieldWithPath("result.days[].exerciseRecorded")
                            .type(JsonFieldType.BOOLEAN)
                            .description("운동 기록 여부")
                    ))
                    .build())
            ));
    }

    @Test
    void getRecords() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(activityRecordService.getRecords(eq(1L), eq(1L), any(), any(), eq(20)))
            .willReturn(new ActivityRecordService.RecordCursorResult(
                List.of(new ActivityRecordService.RecordSummaryResult(
                    10L,
                    cmc.mody.record.domain.RecordType.MEAL,
                    1L,
                    "민석",
                    "https://storage.example.com/profiles/member-1.jpg",
                    java.time.LocalTime.of(12, 30),
                    "샐러드",
                    null,
                    null,
                    "https://storage.example.com/records/1/2026/07/meal.jpg"
                )),
                null,
                false
            ));

        mockMvc.perform(get("/api/v1/groups/{groupId}/records", 1L)
                .header("Authorization", "Bearer access-token")
                .param("date", "2026-06-27")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andDo(document("record-list",
                resource(ResourceSnippetParameters.builder()
                    .tag("Feed")
                    .summary("날짜별 식사/운동 기록 조회")
                    .description("날짜별 기록을 커서 기반으로 조회한다.")
                    .queryParameters(
                        parameterWithName("date").description("조회 날짜, yyyy-MM-dd"),
                        parameterWithName("cursor").optional().description("다음 페이지 커서"),
                        parameterWithName("size").description("조회 개수")
                    )
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.records[].recordId").type(JsonFieldType.NUMBER).description("기록 id"),
                        fieldWithPath("result.records[].recordType")
                            .type(JsonFieldType.STRING)
                            .description("기록 타입: MEAL, EXERCISE"),
                        fieldWithPath("result.records[].memberId")
                            .type(JsonFieldType.NUMBER)
                            .description("작성자 id"),
                        fieldWithPath("result.records[].nickname")
                            .type(JsonFieldType.STRING)
                            .description("작성자 닉네임"),
                        fieldWithPath("result.records[].profileImageUrl")
                            .type(JsonFieldType.STRING)
                            .description("작성자 프로필 이미지"),
                        fieldWithPath("result.records[].recordedTime")
                            .type(JsonFieldType.STRING)
                            .description("기록 시간"),
                        fieldWithPath("result.records[].menu").type(JsonFieldType.STRING).description("메뉴명"),
                        fieldWithPath("result.records[].exerciseDurationMinutes")
                            .type(JsonFieldType.NULL)
                            .description("운동 시간(분). 식사 기록이면 null"),
                        fieldWithPath("result.records[].exerciseName")
                            .type(JsonFieldType.NULL)
                            .description("운동명. 식사 기록이면 null"),
                        fieldWithPath("result.records[].imageUrl")
                            .type(JsonFieldType.STRING)
                            .description("기록 이미지 URL"),
                        fieldWithPath("result.nextCursor").type(JsonFieldType.NULL).description("다음 커서"),
                        fieldWithPath("result.hasNext")
                            .type(JsonFieldType.BOOLEAN)
                            .description("다음 페이지 존재 여부")
                    ))
                    .build())
            ));
    }

    @Test
    void getActivityCalendarWithoutAuthorization() throws Exception {
        mockMvc.perform(get("/api/v1/groups/{groupId}/activities/calendar", 1L)
                .param("yearMonth", "2026-06"))
            .andExpect(status().isUnauthorized())
            .andDo(documentError("activity-calendar-auth-missing", "월/주차별 활동 유무 조회"));
    }

    @Test
    void getRecordsWithoutAuthorization() throws Exception {
        mockMvc.perform(get("/api/v1/groups/{groupId}/records", 1L)
                .param("date", "2026-06-27")
                .param("size", "20"))
            .andExpect(status().isUnauthorized())
            .andDo(documentError("record-list-auth-missing", "날짜별 식사/운동 기록 조회"));
    }

    @Test
    void getRecordsMemberNotFound() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.MEMBER_NOT_FOUND))
            .given(activityRecordService)
            .getRecords(eq(1L), eq(1L), any(), any(), eq(20));

        mockMvc.perform(get("/api/v1/groups/{groupId}/records", 1L)
                .header("Authorization", "Bearer access-token")
                .param("date", "2026-06-27")
                .param("size", "20"))
            .andExpect(status().isNotFound())
            .andDo(documentError("record-list-member-not-found", "날짜별 식사/운동 기록 조회"));
    }

    @Test
    void getRecordsGroupNotFound() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.GROUP_NOT_FOUND))
            .given(activityRecordService)
            .getRecords(eq(1L), eq(1L), any(), any(), eq(20));

        mockMvc.perform(get("/api/v1/groups/{groupId}/records", 1L)
                .header("Authorization", "Bearer access-token")
                .param("date", "2026-06-27")
                .param("size", "20"))
            .andExpect(status().isNotFound())
            .andDo(documentError("record-list-group-not-found", "날짜별 식사/운동 기록 조회"));
    }

    @Test
    void getRecordsGroupMemberNotFound() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.GROUP_MEMBER_NOT_FOUND))
            .given(activityRecordService)
            .getRecords(eq(1L), eq(1L), any(), any(), eq(20));

        mockMvc.perform(get("/api/v1/groups/{groupId}/records", 1L)
                .header("Authorization", "Bearer access-token")
                .param("date", "2026-06-27")
                .param("size", "20"))
            .andExpect(status().isNotFound())
            .andDo(documentError("record-list-group-member-not-found", "날짜별 식사/운동 기록 조회"));
    }

    @Test
    void getRecordDetail() throws Exception {
        mockMvc.perform(get("/api/v1/records/{recordId}", 1L))
            .andExpect(status().isOk())
            .andDo(document("record-detail",
                resource(ResourceSnippetParameters.builder()
                    .tag("Feed")
                    .summary("[미구현] 기록 상세 조회")
                    .description("식사/운동 기록 상세와 댓글을 조회한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.recordId").type(JsonFieldType.NUMBER).description("기록 id"),
                        fieldWithPath("result.recordType").type(JsonFieldType.STRING).description("기록 타입"),
                        fieldWithPath("result.memberId").type(JsonFieldType.NUMBER).description("작성자 id"),
                        fieldWithPath("result.nickname").type(JsonFieldType.STRING).description("작성자 닉네임"),
                        fieldWithPath("result.profileImageUrl")
                            .type(JsonFieldType.STRING)
                            .description("작성자 프로필 이미지"),
                        fieldWithPath("result.recordedTime").type(JsonFieldType.STRING).description("기록 시간"),
                        fieldWithPath("result.menu").type(JsonFieldType.STRING).description("메뉴명"),
                        fieldWithPath("result.imageUrl").type(JsonFieldType.STRING).description("기록 이미지 URL"),
                        fieldWithPath("result.comments[].commentId")
                            .type(JsonFieldType.NUMBER)
                            .description("댓글 id"),
                        fieldWithPath("result.comments[].memberId")
                            .type(JsonFieldType.NUMBER)
                            .description("댓글 작성자 id"),
                        fieldWithPath("result.comments[].nickname")
                            .type(JsonFieldType.STRING)
                            .description("댓글 작성자 닉네임"),
                        fieldWithPath("result.comments[].content")
                            .type(JsonFieldType.STRING)
                            .description("댓글 내용")
                    ))
                    .build())
            ));
    }

    @Test
    void createMealRecord() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(activityRecordService.createRecord(eq(1L), any(RecordCreateCommand.class)))
            .willReturn(new RecordCreateResult(10L));

        mockMvc.perform(postCreateRecord())
            .andExpect(status().isCreated())
            .andDo(document("record-create",
                resource(ResourceSnippetParameters.builder()
                    .tag("Feed")
                    .summary("기록 입력")
                    .description(RECORD_DESCRIPTION)
                    .requestFields(
                        fieldWithPath("groupId")
                            .type(JsonFieldType.NUMBER)
                            .optional()
                            .description("그룹 id. 개인 기록이면 null"),
                        fieldWithPath("recordType")
                            .type(JsonFieldType.STRING)
                            .description("기록 타입: MEAL, EXERCISE"),
                        fieldWithPath("imageKey")
                            .type(JsonFieldType.STRING)
                            .description("records 도메인으로 발급받은 업로드 이미지 키"),
                        fieldWithPath("mealTime")
                            .type(JsonFieldType.STRING)
                            .optional()
                            .description("식사 시간. MEAL일 때 필수"),
                        fieldWithPath("menu")
                            .type(JsonFieldType.STRING)
                            .optional()
                            .description("메뉴명. MEAL일 때 필수"),
                        fieldWithPath("exerciseDurationMinutes")
                            .type(JsonFieldType.NUMBER)
                            .optional()
                            .description("운동 시간(분). EXERCISE일 때 필수"),
                        fieldWithPath("exerciseName")
                            .type(JsonFieldType.STRING)
                            .optional()
                            .description("운동명. EXERCISE일 때 필수")
                    )
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.recordId").type(JsonFieldType.NUMBER).description("생성된 기록 id")
                    ))
                    .build())
            ));
    }

    @Test
    void createExerciseRecord() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(activityRecordService.createRecord(eq(1L), any(RecordCreateCommand.class)))
            .willReturn(new RecordCreateResult(11L));

        mockMvc.perform(post("/api/v1/records")
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "groupId": 1,
                      "recordType": "EXERCISE",
                      "imageKey": "records/1/2026/07/4111584723969.jpg",
                      "mealTime": null,
                      "menu": null,
                      "exerciseDurationMinutes": 40,
                      "exerciseName": "러닝"
                    }
                    """))
            .andExpect(status().isCreated())
            .andDo(document("record-create-exercise",
                resource(ResourceSnippetParameters.builder()
                    .tag("Feed")
                    .summary("기록 입력")
                    .description(RECORD_DESCRIPTION)
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.recordId").type(JsonFieldType.NUMBER).description("생성된 기록 id")
                    ))
                    .build())
            ));
    }

    @Test
    void createRecordValidationError() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);

        mockMvc.perform(post("/api/v1/records")
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "groupId": 1,
                      "recordType": "MEAL",
                      "imageKey": "profiles/1/2026/07/profile.jpg",
                      "mealTime": null,
                      "menu": "",
                      "exerciseDurationMinutes": null,
                      "exerciseName": null
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andDo(documentError("record-create-validation-error", "기록 입력"));
    }

    @Test
    void createRecordUnreadableBody() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);

        mockMvc.perform(post("/api/v1/records")
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "groupId": 1,
                      "recordType": "UNKNOWN",
                      "imageKey": "records/1/2026/07/4111584723968.jpg",
                      "mealTime": "12:30",
                      "menu": "샐러드"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andDo(documentError("record-create-unreadable-body", "기록 입력"));
    }

    @Test
    void createRecordMemberNotFound() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.MEMBER_NOT_FOUND))
            .given(activityRecordService)
            .createRecord(eq(1L), any(RecordCreateCommand.class));

        mockMvc.perform(postCreateRecord())
            .andExpect(status().isNotFound())
            .andDo(documentError("record-create-member-not-found", "기록 입력"));
    }

    @Test
    void createRecordGroupNotFound() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.GROUP_NOT_FOUND))
            .given(activityRecordService)
            .createRecord(eq(1L), any(RecordCreateCommand.class));

        mockMvc.perform(postCreateRecord())
            .andExpect(status().isNotFound())
            .andDo(documentError("record-create-group-not-found", "기록 입력"));
    }

    @Test
    void createRecordGroupMemberNotFound() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.GROUP_MEMBER_NOT_FOUND))
            .given(activityRecordService)
            .createRecord(eq(1L), any(RecordCreateCommand.class));

        mockMvc.perform(postCreateRecord())
            .andExpect(status().isNotFound())
            .andDo(documentError("record-create-group-member-not-found", "기록 입력"));
    }

    @Test
    void createRecordWithoutAuthorization() throws Exception {
        mockMvc.perform(postCreateRecordWithoutAuthorization())
            .andExpect(status().isUnauthorized())
            .andDo(documentError("record-create-auth-missing", "기록 입력"));
    }

    @Test
    void createRecordWithEmptyToken() throws Exception {
        mockMvc.perform(postCreateRecordWithoutAuthorization()
                .header("Authorization", "Bearer "))
            .andExpect(status().isUnauthorized())
            .andDo(documentError("record-create-auth-empty-token", "기록 입력"));
    }

    @Test
    void createRecordWithInvalidAuthorizationHeader() throws Exception {
        mockMvc.perform(postCreateRecordWithoutAuthorization()
                .header("Authorization", "access-token"))
            .andExpect(status().isUnauthorized())
            .andDo(documentError("record-create-auth-invalid-header", "기록 입력"));
    }

    @Test
    void createRecordWithExpiredToken() throws Exception {
        willThrow(new GeneralException(ErrorStatus.EXPIRED_JWT))
            .given(tokenProvider)
            .getMemberIdByAccessToken("expired-token");

        mockMvc.perform(postCreateRecordWithoutAuthorization()
                .header("Authorization", "Bearer expired-token"))
            .andExpect(status().isUnauthorized())
            .andDo(documentError("record-create-auth-expired-token", "기록 입력"));
    }

    @Test
    void createRecordWithUnsupportedToken() throws Exception {
        willThrow(new GeneralException(ErrorStatus.UNSUPPORTED_JWT))
            .given(tokenProvider)
            .getMemberIdByAccessToken("unsupported-token");

        mockMvc.perform(postCreateRecordWithoutAuthorization()
                .header("Authorization", "Bearer unsupported-token"))
            .andExpect(status().isUnauthorized())
            .andDo(documentError("record-create-auth-unsupported-token", "기록 입력"));
    }

    @Test
    void createComment() throws Exception {
        mockMvc.perform(post("/api/v1/records/{recordId}/comments", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "좋다"
                    }
                    """))
            .andExpect(status().isOk())
            .andDo(document("record-comment-create",
                resource(ResourceSnippetParameters.builder()
                    .tag("Feed")
                    .summary("[미구현] 기록 댓글 작성")
                    .description("식사/운동 기록에 댓글을 작성한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.commentId").type(JsonFieldType.NUMBER).description("댓글 id"),
                        fieldWithPath("result.recordId").type(JsonFieldType.NUMBER).description("기록 id")
                    ))
                    .build())
            ));
    }

    private MockHttpServletRequestBuilder postCreateRecord() {
        return postCreateRecordWithoutAuthorization()
            .header("Authorization", "Bearer access-token");
    }

    private MockHttpServletRequestBuilder postCreateRecordWithoutAuthorization() {
        return post("/api/v1/records")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "groupId": 1,
                  "recordType": "MEAL",
                  "imageKey": "records/1/2026/07/4111584723968.jpg",
                  "mealTime": "12:30",
                  "menu": "샐러드",
                  "exerciseDurationMinutes": null,
                  "exerciseName": null
                }
                """);
    }

    private RestDocumentationResultHandler documentError(String identifier, String summary) {
        return document(identifier,
            resource(ResourceSnippetParameters.builder()
                .tag("Feed")
                .summary(summary)
                .description(RECORD_DESCRIPTION)
                .responseFields(commonResponseFields())
                .build())
        );
    }
}
