package cmc.mody.record.presentation;

import static cmc.mody.docs.ApiDocumentUtils.commonResponseFields;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

@WebMvcTest(ActivityRecordController.class)
@AutoConfigureRestDocs
class ActivityRecordControllerDocsTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void getActivityCalendar() throws Exception {
        mockMvc.perform(get("/api/v1/groups/{groupId}/activities/calendar", 1L)
                .param("yearMonth", "2026-06"))
            .andExpect(status().isOk())
            .andDo(document("activity-calendar",
                resource(ResourceSnippetParameters.builder()
                    .tag("Feed")
                    .summary("[미구현] 월/주차별 활동 유무 조회")
                    .description("월 단위로 식사/운동 기록 존재 여부를 조회한다.")
                    .queryParameters(parameterWithName("yearMonth").description("조회 월, yyyy-MM"))
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.days[].date").type(JsonFieldType.STRING).description("날짜"),
                        fieldWithPath("result.days[].mealRecorded").type(JsonFieldType.BOOLEAN).description("식사 기록 여부"),
                        fieldWithPath("result.days[].exerciseRecorded").type(JsonFieldType.BOOLEAN).description("운동 기록 여부")
                    ))
                    .build())
            ));
    }

    @Test
    void getRecords() throws Exception {
        mockMvc.perform(get("/api/v1/groups/{groupId}/records", 1L)
                .param("date", "2026-06-27")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andDo(document("record-list",
                resource(ResourceSnippetParameters.builder()
                    .tag("Feed")
                    .summary("[미구현] 날짜별 식사/운동 기록 조회")
                    .description("날짜별 기록을 커서 기반으로 조회한다.")
                    .queryParameters(
                        parameterWithName("date").description("조회 날짜, yyyy-MM-dd"),
                        parameterWithName("cursor").optional().description("다음 페이지 커서"),
                        parameterWithName("size").description("조회 개수")
                    )
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.records[].recordId").type(JsonFieldType.NUMBER).description("기록 id"),
                        fieldWithPath("result.records[].recordType").type(JsonFieldType.STRING).description("기록 타입: MEAL, EXERCISE"),
                        fieldWithPath("result.records[].memberId").type(JsonFieldType.NUMBER).description("작성자 id"),
                        fieldWithPath("result.records[].nickname").type(JsonFieldType.STRING).description("작성자 닉네임"),
                        fieldWithPath("result.records[].profileImageUrl").type(JsonFieldType.STRING).description("작성자 프로필 이미지"),
                        fieldWithPath("result.records[].recordedTime").type(JsonFieldType.STRING).description("기록 시간"),
                        fieldWithPath("result.records[].menu").type(JsonFieldType.STRING).description("메뉴명"),
                        fieldWithPath("result.records[].imageUrl").type(JsonFieldType.STRING).description("기록 이미지 URL"),
                        fieldWithPath("result.nextCursor").type(JsonFieldType.NUMBER).description("다음 커서"),
                        fieldWithPath("result.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부")
                    ))
                    .build())
            ));
    }

    @Test
    void getRecordDetail() throws Exception {
        mockMvc.perform(get("/api/v1/records/{recordId}", 1L))
            .andExpect(status().isOk())
            .andDo(document("record-detail",
                resource(ResourceSnippetParameters.builder()
                    .tag("Feed")
                    .summary("[미구현] 기록 상세 조회")
                    .description("식사/운동 기록 상세와 댓글을 조회한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.recordId").type(JsonFieldType.NUMBER).description("기록 id"),
                        fieldWithPath("result.recordType").type(JsonFieldType.STRING).description("기록 타입"),
                        fieldWithPath("result.memberId").type(JsonFieldType.NUMBER).description("작성자 id"),
                        fieldWithPath("result.nickname").type(JsonFieldType.STRING).description("작성자 닉네임"),
                        fieldWithPath("result.profileImageUrl").type(JsonFieldType.STRING).description("작성자 프로필 이미지"),
                        fieldWithPath("result.recordedTime").type(JsonFieldType.STRING).description("기록 시간"),
                        fieldWithPath("result.menu").type(JsonFieldType.STRING).description("메뉴명"),
                        fieldWithPath("result.imageUrl").type(JsonFieldType.STRING).description("기록 이미지 URL"),
                        fieldWithPath("result.comments[].commentId").type(JsonFieldType.NUMBER).description("댓글 id"),
                        fieldWithPath("result.comments[].memberId").type(JsonFieldType.NUMBER).description("댓글 작성자 id"),
                        fieldWithPath("result.comments[].nickname").type(JsonFieldType.STRING).description("댓글 작성자 닉네임"),
                        fieldWithPath("result.comments[].content").type(JsonFieldType.STRING).description("댓글 내용")
                    ))
                    .build())
            ));
    }

    @Test
    void createRecord() throws Exception {
        mockMvc.perform(post("/api/v1/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "groupId": 1,
                      "recordType": "MEAL",
                      "imageKey": "records/2026/06/meal.jpg",
                      "mealTime": "12:30",
                      "menu": "샐러드",
                      "exerciseDurationMinutes": null,
                      "exerciseName": null
                    }
                    """))
            .andExpect(status().isOk())
            .andDo(document("record-create",
                resource(ResourceSnippetParameters.builder()
                    .tag("Feed")
                    .summary("[미구현] 기록 입력")
                    .description("식사 또는 운동 기록을 입력한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.recordId").type(JsonFieldType.NUMBER).description("생성된 기록 id")
                    ))
                    .build())
            ));
    }

    @Test
    void createComment() throws Exception {
        mockMvc.perform(post("/api/v1/records/{recordId}/comments", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "좋다"
                    }
                    """))
            .andExpect(status().isOk())
            .andDo(document("record-comment-create",
                resource(ResourceSnippetParameters.builder()
                    .tag("Feed")
                    .summary("[미구현] 기록 댓글 작성")
                    .description("식사/운동 기록에 댓글을 작성한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.commentId").type(JsonFieldType.NUMBER).description("댓글 id"),
                        fieldWithPath("result.recordId").type(JsonFieldType.NUMBER).description("기록 id")
                    ))
                    .build())
            ));
    }
}
