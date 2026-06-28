package cmc.mody.mypage.presentation;

import static cmc.mody.docs.ApiDocumentUtils.commonResponseFields;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

@WebMvcTest(MypageController.class)
@AutoConfigureRestDocs
class MypageControllerDocsTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void getWeightHistory() throws Exception {
        mockMvc.perform(get("/api/v1/mypage/weights"))
            .andExpect(status().isOk())
            .andDo(document("mypage-weight-history",
                resource(ResourceSnippetParameters.builder()
                    .tag("Mypage")
                    .summary("[미구현] 체중 기록 변화 조회")
                    .description("날짜별 체중과 이전 기록 대비 증감 값을 조회한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.weights[].weightRecordId").type(JsonFieldType.NUMBER).description("체중 기록 id"),
                        fieldWithPath("result.weights[].recordedOn").type(JsonFieldType.STRING).description("기록 날짜"),
                        fieldWithPath("result.weights[].weightKg").type(JsonFieldType.NUMBER).description("체중 kg"),
                        fieldWithPath("result.weights[].changeFromPreviousKg").type(JsonFieldType.NUMBER).description("이전 기록 대비 증감 kg")
                    ))
                    .build())
            ));
    }

    @Test
    void createWeight() throws Exception {
        mockMvc.perform(post("/api/v1/mypage/weights")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "weightKg": 72.5
                    }
                    """))
            .andExpect(status().isOk())
            .andDo(document("mypage-weight-create",
                resource(ResourceSnippetParameters.builder()
                    .tag("Mypage")
                    .summary("[미구현] 체중 추가")
                    .description("체중 기록을 추가한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.weightRecordId").type(JsonFieldType.NUMBER).description("체중 기록 id"),
                        fieldWithPath("result.recordedOn").type(JsonFieldType.STRING).description("기록 날짜"),
                        fieldWithPath("result.weightKg").type(JsonFieldType.NUMBER).description("체중 kg"),
                        fieldWithPath("result.changeFromPreviousKg").type(JsonFieldType.NUMBER).description("이전 기록 대비 증감 kg")
                    ))
                    .build())
            ));
    }

    @Test
    void getMyInfo() throws Exception {
        mockMvc.perform(get("/api/v1/mypage/me"))
            .andExpect(status().isOk())
            .andDo(document("mypage-me",
                resource(ResourceSnippetParameters.builder()
                    .tag("Mypage")
                    .summary("[미구현] 내 정보 조회")
                    .description("프사, id, 닉네임, 그룹과 함께한 날짜를 조회한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.memberId").type(JsonFieldType.NUMBER).description("회원 id"),
                        fieldWithPath("result.nickname").type(JsonFieldType.STRING).description("닉네임"),
                        fieldWithPath("result.profileImageUrl").type(JsonFieldType.STRING).description("프로필 이미지 URL"),
                        fieldWithPath("result.daysTogether").type(JsonFieldType.NUMBER).description("그룹과 함께한 일수")
                    ))
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
    void getProfile() throws Exception {
        mockMvc.perform(get("/api/v1/mypage/profile"))
            .andExpect(status().isOk())
            .andDo(document("mypage-profile",
                resource(ResourceSnippetParameters.builder()
                    .tag("Mypage")
                    .summary("[미구현] 프로필 확인")
                    .description("로그인 타입, 이름, 생년월일을 조회한다.")
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
        mockMvc.perform(patch("/api/v1/mypage/profile")
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
                    .summary("[미구현] 프로필 수정")
                    .description("닉네임과 생년월일을 수정한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.nickname").type(JsonFieldType.STRING).description("닉네임"),
                        fieldWithPath("result.birthDate").type(JsonFieldType.STRING).description("생년월일")
                    ))
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
                        fieldWithPath("result.mealReminderEnabled").type(JsonFieldType.BOOLEAN).description("식사 알림 여부"),
                        fieldWithPath("result.commentNotificationEnabled").type(JsonFieldType.BOOLEAN).description("코멘트 알림 여부"),
                        fieldWithPath("result.challengeNotificationEnabled").type(JsonFieldType.BOOLEAN).description("챌린지 알림 여부"),
                        fieldWithPath("result.mealSchedules").type(JsonFieldType.ARRAY).description("식사 설정 목록"),
                        fieldWithPath("result.mealSchedules[].mealType").type(JsonFieldType.STRING).description("식사 타입"),
                        fieldWithPath("result.mealSchedules[].time").type(JsonFieldType.STRING).description("식사 알림 시간. skipped=true이면 null").optional(),
                        fieldWithPath("result.mealSchedules[].skipped").type(JsonFieldType.BOOLEAN).description("먹지 않음 여부"),
                        fieldWithPath("result.exerciseReminderTime").type(JsonFieldType.STRING).description("운동 알림 시간")
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
                    .summary("운동 일정 수정")
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
                    .summary("식사 시간 수정")
                    .description("식사 알림 시간과 먹지 않음 여부를 수정한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.mealSchedules").type(JsonFieldType.ARRAY).description("식사 설정 목록"),
                        fieldWithPath("result.mealSchedules[].mealType").type(JsonFieldType.STRING).description("식사 타입"),
                        fieldWithPath("result.mealSchedules[].time").type(JsonFieldType.STRING).description("식사 알림 시간. skipped=true이면 null").optional(),
                        fieldWithPath("result.mealSchedules[].skipped").type(JsonFieldType.BOOLEAN).description("먹지 않음 여부")
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
                    .description("마이페이지에서 그룹 구성원 id, 닉네임, 프로필 이미지를 조회한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.members[].memberId").type(JsonFieldType.NUMBER).description("회원 id"),
                        fieldWithPath("result.members[].nickname").type(JsonFieldType.STRING).description("닉네임"),
                        fieldWithPath("result.members[].profileImageUrl").type(JsonFieldType.STRING).description("프로필 이미지 URL")
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
}
