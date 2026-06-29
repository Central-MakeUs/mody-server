package cmc.mody.mypage.presentation;

import cmc.mody.auth.presentation.support.CurrentMember;
import cmc.mody.common.api.ApiResponse;
import cmc.mody.mypage.application.MypageService;
import cmc.mody.mypage.application.MypageService.ProfileUpdateCommand;
import cmc.mody.mypage.application.MypageService.WeightCreateCommand;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mypage")
public class MypageController {
    private final MypageService mypageService;

    @GetMapping("/weights")
    public ApiResponse<WeightHistoryResponse> getWeightHistory(
        @Parameter(hidden = true) @CurrentMember Long memberId
    ) {
        MypageService.WeightHistoryResult result = mypageService.getWeightHistory(memberId);
        return ApiResponse.ok(WeightHistoryResponse.from(result));
    }

    @PostMapping("/weights")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WeightCreateResponse> createWeight(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @Valid @RequestBody WeightCreateRequest request
    ) {
        MypageService.WeightCreateResult result = mypageService.createWeight(memberId, request.toCommand());
        return ApiResponse.created(WeightCreateResponse.from(result));
    }

    @GetMapping("/me")
    public ApiResponse<MyInfoResponse> getMyInfo(@Parameter(hidden = true) @CurrentMember Long memberId) {
        MypageService.MyInfoResult result = mypageService.getMyInfo(memberId);
        return ApiResponse.ok(MyInfoResponse.from(result));
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> deleteMe() {
        return ApiResponse.ok();
    }

    @GetMapping("/profile")
    public ApiResponse<ProfileResponse> getProfile(@Parameter(hidden = true) @CurrentMember Long memberId) {
        MypageService.ProfileResult result = mypageService.getProfile(memberId);
        return ApiResponse.ok(ProfileResponse.from(result));
    }

    @PatchMapping("/profile")
    public ApiResponse<ProfileUpdateResponse> updateProfile(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @Valid @RequestBody ProfileUpdateRequest request
    ) {
        MypageService.ProfileUpdateResult result = mypageService.updateProfile(memberId, request.toCommand());
        return ApiResponse.ok(ProfileUpdateResponse.from(result));
    }

    @GetMapping("/notification-settings")
    public ApiResponse<NotificationSettingResponse> getNotificationSettings() {
        return ApiResponse.ok(new NotificationSettingResponse(
            true,
            true,
            true,
            defaultMealSchedules(),
            "20:00"
        ));
    }

    @PatchMapping("/notification-settings")
    public ApiResponse<NotificationSettingResponse> updateNotificationSettings(
        @RequestBody NotificationSettingRequest request
    ) {
        return ApiResponse.ok(new NotificationSettingResponse(
            request.mealReminderEnabled(),
            request.commentNotificationEnabled(),
            request.challengeNotificationEnabled(),
            request.mealSchedules(),
            request.exerciseReminderTime()
        ));
    }

    @PutMapping("/exercise-schedules")
    public ApiResponse<ExerciseScheduleResponse> updateExerciseSchedules(@RequestBody ExerciseScheduleRequest request) {
        return ApiResponse.ok(new ExerciseScheduleResponse(request.schedules()));
    }

    @PutMapping("/meal-times")
    public ApiResponse<MealTimeResponse> updateMealTimes(@RequestBody MealTimeRequest request) {
        return ApiResponse.ok(new MealTimeResponse(request.mealSchedules()));
    }

    private List<MealScheduleItem> defaultMealSchedules() {
        return List.of(
            new MealScheduleItem("BREAKFAST", "08:00", false),
            new MealScheduleItem("LUNCH", null, true),
            new MealScheduleItem("DINNER", "18:00", false)
        );
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
        public static WeightHistoryResponse from(MypageService.WeightHistoryResult result) {
            return new WeightHistoryResponse(result.weights().stream()
                .map(WeightRecordResponse::from)
                .toList());
        }
    }

    public record WeightRecordResponse(
        Long weightRecordId,
        LocalDate recordedOn,
        BigDecimal weightKg,
        BigDecimal changeFromPreviousKg
    ) {
        public static WeightRecordResponse from(MypageService.WeightRecordResult result) {
            return new WeightRecordResponse(
                result.weightRecordId(),
                result.recordedOn(),
                result.weightKg(),
                result.changeFromPreviousKg()
            );
        }
    }

    public record WeightCreateRequest(
        @NotNull(message = "체중은 필수입니다.")
        @DecimalMin(value = "20.0", message = "체중은 20kg 이상이어야 합니다.")
        @DecimalMax(value = "300.0", message = "체중은 300kg 이하여야 합니다.")
        @Digits(integer = 3, fraction = 2, message = "체중은 소수점 둘째 자리까지 입력해주세요.")
        BigDecimal weightKg
    ) {
        public WeightCreateCommand toCommand() {
            return new WeightCreateCommand(weightKg);
        }
    }

    public record WeightCreateResponse(
        Long weightRecordId,
        LocalDate recordedOn,
        BigDecimal weightKg,
        BigDecimal changeFromPreviousKg
    ) {
        public static WeightCreateResponse from(MypageService.WeightCreateResult result) {
            return new WeightCreateResponse(
                result.weightRecordId(),
                result.recordedOn(),
                result.weightKg(),
                result.changeFromPreviousKg()
            );
        }
    }

    public record MyInfoResponse(Long memberId, String nickname, String profileImageUrl, int daysTogether) {
        public static MyInfoResponse from(MypageService.MyInfoResult result) {
            return new MyInfoResponse(
                result.memberId(),
                result.nickname(),
                result.profileImageUrl(),
                result.daysTogether()
            );
        }
    }

    public record ProfileResponse(String loginType, String name, LocalDate birthDate) {
        public static ProfileResponse from(MypageService.ProfileResult result) {
            return new ProfileResponse(result.loginType(), result.name(), result.birthDate());
        }
    }

    public record ProfileUpdateRequest(
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = 14, message = "닉네임은 14자 이하로 입력해주세요.")
        String nickname,
        @NotNull(message = "생년월일은 필수입니다.")
        @Past(message = "생년월일은 과거 날짜여야 합니다.")
        LocalDate birthDate
    ) {
        public ProfileUpdateCommand toCommand() {
            return new ProfileUpdateCommand(nickname, birthDate);
        }
    }

    public record ProfileUpdateResponse(String nickname, LocalDate birthDate) {
        public static ProfileUpdateResponse from(MypageService.ProfileUpdateResult result) {
            return new ProfileUpdateResponse(result.nickname(), result.birthDate());
        }
    }

    public record NotificationSettingResponse(
        boolean mealReminderEnabled,
        boolean commentNotificationEnabled,
        boolean challengeNotificationEnabled,
        List<MealScheduleItem> mealSchedules,
        String exerciseReminderTime
    ) {
    }

    public record NotificationSettingRequest(
        boolean mealReminderEnabled,
        boolean commentNotificationEnabled,
        boolean challengeNotificationEnabled,
        List<MealScheduleItem> mealSchedules,
        String exerciseReminderTime
    ) {
    }

    public record ExerciseScheduleRequest(List<ExerciseScheduleItem> schedules) {
    }

    public record ExerciseScheduleResponse(List<ExerciseScheduleItem> schedules) {
    }

    public record ExerciseScheduleItem(String dayOfWeek, String time) {
    }

    public record MealScheduleItem(String mealType, String time, boolean skipped) {
    }

    public record MealTimeRequest(List<MealScheduleItem> mealSchedules) {
    }

    public record MealTimeResponse(List<MealScheduleItem> mealSchedules) {
    }

    public record GroupMemberListResponse(List<GroupMemberResponse> members) {
    }

    public record GroupMemberResponse(Long memberId, String nickname, String profileImageUrl) {
    }
}
