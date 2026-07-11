package cmc.mody.mypage.presentation;

import static cmc.mody.docs.ApiDocumentDescriptions.AUTHENTICATED_API;
import static cmc.mody.docs.ApiDocumentDescriptions.SCHEDULE_OWNERSHIP_RULES;
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
import cmc.mody.mypage.application.MypageService.ExerciseScheduleCommand;
import cmc.mody.mypage.application.MypageService.ExerciseScheduleResult;
import cmc.mody.mypage.application.MypageService.ExerciseScheduleUpdateCommand;
import cmc.mody.mypage.application.MypageService.ExerciseScheduleUpdateResult;
import cmc.mody.mypage.application.MypageService.GroupMemberListResult;
import cmc.mody.mypage.application.MypageService.GroupMemberResult;
import cmc.mody.mypage.application.MypageService.MealScheduleResult;
import cmc.mody.mypage.application.MypageService.MealTimeUpdateCommand;
import cmc.mody.mypage.application.MypageService.MealTimeUpdateResult;
import cmc.mody.mypage.application.MypageService.MyInfoResult;
import cmc.mody.mypage.application.MypageService.NotificationSettingCommand;
import cmc.mody.mypage.application.MypageService.NotificationSettingResult;
import cmc.mody.mypage.application.MypageService.ProfileResult;
import cmc.mody.mypage.application.MypageService.ProfileUpdateCommand;
import cmc.mody.mypage.application.MypageService.ProfileUpdateResult;
import cmc.mody.mypage.application.MypageService.WeightCreateCommand;
import cmc.mody.mypage.application.MypageService.WeightCreateResult;
import cmc.mody.mypage.application.MypageService.WeightHistoryResult;
import cmc.mody.mypage.application.MypageService.WeightRecordResult;
import cmc.mody.notification.domain.MealType;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
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
import org.springframework.restdocs.payload.FieldDescriptor;
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

        %s

        %s

        발생 가능한 예외 코드:
        - AUTH401: Authorization 헤더가 없거나 비어있음
        - AUTH402: Bearer 뒤 JWT 값이 비어있음
        - AUTH403: JWT 형식이 올바르지 않거나 refresh token을 사용함
        - AUTH404: 만료된 JWT
        - AUTH405: 지원하지 않는 JWT
        - MEMBER302: 토큰의 회원 id에 해당하는 회원 없음
        - MYPAGE301: 마이페이지 입력값 검증 실패
        - MYPAGE302: 소셜 계정 정보 없음
        """.formatted(AUTHENTICATED_API, SCHEDULE_OWNERSHIP_RULES);

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
                            .description("이전 체중 기록 대비 증감 kg. 이전 기록이 없으면 0")
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
                        fieldWithPath("weightKg").type(JsonFieldType.NUMBER).description("체중 kg. 당일 체중 기록으로 저장")
                    )
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.weightRecordId")
                            .type(JsonFieldType.NUMBER)
                            .description("체중 기록 id"),
                        fieldWithPath("result.recordedOn").type(JsonFieldType.STRING).description("기록 날짜"),
                        fieldWithPath("result.weightKg").type(JsonFieldType.NUMBER).description("체중 kg"),
                        fieldWithPath("result.changeFromPreviousKg")
                            .type(JsonFieldType.NUMBER)
                            .description("이전 체중 기록 대비 증감 kg. 이전 기록이 없으면 0")
                    ))
                    .build())
            ));
    }

    @Test
    void getMyInfo() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(mypageService.getMyInfo(1L))
            .willReturn(new MyInfoResult(1L, "민석", "profiles/member-1.jpg", 12, true, true, true));

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
                        fieldWithPath("result.daysTogether")
                            .type(JsonFieldType.NUMBER)
                            .description("회원 가입일 기준 서비스와 함께한 일수"),
                        fieldWithPath("result.personalInfoCompleted")
                            .type(JsonFieldType.BOOLEAN)
                            .description("개인 정보 입력 완료 여부"),
                        fieldWithPath("result.groupOnboardingCompleted")
                            .type(JsonFieldType.BOOLEAN)
                            .description("그룹 생성 또는 참여를 한 번이라도 완료했는지 여부"),
                        fieldWithPath("result.mainAccessible")
                            .type(JsonFieldType.BOOLEAN)
                            .description("개인 정보 입력 완료와 현재 참여 그룹 1개 이상을 모두 만족하는지 여부")
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
                        fieldWithPath("nickname")
                            .type(JsonFieldType.STRING)
                            .description("닉네임. 그룹 내 중복 허용"),
                        fieldWithPath("birthDate").type(JsonFieldType.STRING).description("생년월일(yyyy-MM-dd)")
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
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);

        mockMvc.perform(delete("/api/v1/mypage/me")
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isOk())
            .andDo(document("mypage-delete-me",
                resource(ResourceSnippetParameters.builder()
                    .tag("Mypage")
                    .summary("회원 탈퇴")
                    .description(MYPAGE_DESCRIPTION)
                    .responseFields(commonResponseFields())
                    .build())
            ));
    }

    @Test
    void getNotificationSettings() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(mypageService.getNotificationSettings(1L)).willReturn(notificationSettingResult());

        mockMvc.perform(get("/api/v1/mypage/notification-settings")
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isOk())
            .andDo(document("mypage-notification-settings",
                resource(ResourceSnippetParameters.builder()
                    .tag("Mypage")
                    .summary("알림 설정 조회")
                    .description(MYPAGE_DESCRIPTION)
                    .responseFields(commonResponseFields(
                        notificationSettingResponseFields()
                    ))
                    .build())
            ));
    }

    @Test
    void updateNotificationSettings() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(mypageService.updateNotificationSettings(eq(1L), any(NotificationSettingCommand.class)))
            .willReturn(notificationSettingResult());

        mockMvc.perform(patch("/api/v1/mypage/notification-settings")
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "recordReminderEnabled": true,
                      "commentNotificationEnabled": true,
                      "challengeNotificationEnabled": true
                    }
                    """))
            .andExpect(status().isOk())
            .andDo(document("mypage-notification-settings-update",
                resource(ResourceSnippetParameters.builder()
                    .tag("Mypage")
                    .summary("알림 설정 수정")
                    .description(MYPAGE_DESCRIPTION)
                    .requestFields(
                        fieldWithPath("recordReminderEnabled")
                            .type(JsonFieldType.BOOLEAN)
                            .description("식사와 운동 기록 알림 통합 수신 여부. 시간표 값은 변경하지 않음"),
                        fieldWithPath("commentNotificationEnabled")
                            .type(JsonFieldType.BOOLEAN)
                            .description("코멘트 알림 여부"),
                        fieldWithPath("challengeNotificationEnabled")
                            .type(JsonFieldType.BOOLEAN)
                            .description("챌린지 알림 여부")
                    )
                    .responseFields(commonResponseFields(
                        notificationSettingResponseFields()
                    ))
                    .build())
            ));
    }

    @Test
    void updateExerciseSchedules() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(mypageService.updateExerciseSchedules(eq(1L), any(ExerciseScheduleUpdateCommand.class)))
            .willReturn(exerciseScheduleUpdateResult());

        mockMvc.perform(put("/api/v1/mypage/exercise-schedules")
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "schedules": [
                        {"dayOfWeek": "MONDAY", "time": "07:30"},
                        {"dayOfWeek": "WEDNESDAY", "time": "20:00"},
                        {"dayOfWeek": "FRIDAY", "time": "09:00"}
                      ]
                    }
                    """))
            .andExpect(status().isOk())
            .andDo(document("mypage-exercise-schedules-update",
                resource(ResourceSnippetParameters.builder()
                    .tag("Mypage")
                    .summary("운동 일정 수정")
                    .description(MYPAGE_DESCRIPTION)
                    .requestFields(
                        fieldWithPath("schedules").type(JsonFieldType.ARRAY).description("운동 일정 목록. 최소 개수 제한 없음"),
                        fieldWithPath("schedules[].dayOfWeek")
                            .type(JsonFieldType.STRING)
                            .description("운동 요일. MONDAY~SUNDAY"),
                        fieldWithPath("schedules[].time").type(JsonFieldType.STRING).description("운동 시간(HH:mm)")
                    )
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.schedules[].dayOfWeek").type(JsonFieldType.STRING).description("운동 요일"),
                        fieldWithPath("result.schedules[].time").type(JsonFieldType.STRING).description("운동 시간(HH:mm)")
                    ))
                    .build())
            ));
    }

    @Test
    void updateMealTimes() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(mypageService.updateMealTimes(eq(1L), any(MealTimeUpdateCommand.class)))
            .willReturn(mealTimeUpdateResult());

        mockMvc.perform(put("/api/v1/mypage/meal-times")
                .header("Authorization", "Bearer access-token")
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
                    .summary("식사 시간 수정")
                    .description(MYPAGE_DESCRIPTION)
                    .requestFields(
                        mealScheduleRequestFields("mealSchedules")
                    )
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
    void updateExerciseSchedulesValidationError() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);

        mockMvc.perform(put("/api/v1/mypage/exercise-schedules")
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "schedules": [
                        {"dayOfWeek": "MONDAY", "time": null}
                      ]
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andDo(documentError("mypage-exercise-schedules-update-validation-error", "운동 일정 수정"));
    }

    @Test
    void updateMealTimesValidationError() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);

        mockMvc.perform(put("/api/v1/mypage/meal-times")
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "mealSchedules": [
                        {"mealType": "BREAKFAST", "time": null, "skipped": false},
                        {"mealType": "LUNCH", "time": null, "skipped": true},
                        {"mealType": "DINNER", "time": "18:00", "skipped": false}
                      ]
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andDo(documentError("mypage-meal-times-update-validation-error", "식사 시간 수정"));
    }

    @Test
    void getGroupMembers() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(mypageService.getGroupMembers(1L, 100L))
            .willReturn(new GroupMemberListResult(List.of(
                new GroupMemberResult(2L, "도윤", "profiles/member-2.jpg")
            )));

        mockMvc.perform(get("/api/v1/mypage/groups/{groupId}/members", 100L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isOk())
            .andDo(document("mypage-group-members",
                resource(ResourceSnippetParameters.builder()
                    .tag("Mypage")
                    .summary("그룹 구성원 조회")
                    .description(MYPAGE_DESCRIPTION)
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

        mockMvc.perform(delete("/api/v1/mypage/groups/{groupId}/members/me", 100L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isOk())
            .andDo(document("mypage-group-leave",
                resource(ResourceSnippetParameters.builder()
                    .tag("Mypage")
                    .summary("그룹 나가기")
                    .description(MYPAGE_DESCRIPTION)
                    .responseFields(commonResponseFields())
                    .build())
            ));
    }

    @Test
    void getGroupMembersGroupNotFound() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.GROUP_NOT_FOUND))
            .given(mypageService)
            .getGroupMembers(1L, 100L);

        mockMvc.perform(get("/api/v1/mypage/groups/{groupId}/members", 100L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isNotFound())
            .andDo(documentError("mypage-group-members-group-not-found", "그룹 구성원 조회"));
    }

    @Test
    void getGroupMembersGroupMemberNotFound() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.GROUP_MEMBER_NOT_FOUND))
            .given(mypageService)
            .getGroupMembers(1L, 100L);

        mockMvc.perform(get("/api/v1/mypage/groups/{groupId}/members", 100L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isNotFound())
            .andDo(documentError("mypage-group-members-member-not-found", "그룹 구성원 조회"));
    }

    @Test
    void leaveGroupGroupMemberNotFound() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.GROUP_MEMBER_NOT_FOUND))
            .given(mypageService)
            .leaveGroup(1L, 100L);

        mockMvc.perform(delete("/api/v1/mypage/groups/{groupId}/members/me", 100L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isNotFound())
            .andDo(documentError("mypage-group-leave-member-not-found", "그룹 나가기"));
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
            ),
            new MypageEndpoint(
                "mypage-notification-settings",
                "알림 설정 조회",
                () -> get("/api/v1/mypage/notification-settings")
            ),
            new MypageEndpoint(
                "mypage-notification-settings-update",
                "알림 설정 수정",
                () -> patch("/api/v1/mypage/notification-settings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "recordReminderEnabled": true,
                          "commentNotificationEnabled": true,
                          "challengeNotificationEnabled": true
                        }
                        """)
            ),
            new MypageEndpoint(
                "mypage-exercise-schedules-update",
                "운동 일정 수정",
                () -> put("/api/v1/mypage/exercise-schedules")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "schedules": [
                            {"dayOfWeek": "MONDAY", "time": "07:30"},
                            {"dayOfWeek": "WEDNESDAY", "time": "20:00"},
                            {"dayOfWeek": "FRIDAY", "time": "09:00"}
                          ]
                        }
                        """)
            ),
            new MypageEndpoint(
                "mypage-meal-times-update",
                "식사 시간 수정",
                () -> put("/api/v1/mypage/meal-times")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "mealSchedules": [
                            {"mealType": "BREAKFAST", "time": "08:00", "skipped": false},
                            {"mealType": "LUNCH", "time": null, "skipped": true},
                            {"mealType": "DINNER", "time": "18:00", "skipped": false}
                          ]
                        }
                        """)
            ),
            new MypageEndpoint(
                "mypage-delete-me",
                "회원 탈퇴",
                () -> delete("/api/v1/mypage/me")
            ),
            new MypageEndpoint(
                "mypage-group-members",
                "그룹 구성원 조회",
                () -> get("/api/v1/mypage/groups/{groupId}/members", 100L)
            ),
            new MypageEndpoint(
                "mypage-group-leave",
                "그룹 나가기",
                () -> delete("/api/v1/mypage/groups/{groupId}/members/me", 100L)
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
            case "mypage-notification-settings" -> willThrow(new GeneralException(ErrorStatus.MEMBER_NOT_FOUND))
                .given(mypageService)
                .getNotificationSettings(1L);
            case "mypage-notification-settings-update" -> willThrow(new GeneralException(ErrorStatus.MEMBER_NOT_FOUND))
                .given(mypageService)
                .updateNotificationSettings(eq(1L), any(NotificationSettingCommand.class));
            case "mypage-exercise-schedules-update" -> willThrow(new GeneralException(ErrorStatus.MEMBER_NOT_FOUND))
                .given(mypageService)
                .updateExerciseSchedules(eq(1L), any(ExerciseScheduleUpdateCommand.class));
            case "mypage-meal-times-update" -> willThrow(new GeneralException(ErrorStatus.MEMBER_NOT_FOUND))
                .given(mypageService)
                .updateMealTimes(eq(1L), any(MealTimeUpdateCommand.class));
            case "mypage-delete-me" -> willThrow(new GeneralException(ErrorStatus.MEMBER_NOT_FOUND))
                .given(mypageService)
                .deleteMe(1L);
            case "mypage-group-members" -> willThrow(new GeneralException(ErrorStatus.MEMBER_NOT_FOUND))
                .given(mypageService)
                .getGroupMembers(1L, 100L);
            case "mypage-group-leave" -> willThrow(new GeneralException(ErrorStatus.MEMBER_NOT_FOUND))
                .given(mypageService)
                .leaveGroup(1L, 100L);
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

    private FieldDescriptor[] notificationSettingResponseFields() {
        return new FieldDescriptor[]{
            fieldWithPath("result.recordReminderEnabled")
                .type(JsonFieldType.BOOLEAN)
                .description("식사와 운동 기록 알림 통합 수신 여부"),
            fieldWithPath("result.commentNotificationEnabled")
                .type(JsonFieldType.BOOLEAN)
                .description("코멘트 알림 여부"),
            fieldWithPath("result.challengeNotificationEnabled")
                .type(JsonFieldType.BOOLEAN)
                .description("챌린지 알림 여부"),
            fieldWithPath("result.mealSchedules").type(JsonFieldType.ARRAY).description("식사 설정 목록"),
            fieldWithPath("result.mealSchedules[].mealType").type(JsonFieldType.STRING).description("식사 타입"),
            fieldWithPath("result.mealSchedules[].time")
                .type(JsonFieldType.STRING)
                .description("식사 알림 시간(HH:mm). skipped=true이면 null")
                .optional(),
            fieldWithPath("result.mealSchedules[].skipped")
                .type(JsonFieldType.BOOLEAN)
                .description("먹지 않음 여부"),
            fieldWithPath("result.exerciseSchedules").type(JsonFieldType.ARRAY).description("운동 일정 목록"),
            fieldWithPath("result.exerciseSchedules[].dayOfWeek").type(JsonFieldType.STRING).description("운동 요일"),
            fieldWithPath("result.exerciseSchedules[].time").type(JsonFieldType.STRING).description("운동 시간(HH:mm)")
        };
    }

    private FieldDescriptor[] mealScheduleRequestFields(String prefix) {
        return new FieldDescriptor[]{
            fieldWithPath(prefix).type(JsonFieldType.ARRAY).description("식사 설정 목록"),
            fieldWithPath(prefix + "[].mealType")
                .type(JsonFieldType.STRING)
                .description("식사 타입. BREAKFAST, LUNCH, DINNER"),
            fieldWithPath(prefix + "[].time")
                .type(JsonFieldType.STRING)
                .description("식사 알림 시간(HH:mm). skipped=true이면 null")
                .optional(),
            fieldWithPath(prefix + "[].skipped").type(JsonFieldType.BOOLEAN).description("먹지 않음 여부")
        };
    }

    private NotificationSettingResult notificationSettingResult() {
        return new NotificationSettingResult(
            true,
            true,
            true,
            mealScheduleResults(),
            exerciseScheduleResults()
        );
    }

    private MealTimeUpdateResult mealTimeUpdateResult() {
        return new MealTimeUpdateResult(mealScheduleResults());
    }

    private ExerciseScheduleUpdateResult exerciseScheduleUpdateResult() {
        return new ExerciseScheduleUpdateResult(exerciseScheduleResults());
    }

    private List<MealScheduleResult> mealScheduleResults() {
        return List.of(
            new MealScheduleResult(MealType.BREAKFAST, LocalTime.of(8, 0), false),
            new MealScheduleResult(MealType.LUNCH, null, true),
            new MealScheduleResult(MealType.DINNER, LocalTime.of(18, 0), false)
        );
    }

    private List<ExerciseScheduleResult> exerciseScheduleResults() {
        return List.of(
            new ExerciseScheduleResult(DayOfWeek.MONDAY, LocalTime.of(7, 30)),
            new ExerciseScheduleResult(DayOfWeek.WEDNESDAY, LocalTime.of(20, 0)),
            new ExerciseScheduleResult(DayOfWeek.FRIDAY, LocalTime.of(9, 0))
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
