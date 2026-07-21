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
import cmc.mody.challenge.application.ChallengeHomeService;
import cmc.mody.challenge.application.ChallengeHomeService.ChallengeSummaryResult;
import cmc.mody.challenge.application.ChallengeHomeService.NudgeTargetListResult;
import cmc.mody.challenge.application.ChallengeHomeService.NudgeTargetResult;
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
import cmc.mody.challenge.application.WeeklyChallengeService;
import cmc.mody.challenge.application.WeeklyChallengeService.WeeklyChallengeDetailResult;
import cmc.mody.challenge.application.WeeklyChallengeService.WeeklyChallengeListResult;
import cmc.mody.challenge.application.WeeklyChallengeService.WeeklyChallengeProofCreateCommand;
import cmc.mody.challenge.application.WeeklyChallengeService.WeeklyChallengeProofCreateResult;
import cmc.mody.challenge.application.WeeklyChallengeService.WeeklyChallengeProofListResult;
import cmc.mody.challenge.application.WeeklyChallengeService.WeeklyChallengeProofResult;
import cmc.mody.challenge.application.WeeklyChallengeService.WeeklyChallengeShareResult;
import cmc.mody.challenge.application.WeeklyChallengeService.WeeklyChallengeSummaryResult;
import cmc.mody.challenge.application.WeeklyChallengeService.ImageCropRegionCommand;
import cmc.mody.challenge.application.WeeklyChallengeService.ImageCropRegionResult;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.config.WebConfig;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import java.math.BigDecimal;
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
    private static final String CHALLENGE_HOME_DESCRIPTION = """
        챌린지 홈 API는 access token의 회원 id 기준으로 그룹 참여 여부를 검증한다.
        챌린지 홈 요약은 요청 회원의 그룹 가입일과 이번 달 그룹 활동을 기준으로 계산한다.
        버디 찌르기는 같은 그룹의 본인이 아닌 회원에게 알림 요청을 생성한다.

        발생 가능한 예외 코드:
        - AUTH401: Authorization 헤더가 없거나 비어있음
        - AUTH402: Bearer 뒤 JWT 값이 비어있음
        - AUTH403: JWT 형식이 올바르지 않거나 refresh token을 사용함
        - AUTH404: 만료된 JWT
        - AUTH405: 지원하지 않는 JWT
        - MEMBER302: 토큰의 회원 id에 해당하는 회원 없음
        - GROUP302: 그룹 없음
        - GROUP306: 그룹 참여 정보 없음
        - CHALLENGE301: 본인 찌르기 등 챌린지 요청값 검증 실패
        """;
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
    private static final String WEEKLY_CHALLENGE_DESCRIPTION = """
        주간 챌린지 API는 access token의 회원 id 기준으로 처리한다.
        그룹 기반 API는 요청 회원의 그룹 참여 여부를 검증한다.
        인증 이미지는 Upload API에서 weekly-challenge 도메인으로 발급받은 imageKey를 전달한다.

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
        - CHALLENGE304: 이미 주간 챌린지 인증을 완료함
        - CHALLENGE305: 이미 완료된 챌린지임
        - CHALLENGE306: 완료되지 않은 챌린지임
        - CHALLENGE307: 챌린지 인증 이미지 없음
        - UPLOAD305: 스토리지 이미지 처리 실패
        """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChallengeHomeService challengeHomeService;

    @MockitoBean
    private StepChallengeService stepChallengeService;

    @MockitoBean
    private WeeklyChallengeService weeklyChallengeService;

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
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(challengeHomeService.getChallengeSummary(1L, 1L))
            .willReturn(new ChallengeSummaryResult(12, 7, 360, 2));

        mockMvc.perform(get("/api/v1/groups/{groupId}/challenges/summary", 1L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isOk())
            .andDo(document("challenge-summary",
                resource(ResourceSnippetParameters.builder()
                    .tag("Challenge")
                    .summary("챌린지 홈 요약 조회")
                    .description(CHALLENGE_HOME_DESCRIPTION)
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
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(weeklyChallengeService.getWeeklyChallenges(1L, 1L))
            .willReturn(new WeeklyChallengeListResult(List.of(
                new WeeklyChallengeSummaryResult(1L, "물 2L 마시기", "SUNDAY", 3, "민석")
            )));

        mockMvc.perform(get("/api/v1/groups/{groupId}/challenges/weekly", 1L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isOk())
            .andDo(document("weekly-challenge-list",
                resource(ResourceSnippetParameters.builder()
                    .tag("Weekly Challenge")
                    .summary("이번주의 주간 챌린지 조회")
                    .description(WEEKLY_CHALLENGE_DESCRIPTION)
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
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(challengeHomeService.getNudgeTargets(1L, 1L))
            .willReturn(new NudgeTargetListResult(List.of(
                new NudgeTargetResult(2L, "친구", "https://storage.example.com/profiles/member-2.jpg", false)
            )));

        mockMvc.perform(get("/api/v1/groups/{groupId}/challenges/nudges", 1L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isOk())
            .andDo(document("challenge-nudge-targets",
                resource(ResourceSnippetParameters.builder()
                    .tag("Challenge")
                    .summary("버디 찌르기 대상 조회")
                    .description(CHALLENGE_HOME_DESCRIPTION)
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
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);

        mockMvc.perform(post("/api/v1/groups/{groupId}/challenges/nudges/{memberId}", 1L, 2L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isOk())
            .andDo(document("challenge-nudge",
                resource(ResourceSnippetParameters.builder()
                    .tag("Challenge")
                    .summary("버디 찌르기")
                    .description(CHALLENGE_HOME_DESCRIPTION)
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

    @ParameterizedTest(name = "{0} Authorization 헤더 없음")
    @MethodSource("authenticatedChallengeHomeEndpoints")
    void authenticatedChallengeHomeApisWithoutAuthorization(ChallengeHomeEndpoint endpoint) throws Exception {
        mockMvc.perform(endpoint.request().get())
            .andExpect(status().isUnauthorized())
            .andDo(documentChallengeHomeError(endpoint.documentPrefix() + "-auth-missing", endpoint.summary()));
    }

    @ParameterizedTest(name = "{0} 빈 Bearer 토큰")
    @MethodSource("authenticatedChallengeHomeEndpoints")
    void authenticatedChallengeHomeApisWithEmptyToken(ChallengeHomeEndpoint endpoint) throws Exception {
        mockMvc.perform(endpoint.request().get()
                .header("Authorization", "Bearer "))
            .andExpect(status().isUnauthorized())
            .andDo(documentChallengeHomeError(endpoint.documentPrefix() + "-auth-empty-token", endpoint.summary()));
    }

    @ParameterizedTest(name = "{0} 잘못된 Authorization 헤더")
    @MethodSource("authenticatedChallengeHomeEndpoints")
    void authenticatedChallengeHomeApisWithInvalidAuthorizationHeader(ChallengeHomeEndpoint endpoint) throws Exception {
        mockMvc.perform(endpoint.request().get()
                .header("Authorization", "access-token"))
            .andExpect(status().isUnauthorized())
            .andDo(documentChallengeHomeError(endpoint.documentPrefix() + "-auth-invalid-header", endpoint.summary()));
    }

    @ParameterizedTest(name = "{0} 만료된 JWT")
    @MethodSource("authenticatedChallengeHomeEndpoints")
    void authenticatedChallengeHomeApisWithExpiredToken(ChallengeHomeEndpoint endpoint) throws Exception {
        willThrow(new GeneralException(ErrorStatus.EXPIRED_JWT))
            .given(tokenProvider)
            .getMemberIdByAccessToken("expired-token");

        mockMvc.perform(endpoint.request().get()
                .header("Authorization", "Bearer expired-token"))
            .andExpect(status().isUnauthorized())
            .andDo(documentChallengeHomeError(endpoint.documentPrefix() + "-auth-expired-token", endpoint.summary()));
    }

    @ParameterizedTest(name = "{0} 유효하지 않은 JWT")
    @MethodSource("authenticatedChallengeHomeEndpoints")
    void authenticatedChallengeHomeApisWithInvalidToken(ChallengeHomeEndpoint endpoint) throws Exception {
        willThrow(new GeneralException(ErrorStatus.INVALID_JWT))
            .given(tokenProvider)
            .getMemberIdByAccessToken("invalid-token");

        mockMvc.perform(endpoint.request().get()
                .header("Authorization", "Bearer invalid-token"))
            .andExpect(status().isUnauthorized())
            .andDo(documentChallengeHomeError(endpoint.documentPrefix() + "-auth-invalid-token", endpoint.summary()));
    }

    @ParameterizedTest(name = "{0} 지원하지 않는 JWT")
    @MethodSource("authenticatedChallengeHomeEndpoints")
    void authenticatedChallengeHomeApisWithUnsupportedToken(ChallengeHomeEndpoint endpoint) throws Exception {
        willThrow(new GeneralException(ErrorStatus.UNSUPPORTED_JWT))
            .given(tokenProvider)
            .getMemberIdByAccessToken("unsupported-token");

        mockMvc.perform(endpoint.request().get()
                .header("Authorization", "Bearer unsupported-token"))
            .andExpect(status().isUnauthorized())
            .andDo(documentChallengeHomeError(endpoint.documentPrefix() + "-auth-unsupported-token", endpoint.summary()));
    }

    @ParameterizedTest(name = "{0} 회원 없음")
    @MethodSource("authenticatedChallengeHomeEndpoints")
    void authenticatedChallengeHomeApisMemberNotFound(ChallengeHomeEndpoint endpoint) throws Exception {
        givenChallengeHomeEndpointThrows(endpoint, ErrorStatus.MEMBER_NOT_FOUND);

        mockMvc.perform(endpoint.request().get()
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isNotFound())
            .andDo(documentChallengeHomeError(endpoint.documentPrefix() + "-member-not-found", endpoint.summary()));
    }

    @ParameterizedTest(name = "{0} 그룹 없음")
    @MethodSource("authenticatedChallengeHomeEndpoints")
    void authenticatedChallengeHomeApisGroupNotFound(ChallengeHomeEndpoint endpoint) throws Exception {
        givenChallengeHomeEndpointThrows(endpoint, ErrorStatus.GROUP_NOT_FOUND);

        mockMvc.perform(endpoint.request().get()
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isNotFound())
            .andDo(documentChallengeHomeError(endpoint.documentPrefix() + "-group-not-found", endpoint.summary()));
    }

    @ParameterizedTest(name = "{0} 그룹 참여 정보 없음")
    @MethodSource("authenticatedChallengeHomeEndpoints")
    void authenticatedChallengeHomeApisGroupMemberNotFound(ChallengeHomeEndpoint endpoint) throws Exception {
        givenChallengeHomeEndpointThrows(endpoint, ErrorStatus.GROUP_MEMBER_NOT_FOUND);

        mockMvc.perform(endpoint.request().get()
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isNotFound())
            .andDo(documentChallengeHomeError(endpoint.documentPrefix() + "-group-member-not-found", endpoint.summary()));
    }

    @Test
    void nudgeMemberInvalidSelfRequest() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.CHALLENGE_VALIDATION_FAILED))
            .given(challengeHomeService)
            .nudgeMember(1L, 1L, 1L);

        mockMvc.perform(post("/api/v1/groups/{groupId}/challenges/nudges/{memberId}", 1L, 1L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isBadRequest())
            .andDo(documentChallengeHomeError("challenge-nudge-invalid-self", "버디 찌르기"));
    }

    @Test
    void getWeeklyChallengeDetail() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(weeklyChallengeService.getWeeklyChallengeDetail(1L, 1L))
            .willReturn(new WeeklyChallengeDetailResult(1L, "물 2L 마시기", "하루 동안 물 2L를 마시고 사진으로 인증한다."));

        mockMvc.perform(get("/api/v1/weekly-challenges/{challengeId}", 1L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isOk())
            .andDo(document("weekly-challenge-detail",
                resource(ResourceSnippetParameters.builder()
                    .tag("Weekly Challenge")
                    .summary("주간 챌린지 상세 조회")
                    .description(WEEKLY_CHALLENGE_DESCRIPTION)
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
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(weeklyChallengeService.getWeeklyChallengeProofs(1L, 1L, 1L))
            .willReturn(new WeeklyChallengeProofListResult(List.of(
                new WeeklyChallengeProofResult(
                    1L,
                    "https://storage.example.com/weekly-challenges/1/proof.jpg",
                    imageCropRegionResult(),
                    1L,
                    "민석",
                    "https://storage.example.com/profiles/member-1.jpg"
                )
            )));

        mockMvc.perform(get("/api/v1/groups/{groupId}/weekly-challenges/{groupChallengeId}/proofs", 1L, 1L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isOk())
            .andDo(document("weekly-challenge-proofs",
                resource(ResourceSnippetParameters.builder()
                    .tag("Weekly Challenge")
                    .summary("그룹원 인증 이미지 조회")
                    .description(WEEKLY_CHALLENGE_DESCRIPTION)
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.proofs[].proofId").type(JsonFieldType.NUMBER).description("인증 id"),
                        fieldWithPath("result.proofs[].imageUrl").type(JsonFieldType.STRING).description("인증 이미지 URL"),
                        fieldWithPath("result.proofs[].imageCropRegion")
                            .type(JsonFieldType.OBJECT)
                            .description("원본 이미지 기준 관심 영역 정규화 좌표. 없으면 null"),
                        fieldWithPath("result.proofs[].imageCropRegion.x")
                            .type(JsonFieldType.NUMBER)
                            .description("좌상단 x 좌표. 0~1 정규화 값"),
                        fieldWithPath("result.proofs[].imageCropRegion.y")
                            .type(JsonFieldType.NUMBER)
                            .description("좌상단 y 좌표. 0~1 정규화 값"),
                        fieldWithPath("result.proofs[].imageCropRegion.width")
                            .type(JsonFieldType.NUMBER)
                            .description("관심 영역 width. 0~1 정규화 값"),
                        fieldWithPath("result.proofs[].imageCropRegion.height")
                            .type(JsonFieldType.NUMBER)
                            .description("관심 영역 height. 0~1 정규화 값"),
                        fieldWithPath("result.proofs[].memberId").type(JsonFieldType.NUMBER).description("회원 id"),
                        fieldWithPath("result.proofs[].nickname").type(JsonFieldType.STRING).description("닉네임"),
                        fieldWithPath("result.proofs[].profileImageUrl").type(JsonFieldType.STRING)
                            .description("프로필 이미지")
                    ))
                    .build())
            ));
    }

    @Test
    void createWeeklyChallengeProof() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(weeklyChallengeService.createWeeklyChallengeProof(
            1L,
            1L,
            1L,
            new WeeklyChallengeProofCreateCommand(
                "weekly-challenges/1/proof.jpg",
                imageCropRegionCommand()
            )
        )).willReturn(new WeeklyChallengeProofCreateResult(
            10L,
            1L,
            "https://storage.example.com/weekly-challenges/1/proof.jpg",
            imageCropRegionResult()
        ));

        mockMvc.perform(post("/api/v1/groups/{groupId}/weekly-challenges/{groupChallengeId}/proofs", 1L, 1L)
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "imageKey": "weekly-challenges/1/proof.jpg",
                      "imageCropRegion": {
                        "x": 0.22985781990521326,
                        "y": 0.3815165876777251,
                        "width": 0.5402843601895736,
                        "height": 0.23696682464454974
                      }
                    }
                    """))
            .andExpect(status().isCreated())
            .andDo(document("weekly-challenge-proof-create",
                requestFields(
                    fieldWithPath("imageKey")
                        .type(JsonFieldType.STRING)
                        .description("weekly-challenge 도메인으로 발급받은 업로드 이미지 key"),
                    fieldWithPath("imageCropRegion")
                        .type(JsonFieldType.OBJECT)
                        .optional()
                        .description("원본 이미지 기준 관심 영역 정규화 좌표. 생략 가능"),
                    fieldWithPath("imageCropRegion.x")
                        .type(JsonFieldType.NUMBER)
                        .description("좌상단 x 좌표. 0~1 정규화 값"),
                    fieldWithPath("imageCropRegion.y")
                        .type(JsonFieldType.NUMBER)
                        .description("좌상단 y 좌표. 0~1 정규화 값"),
                    fieldWithPath("imageCropRegion.width")
                        .type(JsonFieldType.NUMBER)
                        .description("관심 영역 width. 0~1 정규화 값"),
                    fieldWithPath("imageCropRegion.height")
                        .type(JsonFieldType.NUMBER)
                        .description("관심 영역 height. 0~1 정규화 값")
                ),
                resource(ResourceSnippetParameters.builder()
                    .tag("Weekly Challenge")
                    .summary("주간 챌린지 인증 업로드")
                    .description(WEEKLY_CHALLENGE_DESCRIPTION)
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.proofId").type(JsonFieldType.NUMBER).description("인증 id"),
                        fieldWithPath("result.groupChallengeId").type(JsonFieldType.NUMBER)
                            .description("그룹 챌린지 id"),
                        fieldWithPath("result.imageUrl").type(JsonFieldType.STRING).description("인증 이미지 URL"),
                        fieldWithPath("result.imageCropRegion")
                            .type(JsonFieldType.OBJECT)
                            .description("원본 이미지 기준 관심 영역 정규화 좌표. 없으면 null"),
                        fieldWithPath("result.imageCropRegion.x")
                            .type(JsonFieldType.NUMBER)
                            .description("좌상단 x 좌표. 0~1 정규화 값"),
                        fieldWithPath("result.imageCropRegion.y")
                            .type(JsonFieldType.NUMBER)
                            .description("좌상단 y 좌표. 0~1 정규화 값"),
                        fieldWithPath("result.imageCropRegion.width")
                            .type(JsonFieldType.NUMBER)
                            .description("관심 영역 width. 0~1 정규화 값"),
                        fieldWithPath("result.imageCropRegion.height")
                            .type(JsonFieldType.NUMBER)
                            .description("관심 영역 height. 0~1 정규화 값")
                    ))
                    .build())
            ));
    }

    @Test
    void createWeeklyChallengeProofAlreadyExists() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.CHALLENGE_PROOF_ALREADY_EXISTS))
            .given(weeklyChallengeService)
            .createWeeklyChallengeProof(
                1L,
                1L,
                1L,
                new WeeklyChallengeProofCreateCommand("weekly-challenges/1/proof.jpg", null)
            );

        mockMvc.perform(post("/api/v1/groups/{groupId}/weekly-challenges/{groupChallengeId}/proofs", 1L, 1L)
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "imageKey": "weekly-challenges/1/proof.jpg"
                    }
                    """))
            .andExpect(status().isConflict())
            .andDo(documentWeeklyError("weekly-challenge-proof-create-already-exists", "주간 챌린지 인증 업로드"));
    }

    @Test
    void createWeeklyChallengeProofAlreadyCompleted() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.CHALLENGE_ALREADY_COMPLETED))
            .given(weeklyChallengeService)
            .createWeeklyChallengeProof(
                1L,
                1L,
                1L,
                new WeeklyChallengeProofCreateCommand("weekly-challenges/1/proof.jpg", null)
            );

        mockMvc.perform(post("/api/v1/groups/{groupId}/weekly-challenges/{groupChallengeId}/proofs", 1L, 1L)
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "imageKey": "weekly-challenges/1/proof.jpg"
                    }
                    """))
            .andExpect(status().isConflict())
            .andDo(documentWeeklyError("weekly-challenge-proof-create-already-completed", "주간 챌린지 인증 업로드"));
    }

    @Test
    void createWeeklyChallengeProofInvalidRequest() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);

        mockMvc.perform(post("/api/v1/groups/{groupId}/weekly-challenges/{groupChallengeId}/proofs", 1L, 1L)
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "imageKey": ""
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andDo(documentWeeklyError("weekly-challenge-proof-create-invalid-request", "주간 챌린지 인증 업로드"));
    }

    @Test
    void getWeeklyChallengeDetailNotFound() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.CHALLENGE_NOT_FOUND))
            .given(weeklyChallengeService)
            .getWeeklyChallengeDetail(1L, 1L);

        mockMvc.perform(get("/api/v1/weekly-challenges/{challengeId}", 1L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isNotFound())
            .andDo(documentWeeklyError("weekly-challenge-detail-not-found", "주간 챌린지 상세 조회"));
    }

    @Test
    void getWeeklyChallengeProofsGroupMemberNotFound() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.GROUP_MEMBER_NOT_FOUND))
            .given(weeklyChallengeService)
            .getWeeklyChallengeProofs(1L, 1L, 1L);

        mockMvc.perform(get("/api/v1/groups/{groupId}/weekly-challenges/{groupChallengeId}/proofs", 1L, 1L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isNotFound())
            .andDo(documentWeeklyError("weekly-challenge-proofs-group-member-not-found", "그룹원 인증 이미지 조회"));
    }

    @Test
    void shareWeeklyChallenge() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(weeklyChallengeService.shareWeeklyChallenge(1L, 1L, 1L))
            .willReturn(new WeeklyChallengeShareResult(
                "https://storage.example.com/weekly-challenge-shares/1/1.jpg",
                null,
                2,
                2
            ));

        mockMvc.perform(post("/api/v1/groups/{groupId}/weekly-challenges/{groupChallengeId}/share", 1L, 1L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isOk())
            .andDo(document("weekly-challenge-share",
                resource(ResourceSnippetParameters.builder()
                    .tag("Weekly Challenge")
                    .summary("챌린지 완료 공유하기")
                    .description(WEEKLY_CHALLENGE_DESCRIPTION)
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.imageUrl").type(JsonFieldType.STRING).description("공유 이미지 URL"),
                        fieldWithPath("result.imageCropRegion")
                            .type(JsonFieldType.NULL)
                            .description("공유 이미지는 서버 합성 이미지이므로 null"),
                        fieldWithPath("result.rows").type(JsonFieldType.NUMBER).description("그리드 행 수"),
                        fieldWithPath("result.columns").type(JsonFieldType.NUMBER).description("그리드 열 수")
                    ))
                    .build())
            ));
    }

    @Test
    void shareWeeklyChallengeNotCompleted() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.CHALLENGE_NOT_COMPLETED))
            .given(weeklyChallengeService)
            .shareWeeklyChallenge(1L, 1L, 1L);

        mockMvc.perform(post("/api/v1/groups/{groupId}/weekly-challenges/{groupChallengeId}/share", 1L, 1L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isConflict())
            .andDo(documentWeeklyError("weekly-challenge-share-not-completed", "챌린지 완료 공유하기"));
    }

    @Test
    void shareWeeklyChallengeProofNotFound() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.CHALLENGE_PROOF_NOT_FOUND))
            .given(weeklyChallengeService)
            .shareWeeklyChallenge(1L, 1L, 1L);

        mockMvc.perform(post("/api/v1/groups/{groupId}/weekly-challenges/{groupChallengeId}/share", 1L, 1L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isNotFound())
            .andDo(documentWeeklyError("weekly-challenge-share-proof-not-found", "챌린지 완료 공유하기"));
    }

    @Test
    void shareWeeklyChallengeStorageFailed() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.UPLOAD_STORAGE_OPERATION_FAILED))
            .given(weeklyChallengeService)
            .shareWeeklyChallenge(1L, 1L, 1L);

        mockMvc.perform(post("/api/v1/groups/{groupId}/weekly-challenges/{groupChallengeId}/share", 1L, 1L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isInternalServerError())
            .andDo(documentWeeklyError("weekly-challenge-share-storage-failed", "챌린지 완료 공유하기"));
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

    private RestDocumentationResultHandler documentChallengeHomeError(String identifier, String summary) {
        return document(identifier,
            resource(ResourceSnippetParameters.builder()
                .tag("Challenge")
                .summary(summary)
                .description(CHALLENGE_HOME_DESCRIPTION)
                .responseFields(commonResponseFields())
                .build())
        );
    }

    private RestDocumentationResultHandler documentWeeklyError(String identifier, String summary) {
        return document(identifier,
            resource(ResourceSnippetParameters.builder()
                .tag("Weekly Challenge")
                .summary(summary)
                .description(WEEKLY_CHALLENGE_DESCRIPTION)
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

    private static Stream<ChallengeHomeEndpoint> authenticatedChallengeHomeEndpoints() {
        return Stream.of(
            new ChallengeHomeEndpoint("challenge-summary", "챌린지 홈 요약 조회",
                () -> get("/api/v1/groups/{groupId}/challenges/summary", 1L)),
            new ChallengeHomeEndpoint("challenge-nudge-targets", "버디 찌르기 대상 조회",
                () -> get("/api/v1/groups/{groupId}/challenges/nudges", 1L)),
            new ChallengeHomeEndpoint("challenge-nudge", "버디 찌르기",
                () -> post("/api/v1/groups/{groupId}/challenges/nudges/{memberId}", 1L, 2L))
        );
    }

    private void givenChallengeHomeEndpointThrows(ChallengeHomeEndpoint endpoint, ErrorStatus errorStatus) {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        GeneralException exception = new GeneralException(errorStatus);
        switch (endpoint.documentPrefix()) {
            case "challenge-summary" -> willThrow(exception)
                .given(challengeHomeService)
                .getChallengeSummary(1L, 1L);
            case "challenge-nudge-targets" -> willThrow(exception)
                .given(challengeHomeService)
                .getNudgeTargets(1L, 1L);
            case "challenge-nudge" -> willThrow(exception)
                .given(challengeHomeService)
                .nudgeMember(1L, 1L, 2L);
            default -> throw new IllegalArgumentException("지원하지 않는 챌린지 홈 문서 prefix입니다.");
        }
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

    private ImageCropRegionCommand imageCropRegionCommand() {
        return new ImageCropRegionCommand(
            new BigDecimal("0.22985781990521326"),
            new BigDecimal("0.3815165876777251"),
            new BigDecimal("0.5402843601895736"),
            new BigDecimal("0.23696682464454974")
        );
    }

    private ImageCropRegionResult imageCropRegionResult() {
        return new ImageCropRegionResult(
            new BigDecimal("0.22985781990521326"),
            new BigDecimal("0.3815165876777251"),
            new BigDecimal("0.5402843601895736"),
            new BigDecimal("0.23696682464454974")
        );
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

    private record ChallengeHomeEndpoint(
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
