package cmc.mody.challenge.presentation;

import static cmc.mody.docs.ApiDocumentUtils.commonResponseFields;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cmc.mody.challenge.application.AdminChallengeService;
import cmc.mody.challenge.application.AdminChallengeService.WeeklyChallengeCreateCommand;
import cmc.mody.challenge.application.AdminChallengeService.WeeklyChallengeCreateResult;
import cmc.mody.common.admin.AdminAccessService;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminChallengeController.class)
@AutoConfigureRestDocs
class AdminChallengeControllerDocsTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminAccessService adminAccessService;

    @MockitoBean
    private AdminChallengeService adminChallengeService;

    @Test
    void createWeeklyChallenge() throws Exception {
        LocalDate startsOn = LocalDate.of(2026, 8, 3);
        LocalDate endsOn = LocalDate.of(2026, 8, 9);
        given(adminChallengeService.createWeeklyChallenge(
            1L,
            new WeeklyChallengeCreateCommand("엘리베이터 안 타고 올라가기", "계단으로 이동한 사진을 인증해주세요.", startsOn, endsOn)
        )).willReturn(new WeeklyChallengeCreateResult(
            10L,
            20L,
            "엘리베이터 안 타고 올라가기",
            "계단으로 이동한 사진을 인증해주세요.",
            startsOn,
            endsOn
        ));

        mockMvc.perform(post("/api/v1/admin/groups/{groupId}/weekly-challenges", 1L)
                .header("X-Admin-Api-Key", "test-admin-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "엘리베이터 안 타고 올라가기",
                      "description": "계단으로 이동한 사진을 인증해주세요.",
                      "startsOn": "2026-08-03",
                      "endsOn": "2026-08-09"
                    }
                    """))
            .andExpect(status().isCreated())
            .andDo(document("admin-weekly-challenge-create",
                resource(ResourceSnippetParameters.builder()
                    .tag("Admin Challenge")
                    .summary("운영자 주간 사진 챌린지 생성")
                    .description("X-Admin-Api-Key 헤더가 ADMIN_API_KEY 환경변수와 일치해야 한다.")
                    .requestFields(
                        fieldWithPath("title").type(JsonFieldType.STRING).description("주간 챌린지 제목"),
                        fieldWithPath("description").type(JsonFieldType.STRING).description("주간 챌린지 설명"),
                        fieldWithPath("startsOn").type(JsonFieldType.STRING).description("시작일"),
                        fieldWithPath("endsOn").type(JsonFieldType.STRING).description("마감일")
                    )
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.groupChallengeId").type(JsonFieldType.NUMBER).description("생성된 그룹 챌린지 id"),
                        fieldWithPath("result.challengeId").type(JsonFieldType.NUMBER).description("생성된 사진 챌린지 id"),
                        fieldWithPath("result.title").type(JsonFieldType.STRING).description("챌린지 제목"),
                        fieldWithPath("result.description").type(JsonFieldType.STRING).description("챌린지 설명"),
                        fieldWithPath("result.startsOn").type(JsonFieldType.STRING).description("시작일"),
                        fieldWithPath("result.endsOn").type(JsonFieldType.STRING).description("마감일")
                    ))
                    .build())
            ));
    }
}
