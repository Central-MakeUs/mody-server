package cmc.mody.notification.presentation;

import cmc.mody.common.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    @GetMapping
    public ApiResponse<NotificationListResponse> getNotifications() {
        return ApiResponse.ok(new NotificationListResponse(List.of(
            new NotificationResponse(1L, "COMMENT", "새 댓글", "친구가 기록에 댓글을 남겼어요.", "2026-06-27T10:00:00", false)
        )));
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> readNotification(@PathVariable Long notificationId) {
        return ApiResponse.ok();
    }

    public record NotificationListResponse(List<NotificationResponse> notifications) {
    }

    public record NotificationResponse(
        Long notificationId,
        String type,
        String title,
        String description,
        String createdAt,
        boolean read
    ) {
    }
}
