package cmc.mody.onboarding.presentation;

import static cmc.mody.docs.ApiDocumentUtils.commonResponseFields;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OnboardingController.class)
@AutoConfigureRestDocs
class OnboardingControllerDocsTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void setupProfile() throws Exception {
        mockMvc.perform(post("/api/v1/onboarding/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nickname": "민석",
                      "birthDate": "2000-01-01",
                      "currentWeightKg": 72.5,
                      "targetWeightKg": 68.0,
                      "mealReminderTimes": ["08:00", "12:00", "18:00"],
                      "exerciseReminderTime": "20:00"
                    }
                    """))
            .andExpect(status().isOk())
            .andDo(document("onboarding-profile",
                resource(ResourceSnippetParameters.builder()
                    .tag("Onboarding")
                    .summary("개인 정보 입력")
                    .description("닉네임, 생년월일, 체중, 식사/운동 알림 정보를 저장한다.")
                    .requestFields(
                        fieldWithPath("nickname").type(JsonFieldType.STRING).description("닉네임"),
                        fieldWithPath("birthDate").type(JsonFieldType.STRING).description("생년월일"),
                        fieldWithPath("currentWeightKg").type(JsonFieldType.NUMBER).description("현재 체중 kg"),
                        fieldWithPath("targetWeightKg").type(JsonFieldType.NUMBER).description("목표 체중 kg"),
                        fieldWithPath("mealReminderTimes").type(JsonFieldType.ARRAY).description("식사 알림 시간 목록"),
                        fieldWithPath("exerciseReminderTime").type(JsonFieldType.STRING).description("운동 알림 시간")
                    )
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.memberId").type(JsonFieldType.NUMBER).description("회원 id"),
                        fieldWithPath("result.onboardingCompleted").type(JsonFieldType.BOOLEAN).description("온보딩 완료 여부")
                    ))
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
                    .summary("체중 입력")
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
                      "mealReminderTimes": ["08:00", "12:00", "18:00"],
                      "exerciseReminderEnabled": true,
                      "exerciseReminderTime": "20:00"
                    }
                    """))
            .andExpect(status().isOk())
            .andDo(document("onboarding-notifications",
                resource(ResourceSnippetParameters.builder()
                    .tag("Onboarding")
                    .summary("알림 설정")
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
                    .summary("건강 앱 연동 상태 저장")
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
                    .summary("그룹 코드 입력")
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
                    .summary("그룹 생성")
                    .description("그룹명을 입력해 그룹을 생성하고 초대 코드를 응답한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.groupId").type(JsonFieldType.NUMBER).description("그룹 id"),
                        fieldWithPath("result.code").type(JsonFieldType.STRING).description("그룹 코드"),
                        fieldWithPath("result.name").type(JsonFieldType.STRING).description("그룹명")
                    ))
                    .build())
            ));
    }
}
