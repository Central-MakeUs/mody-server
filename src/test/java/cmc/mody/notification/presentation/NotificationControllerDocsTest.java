package cmc.mody.notification.presentation;

import static cmc.mody.docs.ApiDocumentUtils.commonResponseFields;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationController.class)
@AutoConfigureRestDocs
class NotificationControllerDocsTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void getNotifications() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
            .andExpect(status().isOk())
            .andDo(document("notification-list",
                resource(ResourceSnippetParameters.builder()
                    .tag("Notification")
                    .summary("알림 리스트 조회")
                    .description("알림 종류, 제목, 설명, 날짜, 읽음 여부를 조회한다.")
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.notifications[].notificationId").type(JsonFieldType.NUMBER).description("알림 id"),
                        fieldWithPath("result.notifications[].type").type(JsonFieldType.STRING).description("알림 종류"),
                        fieldWithPath("result.notifications[].title").type(JsonFieldType.STRING).description("제목"),
                        fieldWithPath("result.notifications[].description").type(JsonFieldType.STRING).description("설명"),
                        fieldWithPath("result.notifications[].createdAt").type(JsonFieldType.STRING).description("생성 일시"),
                        fieldWithPath("result.notifications[].read").type(JsonFieldType.BOOLEAN).description("읽음 여부")
                    ))
                    .build())
            ));
    }

    @Test
    void readNotification() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/{notificationId}/read", 1L))
            .andExpect(status().isOk())
            .andDo(document("notification-read",
                resource(ResourceSnippetParameters.builder()
                    .tag("Notification")
                    .summary("알림 읽음 처리")
                    .description("알림을 읽음 상태로 변경한다.")
                    .responseFields(commonResponseFields())
                    .build())
            ));
    }
}
