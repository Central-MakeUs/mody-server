package cmc.mody.grouping.presentation;

import static cmc.mody.docs.ApiDocumentUtils.commonResponseFields;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

@WebMvcTest(GroupController.class)
@AutoConfigureRestDocs
class GroupControllerDocsTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void createGroup() throws Exception {
        mockMvc.perform(post("/api/v1/groups")
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
                    .summary("[미구현] 그룹 생성")
                    .description("그룹명을 입력해 그룹을 생성한다.")
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
        mockMvc.perform(post("/api/v1/groups/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "ABCDEF"
                    }
                    """))
            .andExpect(status().isOk())
            .andDo(document("group-join",
                resource(ResourceSnippetParameters.builder()
                    .tag("Group")
                    .summary("[미구현] 그룹 참여")
                    .description("그룹 코드로 그룹에 참여한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.groupId").type(JsonFieldType.NUMBER).description("그룹 id"),
                        fieldWithPath("result.code").type(JsonFieldType.STRING).description("그룹 코드"),
                        fieldWithPath("result.name").type(JsonFieldType.STRING).description("그룹명"),
                        fieldWithPath("result.memberCount").type(JsonFieldType.NUMBER).description("그룹 참여 인원")
                    ))
                    .build())
            ));
    }

    @Test
    void getMyGroups() throws Exception {
        mockMvc.perform(get("/api/v1/groups"))
            .andExpect(status().isOk())
            .andDo(document("group-list",
                resource(ResourceSnippetParameters.builder()
                    .tag("Group")
                    .summary("[미구현] 내 그룹 목록")
                    .description("내가 참여 중인 그룹 목록을 조회한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.groups[].groupId").type(JsonFieldType.NUMBER).description("그룹 id"),
                        fieldWithPath("result.groups[].name").type(JsonFieldType.STRING).description("그룹명"),
                        fieldWithPath("result.groups[].code").type(JsonFieldType.STRING).description("그룹 코드"),
                        fieldWithPath("result.groups[].memberCount").type(JsonFieldType.NUMBER).description("그룹 참여 인원"),
                        fieldWithPath("result.groups[].current").type(JsonFieldType.BOOLEAN).description("현재 선택된 그룹 여부")
                    ))
                    .build())
            ));
    }

    @Test
    void getGroupMembers() throws Exception {
        mockMvc.perform(get("/api/v1/groups/{groupId}/members", 1L))
            .andExpect(status().isOk())
            .andDo(document("group-member-list",
                resource(ResourceSnippetParameters.builder()
                    .tag("Group")
                    .summary("[미구현] 그룹 내 인원 조회")
                    .description("그룹 내 구성원 id, 닉네임, 프로필 이미지를 조회한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.members[].memberId").type(JsonFieldType.NUMBER).description("회원 id"),
                        fieldWithPath("result.members[].nickname").type(JsonFieldType.STRING).description("닉네임"),
                        fieldWithPath("result.members[].profileImageUrl").type(JsonFieldType.STRING).description("프로필 이미지 URL")
                    ))
                    .build())
            ));
    }

    @Test
    void leaveGroup() throws Exception {
        mockMvc.perform(delete("/api/v1/groups/{groupId}/members/me", 1L))
            .andExpect(status().isOk())
            .andDo(document("group-leave",
                resource(ResourceSnippetParameters.builder()
                    .tag("Group")
                    .summary("[미구현] 그룹 나가기")
                    .description("현재 회원이 그룹에서 나간다.")
                    .responseFields(commonResponseFields())
                    .build())
            ));
    }
}
