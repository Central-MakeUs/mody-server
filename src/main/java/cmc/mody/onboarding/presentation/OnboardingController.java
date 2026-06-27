package cmc.mody.onboarding.presentation;

import cmc.mody.common.api.ApiResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/onboarding")
public class OnboardingController {
    @PostMapping("/profile")
    public ApiResponse<ProfileSetupResponse> setupProfile(@RequestBody ProfileSetupRequest request) {
        return ApiResponse.ok(new ProfileSetupResponse(1L, true));
    }

    @PostMapping("/weight")
    public ApiResponse<WeightSetupResponse> setupWeight(@RequestBody WeightSetupRequest request) {
        return ApiResponse.ok(new WeightSetupResponse(1L, "2026-06-27", request.currentWeightKg(), BigDecimal.ZERO));
    }

    @PutMapping("/notifications")
    public ApiResponse<NotificationSetupResponse> setupNotifications(@RequestBody NotificationSetupRequest request) {
        return ApiResponse.ok(new NotificationSetupResponse(true));
    }

    @PutMapping("/health-connection")
    public ApiResponse<HealthConnectionResponse> updateHealthConnection(@RequestBody HealthConnectionRequest request) {
        return ApiResponse.ok(new HealthConnectionResponse(request.connected()));
    }

    @PostMapping("/groups/join")
    public ApiResponse<GroupJoinResponse> joinGroup(@RequestBody GroupJoinRequest request) {
        return ApiResponse.ok(new GroupJoinResponse(1L, request.code(), "모디 그룹", 4));
    }

    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GroupCreateResponse> createGroup(@RequestBody GroupCreateRequest request) {
        return ApiResponse.created(new GroupCreateResponse(1L, "ABCDEF", request.name()));
    }

    public record ProfileSetupRequest(
        String nickname,
        String birthDate,
        BigDecimal currentWeightKg,
        BigDecimal targetWeightKg,
        List<String> mealReminderTimes,
        String exerciseReminderTime
    ) {
    }

    public record ProfileSetupResponse(
        Long memberId,
        boolean onboardingCompleted
    ) {
    }

    public record WeightSetupRequest(
        BigDecimal currentWeightKg,
        BigDecimal targetWeightKg
    ) {
    }

    public record WeightSetupResponse(
        Long weightRecordId,
        String recordedOn,
        BigDecimal weightKg,
        BigDecimal changeFromPreviousKg
    ) {
    }

    public record NotificationSetupRequest(
        boolean mealReminderEnabled,
        List<String> mealReminderTimes,
        boolean exerciseReminderEnabled,
        String exerciseReminderTime
    ) {
    }

    public record NotificationSetupResponse(
        boolean enabled
    ) {
    }

    public record HealthConnectionRequest(
        boolean connected
    ) {
    }

    public record HealthConnectionResponse(
        boolean connected
    ) {
    }

    public record GroupJoinRequest(
        String code
    ) {
    }

    public record GroupJoinResponse(
        Long groupId,
        String code,
        String name,
        int memberCount
    ) {
    }

    public record GroupCreateRequest(
        String name
    ) {
    }

    public record GroupCreateResponse(
        Long groupId,
        String code,
        String name
    ) {
    }
}
