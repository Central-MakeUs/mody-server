package cmc.mody.challenge.presentation;

import static cmc.mody.docs.ApiDocumentUtils.commonResponseFields;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cmc.mody.auth.application.token.TokenProvider;
import cmc.mody.auth.presentation.support.CurrentMemberArgumentResolver;
import cmc.mody.challenge.application.StepChallengeService;
import cmc.mody.challenge.application.StepChallengeService.StepChallengeChangeCommand;
import cmc.mody.challenge.application.StepChallengeService.StepChallengeChangeResult;
import cmc.mody.challenge.application.StepChallengeService.StepChallengeOptionListResult;
import cmc.mody.challenge.application.StepChallengeService.StepChallengeOptionResult;
import cmc.mody.challenge.application.StepChallengeService.StepChallengeStatusResult;
import cmc.mody.challenge.application.StepChallengeService.StepRankingListResult;
import cmc.mody.challenge.application.StepChallengeService.StepRankingResult;
import cmc.mody.challenge.application.StepChallengeService.WalkedRegionListResult;
import cmc.mody.challenge.application.StepChallengeService.WalkedRegionResult;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.config.WebConfig;
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
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChallengeController.class)
@AutoConfigureRestDocs
@Import(WebConfig.class)
class ChallengeControllerDocsTest {
    private static final String STEP_CHALLENGE_DESCRIPTION = """
        걸음수 챌린지 API는 access token의 회원 id 기준으로 그룹 참여 여부를 검증한다.

        발생 가능한 예외 코드:
        - AUTH401: Authorization 헤더가 없거나 비어있음
        - AUTH402: Bearer 뒤 JWT 값이 비어있음
        - AUTH403: JWT 형식이 올바르지 않거나 refresh token을 사용함
        - AUTH404: 만료된 JWT
        - AUTH405: 지원하지 않는 JWT
        - MEMBER302: 토큰의 회원 id에 해당하는 회원 없음
        - GROUP302: 그룹 없음
        - GROUP306: 그룹 참여 정보 없음
        - CHALLENGE301: 챌린지 요청값 검증 실패
        - CHALLENGE302: 챌린지 없음
        - CHALLENGE303: 진행 중인 걸음수 챌린지 없음
        """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StepChallengeService stepChallengeService;

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
    void getChallengeSummary() throws Exception {
        mockMvc.perform(get("/api/v1/groups/{groupId}/challenges/summary", 1L))
            .andExpect(status().isOk())
            .andDo(document("challenge-summary",
                resource(ResourceSnippetParameters.builder()
                    .tag("Challenge")
                    .summary("[미구현] 챌린지 홈 요약 조회")
                    .description("그룹과 함께한 날짜, 연속 기록, 이번달 운동 시간, 완료 챌린지 개수를 조회한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.daysTogether").type(JsonFieldType.NUMBER).description("그룹과 함께한 일수"),
                        fieldWithPath("result.allMemberRecordedDays").type(JsonFieldType.NUMBER)
                            .description("모든 구성원이 기록한 일수"),
                        fieldWithPath("result.monthlyExerciseMinutes").type(JsonFieldType.NUMBER)
                            .description("이번달 운동 시간 분"),
                        fieldWithPath("result.monthlyCompletedChallengeCount").type(JsonFieldType.NUMBER)
                            .description("이번달 완료 챌린지 개수")
                    ))
                    .build())
            ));
    }

    @Test
    void getCurrentStepChallenge() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(stepChallengeService.getCurrentStepChallenge(1L, 1L))
            .willReturn(new StepChallengeStatusResult(1L, "서울-인천", 150_000, 34_000));

        mockMvc.perform(get("/api/v1/groups/{groupId}/challenges/step/current", 1L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isOk())
            .andDo(document("step-challenge-current",
                resource(ResourceSnippetParameters.builder()
                    .tag("Step Challenge")
                    .summary("걸음수 챌린지 현황")
                    .description(STEP_CHALLENGE_DESCRIPTION)
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
                    .summary("[미구현] 이번주의 주간 챌린지 조회")
                    .description("이번주 진행 중인 주간 챌린지 목록과 참여 현황을 조회한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.challenges[].groupChallengeId").type(JsonFieldType.NUMBER)
                            .description("그룹 챌린지 id"),
                        fieldWithPath("result.challenges[].title").type(JsonFieldType.STRING).description("챌린지명"),
                        fieldWithPath("result.challenges[].deadlineDayOfWeek").type(JsonFieldType.STRING)
                            .description("마감 요일"),
                        fieldWithPath("result.challenges[].participantCount").type(JsonFieldType.NUMBER)
                            .description("참여 인원"),
                        fieldWithPath("result.challenges[].randomParticipantNickname").type(JsonFieldType.STRING)
                            .description("랜덤 참여자 닉네임")
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
                    .summary("[미구현] 버디 찌르기 대상 조회")
                    .description("오늘 기록하지 않은 그룹 구성원과 기록 여부를 조회한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.members[].memberId").type(JsonFieldType.NUMBER).description("회원 id"),
                        fieldWithPath("result.members[].nickname").type(JsonFieldType.STRING).description("닉네임"),
                        fieldWithPath("result.members[].profileImageUrl").type(JsonFieldType.STRING)
                            .description("프로필 이미지"),
                        fieldWithPath("result.members[].recordedToday").type(JsonFieldType.BOOLEAN)
                            .description("오늘 기록 여부")
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
                    .summary("[미구현] 버디 찌르기")
                    .description("기록하지 않은 그룹 구성원에게 알림을 보낸다.")
                    .responseFields(commonResponseFields())
                    .build())
            ));
    }

    @Test
    void getWalkedRegions() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(stepChallengeService.getWalkedRegions(1L, 1L))
            .willReturn(new WalkedRegionListResult(List.of(new WalkedRegionResult("인천", "regions/인천.png"))));

        mockMvc.perform(get("/api/v1/groups/{groupId}/challenges/step/regions", 1L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isOk())
            .andDo(document("step-challenge-regions",
                resource(ResourceSnippetParameters.builder()
                    .tag("Step Challenge")
                    .summary("지금까지 걸어간 지역 확인")
                    .description(STEP_CHALLENGE_DESCRIPTION)
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.regions[].regionName").type(JsonFieldType.STRING).description("지역명"),
                        fieldWithPath("result.regions[].regionImageUrl").type(JsonFieldType.STRING)
                            .description("지역 이미지 URL")
                    ))
                    .build())
            ));
    }

    @Test
    void getStepChallengeOptions() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(stepChallengeService.getStepChallengeOptions(1L, 1L))
            .willReturn(new StepChallengeOptionListResult(List.of(
                new StepChallengeOptionResult(1L, "서울-인천", "서울", "인천", 60.0, 150_000, true),
                new StepChallengeOptionResult(2L, "서울-천안", "서울", "천안", 90.0, 200_000, false)
            )));

        mockMvc.perform(get("/api/v1/groups/{groupId}/challenges/step/options", 1L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isOk())
            .andDo(document("step-challenge-options",
                resource(ResourceSnippetParameters.builder()
                    .tag("Step Challenge")
                    .summary("변경 가능한 걸음수 챌린지 목록")
                    .description(STEP_CHALLENGE_DESCRIPTION)
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.options[].challengeId").type(JsonFieldType.NUMBER).description("챌린지 id"),
                        fieldWithPath("result.options[].title").type(JsonFieldType.STRING).description("챌린지명"),
                        fieldWithPath("result.options[].departure").type(JsonFieldType.STRING).description("출발지"),
                        fieldWithPath("result.options[].destination").type(JsonFieldType.STRING).description("도착지"),
                        fieldWithPath("result.options[].distanceKm").type(JsonFieldType.NUMBER).description("거리 km"),
                        fieldWithPath("result.options[].targetStepCount").type(JsonFieldType.NUMBER)
                            .description("목표 걸음수"),
                        fieldWithPath("result.options[].selected").type(JsonFieldType.BOOLEAN)
                            .description("현재 진행 중인 챌린지 여부")
                    ))
                    .build())
            ));
    }

    @Test
    void getStepRankings() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(stepChallengeService.getStepRankings(1L, 1L))
            .willReturn(new StepRankingListResult(List.of(
                new StepRankingResult(1, 1L, "민석", "profiles/member-1.jpg", 18_000)
            )));

        mockMvc.perform(get("/api/v1/groups/{groupId}/challenges/step/rankings", 1L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isOk())
            .andDo(document("step-challenge-rankings",
                resource(ResourceSnippetParameters.builder()
                    .tag("Step Challenge")
                    .summary("기여도 순위")
                    .description(STEP_CHALLENGE_DESCRIPTION)
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.rankings[].rank").type(JsonFieldType.NUMBER).description("등수"),
                        fieldWithPath("result.rankings[].memberId").type(JsonFieldType.NUMBER).description("회원 id"),
                        fieldWithPath("result.rankings[].nickname").type(JsonFieldType.STRING).description("닉네임"),
                        fieldWithPath("result.rankings[].profileImageUrl").type(JsonFieldType.STRING)
                            .description("프로필 이미지"),
                        fieldWithPath("result.rankings[].stepCount").type(JsonFieldType.NUMBER).description("걸음 수")
                    ))
                    .build())
            ));
    }

    @Test
    void changeStepChallenge() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(stepChallengeService.changeStepChallenge(1L, 1L, new StepChallengeChangeCommand(2L)))
            .willReturn(new StepChallengeChangeResult(2L, 2L, "서울-천안", 200_000, 0));

        mockMvc.perform(patch("/api/v1/groups/{groupId}/challenges/step/current", 1L)
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "challengeId": 2
                    }
                    """))
            .andExpect(status().isOk())
            .andDo(document("step-challenge-change",
                requestFields(
                    fieldWithPath("challengeId").type(JsonFieldType.NUMBER).description("변경할 걸음수 챌린지 id")
                ),
                resource(ResourceSnippetParameters.builder()
                    .tag("Step Challenge")
                    .summary("챌린지 변경")
                    .description(STEP_CHALLENGE_DESCRIPTION)
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

    @ParameterizedTest(name = "{0} Authorization 헤더 없음")
    @MethodSource("authenticatedStepEndpoints")
    void authenticatedStepApisWithoutAuthorization(StepEndpoint endpoint) throws Exception {
        mockMvc.perform(endpoint.request().get())
            .andExpect(status().isUnauthorized())
            .andDo(documentStepError(endpoint.documentPrefix() + "-auth-missing", endpoint.summary()));
    }

    @ParameterizedTest(name = "{0} 빈 Bearer 토큰")
    @MethodSource("authenticatedStepEndpoints")
    void authenticatedStepApisWithEmptyToken(StepEndpoint endpoint) throws Exception {
        mockMvc.perform(endpoint.request().get()
                .header("Authorization", "Bearer "))
            .andExpect(status().isUnauthorized())
            .andDo(documentStepError(endpoint.documentPrefix() + "-auth-empty-token", endpoint.summary()));
    }

    @ParameterizedTest(name = "{0} 잘못된 Authorization 헤더")
    @MethodSource("authenticatedStepEndpoints")
    void authenticatedStepApisWithInvalidAuthorizationHeader(StepEndpoint endpoint) throws Exception {
        mockMvc.perform(endpoint.request().get()
                .header("Authorization", "access-token"))
            .andExpect(status().isUnauthorized())
            .andDo(documentStepError(endpoint.documentPrefix() + "-auth-invalid-header", endpoint.summary()));
    }

    @ParameterizedTest(name = "{0} 만료된 JWT")
    @MethodSource("authenticatedStepEndpoints")
    void authenticatedStepApisWithExpiredToken(StepEndpoint endpoint) throws Exception {
        willThrow(new GeneralException(ErrorStatus.EXPIRED_JWT))
            .given(tokenProvider)
            .getMemberIdByAccessToken("expired-token");

        mockMvc.perform(endpoint.request().get()
                .header("Authorization", "Bearer expired-token"))
            .andExpect(status().isUnauthorized())
            .andDo(documentStepError(endpoint.documentPrefix() + "-auth-expired-token", endpoint.summary()));
    }

    @ParameterizedTest(name = "{0} 지원하지 않는 JWT")
    @MethodSource("authenticatedStepEndpoints")
    void authenticatedStepApisWithUnsupportedToken(StepEndpoint endpoint) throws Exception {
        willThrow(new GeneralException(ErrorStatus.UNSUPPORTED_JWT))
            .given(tokenProvider)
            .getMemberIdByAccessToken("unsupported-token");

        mockMvc.perform(endpoint.request().get()
                .header("Authorization", "Bearer unsupported-token"))
            .andExpect(status().isUnauthorized())
            .andDo(documentStepError(endpoint.documentPrefix() + "-auth-unsupported-token", endpoint.summary()));
    }

    @Test
    void getCurrentStepChallengeNotFound() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.CHALLENGE_IN_PROGRESS_NOT_FOUND))
            .given(stepChallengeService)
            .getCurrentStepChallenge(1L, 1L);

        mockMvc.perform(get("/api/v1/groups/{groupId}/challenges/step/current", 1L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isNotFound())
            .andDo(documentStepError("step-challenge-current-not-found", "걸음수 챌린지 현황"));
    }

    @Test
    void changeStepChallengeNotFound() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.CHALLENGE_NOT_FOUND))
            .given(stepChallengeService)
            .changeStepChallenge(1L, 1L, new StepChallengeChangeCommand(2L));

        mockMvc.perform(patch("/api/v1/groups/{groupId}/challenges/step/current", 1L)
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "challengeId": 2
                    }
                    """))
            .andExpect(status().isNotFound())
            .andDo(documentStepError("step-challenge-change-challenge-not-found", "챌린지 변경"));
    }

    @ParameterizedTest(name = "{0} 회원 없음")
    @MethodSource("authenticatedStepEndpoints")
    void authenticatedStepApisMemberNotFound(StepEndpoint endpoint) throws Exception {
        givenStepEndpointThrows(endpoint, ErrorStatus.MEMBER_NOT_FOUND);

        mockMvc.perform(endpoint.request().get()
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isNotFound())
            .andDo(documentStepError(endpoint.documentPrefix() + "-member-not-found", endpoint.summary()));
    }

    @ParameterizedTest(name = "{0} 그룹 없음")
    @MethodSource("authenticatedStepEndpoints")
    void authenticatedStepApisGroupNotFound(StepEndpoint endpoint) throws Exception {
        givenStepEndpointThrows(endpoint, ErrorStatus.GROUP_NOT_FOUND);

        mockMvc.perform(endpoint.request().get()
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isNotFound())
            .andDo(documentStepError(endpoint.documentPrefix() + "-group-not-found", endpoint.summary()));
    }

    @ParameterizedTest(name = "{0} 그룹 참여 정보 없음")
    @MethodSource("authenticatedStepEndpoints")
    void authenticatedStepApisGroupMemberNotFound(StepEndpoint endpoint) throws Exception {
        givenStepEndpointThrows(endpoint, ErrorStatus.GROUP_MEMBER_NOT_FOUND);

        mockMvc.perform(endpoint.request().get()
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isNotFound())
            .andDo(documentStepError(endpoint.documentPrefix() + "-group-member-not-found", endpoint.summary()));
    }

    @Test
    void changeStepChallengeInvalidRequest() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);

        mockMvc.perform(patch("/api/v1/groups/{groupId}/challenges/step/current", 1L)
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "challengeId": null
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andDo(documentStepError("step-challenge-change-invalid-request", "챌린지 변경"));
    }

    @Test
    void getWeeklyChallengeDetail() throws Exception {
        mockMvc.perform(get("/api/v1/weekly-challenges/{challengeId}", 1L))
            .andExpect(status().isOk())
            .andDo(document("weekly-challenge-detail",
                resource(ResourceSnippetParameters.builder()
                    .tag("Weekly Challenge")
                    .summary("[미구현] 주간 챌린지 상세 조회")
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
                    .summary("[미구현] 그룹원 인증 이미지 조회")
                    .description("주간 챌린지에 업로드된 그룹원 인증 이미지를 조회한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.proofs[].proofId").type(JsonFieldType.NUMBER).description("인증 id"),
                        fieldWithPath("result.proofs[].imageUrl").type(JsonFieldType.STRING).description("인증 이미지 URL"),
                        fieldWithPath("result.proofs[].memberId").type(JsonFieldType.NUMBER).description("회원 id"),
                        fieldWithPath("result.proofs[].nickname").type(JsonFieldType.STRING).description("닉네임"),
                        fieldWithPath("result.proofs[].profileImageUrl").type(JsonFieldType.STRING)
                            .description("프로필 이미지")
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
                    .summary("[미구현] 챌린지 완료 공유하기")
                    .description("인원수에 맞게 그리드화한 공유 이미지 정보를 조회한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.imageUrl").type(JsonFieldType.STRING).description("공유 이미지 URL"),
                        fieldWithPath("result.rows").type(JsonFieldType.NUMBER).description("그리드 행 수"),
                        fieldWithPath("result.columns").type(JsonFieldType.NUMBER).description("그리드 열 수")
                    ))
                    .build())
            ));
    }

    private RestDocumentationResultHandler documentStepError(String identifier, String summary) {
        return document(identifier,
            resource(ResourceSnippetParameters.builder()
                .tag("Step Challenge")
                .summary(summary)
                .description(STEP_CHALLENGE_DESCRIPTION)
                .responseFields(commonResponseFields())
                .build())
        );
    }

    private static Stream<StepEndpoint> authenticatedStepEndpoints() {
        return Stream.of(
            new StepEndpoint("step-challenge-current", "걸음수 챌린지 현황",
                () -> get("/api/v1/groups/{groupId}/challenges/step/current", 1L)),
            new StepEndpoint("step-challenge-regions", "지금까지 걸어간 지역 확인",
                () -> get("/api/v1/groups/{groupId}/challenges/step/regions", 1L)),
            new StepEndpoint("step-challenge-options", "변경 가능한 걸음수 챌린지 목록",
                () -> get("/api/v1/groups/{groupId}/challenges/step/options", 1L)),
            new StepEndpoint("step-challenge-rankings", "기여도 순위",
                () -> get("/api/v1/groups/{groupId}/challenges/step/rankings", 1L)),
            new StepEndpoint("step-challenge-change", "챌린지 변경",
                () -> patch("/api/v1/groups/{groupId}/challenges/step/current", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "challengeId": 2
                        }
                        """))
        );
    }

    private void givenStepEndpointThrows(StepEndpoint endpoint, ErrorStatus errorStatus) {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        GeneralException exception = new GeneralException(errorStatus);
        switch (endpoint.documentPrefix()) {
            case "step-challenge-current" -> willThrow(exception)
                .given(stepChallengeService)
                .getCurrentStepChallenge(1L, 1L);
            case "step-challenge-regions" -> willThrow(exception)
                .given(stepChallengeService)
                .getWalkedRegions(1L, 1L);
            case "step-challenge-options" -> willThrow(exception)
                .given(stepChallengeService)
                .getStepChallengeOptions(1L, 1L);
            case "step-challenge-rankings" -> willThrow(exception)
                .given(stepChallengeService)
                .getStepRankings(1L, 1L);
            case "step-challenge-change" -> willThrow(exception)
                .given(stepChallengeService)
                .changeStepChallenge(1L, 1L, new StepChallengeChangeCommand(2L));
            default -> throw new IllegalArgumentException("지원하지 않는 걸음수 챌린지 문서 prefix입니다.");
        }
    }

    private record StepEndpoint(
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
