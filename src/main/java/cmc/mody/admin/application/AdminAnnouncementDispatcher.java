package cmc.mody.admin.application;

import cmc.mody.notification.application.PushNotificationClient;
import cmc.mody.notification.application.PushNotificationResult;
import cmc.mody.notification.domain.Notification;
import cmc.mody.notification.domain.NotificationType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 공지 푸시를 실제로 보낸다. 알림 목록에 남기지 않도록 저장하지 않는 {@link Notification}을 만들어 FCM으로 보낸다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAnnouncementDispatcher {
    private static final int MAX_ERROR = 500;
    private final NamedParameterJdbcTemplate jdbc;
    private final PushNotificationClient push;

    @Async("notificationExecutor")
    public void dispatch(long announcementId, String title, String content, String tokenQuery,
        MapSqlParameterSource targetParams) {
        updateStatus(announcementId, "SENDING", null, 0, null);
        try {
            List<String> tokens = jdbc.queryForList(tokenQuery, targetParams, String.class);
            Notification message = new Notification(announcementId, null, NotificationType.ADMIN_ANNOUNCEMENT,
                title, content, null, "ANNOUNCEMENT", announcementId, null, LocalDateTime.now(), 0, null);
            PushNotificationResult result = tokens.isEmpty() ? PushNotificationResult.empty()
                : push.send(message, tokens);
            if (result.hasInvalidTokens()) {
                jdbc.update("""
                    UPDATE member_push_token SET enabled = FALSE, updated_at = :now
                    WHERE fcm_token IN (:tokens) AND deleted_at IS NULL
                    """, new MapSqlParameterSource("tokens", result.invalidTokens())
                    .addValue("now", LocalDateTime.now()));
            }
            jdbc.update("UPDATE admin_announcement SET token_count = :tokens WHERE id = :id",
                new MapSqlParameterSource("tokens", tokens.size()).addValue("id", announcementId));
            updateStatus(announcementId, "SENT", null, result.invalidTokens().size(), LocalDateTime.now());
        } catch (RuntimeException exception) {
            log.warn("admin announcement push failed id={}", announcementId, exception);
            String error = String.valueOf(exception.getMessage());
            updateStatus(announcementId, "FAILED", error.substring(0, Math.min(error.length(), MAX_ERROR)), 0,
                LocalDateTime.now());
        }
    }

    private void updateStatus(long id, String status, String error, int invalid, LocalDateTime completedAt) {
        jdbc.update("""
            UPDATE admin_announcement SET send_status = :status, error_message = :error,
                invalid_token_count = :invalid, completed_at = :completedAt
            WHERE id = :id
            """, new MapSqlParameterSource().addValue("status", status).addValue("error", error)
            .addValue("invalid", invalid).addValue("completedAt", completedAt).addValue("id", id));
    }
}
