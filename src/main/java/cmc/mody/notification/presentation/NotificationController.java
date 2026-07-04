package cmc.mody.notification.presentation;

import cmc.mody.auth.presentation.support.CurrentMember;
import cmc.mody.common.api.ApiResponse;
import cmc.mody.notification.application.NotificationService;
import cmc.mody.notification.domain.NotificationType;
import io.swagger.v3.oas.annotations.Parameter;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<NotificationListResponse> getNotifications(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @RequestParam(required = false) Long cursor,
        @RequestParam(defaultValue = "20") int size
    ) {
        NotificationService.NotificationListResult result = notificationService.getNotifications(memberId, cursor, size);
        return ApiResponse.ok(NotificationListResponse.from(result));
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> readNotification(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @PathVariable Long notificationId
    ) {
        notificationService.readNotification(memberId, notificationId);
        return ApiResponse.ok();
    }

    public record NotificationListResponse(List<NotificationResponse> notifications, Long nextCursor, boolean hasNext) {
        public static NotificationListResponse from(NotificationService.NotificationListResult result) {
            return new NotificationListResponse(result.notifications().stream()
                .map(NotificationResponse::from)
                .toList(), result.nextCursor(), result.hasNext());
        }
    }

    public record NotificationResponse(
        Long notificationId,
        NotificationType type,
        String title,
        String description,
        LocalDateTime createdAt,
        boolean read
    ) {
        public static NotificationResponse from(NotificationService.NotificationResult result) {
            return new NotificationResponse(
                result.notificationId(),
                result.type(),
                result.title(),
                result.description(),
                result.createdAt(),
                result.read()
            );
        }
    }
}
