package cmc.mody.record.presentation;

import static cmc.mody.docs.ApiDocumentDescriptions.AUTHENTICATED_API;
import static cmc.mody.docs.ApiDocumentDescriptions.CURSOR_PAGING;
import static cmc.mody.docs.ApiDocumentDescriptions.IMAGE_UPLOAD_FLOW;
import static cmc.mody.docs.ApiDocumentDescriptions.RECORD_CREATE_RULES;
import static cmc.mody.docs.ApiDocumentUtils.commonResponseFields;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cmc.mody.auth.application.token.TokenProvider;
import cmc.mody.auth.presentation.support.CurrentMemberArgumentResolver;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.config.WebConfig;
import cmc.mody.record.application.ActivityRecordService;
import cmc.mody.record.application.ActivityRecordService.CommentCreateCommand;
import cmc.mody.record.application.ActivityRecordService.CommentCreateResult;
import cmc.mody.record.application.ActivityRecordService.CommentCursorResult;
import cmc.mody.record.application.ActivityRecordService.CommentResult;
import cmc.mody.record.application.ActivityRecordService.RecordCreateCommand;
import cmc.mody.record.application.ActivityRecordService.RecordCreateResult;
import cmc.mody.record.application.ActivityRecordService.RecordDetailPageResult;
import cmc.mody.record.application.ActivityRecordService.RecordDetailResult;
import cmc.mody.record.domain.RecordType;
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

        %s

        %s

        %s

        %s

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
        - RECORD303: 그룹 id 형식 오류
        - RECORD304: 기록 타입 오류
        - RECORD305: 기록 이미지 키 오류
        - RECORD306: 식사 기록 payload 조합 오류
        - RECORD307: 운동 기록 payload 조합 오류
        - RECORD308: 운동 시간 범위 오류
        - RECORD309: 댓글 내용 오류
        - RECORD302: 기록 없음 또는 접근할 수 없는 기록
        """.formatted(AUTHENTICATED_API, IMAGE_UPLOAD_FLOW, CURSOR_PAGING, RECORD_CREATE_RULES);

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
                    .description("""
                        월 단위로 식사/운동 기록 존재 여부를 조회한다.
                        yearMonth에 포함된 날짜만 응답하며, 클라이언트는 달력 UI의 표시 여부에 사용한다.
                        """)
                    .queryParameters(parameterWithName("yearMonth").description("조회 월, yyyy-MM"))
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.days[].date").type(JsonFieldType.STRING).description("날짜"),
                        fieldWithPath("result.days[].mealRecorded")
                            .type(JsonFieldType.BOOLEAN)
                            .description("해당 날짜에 식사 기록이 1개 이상 있으면 true"),
                        fieldWithPath("result.days[].exerciseRecorded")
                            .type(JsonFieldType.BOOLEAN)
                            .description("해당 날짜에 운동 기록이 1개 이상 있으면 true")
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
                    "https://storage.example.com/records/1/2026/07/meal.jpg",
                    4
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
                    .description("""
                        날짜별 식사/운동 기록을 커서 기반으로 조회한다.
                        그룹에 참여 중인 회원의 활성 기록만 응답하며, recordingStreakDays는 각 작성자의 기준 날짜 연속 기록 일수다.
                        """)
                    .queryParameters(
                        parameterWithName("date").description("조회 날짜, yyyy-MM-dd"),
                        parameterWithName("cursor").optional().description("다음 페이지 커서. 최초 조회 시 생략"),
                        parameterWithName("size").description("조회 개수. 기본 UI 페이지 크기에 맞춰 전달")
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
                        fieldWithPath("result.records[].recordingStreakDays")
                            .type(JsonFieldType.NUMBER)
                            .description("작성자의 기준 날짜 연속 기록 일수. 같은 날짜에 여러 기록이 있어도 1일로 계산"),
                        fieldWithPath("result.nextCursor").type(JsonFieldType.NULL).description("다음 페이지 조회용 커서. 마지막 페이지면 null"),
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
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(activityRecordService.getRecordDetail(1L, 1L, null, 20))
            .willReturn(new RecordDetailPageResult(
                2,
                0,
                List.of(
                    new RecordDetailResult(
                        1L,
                        RecordType.MEAL,
                        1L,
                        "민석",
                        "https://storage.example.com/profiles/member-1.jpg",
                        java.time.LocalTime.of(12, 30),
                        "샐러드",
                        null,
                        null,
                        "https://storage.example.com/records/1/2026/07/meal.jpg"
                    ),
                    new RecordDetailResult(
                        2L,
                        RecordType.EXERCISE,
                        1L,
                        "민석",
                        "https://storage.example.com/profiles/member-1.jpg",
                        java.time.LocalTime.of(20, 0),
                        null,
                        40,
                        "러닝",
                        "https://storage.example.com/records/1/2026/07/exercise.jpg"
                    )
                ),
                2L,
                false
            ));

        mockMvc.perform(get("/api/v1/records/{recordId}", 1L)
                .param("size", "20")
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isOk())
            .andDo(document("record-detail",
                resource(ResourceSnippetParameters.builder()
                    .tag("Feed")
                    .summary("기록 상세 조회")
                    .description("""
                        선택한 기록 작성자의 해당 날짜 기록 목록을 커서 페이징으로 조회한다.
                        앱은 totalCount, currentIndex, records로 닷 인디케이터와 좌우 스와이프 캐러셀을 구성한다.
                        상세 진입 시 현재 로그인 회원 기준의 미확인 기록 기준 시각이 갱신된다.
                        """)
                    .queryParameters(
                        parameterWithName("cursor").optional().description("이전 페이지의 마지막 기록 id (최초 조회 시 생략)"),
                        parameterWithName("size").optional().description("페이지 크기 (기본값: 20)")
                    )
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.totalCount").type(JsonFieldType.NUMBER).description("캐러셀 전체 기록 수"),
                        fieldWithPath("result.currentIndex")
                             .type(JsonFieldType.NUMBER)
                             .description("현재 선택된 기록의 0-based 인덱스. 최초 조회는 요청 recordId가 0번"),
                        fieldWithPath("result.records[].recordId").type(JsonFieldType.NUMBER).description("기록 id"),
                        fieldWithPath("result.records[].recordType")
                             .type(JsonFieldType.STRING)
                             .description("기록 타입: MEAL, EXERCISE"),
                        fieldWithPath("result.records[].memberId").type(JsonFieldType.NUMBER).description("작성자 id"),
                        fieldWithPath("result.records[].nickname").type(JsonFieldType.STRING).description("작성자 닉네임"),
                        fieldWithPath("result.records[].profileImageUrl")
                             .type(JsonFieldType.STRING)
                             .description("작성자 프로필 이미지"),
                        fieldWithPath("result.records[].recordedTime").type(JsonFieldType.STRING).description("기록 시간"),
                        fieldWithPath("result.records[].menu")
                             .type(JsonFieldType.VARIES)
                             .optional()
                             .description("메뉴명. 운동 기록이면 null"),
                        fieldWithPath("result.records[].exerciseDurationMinutes")
                             .type(JsonFieldType.VARIES)
                             .optional()
                             .description("운동 시간(분). 식사 기록이면 null"),
                        fieldWithPath("result.records[].exerciseName")
                             .type(JsonFieldType.VARIES)
                             .optional()
                             .description("운동명. 식사 기록이면 null"),
                        fieldWithPath("result.records[].imageUrl")
                             .type(JsonFieldType.STRING)
                             .description("기록 이미지 URL"),
                        fieldWithPath("result.nextCursor").type(JsonFieldType.NUMBER).optional().description("다음 페이지용 커서 id. 더 이상 없으면 null"),
                        fieldWithPath("result.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부")
                    ))
                    .build())
            ));
    }

    @Test
    void getRecordComments() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(activityRecordService.getRecordComments(1L, 1L, null, 20))
            .willReturn(new CommentCursorResult(
                List.of(
                    new CommentResult(
                        10L,
                        2L,
                        "친구",
                        "https://storage.example.com/profiles/member-2.jpg",
                        "좋다",
                        false
                    ),
                    new CommentResult(
                        11L,
                        1L,
                        "민석",
                        "https://storage.example.com/profiles/member-1.jpg",
                        "고마워",
                        true
                    )
                ),
                11L,
                true
            ));

        mockMvc.perform(get("/api/v1/records/{recordId}/comments", 1L)
                .header("Authorization", "Bearer access-token")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andDo(document("record-comment-list",
                resource(ResourceSnippetParameters.builder()
                    .tag("Feed")
                    .summary("기록 댓글 목록 조회")
                    .description("""
                        기록 댓글을 커서 기반으로 조회한다.
                        isMine은 댓글 말풍선 정렬 등 현재 로그인 회원이 작성한 댓글 UI 처리에 사용한다.
                        """)
                    .queryParameters(
                        parameterWithName("cursor").optional().description("다음 페이지 커서. 최초 조회 시 생략"),
                        parameterWithName("size").description("조회 개수")
                    )
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.comments[].commentId").type(JsonFieldType.NUMBER).description("댓글 id"),
                        fieldWithPath("result.comments[].memberId")
                            .type(JsonFieldType.NUMBER)
                            .description("댓글 작성자 id"),
                        fieldWithPath("result.comments[].nickname")
                            .type(JsonFieldType.STRING)
                            .description("댓글 작성자 닉네임"),
                        fieldWithPath("result.comments[].profileImageUrl")
                            .type(JsonFieldType.STRING)
                            .description("댓글 작성자 프로필 이미지"),
                        fieldWithPath("result.comments[].content")
                            .type(JsonFieldType.STRING)
                            .description("댓글 내용"),
                        fieldWithPath("result.comments[].isMine")
                            .type(JsonFieldType.BOOLEAN)
                            .description("현재 로그인한 회원이 작성한 댓글 여부"),
                        fieldWithPath("result.nextCursor").type(JsonFieldType.NUMBER).description("다음 페이지 조회용 커서"),
                        fieldWithPath("result.hasNext")
                            .type(JsonFieldType.BOOLEAN)
                            .description("다음 페이지 존재 여부")
                    ))
                    .build())
            ));
    }

    @Test
    void getRecordDetailRecordNotFound() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.RECORD_NOT_FOUND))
            .given(activityRecordService)
            .getRecordDetail(anyLong(), anyLong(), any(), anyInt());

        mockMvc.perform(get("/api/v1/records/{recordId}", 1L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isNotFound())
            .andDo(documentError("record-detail-record-not-found", "기록 상세 조회"));
    }

    @Test
    void getRecordDetailMemberNotFound() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.MEMBER_NOT_FOUND))
            .given(activityRecordService)
            .getRecordDetail(anyLong(), anyLong(), any(), anyInt());

        mockMvc.perform(get("/api/v1/records/{recordId}", 1L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isNotFound())
            .andDo(documentError("record-detail-member-not-found", "기록 상세 조회"));
    }

    @Test
    void getRecordDetailGroupNotFound() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.GROUP_NOT_FOUND))
            .given(activityRecordService)
            .getRecordDetail(anyLong(), anyLong(), any(), anyInt());

        mockMvc.perform(get("/api/v1/records/{recordId}", 1L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isNotFound())
            .andDo(documentError("record-detail-group-not-found", "기록 상세 조회"));
    }

    @Test
    void getRecordDetailGroupMemberNotFound() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.GROUP_MEMBER_NOT_FOUND))
            .given(activityRecordService)
            .getRecordDetail(anyLong(), anyLong(), any(), anyInt());

        mockMvc.perform(get("/api/v1/records/{recordId}", 1L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isNotFound())
            .andDo(documentError("record-detail-group-member-not-found", "기록 상세 조회"));
    }

    @Test
    void getRecordDetailWithoutAuthorization() throws Exception {
        mockMvc.perform(get("/api/v1/records/{recordId}", 1L))
            .andExpect(status().isUnauthorized())
            .andDo(documentError("record-detail-auth-missing", "기록 상세 조회"));
    }

    @Test
    void getRecordDetailWithEmptyToken() throws Exception {
        mockMvc.perform(get("/api/v1/records/{recordId}", 1L)
                .header("Authorization", "Bearer "))
            .andExpect(status().isUnauthorized())
            .andDo(documentError("record-detail-auth-empty-token", "기록 상세 조회"));
    }

    @Test
    void getRecordDetailWithInvalidAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/v1/records/{recordId}", 1L)
                .header("Authorization", "access-token"))
            .andExpect(status().isUnauthorized())
            .andDo(documentError("record-detail-auth-invalid-header", "기록 상세 조회"));
    }

    @Test
    void getRecordDetailWithInvalidToken() throws Exception {
        willThrow(new GeneralException(ErrorStatus.INVALID_JWT))
            .given(tokenProvider)
            .getMemberIdByAccessToken("invalid-token");

        mockMvc.perform(get("/api/v1/records/{recordId}", 1L)
                .header("Authorization", "Bearer invalid-token"))
            .andExpect(status().isUnauthorized())
            .andDo(documentError("record-detail-auth-invalid-token", "기록 상세 조회"));
    }

    @Test
    void getRecordDetailWithExpiredToken() throws Exception {
        willThrow(new GeneralException(ErrorStatus.EXPIRED_JWT))
            .given(tokenProvider)
            .getMemberIdByAccessToken("expired-token");

        mockMvc.perform(get("/api/v1/records/{recordId}", 1L)
                .header("Authorization", "Bearer expired-token"))
            .andExpect(status().isUnauthorized())
            .andDo(documentError("record-detail-auth-expired-token", "기록 상세 조회"));
    }

    @Test
    void getRecordDetailWithUnsupportedToken() throws Exception {
        willThrow(new GeneralException(ErrorStatus.UNSUPPORTED_JWT))
            .given(tokenProvider)
            .getMemberIdByAccessToken("unsupported-token");

        mockMvc.perform(get("/api/v1/records/{recordId}", 1L)
                .header("Authorization", "Bearer unsupported-token"))
            .andExpect(status().isUnauthorized())
            .andDo(documentError("record-detail-auth-unsupported-token", "기록 상세 조회"));
    }

    @Test
    void getRecordCommentsRecordNotFound() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.RECORD_NOT_FOUND))
            .given(activityRecordService)
            .getRecordComments(1L, 1L, null, 20);

        mockMvc.perform(get("/api/v1/records/{recordId}/comments", 1L)
                .header("Authorization", "Bearer access-token")
                .param("size", "20"))
            .andExpect(status().isNotFound())
            .andDo(documentError("record-comment-list-record-not-found", "기록 댓글 목록 조회"));
    }

    @Test
    void getRecordCommentsMemberNotFound() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.MEMBER_NOT_FOUND))
            .given(activityRecordService)
            .getRecordComments(1L, 1L, null, 20);

        mockMvc.perform(get("/api/v1/records/{recordId}/comments", 1L)
                .header("Authorization", "Bearer access-token")
                .param("size", "20"))
            .andExpect(status().isNotFound())
            .andDo(documentError("record-comment-list-member-not-found", "기록 댓글 목록 조회"));
    }

    @Test
    void getRecordCommentsGroupNotFound() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.GROUP_NOT_FOUND))
            .given(activityRecordService)
            .getRecordComments(1L, 1L, null, 20);

        mockMvc.perform(get("/api/v1/records/{recordId}/comments", 1L)
                .header("Authorization", "Bearer access-token")
                .param("size", "20"))
            .andExpect(status().isNotFound())
            .andDo(documentError("record-comment-list-group-not-found", "기록 댓글 목록 조회"));
    }

    @Test
    void getRecordCommentsGroupMemberNotFound() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.GROUP_MEMBER_NOT_FOUND))
            .given(activityRecordService)
            .getRecordComments(1L, 1L, null, 20);

        mockMvc.perform(get("/api/v1/records/{recordId}/comments", 1L)
                .header("Authorization", "Bearer access-token")
                .param("size", "20"))
            .andExpect(status().isNotFound())
            .andDo(documentError("record-comment-list-group-member-not-found", "기록 댓글 목록 조회"));
    }

    @Test
    void getRecordCommentsWithoutAuthorization() throws Exception {
        mockMvc.perform(get("/api/v1/records/{recordId}/comments", 1L)
                .param("size", "20"))
            .andExpect(status().isUnauthorized())
            .andDo(documentError("record-comment-list-auth-missing", "기록 댓글 목록 조회"));
    }

    @Test
    void getRecordCommentsWithEmptyToken() throws Exception {
        mockMvc.perform(get("/api/v1/records/{recordId}/comments", 1L)
                .header("Authorization", "Bearer ")
                .param("size", "20"))
            .andExpect(status().isUnauthorized())
            .andDo(documentError("record-comment-list-auth-empty-token", "기록 댓글 목록 조회"));
    }

    @Test
    void getRecordCommentsWithInvalidAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/v1/records/{recordId}/comments", 1L)
                .header("Authorization", "access-token")
                .param("size", "20"))
            .andExpect(status().isUnauthorized())
            .andDo(documentError("record-comment-list-auth-invalid-header", "기록 댓글 목록 조회"));
    }

    @Test
    void getRecordCommentsWithInvalidToken() throws Exception {
        willThrow(new GeneralException(ErrorStatus.INVALID_JWT))
            .given(tokenProvider)
            .getMemberIdByAccessToken("invalid-token");

        mockMvc.perform(get("/api/v1/records/{recordId}/comments", 1L)
                .header("Authorization", "Bearer invalid-token")
                .param("size", "20"))
            .andExpect(status().isUnauthorized())
            .andDo(documentError("record-comment-list-auth-invalid-token", "기록 댓글 목록 조회"));
    }

    @Test
    void getRecordCommentsWithExpiredToken() throws Exception {
        willThrow(new GeneralException(ErrorStatus.EXPIRED_JWT))
            .given(tokenProvider)
            .getMemberIdByAccessToken("expired-token");

        mockMvc.perform(get("/api/v1/records/{recordId}/comments", 1L)
                .header("Authorization", "Bearer expired-token")
                .param("size", "20"))
            .andExpect(status().isUnauthorized())
            .andDo(documentError("record-comment-list-auth-expired-token", "기록 댓글 목록 조회"));
    }

    @Test
    void getRecordCommentsWithUnsupportedToken() throws Exception {
        willThrow(new GeneralException(ErrorStatus.UNSUPPORTED_JWT))
            .given(tokenProvider)
            .getMemberIdByAccessToken("unsupported-token");

        mockMvc.perform(get("/api/v1/records/{recordId}/comments", 1L)
                .header("Authorization", "Bearer unsupported-token")
                .param("size", "20"))
            .andExpect(status().isUnauthorized())
            .andDo(documentError("record-comment-list-auth-unsupported-token", "기록 댓글 목록 조회"));
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
                            .description("record 도메인으로 발급받은 업로드 이미지 key"),
                        fieldWithPath("mealTime")
                            .type(JsonFieldType.STRING)
                            .optional()
                            .description("식사 시간. MEAL일 때 필수"),
	                        fieldWithPath("menu")
	                            .type(JsonFieldType.STRING)
	                            .optional()
	                            .description("메뉴명. MEAL일 때 필수"),
	                        fieldWithPath("exerciseDurationHours")
	                            .type(JsonFieldType.NUMBER)
	                            .optional()
	                            .description("운동 시간(시). EXERCISE일 때 분과 합산해 1분 이상 필요"),
	                        fieldWithPath("exerciseDurationMinutes")
	                            .type(JsonFieldType.NUMBER)
	                            .optional()
	                            .description("운동 시간(분). 0~59"),
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
	                      "exerciseDurationHours": 0,
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
            .andExpect(jsonPath("$.code").value(ErrorStatus.RECORD_IMAGE_INVALID.getCode()))
            .andDo(documentError("record-create-validation-error", "기록 입력"));
    }

    @Test
    void createRecordMealPayloadValidationError() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);

        mockMvc.perform(post("/api/v1/records")
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "groupId": 1,
                      "recordType": "MEAL",
                      "imageKey": "records/1/2026/07/4111584723968.jpg",
                      "mealTime": null,
                      "menu": "",
                      "exerciseDurationHours": null,
                      "exerciseDurationMinutes": null,
                      "exerciseName": null
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorStatus.RECORD_MEAL_PAYLOAD_INVALID.getCode()))
            .andDo(documentError("record-create-meal-payload-invalid", "기록 입력"));
    }

    @Test
    void createRecordExercisePayloadValidationError() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);

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
                      "exerciseDurationHours": 0,
                      "exerciseDurationMinutes": 0,
                      "exerciseName": ""
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorStatus.RECORD_EXERCISE_PAYLOAD_INVALID.getCode()))
            .andDo(documentError("record-create-exercise-payload-invalid", "기록 입력"));
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
	                      "menu": "샐러드",
	                      "exerciseDurationHours": null,
	                      "exerciseDurationMinutes": null,
	                      "exerciseName": null
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
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(activityRecordService.createComment(eq(1L), eq(1L), any(CommentCreateCommand.class)))
            .willReturn(new CommentCreateResult(10L, 1L));

        mockMvc.perform(post("/api/v1/records/{recordId}/comments", 1L)
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "좋다"
                    }
                    """))
            .andExpect(status().isCreated())
            .andDo(document("record-comment-create",
                resource(ResourceSnippetParameters.builder()
                    .tag("Feed")
                    .summary("기록 댓글 작성")
                    .description("""
                        식사/운동 기록에 댓글을 작성한다.
                        댓글 작성자는 access token의 회원이며, 댓글 목록에서는 isMine으로 본인 댓글 여부를 확인한다.
                        """)
                    .requestFields(
                        fieldWithPath("content")
                            .type(JsonFieldType.STRING)
                            .description("댓글 내용. 100자 이하")
                    )
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.commentId").type(JsonFieldType.NUMBER).description("댓글 id"),
                        fieldWithPath("result.recordId").type(JsonFieldType.NUMBER).description("기록 id")
                    ))
                    .build())
            ));
    }

    @Test
    void createCommentValidationError() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);

        mockMvc.perform(post("/api/v1/records/{recordId}/comments", 1L)
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": ""
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(ErrorStatus.RECORD_COMMENT_INVALID.getCode()))
            .andDo(documentError("record-comment-create-validation-error", "기록 댓글 작성"));
    }

    @Test
    void createCommentRecordNotFound() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.RECORD_NOT_FOUND))
            .given(activityRecordService)
            .createComment(eq(1L), eq(1L), any(CommentCreateCommand.class));

        mockMvc.perform(postCreateComment())
            .andExpect(status().isNotFound())
            .andDo(documentError("record-comment-create-record-not-found", "기록 댓글 작성"));
    }

    @Test
    void createCommentGroupMemberNotFound() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.GROUP_MEMBER_NOT_FOUND))
            .given(activityRecordService)
            .createComment(eq(1L), eq(1L), any(CommentCreateCommand.class));

        mockMvc.perform(postCreateComment())
            .andExpect(status().isNotFound())
            .andDo(documentError("record-comment-create-group-member-not-found", "기록 댓글 작성"));
    }

    @Test
    void createCommentWithoutAuthorization() throws Exception {
        mockMvc.perform(postCreateCommentWithoutAuthorization())
            .andExpect(status().isUnauthorized())
            .andDo(documentError("record-comment-create-auth-missing", "기록 댓글 작성"));
    }

    private MockHttpServletRequestBuilder postCreateRecord() {
        return postCreateRecordWithoutAuthorization()
            .header("Authorization", "Bearer access-token");
    }

    private MockHttpServletRequestBuilder postCreateComment() {
        return postCreateCommentWithoutAuthorization()
            .header("Authorization", "Bearer access-token");
    }

    private MockHttpServletRequestBuilder postCreateCommentWithoutAuthorization() {
        return post("/api/v1/records/{recordId}/comments", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "content": "좋다"
                }
                """);
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
                  "exerciseDurationHours": null,
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
