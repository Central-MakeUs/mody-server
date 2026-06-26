package cmc.mody.challenge.presentation;

import static cmc.mody.docs.ApiDocumentUtils.commonResponseFields;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

@WebMvcTest(ChallengeController.class)
@AutoConfigureRestDocs
class ChallengeControllerDocsTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void getChallengeSummary() throws Exception {
        mockMvc.perform(get("/api/v1/groups/{groupId}/challenges/summary", 1L))
            .andExpect(status().isOk())
            .andDo(document("challenge-summary",
                resource(ResourceSnippetParameters.builder()
                    .tag("Challenge")
                    .summary("챌린지 홈 요약 조회")
                    .description("그룹과 함께한 날짜, 연속 기록, 이번달 운동 시간, 완료 챌린지 개수를 조회한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.daysTogether").type(JsonFieldType.NUMBER).description("그룹과 함께한 일수"),
                        fieldWithPath("result.allMemberRecordedDays").type(JsonFieldType.NUMBER).description("모든 구성원이 기록한 일수"),
                        fieldWithPath("result.monthlyExerciseMinutes").type(JsonFieldType.NUMBER).description("이번달 운동 시간 분"),
                        fieldWithPath("result.monthlyCompletedChallengeCount").type(JsonFieldType.NUMBER).description("이번달 완료 챌린지 개수")
                    ))
                    .build())
            ));
    }

    @Test
    void getCurrentStepChallenge() throws Exception {
        mockMvc.perform(get("/api/v1/groups/{groupId}/challenges/step/current", 1L))
            .andExpect(status().isOk())
            .andDo(document("step-challenge-current",
                resource(ResourceSnippetParameters.builder()
                    .tag("Step Challenge")
                    .summary("걸음수 챌린지 현황")
                    .description("현재 진행 중인 걸음수 챌린지명, 목표 걸음수, 현재 걸음수를 조회한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.groupChallengeId").type(JsonFieldType.NUMBER).description("그룹 챌린지 id"),
                        fieldWithPath("result.title").type(JsonFieldType.STRING).description("챌린지명"),
                        fieldWithPath("result.targetStepCount").type(JsonFieldType.NUMBER).description("목표 걸음수"),
                        fieldWithPath("result.currentStepCount").type(JsonFieldType.NUMBER).description("현재 걸음수")
                    ))
                    .build())
            ));
    }

    @Test
    void getWeeklyChallenges() throws Exception {
        mockMvc.perform(get("/api/v1/groups/{groupId}/challenges/weekly", 1L))
            .andExpect(status().isOk())
            .andDo(document("weekly-challenge-list",
                resource(ResourceSnippetParameters.builder()
                    .tag("Weekly Challenge")
                    .summary("이번주의 주간 챌린지 조회")
                    .description("이번주 진행 중인 주간 챌린지 목록과 참여 현황을 조회한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.challenges[].groupChallengeId").type(JsonFieldType.NUMBER).description("그룹 챌린지 id"),
                        fieldWithPath("result.challenges[].title").type(JsonFieldType.STRING).description("챌린지명"),
                        fieldWithPath("result.challenges[].deadlineDayOfWeek").type(JsonFieldType.STRING).description("마감 요일"),
                        fieldWithPath("result.challenges[].participantCount").type(JsonFieldType.NUMBER).description("참여 인원"),
                        fieldWithPath("result.challenges[].randomParticipantNickname").type(JsonFieldType.STRING).description("랜덤 참여자 닉네임")
                    ))
                    .build())
            ));
    }

    @Test
    void getNudgeTargets() throws Exception {
        mockMvc.perform(get("/api/v1/groups/{groupId}/challenges/nudges", 1L))
            .andExpect(status().isOk())
            .andDo(document("challenge-nudge-targets",
                resource(ResourceSnippetParameters.builder()
                    .tag("Challenge")
                    .summary("버디 찌르기 대상 조회")
                    .description("오늘 기록하지 않은 그룹 구성원과 기록 여부를 조회한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.members[].memberId").type(JsonFieldType.NUMBER).description("회원 id"),
                        fieldWithPath("result.members[].nickname").type(JsonFieldType.STRING).description("닉네임"),
                        fieldWithPath("result.members[].profileImageUrl").type(JsonFieldType.STRING).description("프로필 이미지"),
                        fieldWithPath("result.members[].recordedToday").type(JsonFieldType.BOOLEAN).description("오늘 기록 여부")
                    ))
                    .build())
            ));
    }

    @Test
    void nudgeMember() throws Exception {
        mockMvc.perform(post("/api/v1/groups/{groupId}/challenges/nudges/{memberId}", 1L, 2L))
            .andExpect(status().isOk())
            .andDo(document("challenge-nudge",
                resource(ResourceSnippetParameters.builder()
                    .tag("Challenge")
                    .summary("버디 찌르기")
                    .description("기록하지 않은 그룹 구성원에게 알림을 보낸다.")
                    .responseFields(commonResponseFields())
                    .build())
            ));
    }

    @Test
    void getWalkedRegions() throws Exception {
        mockMvc.perform(get("/api/v1/groups/{groupId}/challenges/step/regions", 1L))
            .andExpect(status().isOk())
            .andDo(document("step-challenge-regions",
                resource(ResourceSnippetParameters.builder()
                    .tag("Step Challenge")
                    .summary("지금까지 걸어간 지역 확인")
                    .description("완료 또는 변경 종료된 걸음수 챌린지 지역 내역을 조회한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.regions[].regionName").type(JsonFieldType.STRING).description("지역명"),
                        fieldWithPath("result.regions[].regionImageUrl").type(JsonFieldType.STRING).description("지역 이미지 URL")
                    ))
                    .build())
            ));
    }

    @Test
    void getStepRankings() throws Exception {
        mockMvc.perform(get("/api/v1/groups/{groupId}/challenges/step/rankings", 1L))
            .andExpect(status().isOk())
            .andDo(document("step-challenge-rankings",
                resource(ResourceSnippetParameters.builder()
                    .tag("Step Challenge")
                    .summary("기여도 순위")
                    .description("현재 걸음수 챌린지의 회원별 기여도 순위를 조회한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.rankings[].rank").type(JsonFieldType.NUMBER).description("등수"),
                        fieldWithPath("result.rankings[].memberId").type(JsonFieldType.NUMBER).description("회원 id"),
                        fieldWithPath("result.rankings[].nickname").type(JsonFieldType.STRING).description("닉네임"),
                        fieldWithPath("result.rankings[].profileImageUrl").type(JsonFieldType.STRING).description("프로필 이미지"),
                        fieldWithPath("result.rankings[].stepCount").type(JsonFieldType.NUMBER).description("걸음 수")
                    ))
                    .build())
            ));
    }

    @Test
    void changeStepChallenge() throws Exception {
        mockMvc.perform(patch("/api/v1/groups/{groupId}/challenges/step/current", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "challengeId": 2
                    }
                    """))
            .andExpect(status().isOk())
            .andDo(document("step-challenge-change",
                resource(ResourceSnippetParameters.builder()
                    .tag("Step Challenge")
                    .summary("챌린지 변경")
                    .description("진행 중인 걸음수 챌린지를 변경하고 현재 진행률을 초기화한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.groupChallengeId").type(JsonFieldType.NUMBER).description("새 그룹 챌린지 id"),
                        fieldWithPath("result.challengeId").type(JsonFieldType.NUMBER).description("챌린지 id"),
                        fieldWithPath("result.title").type(JsonFieldType.STRING).description("챌린지명"),
                        fieldWithPath("result.targetStepCount").type(JsonFieldType.NUMBER).description("목표 걸음수"),
                        fieldWithPath("result.currentStepCount").type(JsonFieldType.NUMBER).description("현재 걸음수")
                    ))
                    .build())
            ));
    }

    @Test
    void getWeeklyChallengeDetail() throws Exception {
        mockMvc.perform(get("/api/v1/weekly-challenges/{challengeId}", 1L))
            .andExpect(status().isOk())
            .andDo(document("weekly-challenge-detail",
                resource(ResourceSnippetParameters.builder()
                    .tag("Weekly Challenge")
                    .summary("주간 챌린지 상세 조회")
                    .description("주간 챌린지명과 상세 설명을 조회한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.challengeId").type(JsonFieldType.NUMBER).description("챌린지 id"),
                        fieldWithPath("result.title").type(JsonFieldType.STRING).description("챌린지명"),
                        fieldWithPath("result.description").type(JsonFieldType.STRING).description("상세 설명")
                    ))
                    .build())
            ));
    }

    @Test
    void getWeeklyChallengeProofs() throws Exception {
        mockMvc.perform(get("/api/v1/groups/{groupId}/weekly-challenges/{groupChallengeId}/proofs", 1L, 1L))
            .andExpect(status().isOk())
            .andDo(document("weekly-challenge-proofs",
                resource(ResourceSnippetParameters.builder()
                    .tag("Weekly Challenge")
                    .summary("그룹원 인증 이미지 조회")
                    .description("주간 챌린지에 업로드된 그룹원 인증 이미지를 조회한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.proofs[].proofId").type(JsonFieldType.NUMBER).description("인증 id"),
                        fieldWithPath("result.proofs[].imageUrl").type(JsonFieldType.STRING).description("인증 이미지 URL"),
                        fieldWithPath("result.proofs[].memberId").type(JsonFieldType.NUMBER).description("회원 id"),
                        fieldWithPath("result.proofs[].nickname").type(JsonFieldType.STRING).description("닉네임"),
                        fieldWithPath("result.proofs[].profileImageUrl").type(JsonFieldType.STRING).description("프로필 이미지")
                    ))
                    .build())
            ));
    }

    @Test
    void shareWeeklyChallenge() throws Exception {
        mockMvc.perform(post("/api/v1/groups/{groupId}/weekly-challenges/{groupChallengeId}/share", 1L, 1L))
            .andExpect(status().isOk())
            .andDo(document("weekly-challenge-share",
                resource(ResourceSnippetParameters.builder()
                    .tag("Weekly Challenge")
                    .summary("챌린지 완료 공유하기")
                    .description("인원수에 맞게 그리드화한 공유 이미지 정보를 조회한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.imageUrl").type(JsonFieldType.STRING).description("공유 이미지 URL"),
                        fieldWithPath("result.rows").type(JsonFieldType.NUMBER).description("그리드 행 수"),
                        fieldWithPath("result.columns").type(JsonFieldType.NUMBER).description("그리드 열 수")
                    ))
                    .build())
            ));
    }
}
