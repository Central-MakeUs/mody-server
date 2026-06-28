package cmc.mody.onboarding.presentation;

import static cmc.mody.docs.ApiDocumentUtils.commonResponseFields;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cmc.mody.auth.application.token.TokenProvider;
import cmc.mody.auth.presentation.support.CurrentMemberArgumentResolver;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.config.WebConfig;
import cmc.mody.onboarding.application.OnboardingService;
import cmc.mody.onboarding.application.OnboardingService.ProfileSetupCommand;
import cmc.mody.onboarding.application.OnboardingService.ProfileSetupResult;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OnboardingController.class)
@AutoConfigureRestDocs
@Import(WebConfig.class)
class OnboardingControllerDocsTest {
    private static final String PROFILE_DESCRIPTION = """
        소셜 로그인 후 발급받은 access token의 회원 id로 닉네임, 생년월일, 체중, 식사/운동 알림 정보를 저장한다.

        발생 가능한 예외 코드:
        - AUTH401: Authorization 헤더가 없거나 비어있음
        - AUTH402: Bearer 뒤 JWT 값이 비어있음
        - AUTH403: JWT 형식이 올바르지 않거나 refresh token을 사용함
        - AUTH404: 만료된 JWT
        - AUTH405: 지원하지 않는 JWT
        - MEMBER301: 회원 가입 입력값 검증 실패
        - MEMBER302: 토큰의 회원 id에 해당하는 회원 없음
        - MEMBER303: 이미 개인 정보 입력이 완료된 회원
        """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OnboardingService onboardingService;

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
    void setupProfile() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(onboardingService.setupProfile(eq(1L), any(ProfileSetupCommand.class)))
            .willReturn(new ProfileSetupResult(1L, 10L, true));

        mockMvc.perform(post("/api/v1/onboarding/profile")
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nickname": "민석",
                      "birthDate": "2000-01-01",
                      "currentWeightKg": 72.5,
                      "targetWeightKg": 68.0,
                      "mealSchedules": [
                        {"mealType": "BREAKFAST", "time": "08:00", "skipped": false},
                        {"mealType": "LUNCH", "time": null, "skipped": true},
                        {"mealType": "DINNER", "time": "18:00", "skipped": false}
                      ],
                      "exerciseSchedules": [
                        {"dayOfWeek": "MONDAY", "time": "07:30"},
                        {"dayOfWeek": "WEDNESDAY", "time": "20:00"},
                        {"dayOfWeek": "FRIDAY", "time": "09:00"}
                      ]
                    }
                    """))
            .andExpect(status().isOk())
            .andDo(document("onboarding-profile",
                resource(ResourceSnippetParameters.builder()
                    .tag("Onboarding")
                    .summary("개인 정보 입력")
                    .description(PROFILE_DESCRIPTION)
                    .requestFields(
                        fieldWithPath("nickname").type(JsonFieldType.STRING).description("닉네임"),
                        fieldWithPath("birthDate").type(JsonFieldType.STRING).description("생년월일(yyyy-MM-dd)"),
                        fieldWithPath("currentWeightKg").type(JsonFieldType.NUMBER).description("현재 체중 kg"),
                        fieldWithPath("targetWeightKg").type(JsonFieldType.NUMBER).description("목표 체중 kg"),
                        fieldWithPath("mealSchedules").type(JsonFieldType.ARRAY).description("식사 설정 목록. 아침/점심/저녁 3개 입력"),
                        fieldWithPath("mealSchedules[].mealType").type(JsonFieldType.STRING).description("식사 타입(BREAKFAST, LUNCH, DINNER)"),
                        fieldWithPath("mealSchedules[].time").type(JsonFieldType.STRING).description("식사 알림 시간(HH:mm). skipped=true이면 null").optional(),
                        fieldWithPath("mealSchedules[].skipped").type(JsonFieldType.BOOLEAN).description("먹지 않음 여부"),
                        fieldWithPath("exerciseSchedules").type(JsonFieldType.ARRAY).description("운동 일정 목록. 주 3회 이상 입력"),
                        fieldWithPath("exerciseSchedules[].dayOfWeek").type(JsonFieldType.STRING).description("운동 요일"),
                        fieldWithPath("exerciseSchedules[].time").type(JsonFieldType.STRING).description("운동 시간(HH:mm)")
                    )
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.memberId").type(JsonFieldType.NUMBER).description("회원 id"),
                        fieldWithPath("result.weightRecordId").type(JsonFieldType.NUMBER).description("생성된 체중 기록 id"),
                        fieldWithPath("result.personalInfoCompleted").type(JsonFieldType.BOOLEAN).description("개인 정보 입력 완료 여부")
                    ))
                    .build())
            ));
    }

    @Test
    void setupProfileWithoutAuthorization() throws Exception {
        mockMvc.perform(post("/api/v1/onboarding/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nickname": "민석",
                      "birthDate": "2000-01-01",
                      "currentWeightKg": 72.5,
                      "targetWeightKg": 68.0,
                      "mealSchedules": [
                        {"mealType": "BREAKFAST", "time": "08:00", "skipped": false},
                        {"mealType": "LUNCH", "time": null, "skipped": true},
                        {"mealType": "DINNER", "time": "18:00", "skipped": false}
                      ],
                      "exerciseSchedules": [
                        {"dayOfWeek": "MONDAY", "time": "07:30"},
                        {"dayOfWeek": "WEDNESDAY", "time": "20:00"},
                        {"dayOfWeek": "FRIDAY", "time": "09:00"}
                      ]
                    }
                    """))
            .andExpect(status().isUnauthorized())
            .andDo(document("onboarding-profile-auth-error",
                resource(ResourceSnippetParameters.builder()
                    .tag("Onboarding")
                    .summary("개인 정보 입력")
                    .description(PROFILE_DESCRIPTION)
                    .responseFields(commonResponseFields())
                    .build())
            ));
    }

    @Test
    void setupProfileWithEmptyToken() throws Exception {
        mockMvc.perform(post("/api/v1/onboarding/profile")
                .header("Authorization", "Bearer ")
                .contentType(MediaType.APPLICATION_JSON)
                .content(profileRequest()))
            .andExpect(status().isUnauthorized())
            .andDo(document("onboarding-profile-empty-token",
                resource(ResourceSnippetParameters.builder()
                    .tag("Onboarding")
                    .summary("개인 정보 입력")
                    .description(PROFILE_DESCRIPTION)
                    .responseFields(commonResponseFields())
                    .build())
            ));
    }

    @Test
    void setupProfileWithInvalidAuthorizationHeader() throws Exception {
        mockMvc.perform(post("/api/v1/onboarding/profile")
                .header("Authorization", "access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(profileRequest()))
            .andExpect(status().isUnauthorized())
            .andDo(document("onboarding-profile-invalid-token",
                resource(ResourceSnippetParameters.builder()
                    .tag("Onboarding")
                    .summary("개인 정보 입력")
                    .description(PROFILE_DESCRIPTION)
                    .responseFields(commonResponseFields())
                    .build())
            ));
    }

    @Test
    void setupProfileWithExpiredToken() throws Exception {
        willThrow(new GeneralException(ErrorStatus.EXPIRED_JWT))
            .given(tokenProvider)
            .getMemberIdByAccessToken("expired-token");

        mockMvc.perform(post("/api/v1/onboarding/profile")
                .header("Authorization", "Bearer expired-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(profileRequest()))
            .andExpect(status().isUnauthorized())
            .andDo(document("onboarding-profile-expired-token",
                resource(ResourceSnippetParameters.builder()
                    .tag("Onboarding")
                    .summary("개인 정보 입력")
                    .description(PROFILE_DESCRIPTION)
                    .responseFields(commonResponseFields())
                    .build())
            ));
    }

    @Test
    void setupProfileWithUnsupportedToken() throws Exception {
        willThrow(new GeneralException(ErrorStatus.UNSUPPORTED_JWT))
            .given(tokenProvider)
            .getMemberIdByAccessToken("unsupported-token");

        mockMvc.perform(post("/api/v1/onboarding/profile")
                .header("Authorization", "Bearer unsupported-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(profileRequest()))
            .andExpect(status().isUnauthorized())
            .andDo(document("onboarding-profile-unsupported-token",
                resource(ResourceSnippetParameters.builder()
                    .tag("Onboarding")
                    .summary("개인 정보 입력")
                    .description(PROFILE_DESCRIPTION)
                    .responseFields(commonResponseFields())
                    .build())
            ));
    }

    @Test
    void setupProfileValidationError() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);

        mockMvc.perform(post("/api/v1/onboarding/profile")
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nickname": "",
                      "birthDate": "2000-01-01",
                      "currentWeightKg": 72.5,
                      "targetWeightKg": 68.0,
                      "mealSchedules": [
                        {"mealType": "BREAKFAST", "time": "08:00", "skipped": false},
                        {"mealType": "LUNCH", "time": null, "skipped": true},
                        {"mealType": "DINNER", "time": "18:00", "skipped": false}
                      ],
                      "exerciseSchedules": [
                        {"dayOfWeek": "MONDAY", "time": "07:30"},
                        {"dayOfWeek": "WEDNESDAY", "time": "20:00"},
                        {"dayOfWeek": "FRIDAY", "time": "09:00"}
                      ]
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andDo(document("onboarding-profile-validation-error",
                resource(ResourceSnippetParameters.builder()
                    .tag("Onboarding")
                    .summary("개인 정보 입력")
                    .description(PROFILE_DESCRIPTION)
                    .responseFields(commonResponseFields())
                    .build())
            ));
    }

    @Test
    void setupProfileMemberNotFound() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.MEMBER_NOT_FOUND))
            .given(onboardingService)
            .setupProfile(eq(1L), any(ProfileSetupCommand.class));

        mockMvc.perform(post("/api/v1/onboarding/profile")
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(profileRequest()))
            .andExpect(status().isNotFound())
            .andDo(document("onboarding-profile-member-not-found",
                resource(ResourceSnippetParameters.builder()
                    .tag("Onboarding")
                    .summary("개인 정보 입력")
                    .description(PROFILE_DESCRIPTION)
                    .responseFields(commonResponseFields())
                    .build())
            ));
    }

    @Test
    void setupProfileAlreadyCompleted() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.MEMBER_PROFILE_ALREADY_COMPLETED))
            .given(onboardingService)
            .setupProfile(eq(1L), any(ProfileSetupCommand.class));

        mockMvc.perform(post("/api/v1/onboarding/profile")
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(profileRequest()))
            .andExpect(status().isConflict())
            .andDo(document("onboarding-profile-already-completed",
                resource(ResourceSnippetParameters.builder()
                    .tag("Onboarding")
                    .summary("개인 정보 입력")
                    .description(PROFILE_DESCRIPTION)
                    .responseFields(commonResponseFields())
                    .build())
            ));
    }

    @Test
    void setupWeight() throws Exception {
        mockMvc.perform(post("/api/v1/onboarding/weight")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "currentWeightKg": 72.5,
                      "targetWeightKg": 68.0
                    }
                    """))
            .andExpect(status().isOk())
            .andDo(document("onboarding-weight",
                resource(ResourceSnippetParameters.builder()
                    .tag("Onboarding")
                    .summary("[미구현] 체중 입력")
                    .description("현재 체중과 목표 체중을 저장한다.")
                    .requestFields(
                        fieldWithPath("currentWeightKg").type(JsonFieldType.NUMBER).description("현재 체중 kg"),
                        fieldWithPath("targetWeightKg").type(JsonFieldType.NUMBER).description("목표 체중 kg")
                    )
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.weightRecordId").type(JsonFieldType.NUMBER).description("체중 기록 id"),
                        fieldWithPath("result.recordedOn").type(JsonFieldType.STRING).description("기록 날짜"),
                        fieldWithPath("result.weightKg").type(JsonFieldType.NUMBER).description("현재 체중 kg"),
                        fieldWithPath("result.changeFromPreviousKg").type(JsonFieldType.NUMBER).description("이전 기록 대비 증감 kg")
                    ))
                    .build())
            ));
    }

    @Test
    void setupNotifications() throws Exception {
        mockMvc.perform(put("/api/v1/onboarding/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "mealReminderEnabled": true,
                      "mealSchedules": [
                        {"mealType": "BREAKFAST", "time": "08:00", "skipped": false},
                        {"mealType": "LUNCH", "time": null, "skipped": true},
                        {"mealType": "DINNER", "time": "18:00", "skipped": false}
                      ],
                      "exerciseReminderEnabled": true,
                      "exerciseReminderTime": "20:00"
                    }
                    """))
            .andExpect(status().isOk())
            .andDo(document("onboarding-notifications",
                resource(ResourceSnippetParameters.builder()
                    .tag("Onboarding")
                    .summary("[미구현] 알림 설정")
                    .description("식사 및 운동 기록 알림 시간을 저장한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.enabled").type(JsonFieldType.BOOLEAN).description("알림 설정 여부")
                    ))
                    .build())
            ));
    }

    @Test
    void updateHealthConnection() throws Exception {
        mockMvc.perform(put("/api/v1/onboarding/health-connection")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "connected": true
                    }
                    """))
            .andExpect(status().isOk())
            .andDo(document("onboarding-health-connection",
                resource(ResourceSnippetParameters.builder()
                    .tag("Onboarding")
                    .summary("[미구현] 건강 앱 연동 상태 저장")
                    .description("HealthKit 또는 Health Connect 연동 상태를 저장한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.connected").type(JsonFieldType.BOOLEAN).description("건강 앱 연동 여부")
                    ))
                    .build())
            ));
    }

    @Test
    void joinGroup() throws Exception {
        mockMvc.perform(post("/api/v1/onboarding/groups/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "ABCDEF"
                    }
                    """))
            .andExpect(status().isOk())
            .andDo(document("onboarding-group-join",
                resource(ResourceSnippetParameters.builder()
                    .tag("Onboarding")
                    .summary("[미구현] 그룹 코드 입력")
                    .description("그룹 코드로 온보딩 중 그룹에 참여한다.")
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
    void createGroup() throws Exception {
        mockMvc.perform(post("/api/v1/onboarding/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "모디 그룹"
                    }
                    """))
            .andExpect(status().isCreated())
            .andDo(document("onboarding-group-create",
                resource(ResourceSnippetParameters.builder()
                    .tag("Onboarding")
                    .summary("[미구현] 그룹 생성")
                    .description("그룹명을 입력해 그룹을 생성하고 초대 코드를 응답한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.groupId").type(JsonFieldType.NUMBER).description("그룹 id"),
                        fieldWithPath("result.code").type(JsonFieldType.STRING).description("그룹 코드"),
                        fieldWithPath("result.name").type(JsonFieldType.STRING).description("그룹명")
                    ))
                    .build())
            ));
    }

    private String profileRequest() {
        return """
            {
              "nickname": "민석",
              "birthDate": "2000-01-01",
              "currentWeightKg": 72.5,
              "targetWeightKg": 68.0,
              "mealSchedules": [
                {"mealType": "BREAKFAST", "time": "08:00", "skipped": false},
                {"mealType": "LUNCH", "time": null, "skipped": true},
                {"mealType": "DINNER", "time": "18:00", "skipped": false}
              ],
              "exerciseSchedules": [
                {"dayOfWeek": "MONDAY", "time": "07:30"},
                {"dayOfWeek": "WEDNESDAY", "time": "20:00"},
                {"dayOfWeek": "FRIDAY", "time": "09:00"}
              ]
            }
            """;
    }
}
