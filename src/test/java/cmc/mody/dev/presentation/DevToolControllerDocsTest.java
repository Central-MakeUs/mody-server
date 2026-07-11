package cmc.mody.dev.presentation;

import static cmc.mody.docs.ApiDocumentUtils.commonResponseFields;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.config.WebConfig;
import cmc.mody.dev.application.DevToolService;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DevToolController.class)
@AutoConfigureRestDocs
@Import(WebConfig.class)
@ActiveProfiles("dev")
class DevToolControllerDocsTest {
    private static final String DEV_DESCRIPTION = """
        개발 편의 API는 local/dev 프로필에서만 활성화된다.
        운영 프로필에서는 컨트롤러가 로드되지 않는다.
        클라이언트 개발 편의를 위해 별도 인증 없이 호출할 수 있으며, 대상 회원이 필요한 API는 memberId를 명시적으로 받는다.

        발생 가능한 예외 코드:
        - MEMBER302: 회원 없음
        - GROUP302: 그룹 없음
        - COMMON4003: 입력값 검증 실패
        """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DevToolService devToolService;

    @Test
    void createMockMember() throws Exception {
        given(devToolService.createMockMember(any(DevToolService.CreateMockMemberCommand.class)))
            .willReturn(new DevToolService.MockMemberResult(
                100L,
                "mock-member",
                LocalDate.of(2000, 1, 1),
                BigDecimal.valueOf(60.0),
                true,
                false,
                0
            ));

        mockMvc.perform(post("/api/v1/dev/members/mock")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nickname": "mock-member",
                      "personalInfoCompleted": true,
                      "groupOnboardingCompleted": false
                    }
                    """))
            .andExpect(status().isOk())
            .andDo(document("dev-mock-member-create",
                resource(ResourceSnippetParameters.builder()
                    .tag("Dev")
                    .summary("Mock 회원 생성")
                    .description(DEV_DESCRIPTION)
                    .requestFields(
                        fieldWithPath("nickname")
                            .type(JsonFieldType.STRING)
                            .description("닉네임. 생략하거나 빈 문자열이면 mock-{id suffix}로 자동 생성")
                            .optional(),
                        fieldWithPath("birthDate")
                            .type(JsonFieldType.STRING)
                            .description("생년월일. personalInfoCompleted=true이고 생략하면 2000-01-01")
                            .optional(),
                        fieldWithPath("targetWeightKg")
                            .type(JsonFieldType.NUMBER)
                            .description("목표 체중. personalInfoCompleted=true이고 생략하면 60.0")
                            .optional(),
                        fieldWithPath("personalInfoCompleted")
                            .type(JsonFieldType.BOOLEAN)
                            .description("개인 정보 입력 완료 여부. 생략 시 true")
                            .optional(),
                        fieldWithPath("groupOnboardingCompleted")
                            .type(JsonFieldType.BOOLEAN)
                            .description("그룹 온보딩 완료 여부. 생략 시 false")
                            .optional()
                    )
                    .responseFields(commonResponseFields(mockMemberFields("result")))
                    .build())
            ));
    }

    @Test
    void getMembers() throws Exception {
        given(devToolService.getMembers()).willReturn(new DevToolService.MemberListResult(List.of(
            new DevToolService.MockMemberResult(
                100L,
                "mock-member",
                LocalDate.of(2000, 1, 1),
                BigDecimal.valueOf(60.0),
                true,
                false,
                1
            )
        )));

        mockMvc.perform(get("/api/v1/dev/members"))
            .andExpect(status().isOk())
            .andDo(document("dev-member-list",
                resource(ResourceSnippetParameters.builder()
                    .tag("Dev")
                    .summary("회원 목록 조회")
                    .description(DEV_DESCRIPTION)
                    .responseFields(commonResponseFields(mockMemberFields("result.members[]")))
                    .build())
            ));
    }

    @Test
    void issueToken() throws Exception {
        given(devToolService.issueToken(any(DevToolService.IssueTokenCommand.class)))
            .willReturn(new DevToolService.DevTokenResult(
                100L,
                "access-token",
                "refresh-token",
                true,
                true,
                true,
                1
            ));

        mockMvc.perform(post("/api/v1/dev/auth/tokens")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "memberId": 100
                    }
                    """))
            .andExpect(status().isOk())
            .andDo(document("dev-token-issue",
                resource(ResourceSnippetParameters.builder()
                    .tag("Dev")
                    .summary("회원 id 기반 토큰 발급")
                    .description(DEV_DESCRIPTION)
                    .requestFields(
                        fieldWithPath("memberId").type(JsonFieldType.NUMBER).description("토큰을 발급할 회원 id")
                    )
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.memberId").type(JsonFieldType.NUMBER).description("회원 id"),
                        fieldWithPath("result.accessToken").type(JsonFieldType.STRING).description("access token"),
                        fieldWithPath("result.refreshToken").type(JsonFieldType.STRING).description("refresh token. DB에 저장됨"),
                        fieldWithPath("result.personalInfoCompleted")
                            .type(JsonFieldType.BOOLEAN)
                            .description("개인 정보 입력 완료 여부"),
                        fieldWithPath("result.groupOnboardingCompleted")
                            .type(JsonFieldType.BOOLEAN)
                            .description("그룹 온보딩 완료 경험 여부"),
                        fieldWithPath("result.mainAccessible")
                            .type(JsonFieldType.BOOLEAN)
                            .description("메인 화면 진입 가능 여부"),
                        fieldWithPath("result.joinedGroupCount")
                            .type(JsonFieldType.NUMBER)
                            .description("현재 참여 중인 그룹 수")
                    ))
                    .build())
            ));
    }

    @Test
    void sendTestPush() throws Exception {
        given(devToolService.sendTestPush(any(DevToolService.TestPushCommand.class)))
            .willReturn(new DevToolService.TestPushResult(true, false));

        mockMvc.perform(post("/api/v1/dev/notifications/test-push")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fcmToken": "fcm-token",
                      "title": "테스트 알림",
                      "body": "개발 테스트 본문"
                    }
                    """))
            .andExpect(status().isOk())
            .andDo(document("dev-notification-test-push",
                resource(ResourceSnippetParameters.builder()
                    .tag("Dev")
                    .summary("FCM 테스트 푸시 발송")
                    .description(DEV_DESCRIPTION)
                    .requestFields(
                        fieldWithPath("fcmToken").type(JsonFieldType.STRING).description("발송 대상 FCM 토큰"),
                        fieldWithPath("title").type(JsonFieldType.STRING).description("푸시 제목"),
                        fieldWithPath("body").type(JsonFieldType.STRING).description("푸시 본문")
                    )
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.fcmEnabled")
                            .type(JsonFieldType.BOOLEAN)
                            .description("현재 FCM 발송 활성화 여부"),
                        fieldWithPath("result.invalidToken")
                            .type(JsonFieldType.BOOLEAN)
                            .description("FCM이 유효하지 않은 토큰으로 판단했는지 여부")
                    ))
                    .build())
            ));
    }

    @Test
    void createInboxNotification() throws Exception {
        given(devToolService.createInboxNotification(any(Long.class), any(DevToolService.InboxNotificationCommand.class)))
            .willReturn(new DevToolService.InboxNotificationResult(200L));

        mockMvc.perform(post("/api/v1/dev/notifications/inbox-test")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "memberId": 100,
                      "title": "테스트 알림",
                      "body": "알림함 테스트 본문"
                    }
                    """))
            .andExpect(status().isOk())
            .andDo(document("dev-notification-inbox-create",
                resource(ResourceSnippetParameters.builder()
                    .tag("Dev")
                    .summary("알림함 테스트 알림 생성")
                    .description(DEV_DESCRIPTION)
                    .requestFields(
                        fieldWithPath("memberId").type(JsonFieldType.NUMBER).description("알림을 받을 회원 id"),
                        fieldWithPath("title").type(JsonFieldType.STRING).description("알림 제목"),
                        fieldWithPath("body").type(JsonFieldType.STRING).description("알림 본문")
                    )
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.notificationId").type(JsonFieldType.NUMBER).description("생성된 알림 id")
                    ))
                    .build())
            ));
    }

    @Test
    void getMemberState() throws Exception {
        given(devToolService.getMe(100L)).willReturn(new DevToolService.DevMeResult(
            100L,
            "mock-member",
            true,
            true,
            true,
            1,
            List.of(new DevToolService.DevGroupResult(10L, "모디 그룹", "ABCD2345"))
        ));

        mockMvc.perform(get("/api/v1/dev/members/{memberId}/state", 100L))
            .andExpect(status().isOk())
            .andDo(document("dev-member-state",
                resource(ResourceSnippetParameters.builder()
                    .tag("Dev")
                    .summary("회원 앱 상태 조회")
                    .description(DEV_DESCRIPTION)
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.memberId").type(JsonFieldType.NUMBER).description("회원 id"),
                        fieldWithPath("result.nickname").type(JsonFieldType.STRING).description("닉네임"),
                        fieldWithPath("result.personalInfoCompleted")
                            .type(JsonFieldType.BOOLEAN)
                            .description("개인 정보 입력 완료 여부"),
                        fieldWithPath("result.groupOnboardingCompleted")
                            .type(JsonFieldType.BOOLEAN)
                            .description("그룹 온보딩 완료 경험 여부"),
                        fieldWithPath("result.mainAccessible")
                            .type(JsonFieldType.BOOLEAN)
                            .description("메인 화면 진입 가능 여부"),
                        fieldWithPath("result.joinedGroupCount").type(JsonFieldType.NUMBER).description("참여 그룹 수"),
                        fieldWithPath("result.groups[].groupId").type(JsonFieldType.NUMBER).description("그룹 id"),
                        fieldWithPath("result.groups[].name").type(JsonFieldType.STRING).description("그룹명"),
                        fieldWithPath("result.groups[].code").type(JsonFieldType.STRING).description("그룹 코드")
                    ))
                    .build())
            ));
    }

    @Test
    void getGroups() throws Exception {
        given(devToolService.getGroups()).willReturn(new DevToolService.DevGroupListResult(List.of(
            new DevToolService.DevGroupSummaryResult(10L, "모디 그룹", "ABCD2345", 3)
        )));

        mockMvc.perform(get("/api/v1/dev/groups"))
            .andExpect(status().isOk())
            .andDo(document("dev-group-list",
                resource(ResourceSnippetParameters.builder()
                    .tag("Dev")
                    .summary("그룹 목록 조회")
                    .description(DEV_DESCRIPTION)
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.groups[].groupId").type(JsonFieldType.NUMBER).description("그룹 id"),
                        fieldWithPath("result.groups[].name").type(JsonFieldType.STRING).description("그룹명"),
                        fieldWithPath("result.groups[].code").type(JsonFieldType.STRING).description("그룹 코드"),
                        fieldWithPath("result.groups[].memberCount").type(JsonFieldType.NUMBER).description("참여 인원 수")
                    ))
                    .build())
            ));
    }

    @Test
    void getGroup() throws Exception {
        given(devToolService.getGroup(10L)).willReturn(new DevToolService.DevGroupDetailResult(
            10L,
            "모디 그룹",
            "ABCD2345",
            1,
            List.of(new DevToolService.DevGroupMemberResult(
                100L,
                "mock-member",
                "profiles/mock.jpg",
                LocalDateTime.of(2026, 7, 6, 12, 0)
            ))
        ));

        mockMvc.perform(get("/api/v1/dev/groups/{groupId}", 10L))
            .andExpect(status().isOk())
            .andDo(document("dev-group-detail",
                resource(ResourceSnippetParameters.builder()
                    .tag("Dev")
                    .summary("그룹 상세 조회")
                    .description(DEV_DESCRIPTION)
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.groupId").type(JsonFieldType.NUMBER).description("그룹 id"),
                        fieldWithPath("result.name").type(JsonFieldType.STRING).description("그룹명"),
                        fieldWithPath("result.code").type(JsonFieldType.STRING).description("그룹 코드"),
                        fieldWithPath("result.memberCount").type(JsonFieldType.NUMBER).description("참여 인원 수"),
                        fieldWithPath("result.members[].memberId").type(JsonFieldType.NUMBER).description("회원 id"),
                        fieldWithPath("result.members[].nickname").type(JsonFieldType.STRING).description("그룹 내 표시 닉네임"),
                        fieldWithPath("result.members[].profileImageKey").type(JsonFieldType.STRING).description("프로필 이미지 key"),
                        fieldWithPath("result.members[].joinedAt").type(JsonFieldType.STRING).description("그룹 참여 일시")
                    ))
                    .build())
            ));
    }

    @Test
    void issueTokenMemberNotFound() throws Exception {
        willThrow(new GeneralException(ErrorStatus.MEMBER_NOT_FOUND))
            .given(devToolService)
            .issueToken(any(DevToolService.IssueTokenCommand.class));

        mockMvc.perform(post("/api/v1/dev/auth/tokens")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "memberId": 999
                    }
                    """))
            .andExpect(status().isNotFound())
            .andDo(documentError("dev-token-issue-member-not-found", "회원 id 기반 토큰 발급"));
    }

    @Test
    void getGroupNotFound() throws Exception {
        willThrow(new GeneralException(ErrorStatus.GROUP_NOT_FOUND))
            .given(devToolService)
            .getGroup(999L);

        mockMvc.perform(get("/api/v1/dev/groups/{groupId}", 999L))
            .andExpect(status().isNotFound())
            .andDo(documentError("dev-group-detail-not-found", "그룹 상세 조회"));
    }

    @Test
    void createMockMemberInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/v1/dev/members/mock")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nickname": "123456789012345",
                      "targetWeightKg": 0
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andDo(documentError("dev-mock-member-create-invalid-request", "Mock 회원 생성"));
    }

    @Test
    void getMemberStateMemberNotFound() throws Exception {
        willThrow(new GeneralException(ErrorStatus.MEMBER_NOT_FOUND))
            .given(devToolService)
            .getMe(999L);

        mockMvc.perform(get("/api/v1/dev/members/{memberId}/state", 999L))
            .andExpect(status().isNotFound())
            .andDo(documentError("dev-member-state-member-not-found", "회원 앱 상태 조회"));
    }

    private org.springframework.restdocs.payload.FieldDescriptor[] mockMemberFields(String prefix) {
        return new org.springframework.restdocs.payload.FieldDescriptor[] {
            fieldWithPath(prefix + ".memberId").type(JsonFieldType.NUMBER).description("회원 id"),
            fieldWithPath(prefix + ".nickname").type(JsonFieldType.STRING).description("닉네임"),
            fieldWithPath(prefix + ".birthDate").type(JsonFieldType.STRING).description("생년월일").optional(),
            fieldWithPath(prefix + ".targetWeightKg").type(JsonFieldType.NUMBER).description("목표 체중").optional(),
            fieldWithPath(prefix + ".personalInfoCompleted")
                .type(JsonFieldType.BOOLEAN)
                .description("개인 정보 입력 완료 여부"),
            fieldWithPath(prefix + ".groupOnboardingCompleted")
                .type(JsonFieldType.BOOLEAN)
                .description("그룹 온보딩 완료 경험 여부"),
            fieldWithPath(prefix + ".joinedGroupCount")
                .type(JsonFieldType.NUMBER)
                .description("현재 참여 중인 그룹 수")
        };
    }

    private RestDocumentationResultHandler documentError(String identifier, String summary) {
        return document(identifier,
            resource(ResourceSnippetParameters.builder()
                .tag("Dev")
                .summary(summary)
                .description(DEV_DESCRIPTION)
                .responseFields(commonResponseFields())
                .build())
        );
    }
}
