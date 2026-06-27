package cmc.mody.mypage.presentation;

import cmc.mody.common.api.ApiResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mypage")
public class MypageController {
    @GetMapping("/weights")
    public ApiResponse<WeightHistoryResponse> getWeightHistory() {
        return ApiResponse.ok(new WeightHistoryResponse(List.of(
            new WeightRecordResponse(1L, "2026-06-27", new BigDecimal("72.5"), new BigDecimal("-0.3"))
        )));
    }

    @PostMapping("/weights")
    public ApiResponse<WeightCreateResponse> createWeight(@RequestBody WeightCreateRequest request) {
        return ApiResponse.created(new WeightCreateResponse(1L, "2026-06-27", request.weightKg(), new BigDecimal("-0.3")));
    }

    @GetMapping("/me")
    public ApiResponse<MyInfoResponse> getMyInfo() {
        return ApiResponse.ok(new MyInfoResponse(1L, "민석", "profiles/member-1.jpg", 12));
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> deleteMe() {
        return ApiResponse.ok();
    }

    @GetMapping("/profile")
    public ApiResponse<ProfileResponse> getProfile() {
        return ApiResponse.ok(new ProfileResponse("KAKAO", "민석", "2000-01-01"));
    }

    @PatchMapping("/profile")
    public ApiResponse<ProfileUpdateResponse> updateProfile(@RequestBody ProfileUpdateRequest request) {
        return ApiResponse.ok(new ProfileUpdateResponse(request.nickname(), request.birthDate()));
    }

    @GetMapping("/notification-settings")
    public ApiResponse<NotificationSettingResponse> getNotificationSettings() {
        return ApiResponse.ok(new NotificationSettingResponse(true, true, true, List.of("08:00", "12:00", "18:00"), "20:00"));
    }

    @PatchMapping("/notification-settings")
    public ApiResponse<NotificationSettingResponse> updateNotificationSettings(@RequestBody NotificationSettingRequest request) {
        return ApiResponse.ok(new NotificationSettingResponse(
            request.mealReminderEnabled(),
            request.commentNotificationEnabled(),
            request.challengeNotificationEnabled(),
            request.mealReminderTimes(),
            request.exerciseReminderTime()
        ));
    }

    @PutMapping("/exercise-schedules")
    public ApiResponse<ExerciseScheduleResponse> updateExerciseSchedules(@RequestBody ExerciseScheduleRequest request) {
        return ApiResponse.ok(new ExerciseScheduleResponse(request.schedules()));
    }

    @PutMapping("/meal-times")
    public ApiResponse<MealTimeResponse> updateMealTimes(@RequestBody MealTimeRequest request) {
        return ApiResponse.ok(new MealTimeResponse(request.mealReminderTimes()));
    }

    @GetMapping("/groups/{groupId}/members")
    public ApiResponse<GroupMemberListResponse> getGroupMembers(@PathVariable Long groupId) {
        return ApiResponse.ok(new GroupMemberListResponse(List.of(
            new GroupMemberResponse(1L, "민석", "profiles/member-1.jpg")
        )));
    }

    @DeleteMapping("/groups/{groupId}/members/me")
    public ApiResponse<Void> leaveGroup(@PathVariable Long groupId) {
        return ApiResponse.ok();
    }

    public record WeightHistoryResponse(List<WeightRecordResponse> weights) {
    }

    public record WeightRecordResponse(Long weightRecordId, String recordedOn, BigDecimal weightKg, BigDecimal changeFromPreviousKg) {
    }

    public record WeightCreateRequest(BigDecimal weightKg) {
    }

    public record WeightCreateResponse(Long weightRecordId, String recordedOn, BigDecimal weightKg, BigDecimal changeFromPreviousKg) {
    }

    public record MyInfoResponse(Long memberId, String nickname, String profileImageUrl, int daysTogether) {
    }

    public record ProfileResponse(String loginType, String name, String birthDate) {
    }

    public record ProfileUpdateRequest(String nickname, String birthDate) {
    }

    public record ProfileUpdateResponse(String nickname, String birthDate) {
    }

    public record NotificationSettingResponse(
        boolean mealReminderEnabled,
        boolean commentNotificationEnabled,
        boolean challengeNotificationEnabled,
        List<String> mealReminderTimes,
        String exerciseReminderTime
    ) {
    }

    public record NotificationSettingRequest(
        boolean mealReminderEnabled,
        boolean commentNotificationEnabled,
        boolean challengeNotificationEnabled,
        List<String> mealReminderTimes,
        String exerciseReminderTime
    ) {
    }

    public record ExerciseScheduleRequest(List<ExerciseScheduleItem> schedules) {
    }

    public record ExerciseScheduleResponse(List<ExerciseScheduleItem> schedules) {
    }

    public record ExerciseScheduleItem(String dayOfWeek, String dayTime, String eveningTime) {
    }

    public record MealTimeRequest(List<String> mealReminderTimes) {
    }

    public record MealTimeResponse(List<String> mealReminderTimes) {
    }

    public record GroupMemberListResponse(List<GroupMemberResponse> members) {
    }

    public record GroupMemberResponse(Long memberId, String nickname, String profileImageUrl) {
    }
}
