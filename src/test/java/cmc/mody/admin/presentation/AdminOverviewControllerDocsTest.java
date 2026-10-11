package cmc.mody.admin.presentation;

import static cmc.mody.docs.ApiDocumentUtils.commonResponseFields;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cmc.mody.admin.application.AdminChallengeOverviewService;
import cmc.mody.admin.application.AdminFeedService;
import cmc.mody.common.admin.AdminAccessService;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({AdminFeedController.class, AdminChallengeOverviewController.class})
@AutoConfigureRestDocs
class AdminOverviewControllerDocsTest {
    private static final LocalDate FROM = LocalDate.of(2026, 10, 5);
    private static final LocalDate TO = LocalDate.of(2026, 10, 11);
    @Autowired private MockMvc mvc;
    @MockitoBean private AdminAccessService access;
    @MockitoBean private AdminFeedService feed;
    @MockitoBean private AdminChallengeOverviewService overview;

    @Test
    void summaryDocumentsCountsIndependentOfPagination() throws Exception {
        given(feed.summary(FROM, TO, null, null, null, null, null, null))
            .willReturn(new AdminFeedService.Summary(287, 245, 39, 3, FROM, TO, "Asia/Seoul"));
        mvc.perform(get("/api/v1/admin/feed/summary").param("from", FROM.toString()).param("to", TO.toString())
            .header("X-Admin-Api-Key", "test-admin-key"))
            .andExpect(status().isOk())
            .andDo(document("admin-feed-summary", resource(ResourceSnippetParameters.builder()
                .tag("Admin Console").summary("필터에 해당하는 전체 인증 건수 집계")
                .description("목록과 동일한 from/to/type/memberId/groupId/groupChallengeId/globalWeeklyChallengeId/"
                    + "keyword 필터를 사용한다. cursor/size와 무관한 전체 건수이며 기간은 한국시간 양끝 날짜 포함, "
                    + "최대 366일이다. ACTIVITY는 식사+운동이다. 목록의 공유 기록은 중복 집계하지 않는다.")
                .responseFields(commonResponseFields(
                    fieldWithPath("result.totalCount").type(JsonFieldType.NUMBER).description("전체 인증 수"),
                    fieldWithPath("result.mealCount").type(JsonFieldType.NUMBER).description("식사 인증 수"),
                    fieldWithPath("result.exerciseCount").type(JsonFieldType.NUMBER).description("운동 인증 수"),
                    fieldWithPath("result.challengeCount").type(JsonFieldType.NUMBER).description("챌린지 인증 수"),
                    fieldWithPath("result.from").description("포함 시작일"),
                    fieldWithPath("result.to").description("포함 종료일"),
                    fieldWithPath("result.timezone").description("Asia/Seoul")
                )).build())));
    }

    @Test
    void overviewDocumentsGlobalAndGroupChallengeStatistics() throws Exception {
        var item = new AdminChallengeOverviewService.Item("50", "GLOBAL", "물 한 잔", "사진 인증", FROM, TO,
            "50", null, null, "ACTIVE", 181, 3, 2, 2);
        given(overview.list(FROM, TO, null, "ALL", null, null, 24))
            .willReturn(new AdminChallengeOverviewService.Page(List.of(item), 1,
                new AdminChallengeOverviewService.Summary(1, 1, 0, 0), false, null, FROM, TO, "Asia/Seoul"));
        mvc.perform(get("/api/v1/admin/challenge-overview").param("from", FROM.toString()).param("to", TO.toString())
            .header("X-Admin-Api-Key", "test-admin-key"))
            .andExpect(status().isOk())
            .andDo(document("admin-challenge-overview", resource(ResourceSnippetParameters.builder()
                .tag("Admin Console").summary("전역·그룹 주간 챌린지 통합 현황")
                .description("from/to와 겹치는 기간의 챌린지를 조회한다(최대 366일, 기본 최근 90일~향후 30일). "
                    + "groupId 미지정은 전역 원본+그룹 개별, 지정 시 전역 연결분을 포함한 해당 그룹 목록이다. "
                    + "phase는 ALL/ACTIVE/UPCOMING/ENDED, keyword는 제목·안내 검색이다. size 기본 24, 최대 100. "
                    + "인증 통계는 각 챌린지 전체 기간 내 유효 인증, 회원 수는 중복 제거한 인원이다.")
                .responseFields(commonResponseFields(
                    subsectionWithPath("result.items").type(JsonFieldType.ARRAY)
                        .description("id, kind(GLOBAL/GROUP), 제목·안내·기간·그룹·전역 ID, phase, "
                            + "linkedGroupCount/proofCount/participantCount/participatingGroupCount"),
                    fieldWithPath("result.totalCount").type(JsonFieldType.NUMBER).description("선택 상태 전체 수"),
                    fieldWithPath("result.summary.totalCount").description("선택 기간·그룹·검색어의 전체 수"),
                    fieldWithPath("result.summary.activeCount").description("한국시간 오늘 진행 중인 수"),
                    fieldWithPath("result.summary.upcomingCount").description("예정 수"),
                    fieldWithPath("result.summary.endedCount").description("종료 수"),
                    fieldWithPath("result.hasMore").description("추가 페이지 여부"),
                    fieldWithPath("result.nextCursor").type(JsonFieldType.STRING).optional().description("다음 커서"),
                    fieldWithPath("result.from").description("포함 시작일"),
                    fieldWithPath("result.to").description("포함 종료일"),
                    fieldWithPath("result.timezone").description("Asia/Seoul")
                )).build())));
    }
}
