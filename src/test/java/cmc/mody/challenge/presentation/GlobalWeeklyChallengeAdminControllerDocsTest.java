package cmc.mody.challenge.presentation;

import static cmc.mody.docs.ApiDocumentUtils.commonResponseFields;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cmc.mody.challenge.application.GlobalWeeklyChallengeService;
import cmc.mody.challenge.application.GlobalWeeklyChallengeService.GlobalWeeklyChallengeCommand;
import cmc.mody.challenge.application.GlobalWeeklyChallengeService.GlobalWeeklyChallengeCreateResult;
import cmc.mody.challenge.application.GlobalWeeklyChallengeService.GlobalWeeklyChallengeListResult;
import cmc.mody.challenge.application.GlobalWeeklyChallengeService.GlobalWeeklyChallengeResult;
import cmc.mody.common.admin.AdminAccessService;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GlobalWeeklyChallengeAdminController.class)
@AutoConfigureRestDocs
class GlobalWeeklyChallengeAdminControllerDocsTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminAccessService adminAccessService;

    @MockitoBean
    private GlobalWeeklyChallengeService globalWeeklyChallengeService;

    @Test
    void createGlobalWeeklyChallenge() throws Exception {
        LocalDate startsOn = LocalDate.of(2026, 8, 10);
        LocalDate endsOn = LocalDate.of(2026, 8, 16);
        given(globalWeeklyChallengeService.create(new GlobalWeeklyChallengeCommand(
            "엘리베이터 안 타고 올라가기", "계단으로 이동한 사진을 인증해주세요.", startsOn, endsOn
        ))).willReturn(new GlobalWeeklyChallengeCreateResult(
            10L, 20L, "엘리베이터 안 타고 올라가기", "계단으로 이동한 사진을 인증해주세요.", startsOn, endsOn, 3
        ));

        mockMvc.perform(post("/api/v1/admin/weekly-challenges")
                .header("X-Admin-Api-Key", "test-admin-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody()))
            .andExpect(status().isCreated())
            .andDo(document("admin-global-weekly-challenge-create",
                resource(ResourceSnippetParameters.builder()
                    .tag("Admin Global Weekly Challenge")
                    .summary("전역 주간 사진 챌린지 생성")
                    .description("한 번 등록하면 모든 활성 그룹에 연결된 별도 진행 항목이 생성된다.")
                    .requestFields(requestFields())
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.globalWeeklyChallengeId").type(JsonFieldType.NUMBER).description("전역 원본 id"),
                        fieldWithPath("result.challengeId").type(JsonFieldType.NUMBER).description("사진 챌린지 id"),
                        fieldWithPath("result.title").type(JsonFieldType.STRING).description("챌린지 제목"),
                        fieldWithPath("result.description").type(JsonFieldType.STRING).description("챌린지 설명"),
                        fieldWithPath("result.startsOn").type(JsonFieldType.STRING).description("시작일"),
                        fieldWithPath("result.endsOn").type(JsonFieldType.STRING).description("마감일"),
                        fieldWithPath("result.linkedGroupCount").type(JsonFieldType.NUMBER).description("연결된 그룹 수")
                    ))
                    .build())
            ));
    }

    @Test
    void getGlobalWeeklyChallenges() throws Exception {
        given(globalWeeklyChallengeService.getAll()).willReturn(new GlobalWeeklyChallengeListResult(List.of(
            new GlobalWeeklyChallengeResult(
                10L, 20L, "엘리베이터 안 타고 올라가기", "계단으로 이동한 사진을 인증해주세요.",
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 16), 3L
            )
        )));

        mockMvc.perform(get("/api/v1/admin/weekly-challenges").header("X-Admin-Api-Key", "test-admin-key"))
            .andExpect(status().isOk())
            .andDo(document("admin-global-weekly-challenge-list",
                resource(ResourceSnippetParameters.builder()
                    .tag("Admin Global Weekly Challenge")
                    .summary("전역 주간 사진 챌린지 목록 조회")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.challenges[].globalWeeklyChallengeId").type(JsonFieldType.NUMBER)
                            .description("전역 원본 id"),
                        fieldWithPath("result.challenges[].challengeId").type(JsonFieldType.NUMBER).description("사진 챌린지 id"),
                        fieldWithPath("result.challenges[].title").type(JsonFieldType.STRING).description("챌린지 제목"),
                        fieldWithPath("result.challenges[].description").type(JsonFieldType.STRING).description("챌린지 설명"),
                        fieldWithPath("result.challenges[].startsOn").type(JsonFieldType.STRING).description("시작일"),
                        fieldWithPath("result.challenges[].endsOn").type(JsonFieldType.STRING).description("마감일"),
                        fieldWithPath("result.challenges[].linkedGroupCount").type(JsonFieldType.NUMBER)
                            .description("연결된 그룹 수")
                    ))
                    .build())
            ));
    }

    @Test
    void updateGlobalWeeklyChallenge() throws Exception {
        LocalDate startsOn = LocalDate.of(2026, 8, 10);
        LocalDate endsOn = LocalDate.of(2026, 8, 16);
        given(globalWeeklyChallengeService.update(
            10L,
            new GlobalWeeklyChallengeCommand("엘리베이터 안 타고 올라가기", "계단으로 이동한 사진을 인증해주세요.", startsOn, endsOn)
        )).willReturn(new GlobalWeeklyChallengeResult(
            10L, 20L, "엘리베이터 안 타고 올라가기", "계단으로 이동한 사진을 인증해주세요.", startsOn, endsOn, 3L
        ));

        mockMvc.perform(put("/api/v1/admin/weekly-challenges/{globalWeeklyChallengeId}", 10L)
                .header("X-Admin-Api-Key", "test-admin-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody()))
            .andExpect(status().isOk());
    }

    @Test
    void deleteGlobalWeeklyChallenge() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/weekly-challenges/{globalWeeklyChallengeId}", 10L)
                .header("X-Admin-Api-Key", "test-admin-key"))
            .andExpect(status().isOk());
    }

    private String requestBody() {
        return """
            {
              "title": "엘리베이터 안 타고 올라가기",
              "description": "계단으로 이동한 사진을 인증해주세요.",
              "startsOn": "2026-08-10",
              "endsOn": "2026-08-16"
            }
            """;
    }

    private org.springframework.restdocs.payload.FieldDescriptor[] requestFields() {
        return new org.springframework.restdocs.payload.FieldDescriptor[]{
            fieldWithPath("title").type(JsonFieldType.STRING).description("주간 챌린지 제목"),
            fieldWithPath("description").type(JsonFieldType.STRING).description("주간 챌린지 설명"),
            fieldWithPath("startsOn").type(JsonFieldType.STRING).description("시작일"),
            fieldWithPath("endsOn").type(JsonFieldType.STRING).description("마감일")
        };
    }
}
