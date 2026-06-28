package cmc.mody.grouping.presentation;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GroupController.class)
@AutoConfigureRestDocs
@Import(WebConfig.class)
class GroupControllerDocsTest {
    private static final String GROUP_DESCRIPTION = """
        모든 그룹 API는 소셜 로그인 후 발급받은 access token의 회원 id 기준으로 처리한다.

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
        """;

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
                        fieldWithPath("result.code").type(JsonFieldType.STRING).description("랜덤 그룹 코드")
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
                        fieldWithPath("name").type(JsonFieldType.STRING).description("그룹명. 최대 30자")
                    )
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.groupId").type(JsonFieldType.NUMBER).description("그룹 id"),
                        fieldWithPath("result.code").type(JsonFieldType.STRING).description("그룹 코드"),
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
                            .description("그룹 참여 인원")
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
                            .description("그룹 참여 인원")
                    ))
                    .build())
            ));
    }

    @Test
    void getGroupMembers() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(groupService.getGroupMembers(1L, 10L))
            .willReturn(new GroupMemberListResult(List.of(
                new GroupMemberResult(1L, "민석", "profiles/member-1.jpg"),
                new GroupMemberResult(2L, "친구", "profiles/member-2.jpg")
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
                            .description("프로필 이미지 URL")
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
}
