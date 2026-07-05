package cmc.mody.notification.presentation;

import static cmc.mody.docs.ApiDocumentUtils.commonResponseFields;
import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cmc.mody.auth.application.token.TokenProvider;
import cmc.mody.auth.presentation.support.CurrentMemberArgumentResolver;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.config.WebConfig;
import cmc.mody.notification.application.NotificationService;
import cmc.mody.notification.application.NotificationService.NotificationListResult;
import cmc.mody.notification.application.NotificationService.NotificationResult;
import cmc.mody.notification.domain.NotificationType;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationController.class)
@AutoConfigureRestDocs
@Import(WebConfig.class)
class NotificationControllerDocsTest {
    private static final String NOTIFICATION_DESCRIPTION = """
        알림 API는 access token의 회원 id 기준으로 처리한다.

        발생 가능한 예외 코드:
        - AUTH401: Authorization 헤더가 없거나 비어있음
        - AUTH402: Bearer 뒤 JWT 값이 비어있음
        - AUTH403: JWT 형식이 올바르지 않거나 refresh token을 사용함
        - AUTH404: 만료된 JWT
        - AUTH405: 지원하지 않는 JWT
        - MEMBER302: 토큰의 회원 id에 해당하는 회원 없음
        - NOTIFICATION302: 알림 없음 또는 다른 회원 알림 접근
        """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

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
    void getNotifications() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        given(notificationService.getNotifications(1L, null, 20)).willReturn(new NotificationListResult(List.of(
            new NotificationResult(
                10L,
                NotificationType.COMMENT,
                "새 댓글",
                "친구가 기록에 댓글을 남겼어요.",
                LocalDateTime.of(2026, 7, 4, 10, 0),
                false
            )
        ), 10L, true));

        mockMvc.perform(get("/api/v1/notifications")
                .header("Authorization", "Bearer access-token")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andDo(document("notification-list",
                resource(ResourceSnippetParameters.builder()
                    .tag("Notification")
                    .summary("알림 리스트 조회")
                    .description(NOTIFICATION_DESCRIPTION)
                    .queryParameters(
                        parameterWithName("cursor").optional().description("다음 페이지 커서"),
                        parameterWithName("size").description("조회 개수")
                    )
                    .responseFields(commonResponseFields(
                        fieldWithPath("result.notifications[].notificationId").type(JsonFieldType.NUMBER).description("알림 id"),
                        fieldWithPath("result.notifications[].type").type(JsonFieldType.STRING).description("알림 종류"),
                        fieldWithPath("result.notifications[].title").type(JsonFieldType.STRING).description("제목"),
                        fieldWithPath("result.notifications[].description").type(JsonFieldType.STRING).description("설명"),
                        fieldWithPath("result.notifications[].createdAt").type(JsonFieldType.STRING).description("생성 일시"),
                        fieldWithPath("result.notifications[].read").type(JsonFieldType.BOOLEAN).description("읽음 여부"),
                        fieldWithPath("result.nextCursor").type(JsonFieldType.NUMBER).description("다음 커서"),
                        fieldWithPath("result.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부")
                    ))
                    .build())
            ));
    }

    @Test
    void readNotification() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);

        mockMvc.perform(patch("/api/v1/notifications/{notificationId}/read", 10L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isOk())
            .andDo(document("notification-read",
                resource(ResourceSnippetParameters.builder()
                    .tag("Notification")
                    .summary("알림 읽음 처리")
                    .description(NOTIFICATION_DESCRIPTION)
                    .responseFields(commonResponseFields())
                    .build())
            ));
    }

    @Test
    void getNotificationsWithoutAuthorization() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
            .andExpect(status().isUnauthorized())
            .andDo(documentError("notification-list-auth-missing", "알림 리스트 조회"));
    }

    @Test
    void readNotificationWithExpiredToken() throws Exception {
        willThrow(new GeneralException(ErrorStatus.EXPIRED_JWT))
            .given(tokenProvider)
            .getMemberIdByAccessToken("expired-token");

        mockMvc.perform(patch("/api/v1/notifications/{notificationId}/read", 10L)
                .header("Authorization", "Bearer expired-token"))
            .andExpect(status().isUnauthorized())
            .andDo(documentError("notification-read-auth-expired-token", "알림 읽음 처리"));
    }

    @Test
    void getNotificationsMemberNotFound() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.MEMBER_NOT_FOUND))
            .given(notificationService)
            .getNotifications(1L, null, 20);

        mockMvc.perform(get("/api/v1/notifications")
                .header("Authorization", "Bearer access-token")
                .param("size", "20"))
            .andExpect(status().isNotFound())
            .andDo(documentError("notification-list-member-not-found", "알림 리스트 조회"));
    }

    @Test
    void readNotificationNotFound() throws Exception {
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);
        willThrow(new GeneralException(ErrorStatus.NOTIFICATION_NOT_FOUND))
            .given(notificationService)
            .readNotification(1L, 10L);

        mockMvc.perform(patch("/api/v1/notifications/{notificationId}/read", 10L)
                .header("Authorization", "Bearer access-token"))
            .andExpect(status().isNotFound())
            .andDo(documentError("notification-read-not-found", "알림 읽음 처리"));
    }

    private RestDocumentationResultHandler documentError(String identifier, String summary) {
        return document(identifier,
            resource(ResourceSnippetParameters.builder()
                .tag("Notification")
                .summary(summary)
                .description(NOTIFICATION_DESCRIPTION)
                .responseFields(commonResponseFields())
                .build())
        );
    }
}
