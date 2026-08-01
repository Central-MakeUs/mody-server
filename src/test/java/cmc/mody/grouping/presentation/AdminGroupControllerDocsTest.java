package cmc.mody.grouping.presentation;

import static cmc.mody.docs.ApiDocumentUtils.commonResponseFields;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cmc.mody.common.admin.AdminAccessService;
import cmc.mody.grouping.application.AdminGroupService;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminGroupController.class)
@AutoConfigureRestDocs
class AdminGroupControllerDocsTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminAccessService adminAccessService;

    @MockitoBean
    private AdminGroupService adminGroupService;

    @Test
    void getGroups() throws Exception {
        given(adminGroupService.getGroups()).willReturn(new AdminGroupService.AdminGroupListResult(List.of(
            new AdminGroupService.AdminGroupResult(1L, "모디 크루", "GROUP001", 3L)
        )));

        mockMvc.perform(get("/api/v1/admin/groups").header("X-Admin-Api-Key", "test-admin-key"))
            .andExpect(status().isOk())
            .andDo(document("admin-group-list",
                resource(ResourceSnippetParameters.builder()
                    .tag("Admin Group")
                    .summary("운영 그룹 목록 조회")
                    .description("관리자 페이지에서 주간 사진 챌린지의 대상 그룹을 선택할 때 사용한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.groups").type(JsonFieldType.ARRAY).description("운영 그룹 목록"),
                        fieldWithPath("result.groups[].groupId").type(JsonFieldType.NUMBER).description("그룹 id"),
                        fieldWithPath("result.groups[].name").type(JsonFieldType.STRING).description("그룹명"),
                        fieldWithPath("result.groups[].code").type(JsonFieldType.STRING).description("그룹 초대 코드"),
                        fieldWithPath("result.groups[].memberCount").type(JsonFieldType.NUMBER).description("가입 중인 회원 수")
                    ))
                    .build())
            ));
    }
}
