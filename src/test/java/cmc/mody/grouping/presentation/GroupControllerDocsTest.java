package cmc.mody.grouping.presentation;

import static cmc.mody.docs.ApiDocumentDescriptions.AUTHENTICATED_API;
import static cmc.mody.docs.ApiDocumentDescriptions.GROUP_ACCESS_RULES;
import static cmc.mody.docs.ApiDocumentUtils.commonResponseFields;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cmc.mody.auth.application.token.TokenProvider;
import cmc.mody.auth.presentation.support.CurrentMemberArgumentResolver;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.config.WebConfig;
import cmc.mody.grouping.application.GroupService;
import cmc.mody.grouping.application.GroupService.GroupCreateCommand;
import cmc.mody.grouping.application.GroupService.GroupCreateResult;
import cmc.mody.grouping.application.GroupService.GroupJoinCommand;
import cmc.mody.grouping.application.GroupService.GroupJoinResult;
import cmc.mody.grouping.application.GroupService.GroupListResult;
import cmc.mody.grouping.application.GroupService.GroupMemberListResult;
import cmc.mody.grouping.application.GroupService.GroupMemberResult;
import cmc.mody.grouping.application.GroupService.GroupSummaryResult;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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

@WebMvcTest(GroupController.class)
@AutoConfigureRestDocs
@Import(WebConfig.class)
class GroupControllerDocsTest {
    private static final String GROUP_DESCRIPTION = """
        모든 그룹 API는 소셜 로그인 후 발급받은 access token의 회원 id 기준으로 처리한다.

        %s

        %s

        발생 가능한 예외 코드:
        - AUTH401: Authorization 헤더가 없거나 비어있음
        - AUTH402: Bearer 뒤 JWT 값이 비어있음
        - AUTH403: JWT 형식이 올바르지 않거나 refresh token을 사용함
        - AUTH404: 만료된 JWT
        - AUTH405: 지원하지 않는 JWT
        - MEMBER302: 토큰의 회원 id에 해당하는 회원 없음
        - GROUP301: 그룹 입력값 검증 실패
        - GROUP302: 그룹 없음
        - GROUP303: 그룹 코드 생성 실패
        - GROUP304: 참여 가능 그룹 수 초과
        - GROUP305: 이미 참여 중인 그룹
        - GROUP306: 그룹 참여 정보 없음
        - GROUP307: 그룹 최대 인원 초과
        """.formatted(AUTHENTICATED_API, GROUP_ACCESS_RULES);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GroupService groupService;

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
    void generateGroupCode() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(groupService.generateCode(1L)).willReturn(new GroupService.GroupCodeResult("ABC123"));

        mockMvc.perform(get("/api/v1/groups/code")
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isOk())
            .andDo(document("group-code-generate",
                resource(ResourceSnippetParameters.builder()
                    .tag("Group")
                    .summary("랜덤 그룹 코드 생성")
                    .description(GROUP_DESCRIPTION)
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.code")
                            .type(JsonFieldType.STRING)
                            .description("랜덤 그룹 코드. 그룹 생성 전에 미리보기/초대 코드 노출에 사용할 수 있음")
                    ))
                    .build())
            ));
    }

    @Test
    void createGroup() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(groupService.createGroup(eq(1L), any(GroupCreateCommand.class)))
            .willReturn(new GroupCreateResult(10L, "ABC123", "모디 그룹"));

        mockMvc.perform(post("/api/v1/groups")
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "모디 그룹"
                    }
                    """))
            .andExpect(status().isCreated())
            .andDo(document("group-create",
                resource(ResourceSnippetParameters.builder()
                    .tag("Group")
                    .summary("그룹 생성")
                    .description(GROUP_DESCRIPTION)
                    .requestFields(
                        fieldWithPath("name")
                            .type(JsonFieldType.STRING)
                            .description("그룹명. 최대 30자이며 실제 앱 화면에 노출되는 이름")
                    )
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.groupId").type(JsonFieldType.NUMBER).description("그룹 id"),
                        fieldWithPath("result.code")
                            .type(JsonFieldType.STRING)
                            .description("그룹 초대 코드. 다른 회원의 그룹 참여에 사용"),
                        fieldWithPath("result.name").type(JsonFieldType.STRING).description("그룹명")
                    ))
                    .build())
            ));
    }

    @Test
    void joinGroup() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(groupService.joinGroup(eq(1L), any(GroupJoinCommand.class)))
            .willReturn(new GroupJoinResult(10L, "ABC123", "모디 그룹", 4));

        mockMvc.perform(post("/api/v1/groups/join")
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "ABC123"
                    }
                    """))
            .andExpect(status().isOk())
            .andDo(document("group-join",
                resource(ResourceSnippetParameters.builder()
                    .tag("Group")
                    .summary("그룹 참여")
                    .description(GROUP_DESCRIPTION)
                    .requestFields(
                        fieldWithPath("code")
                            .type(JsonFieldType.STRING)
                            .description("그룹 코드. 영문/숫자 6자리")
                    )
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.groupId").type(JsonFieldType.NUMBER).description("그룹 id"),
                        fieldWithPath("result.code").type(JsonFieldType.STRING).description("그룹 코드"),
                        fieldWithPath("result.name").type(JsonFieldType.STRING).description("그룹명"),
                        fieldWithPath("result.memberCount")
                            .type(JsonFieldType.NUMBER)
                            .description("그룹 참여 인원. 최대 12명")
                    ))
                    .build())
            ));
    }

    @Test
    void getMyGroups() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(groupService.getMyGroups(1L))
            .willReturn(new GroupListResult(List.of(
                new GroupSummaryResult(10L, "모디 그룹", "ABC123", 4)
            )));

        mockMvc.perform(get("/api/v1/groups")
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isOk())
            .andDo(document("group-list",
                resource(ResourceSnippetParameters.builder()
                    .tag("Group")
                    .summary("내 그룹 목록")
                    .description(GROUP_DESCRIPTION)
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.groups[].groupId").type(JsonFieldType.NUMBER).description("그룹 id"),
                        fieldWithPath("result.groups[].name").type(JsonFieldType.STRING).description("그룹명"),
                        fieldWithPath("result.groups[].code").type(JsonFieldType.STRING).description("그룹 코드"),
                        fieldWithPath("result.groups[].memberCount")
                            .type(JsonFieldType.NUMBER)
                            .description("그룹 참여 인원. 최대 12명")
                    ))
                    .build())
            ));
    }

    @Test
    void getGroupMembers() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(groupService.getGroupMembers(1L, 10L))
            .willReturn(new GroupMemberListResult(List.of(
                new GroupMemberResult(1L, "민석", "profiles/member-1.jpg", 0),
                new GroupMemberResult(2L, "친구", "profiles/member-2.jpg", 3)
            )));

        mockMvc.perform(get("/api/v1/groups/{groupId}/members", 10L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isOk())
            .andDo(document("group-member-list",
                resource(ResourceSnippetParameters.builder()
                    .tag("Group")
                    .summary("그룹 내 인원 조회")
                    .description(GROUP_DESCRIPTION)
                    .pathParameters(
                        parameterWithName("groupId").description("그룹 id")
                    )
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.members[].memberId").type(JsonFieldType.NUMBER).description("회원 id"),
                        fieldWithPath("result.members[].nickname").type(JsonFieldType.STRING).description("닉네임"),
                        fieldWithPath("result.members[].profileImageUrl")
                            .type(JsonFieldType.STRING)
                            .description("프로필 이미지 URL"),
                        fieldWithPath("result.members[].unreadRecordCount")
                            .type(JsonFieldType.NUMBER)
                            .description("현재 로그인 회원이 해당 구성원의 기록 상세에 마지막으로 진입한 이후 올라온 기록 수. 본인은 0")
                    ))
                    .build())
            ));
    }

    @Test
    void leaveGroup() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);

        mockMvc.perform(delete("/api/v1/groups/{groupId}/members/me", 10L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isOk())
            .andDo(document("group-leave",
                resource(ResourceSnippetParameters.builder()
                    .tag("Group")
                    .summary("그룹 나가기")
                    .description(GROUP_DESCRIPTION)
                    .pathParameters(
                        parameterWithName("groupId").description("그룹 id")
                    )
                    .responseFields(commonResponseFields())
                    .build())
            ));
    }

    @Test
    void createGroupValidationError() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);

        mockMvc.perform(post("/api/v1/groups")
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": ""
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andDo(document("group-create-validation-error",
                resource(ResourceSnippetParameters.builder()
                    .tag("Group")
                    .summary("그룹 생성")
                    .description(GROUP_DESCRIPTION)
                    .responseFields(commonResponseFields())
                    .build())
            ));
    }

    @Test
    void createGroupCodeGenerationFailed() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.GROUP_CODE_GENERATION_FAILED))
            .given(groupService)
            .createGroup(eq(1L), any(GroupCreateCommand.class));

        mockMvc.perform(post("/api/v1/groups")
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "모디 그룹"
                    }
                    """))
            .andExpect(status().isConflict())
            .andDo(documentError("group-create-code-generation-failed", "그룹 생성"));
    }

    @Test
    void createGroupLimitExceeded() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.GROUP_LIMIT_EXCEEDED))
            .given(groupService)
            .createGroup(eq(1L), any(GroupCreateCommand.class));

        mockMvc.perform(post("/api/v1/groups")
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "모디 그룹"
                    }
                    """))
            .andExpect(status().isConflict())
            .andDo(documentError("group-create-limit-exceeded", "그룹 생성"));
    }

    @Test
    void joinGroupValidationError() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);

        mockMvc.perform(post("/api/v1/groups/join")
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "ABC"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andDo(documentError("group-join-validation-error", "그룹 참여"));
    }

    @Test
    void joinGroupNotFound() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.GROUP_NOT_FOUND))
            .given(groupService)
            .joinGroup(eq(1L), any(GroupJoinCommand.class));

        mockMvc.perform(post("/api/v1/groups/join")
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "ABC123"
                    }
                    """))
            .andExpect(status().isNotFound())
            .andDo(document("group-join-not-found",
                resource(ResourceSnippetParameters.builder()
                    .tag("Group")
                    .summary("그룹 참여")
                    .description(GROUP_DESCRIPTION)
                    .responseFields(commonResponseFields())
                    .build())
            ));
    }

    @Test
    void joinGroupLimitExceeded() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.GROUP_LIMIT_EXCEEDED))
            .given(groupService)
            .joinGroup(eq(1L), any(GroupJoinCommand.class));

        mockMvc.perform(post("/api/v1/groups/join")
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "ABC123"
                    }
                    """))
            .andExpect(status().isConflict())
            .andDo(document("group-join-limit-exceeded",
                resource(ResourceSnippetParameters.builder()
                    .tag("Group")
                    .summary("그룹 참여")
                    .description(GROUP_DESCRIPTION)
                    .responseFields(commonResponseFields())
                    .build())
            ));
    }

    @Test
    void joinGroupAlreadyJoined() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.GROUP_ALREADY_JOINED))
            .given(groupService)
            .joinGroup(eq(1L), any(GroupJoinCommand.class));

        mockMvc.perform(post("/api/v1/groups/join")
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "ABC123"
                    }
                    """))
            .andExpect(status().isConflict())
            .andDo(document("group-join-already-joined",
                resource(ResourceSnippetParameters.builder()
                    .tag("Group")
                    .summary("그룹 참여")
                    .description(GROUP_DESCRIPTION)
                    .responseFields(commonResponseFields())
                    .build())
            ));
    }

    @Test
    void joinGroupCapacityExceeded() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.GROUP_CAPACITY_EXCEEDED))
            .given(groupService)
            .joinGroup(eq(1L), any(GroupJoinCommand.class));

        mockMvc.perform(post("/api/v1/groups/join")
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "ABC123"
                    }
                    """))
            .andExpect(status().isConflict())
            .andDo(document("group-join-capacity-exceeded",
                resource(ResourceSnippetParameters.builder()
                    .tag("Group")
                    .summary("그룹 참여")
                    .description(GROUP_DESCRIPTION)
                    .responseFields(commonResponseFields())
                    .build())
            ));
    }

    @Test
    void generateGroupCodeGenerationFailed() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.GROUP_CODE_GENERATION_FAILED))
            .given(groupService)
            .generateCode(1L);

        mockMvc.perform(get("/api/v1/groups/code")
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isConflict())
            .andDo(documentError("group-code-generate-generation-failed", "랜덤 그룹 코드 생성"));
    }

    @Test
    void getMyGroupsGroupNotFound() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.GROUP_NOT_FOUND))
            .given(groupService)
            .getMyGroups(1L);

        mockMvc.perform(get("/api/v1/groups")
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isNotFound())
            .andDo(documentError("group-list-group-not-found", "내 그룹 목록"));
    }

    @Test
    void getGroupMembersGroupNotFound() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.GROUP_NOT_FOUND))
            .given(groupService)
            .getGroupMembers(1L, 10L);

        mockMvc.perform(get("/api/v1/groups/{groupId}/members", 10L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isNotFound())
            .andDo(documentError("group-member-list-group-not-found", "그룹 내 인원 조회"));
    }

    @Test
    void getGroupMembersMemberNotFound() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.GROUP_MEMBER_NOT_FOUND))
            .given(groupService)
            .getGroupMembers(1L, 10L);

        mockMvc.perform(get("/api/v1/groups/{groupId}/members", 10L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isNotFound())
            .andDo(documentError("group-member-list-member-not-found", "그룹 내 인원 조회"));
    }

    @Test
    void leaveGroupMemberNotFound() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.GROUP_MEMBER_NOT_FOUND))
            .given(groupService)
            .leaveGroup(1L, 10L);

        mockMvc.perform(delete("/api/v1/groups/{groupId}/members/me", 10L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isNotFound())
            .andDo(document("group-leave-member-not-found",
                resource(ResourceSnippetParameters.builder()
                    .tag("Group")
                    .summary("그룹 나가기")
                    .description(GROUP_DESCRIPTION)
                    .responseFields(commonResponseFields())
                    .build())
            ));
    }

    @ParameterizedTest(name = "{0} Authorization 헤더 없음")
    @MethodSource("authenticatedGroupEndpoints")
    void authenticatedApisWithoutAuthorization(GroupEndpoint endpoint) throws Exception {
        mockMvc.perform(endpoint.request().get())
            .andExpect(status().isUnauthorized())
            .andDo(documentError(endpoint.documentPrefix() + "-auth-missing", endpoint.summary()));
    }

    @ParameterizedTest(name = "{0} 빈 Bearer 토큰")
    @MethodSource("authenticatedGroupEndpoints")
    void authenticatedApisWithEmptyToken(GroupEndpoint endpoint) throws Exception {
        mockMvc.perform(endpoint.request().get()
                .header("Authorization", "Bearer "))
            .andExpect(status().isUnauthorized())
            .andDo(documentError(endpoint.documentPrefix() + "-auth-empty-token", endpoint.summary()));
    }

    @ParameterizedTest(name = "{0} 잘못된 Authorization 헤더")
    @MethodSource("authenticatedGroupEndpoints")
    void authenticatedApisWithInvalidAuthorizationHeader(GroupEndpoint endpoint) throws Exception {
        mockMvc.perform(endpoint.request().get()
                .header("Authorization", "access-token"))
            .andExpect(status().isUnauthorized())
            .andDo(documentError(endpoint.documentPrefix() + "-auth-invalid-header", endpoint.summary()));
    }

    @ParameterizedTest(name = "{0} 만료된 JWT")
    @MethodSource("authenticatedGroupEndpoints")
    void authenticatedApisWithExpiredToken(GroupEndpoint endpoint) throws Exception {
        willThrow(new GeneralException(ErrorStatus.EXPIRED_JWT))
            .given(tokenProvider)
            .getMemberIdByAccessToken("expired-token");

        mockMvc.perform(endpoint.request().get()
                .header("Authorization", "Bearer expired-token"))
            .andExpect(status().isUnauthorized())
            .andDo(documentError(endpoint.documentPrefix() + "-auth-expired-token", endpoint.summary()));
    }

    @ParameterizedTest(name = "{0} 지원하지 않는 JWT")
    @MethodSource("authenticatedGroupEndpoints")
    void authenticatedApisWithUnsupportedToken(GroupEndpoint endpoint) throws Exception {
        willThrow(new GeneralException(ErrorStatus.UNSUPPORTED_JWT))
            .given(tokenProvider)
            .getMemberIdByAccessToken("unsupported-token");

        mockMvc.perform(endpoint.request().get()
                .header("Authorization", "Bearer unsupported-token"))
            .andExpect(status().isUnauthorized())
            .andDo(documentError(endpoint.documentPrefix() + "-auth-unsupported-token", endpoint.summary()));
    }

    @ParameterizedTest(name = "{0} 회원 없음")
    @MethodSource("memberLookupGroupEndpoints")
    void memberLookupApisMemberNotFound(GroupEndpoint endpoint) throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        givenMemberNotFound(endpoint.documentPrefix());

        mockMvc.perform(endpoint.request().get()
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isNotFound())
            .andDo(documentError(endpoint.documentPrefix() + "-member-not-found", endpoint.summary()));
    }

    private static Stream<GroupEndpoint> authenticatedGroupEndpoints() {
        return Stream.of(
            new GroupEndpoint(
                "group-code-generate",
                "랜덤 그룹 코드 생성",
                () -> get("/api/v1/groups/code")
            ),
            new GroupEndpoint(
                "group-create",
                "그룹 생성",
                () -> post("/api/v1/groups")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "name": "모디 그룹"
                        }
                        """)
            ),
            new GroupEndpoint(
                "group-join",
                "그룹 참여",
                () -> post("/api/v1/groups/join")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "code": "ABC123"
                        }
                        """)
            ),
            new GroupEndpoint(
                "group-list",
                "내 그룹 목록",
                () -> get("/api/v1/groups")
            ),
            new GroupEndpoint(
                "group-member-list",
                "그룹 내 인원 조회",
                () -> get("/api/v1/groups/{groupId}/members", 10L)
            ),
            new GroupEndpoint(
                "group-leave",
                "그룹 나가기",
                () -> delete("/api/v1/groups/{groupId}/members/me", 10L)
            )
        );
    }

    private static Stream<GroupEndpoint> memberLookupGroupEndpoints() {
        return authenticatedGroupEndpoints()
            .filter(endpoint -> switch (endpoint.documentPrefix()) {
                case "group-code-generate", "group-create", "group-join", "group-list" -> true;
                default -> false;
            });
    }

    private void givenMemberNotFound(String documentPrefix) {
        switch (documentPrefix) {
            case "group-code-generate" -> willThrow(new GeneralException(ErrorStatus.MEMBER_NOT_FOUND))
                .given(groupService)
                .generateCode(1L);
            case "group-create" -> willThrow(new GeneralException(ErrorStatus.MEMBER_NOT_FOUND))
                .given(groupService)
                .createGroup(eq(1L), any(GroupCreateCommand.class));
            case "group-join" -> willThrow(new GeneralException(ErrorStatus.MEMBER_NOT_FOUND))
                .given(groupService)
                .joinGroup(eq(1L), any(GroupJoinCommand.class));
            case "group-list" -> willThrow(new GeneralException(ErrorStatus.MEMBER_NOT_FOUND))
                .given(groupService)
                .getMyGroups(1L);
            default -> throw new IllegalArgumentException("지원하지 않는 그룹 문서 prefix입니다.");
        }
    }

    private RestDocumentationResultHandler documentError(String identifier, String summary) {
        return document(identifier,
            resource(ResourceSnippetParameters.builder()
                .tag("Group")
                .summary(summary)
                .description(GROUP_DESCRIPTION)
                .responseFields(commonResponseFields())
                .build())
        );
    }

    private record GroupEndpoint(
        String documentPrefix,
        String summary,
        Supplier<MockHttpServletRequestBuilder> request
    ) {
        @Override
        public String toString() {
            return summary;
        }
    }
}
