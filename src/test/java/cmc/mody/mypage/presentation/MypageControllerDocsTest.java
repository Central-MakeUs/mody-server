package cmc.mody.mypage.presentation;

import static cmc.mody.docs.ApiDocumentUtils.commonResponseFields;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cmc.mody.auth.application.token.TokenProvider;
import cmc.mody.auth.presentation.support.CurrentMemberArgumentResolver;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.config.WebConfig;
import cmc.mody.mypage.application.MypageService;
import cmc.mody.mypage.application.MypageService.MyInfoResult;
import cmc.mody.mypage.application.MypageService.ProfileResult;
import cmc.mody.mypage.application.MypageService.ProfileUpdateCommand;
import cmc.mody.mypage.application.MypageService.ProfileUpdateResult;
import cmc.mody.mypage.application.MypageService.WeightCreateCommand;
import cmc.mody.mypage.application.MypageService.WeightCreateResult;
import cmc.mody.mypage.application.MypageService.WeightHistoryResult;
import cmc.mody.mypage.application.MypageService.WeightRecordResult;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import java.math.BigDecimal;
import java.time.LocalDate;
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

@WebMvcTest(MypageController.class)
@AutoConfigureRestDocs
@Import(WebConfig.class)
class MypageControllerDocsTest {
    private static final String MYPAGE_DESCRIPTION = """
        구현된 마이페이지 API는 access token의 회원 id 기준으로 처리한다.

        발생 가능한 예외 코드:
        - AUTH401: Authorization 헤더가 없거나 비어있음
        - AUTH402: Bearer 뒤 JWT 값이 비어있음
        - AUTH403: JWT 형식이 올바르지 않거나 refresh token을 사용함
        - AUTH404: 만료된 JWT
        - AUTH405: 지원하지 않는 JWT
        - MEMBER302: 토큰의 회원 id에 해당하는 회원 없음
        - MYPAGE301: 마이페이지 입력값 검증 실패
        - MYPAGE302: 소셜 계정 정보 없음
        """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MypageService mypageService;

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
    void getWeightHistory() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(mypageService.getWeightHistory(1L))
            .willReturn(new WeightHistoryResult(List.of(
                new WeightRecordResult(
                    10L,
                    LocalDate.of(2026, 6, 28),
                    new BigDecimal("72.50"),
                    new BigDecimal("-0.50")
                )
            )));

        mockMvc.perform(get("/api/v1/mypage/weights")
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isOk())
            .andDo(document("mypage-weight-history",
                resource(ResourceSnippetParameters.builder()
                    .tag("Mypage")
                    .summary("체중 기록 변화 조회")
                    .description(MYPAGE_DESCRIPTION)
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.weights[].weightRecordId")
                            .type(JsonFieldType.NUMBER)
                            .description("체중 기록 id"),
                        fieldWithPath("result.weights[].recordedOn")
                            .type(JsonFieldType.STRING)
                            .description("기록 날짜"),
                        fieldWithPath("result.weights[].weightKg").type(JsonFieldType.NUMBER).description("체중 kg"),
                        fieldWithPath("result.weights[].changeFromPreviousKg")
                            .type(JsonFieldType.NUMBER)
                            .description("이전 기록 대비 증감 kg")
                    ))
                    .build())
            ));
    }

    @Test
    void createWeight() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(mypageService.createWeight(eq(1L), any(WeightCreateCommand.class)))
            .willReturn(new WeightCreateResult(
                10L,
                LocalDate.of(2026, 6, 28),
                new BigDecimal("72.50"),
                new BigDecimal("-0.50")
            ));

        mockMvc.perform(post("/api/v1/mypage/weights")
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "weightKg": 72.5
                    }
                    """))
            .andExpect(status().isCreated())
            .andDo(document("mypage-weight-create",
                resource(ResourceSnippetParameters.builder()
                    .tag("Mypage")
                    .summary("체중 추가")
                    .description(MYPAGE_DESCRIPTION)
                    .requestFields(
                        fieldWithPath("weightKg").type(JsonFieldType.NUMBER).description("체중 kg")
                    )
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.weightRecordId")
                            .type(JsonFieldType.NUMBER)
                            .description("체중 기록 id"),
                        fieldWithPath("result.recordedOn").type(JsonFieldType.STRING).description("기록 날짜"),
                        fieldWithPath("result.weightKg").type(JsonFieldType.NUMBER).description("체중 kg"),
                        fieldWithPath("result.changeFromPreviousKg")
                            .type(JsonFieldType.NUMBER)
                            .description("이전 기록 대비 증감 kg")
                    ))
                    .build())
            ));
    }

    @Test
    void getMyInfo() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(mypageService.getMyInfo(1L))
            .willReturn(new MyInfoResult(1L, "민석", "profiles/member-1.jpg", 12));

        mockMvc.perform(get("/api/v1/mypage/me")
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isOk())
            .andDo(document("mypage-me",
                resource(ResourceSnippetParameters.builder()
                    .tag("Mypage")
                    .summary("내 정보 조회")
                    .description(MYPAGE_DESCRIPTION)
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.memberId").type(JsonFieldType.NUMBER).description("회원 id"),
                        fieldWithPath("result.nickname").type(JsonFieldType.STRING).description("닉네임"),
                        fieldWithPath("result.profileImageUrl")
                            .type(JsonFieldType.STRING)
                            .description("프로필 이미지 URL"),
                        fieldWithPath("result.daysTogether").type(JsonFieldType.NUMBER).description("함께한 일수")
                    ))
                    .build())
            ));
    }

    @Test
    void getProfile() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(mypageService.getProfile(1L))
            .willReturn(new ProfileResult("KAKAO", "민석", LocalDate.of(2000, 1, 1)));

        mockMvc.perform(get("/api/v1/mypage/profile")
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isOk())
            .andDo(document("mypage-profile",
                resource(ResourceSnippetParameters.builder()
                    .tag("Mypage")
                    .summary("프로필 확인")
                    .description(MYPAGE_DESCRIPTION)
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.loginType").type(JsonFieldType.STRING).description("로그인 타입"),
                        fieldWithPath("result.name").type(JsonFieldType.STRING).description("이름"),
                        fieldWithPath("result.birthDate").type(JsonFieldType.STRING).description("생년월일")
                    ))
                    .build())
            ));
    }

    @Test
    void updateProfile() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(mypageService.updateProfile(eq(1L), any(ProfileUpdateCommand.class)))
            .willReturn(new ProfileUpdateResult("민석", LocalDate.of(2000, 1, 1)));

        mockMvc.perform(patch("/api/v1/mypage/profile")
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nickname": "민석",
                      "birthDate": "2000-01-01"
                    }
                    """))
            .andExpect(status().isOk())
            .andDo(document("mypage-profile-update",
                resource(ResourceSnippetParameters.builder()
                    .tag("Mypage")
                    .summary("프로필 수정")
                    .description(MYPAGE_DESCRIPTION)
                    .requestFields(
                        fieldWithPath("nickname").type(JsonFieldType.STRING).description("닉네임"),
                        fieldWithPath("birthDate").type(JsonFieldType.STRING).description("생년월일")
                    )
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.nickname").type(JsonFieldType.STRING).description("닉네임"),
                        fieldWithPath("result.birthDate").type(JsonFieldType.STRING).description("생년월일")
                    ))
                    .build())
            ));
    }

    @Test
    void updateProfileValidationError() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);

        mockMvc.perform(patch("/api/v1/mypage/profile")
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nickname": "",
                      "birthDate": "2030-01-01"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andDo(documentError("mypage-profile-update-validation-error", "프로필 수정"));
    }

    @ParameterizedTest(name = "{0} Authorization 헤더 없음")
    @MethodSource("authenticatedMypageEndpoints")
    void authenticatedApisWithoutAuthorization(MypageEndpoint endpoint) throws Exception {
        mockMvc.perform(endpoint.request().get())
            .andExpect(status().isUnauthorized())
            .andDo(documentError(endpoint.documentPrefix() + "-auth-missing", endpoint.summary()));
    }

    @ParameterizedTest(name = "{0} 빈 Bearer 토큰")
    @MethodSource("authenticatedMypageEndpoints")
    void authenticatedApisWithEmptyToken(MypageEndpoint endpoint) throws Exception {
        mockMvc.perform(endpoint.request().get()
                .header("Authorization", "Bearer "))
            .andExpect(status().isUnauthorized())
            .andDo(documentError(endpoint.documentPrefix() + "-auth-empty-token", endpoint.summary()));
    }

    @ParameterizedTest(name = "{0} 잘못된 Authorization 헤더")
    @MethodSource("authenticatedMypageEndpoints")
    void authenticatedApisWithInvalidAuthorizationHeader(MypageEndpoint endpoint) throws Exception {
        mockMvc.perform(endpoint.request().get()
                .header("Authorization", "access-token"))
            .andExpect(status().isUnauthorized())
            .andDo(documentError(endpoint.documentPrefix() + "-auth-invalid-header", endpoint.summary()));
    }

    @ParameterizedTest(name = "{0} 만료된 JWT")
    @MethodSource("authenticatedMypageEndpoints")
    void authenticatedApisWithExpiredToken(MypageEndpoint endpoint) throws Exception {
        willThrow(new GeneralException(ErrorStatus.EXPIRED_JWT))
            .given(tokenProvider)
            .getMemberIdByAccessToken("expired-token");

        mockMvc.perform(endpoint.request().get()
                .header("Authorization", "Bearer expired-token"))
            .andExpect(status().isUnauthorized())
            .andDo(documentError(endpoint.documentPrefix() + "-auth-expired-token", endpoint.summary()));
    }

    @ParameterizedTest(name = "{0} 지원하지 않는 JWT")
    @MethodSource("authenticatedMypageEndpoints")
    void authenticatedApisWithUnsupportedToken(MypageEndpoint endpoint) throws Exception {
        willThrow(new GeneralException(ErrorStatus.UNSUPPORTED_JWT))
            .given(tokenProvider)
            .getMemberIdByAccessToken("unsupported-token");

        mockMvc.perform(endpoint.request().get()
                .header("Authorization", "Bearer unsupported-token"))
            .andExpect(status().isUnauthorized())
            .andDo(documentError(endpoint.documentPrefix() + "-auth-unsupported-token", endpoint.summary()));
    }

    @Test
    void createWeightValidationError() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);

        mockMvc.perform(post("/api/v1/mypage/weights")
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "weightKg": 10.0
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andDo(document("mypage-weight-create-validation-error",
                resource(ResourceSnippetParameters.builder()
                    .tag("Mypage")
                    .summary("체중 추가")
                    .description(MYPAGE_DESCRIPTION)
                    .responseFields(commonResponseFields())
                    .build())
            ));
    }

    @ParameterizedTest(name = "{0} 회원 없음")
    @MethodSource("authenticatedMypageEndpoints")
    void authenticatedApisMemberNotFound(MypageEndpoint endpoint) throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        givenMemberNotFound(endpoint.documentPrefix());

        mockMvc.perform(endpoint.request().get()
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isNotFound())
            .andDo(documentError(endpoint.documentPrefix() + "-member-not-found", endpoint.summary()));
    }

    @Test
    void getProfileSocialAccountNotFound() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.MYPAGE_SOCIAL_ACCOUNT_NOT_FOUND))
            .given(mypageService)
            .getProfile(1L);

        mockMvc.perform(get("/api/v1/mypage/profile")
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isNotFound())
            .andDo(document("mypage-profile-social-account-not-found",
                resource(ResourceSnippetParameters.builder()
                    .tag("Mypage")
                    .summary("프로필 확인")
                    .description(MYPAGE_DESCRIPTION)
                    .responseFields(commonResponseFields())
                    .build())
            ));
    }

    @Test
    void deleteMe() throws Exception {
        mockMvc.perform(delete("/api/v1/mypage/me"))
            .andExpect(status().isOk())
            .andDo(document("mypage-delete-me",
                resource(ResourceSnippetParameters.builder()
                    .tag("Mypage")
                    .summary("[미구현] 회원 탈퇴")
                    .description("현재 회원을 탈퇴 처리한다.")
                    .responseFields(commonResponseFields())
                    .build())
            ));
    }

    @Test
    void updateNotificationSettings() throws Exception {
        mockMvc.perform(patch("/api/v1/mypage/notification-settings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "mealReminderEnabled": true,
                      "commentNotificationEnabled": true,
                      "challengeNotificationEnabled": true,
                      "mealSchedules": [
                        {"mealType": "BREAKFAST", "time": "08:00", "skipped": false},
                        {"mealType": "LUNCH", "time": null, "skipped": true},
                        {"mealType": "DINNER", "time": "18:00", "skipped": false}
                      ],
                      "exerciseReminderTime": "20:00"
                    }
                    """))
            .andExpect(status().isOk())
            .andDo(document("mypage-notification-settings-update",
                resource(ResourceSnippetParameters.builder()
                    .tag("Mypage")
                    .summary("[미구현] 알림 설정 수정")
                    .description("식사/운동, 코멘트, 챌린지 알림 설정을 수정한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.mealReminderEnabled")
                            .type(JsonFieldType.BOOLEAN)
                            .description("식사 알림 여부"),
                        fieldWithPath("result.commentNotificationEnabled")
                            .type(JsonFieldType.BOOLEAN)
                            .description("코멘트 알림 여부"),
                        fieldWithPath("result.challengeNotificationEnabled")
                            .type(JsonFieldType.BOOLEAN)
                            .description("챌린지 알림 여부"),
                        fieldWithPath("result.mealSchedules")
                            .type(JsonFieldType.ARRAY)
                            .description("식사 설정 목록"),
                        fieldWithPath("result.mealSchedules[].mealType")
                            .type(JsonFieldType.STRING)
                            .description("식사 타입"),
                        fieldWithPath("result.mealSchedules[].time")
                            .type(JsonFieldType.STRING)
                            .description("식사 알림 시간. skipped=true이면 null")
                            .optional(),
                        fieldWithPath("result.mealSchedules[].skipped")
                            .type(JsonFieldType.BOOLEAN)
                            .description("먹지 않음 여부"),
                        fieldWithPath("result.exerciseReminderTime")
                            .type(JsonFieldType.STRING)
                            .description("운동 알림 시간")
                    ))
                    .build())
            ));
    }

    @Test
    void updateExerciseSchedules() throws Exception {
        mockMvc.perform(put("/api/v1/mypage/exercise-schedules")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "schedules": [
                        {
                          "dayOfWeek": "MONDAY",
                          "time": "20:00"
                        }
                      ]
                    }
                    """))
            .andExpect(status().isOk())
            .andDo(document("mypage-exercise-schedules-update",
                resource(ResourceSnippetParameters.builder()
                    .tag("Mypage")
                    .summary("[미구현] 운동 일정 수정")
                    .description("요일별 운동 일정을 수정한다. 최소 3개 이상 입력한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.schedules[].dayOfWeek").type(JsonFieldType.STRING).description("요일"),
                        fieldWithPath("result.schedules[].time").type(JsonFieldType.STRING).description("운동 시간")
                    ))
                    .build())
            ));
    }

    @Test
    void updateMealTimes() throws Exception {
        mockMvc.perform(put("/api/v1/mypage/meal-times")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "mealSchedules": [
                        {"mealType": "BREAKFAST", "time": "08:00", "skipped": false},
                        {"mealType": "LUNCH", "time": null, "skipped": true},
                        {"mealType": "DINNER", "time": "18:00", "skipped": false}
                      ]
                    }
                    """))
            .andExpect(status().isOk())
            .andDo(document("mypage-meal-times-update",
                resource(ResourceSnippetParameters.builder()
                    .tag("Mypage")
                    .summary("[미구현] 식사 시간 수정")
                    .description("식사 알림 시간과 먹지 않음 여부를 수정한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.mealSchedules")
                            .type(JsonFieldType.ARRAY)
                            .description("식사 설정 목록"),
                        fieldWithPath("result.mealSchedules[].mealType")
                            .type(JsonFieldType.STRING)
                            .description("식사 타입"),
                        fieldWithPath("result.mealSchedules[].time")
                            .type(JsonFieldType.STRING)
                            .description("식사 알림 시간. skipped=true이면 null")
                            .optional(),
                        fieldWithPath("result.mealSchedules[].skipped")
                            .type(JsonFieldType.BOOLEAN)
                            .description("먹지 않음 여부")
                    ))
                    .build())
            ));
    }

    @Test
    void getGroupMembers() throws Exception {
        mockMvc.perform(get("/api/v1/mypage/groups/{groupId}/members", 1L))
            .andExpect(status().isOk())
            .andDo(document("mypage-group-members",
                resource(ResourceSnippetParameters.builder()
                    .tag("Mypage")
                    .summary("[미구현] 그룹 구성원 조회")
                    .description("마이페이지에서 그룹 구성원 정보를 조회한다.")
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
        mockMvc.perform(delete("/api/v1/mypage/groups/{groupId}/members/me", 1L))
            .andExpect(status().isOk())
            .andDo(document("mypage-group-leave",
                resource(ResourceSnippetParameters.builder()
                    .tag("Mypage")
                    .summary("[미구현] 그룹 나가기")
                    .description("마이페이지에서 현재 회원이 그룹을 나간다.")
                    .responseFields(commonResponseFields())
                    .build())
            ));
    }

    private static Stream<MypageEndpoint> authenticatedMypageEndpoints() {
        return Stream.of(
            new MypageEndpoint(
                "mypage-weight-history",
                "체중 기록 변화 조회",
                () -> get("/api/v1/mypage/weights")
            ),
            new MypageEndpoint(
                "mypage-weight-create",
                "체중 추가",
                () -> post("/api/v1/mypage/weights")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "weightKg": 72.5
                        }
                        """)
            ),
            new MypageEndpoint(
                "mypage-me",
                "내 정보 조회",
                () -> get("/api/v1/mypage/me")
            ),
            new MypageEndpoint(
                "mypage-profile",
                "프로필 확인",
                () -> get("/api/v1/mypage/profile")
            ),
            new MypageEndpoint(
                "mypage-profile-update",
                "프로필 수정",
                () -> patch("/api/v1/mypage/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "nickname": "민석",
                          "birthDate": "2000-01-01"
                        }
                        """)
            )
        );
    }

    private void givenMemberNotFound(String documentPrefix) {
        switch (documentPrefix) {
            case "mypage-weight-history" -> willThrow(new GeneralException(ErrorStatus.MEMBER_NOT_FOUND))
                .given(mypageService)
                .getWeightHistory(1L);
            case "mypage-weight-create" -> willThrow(new GeneralException(ErrorStatus.MEMBER_NOT_FOUND))
                .given(mypageService)
                .createWeight(eq(1L), any(WeightCreateCommand.class));
            case "mypage-me" -> willThrow(new GeneralException(ErrorStatus.MEMBER_NOT_FOUND))
                .given(mypageService)
                .getMyInfo(1L);
            case "mypage-profile" -> willThrow(new GeneralException(ErrorStatus.MEMBER_NOT_FOUND))
                .given(mypageService)
                .getProfile(1L);
            case "mypage-profile-update" -> willThrow(new GeneralException(ErrorStatus.MEMBER_NOT_FOUND))
                .given(mypageService)
                .updateProfile(eq(1L), any(ProfileUpdateCommand.class));
            default -> throw new IllegalArgumentException(
                "지원하지 않는 마이페이지 문서 prefix입니다."
            );
        }
    }

    private RestDocumentationResultHandler documentError(String identifier, String summary) {
        return document(identifier,
            resource(ResourceSnippetParameters.builder()
                .tag("Mypage")
                .summary(summary)
                .description(MYPAGE_DESCRIPTION)
                .responseFields(commonResponseFields())
                .build())
        );
    }

    private record MypageEndpoint(
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
