package cmc.mody.notification.presentation;

import cmc.mody.auth.presentation.support.CurrentMember;
import cmc.mody.common.api.ApiResponse;
import cmc.mody.notification.application.NotificationPushTokenService;
import cmc.mody.notification.application.NotificationPushTokenService.DisablePushTokenCommand;
import cmc.mody.notification.application.NotificationPushTokenService.RegisterPushTokenCommand;
import cmc.mody.notification.application.NotificationService;
import cmc.mody.notification.domain.NotificationType;
import cmc.mody.notification.domain.PushPlatform;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    private final NotificationPushTokenService notificationPushTokenService;

    @GetMapping
    public ApiResponse<NotificationListResponse> getNotifications(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @RequestParam(required = false) Long cursor,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "false") boolean allRead
    ) {
        NotificationService.NotificationListResult result = notificationService.getNotifications(
            memberId,
            cursor,
            size,
            allRead
        );
        return ApiResponse.ok(NotificationListResponse.from(result));
    }

    @GetMapping("/unread-exists")
    public ApiResponse<UnreadExistsResponse> hasUnreadNotification(
        @Parameter(hidden = true) @CurrentMember Long memberId
    ) {
        NotificationService.UnreadExistsResult result = notificationService.hasUnreadNotification(memberId);
        return ApiResponse.ok(UnreadExistsResponse.from(result));
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> readNotification(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @PathVariable Long notificationId
    ) {
        notificationService.readNotification(memberId, notificationId);
        return ApiResponse.ok();
    }

    @PostMapping("/push-token")
    public ApiResponse<Void> registerPushToken(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @Valid @RequestBody PushTokenRegisterRequest request
    ) {
        notificationPushTokenService.register(memberId, request.toCommand());
        return ApiResponse.ok();
    }

    @DeleteMapping("/push-token")
    public ApiResponse<Void> disablePushToken(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @Valid @RequestBody PushTokenDisableRequest request
    ) {
        notificationPushTokenService.disable(memberId, request.toCommand());
        return ApiResponse.ok();
    }

    public record PushTokenRegisterRequest(
        @NotBlank(message = "디바이스 id는 필수입니다.")
        @Size(max = 100, message = "디바이스 id는 100자 이하로 입력해주세요.")
        String deviceId,
        @NotNull(message = "플랫폼은 필수입니다.")
        PushPlatform platform,
        @NotBlank(message = "FCM 토큰은 필수입니다.")
        @Size(max = 500, message = "FCM 토큰은 500자 이하로 입력해주세요.")
        String fcmToken
    ) {
        RegisterPushTokenCommand toCommand() {
            return new RegisterPushTokenCommand(deviceId, platform, fcmToken);
        }
    }

    public record PushTokenDisableRequest(
        @NotBlank(message = "디바이스 id는 필수입니다.")
        @Size(max = 100, message = "디바이스 id는 100자 이하로 입력해주세요.")
        String deviceId
    ) {
        DisablePushTokenCommand toCommand() {
            return new DisablePushTokenCommand(deviceId);
        }
    }

    public record NotificationListResponse(List<NotificationResponse> notifications, Long nextCursor, boolean hasNext) {
        public static NotificationListResponse from(NotificationService.NotificationListResult result) {
            return new NotificationListResponse(result.notifications().stream()
                .map(NotificationResponse::from)
                .toList(), result.nextCursor(), result.hasNext());
        }
    }

    public record UnreadExistsResponse(boolean hasUnread) {
        public static UnreadExistsResponse from(NotificationService.UnreadExistsResult result) {
            return new UnreadExistsResponse(result.hasUnread());
        }
    }

    public record NotificationResponse(
        Long notificationId,
        NotificationType type,
        String title,
        String description,
        String link,
        LocalDateTime createdAt,
        boolean read
    ) {
        public static NotificationResponse from(NotificationService.NotificationResult result) {
            return new NotificationResponse(
                result.notificationId(),
                result.type(),
                result.title(),
                result.description(),
                result.link(),
                result.createdAt(),
                result.read()
            );
        }
    }
}
