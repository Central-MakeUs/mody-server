package cmc.mody.mypage.presentation;

import cmc.mody.auth.presentation.support.CurrentMember;
import cmc.mody.common.api.ApiResponse;
import cmc.mody.mypage.application.MypageService;
import cmc.mody.mypage.application.MypageService.ExerciseScheduleUpdateCommand;
import cmc.mody.mypage.application.MypageService.ProfileUpdateCommand;
import cmc.mody.mypage.application.MypageService.MealTimeUpdateCommand;
import cmc.mody.mypage.application.MypageService.NotificationSettingCommand;
import cmc.mody.mypage.application.MypageService.WeightCreateCommand;
import cmc.mody.notification.domain.MealType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
    public ApiResponse<Void> deleteMe(@Parameter(hidden = true) @CurrentMember Long memberId) {
        mypageService.deleteMe(memberId);
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
    public ApiResponse<NotificationSettingResponse> getNotificationSettings(
        @Parameter(hidden = true) @CurrentMember Long memberId
    ) {
        MypageService.NotificationSettingResult result = mypageService.getNotificationSettings(memberId);
        return ApiResponse.ok(NotificationSettingResponse.from(result));
    }

    @PatchMapping("/notification-settings")
    public ApiResponse<NotificationSettingResponse> updateNotificationSettings(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @Valid @RequestBody NotificationSettingRequest request
    ) {
        MypageService.NotificationSettingResult result = mypageService.updateNotificationSettings(
            memberId,
            request.toCommand()
        );
        return ApiResponse.ok(NotificationSettingResponse.from(result));
    }

    @PutMapping("/exercise-schedules")
    public ApiResponse<ExerciseScheduleResponse> updateExerciseSchedules(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @Valid @RequestBody ExerciseScheduleRequest request
    ) {
        MypageService.ExerciseScheduleUpdateResult result = mypageService.updateExerciseSchedules(
            memberId,
            request.toCommand()
        );
        return ApiResponse.ok(ExerciseScheduleResponse.from(result));
    }

    @PutMapping("/meal-times")
    public ApiResponse<MealTimeResponse> updateMealTimes(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @Valid @RequestBody MealTimeRequest request
    ) {
        MypageService.MealTimeUpdateResult result = mypageService.updateMealTimes(
            memberId,
            request.toCommand()
        );
        return ApiResponse.ok(MealTimeResponse.from(result));
    }

    @GetMapping("/groups/{groupId}/members")
    public ApiResponse<GroupMemberListResponse> getGroupMembers(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @PathVariable Long groupId
    ) {
        MypageService.GroupMemberListResult result = mypageService.getGroupMembers(memberId, groupId);
        return ApiResponse.ok(GroupMemberListResponse.from(result));
    }

    @DeleteMapping("/groups/{groupId}/members/me")
    public ApiResponse<Void> leaveGroup(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @PathVariable Long groupId
    ) {
        mypageService.leaveGroup(memberId, groupId);
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
        boolean recordReminderEnabled,
        boolean commentNotificationEnabled,
        boolean challengeNotificationEnabled,
        List<MealScheduleItem> mealSchedules,
        List<ExerciseScheduleItem> exerciseSchedules
    ) {
        public static NotificationSettingResponse from(MypageService.NotificationSettingResult result) {
            return new NotificationSettingResponse(
                result.recordReminderEnabled(),
                result.commentNotificationEnabled(),
                result.challengeNotificationEnabled(),
                result.mealSchedules().stream()
                    .map(MealScheduleItem::from)
                    .toList(),
                result.exerciseSchedules().stream()
                    .map(ExerciseScheduleItem::from)
                    .toList()
            );
        }
    }

    public record NotificationSettingRequest(
        boolean recordReminderEnabled,
        boolean commentNotificationEnabled,
        boolean challengeNotificationEnabled
    ) {
        public NotificationSettingCommand toCommand() {
            return new NotificationSettingCommand(
                recordReminderEnabled,
                commentNotificationEnabled,
                challengeNotificationEnabled
            );
        }
    }

    public record ExerciseScheduleRequest(
        @NotNull(message = "운동 일정은 필수입니다.")
        List<@Valid ExerciseScheduleItem> schedules
    ) {
        public ExerciseScheduleUpdateCommand toCommand() {
            return new ExerciseScheduleUpdateCommand(schedules.stream()
                .map(ExerciseScheduleItem::toCommand)
                .toList());
        }
    }

    public record ExerciseScheduleResponse(List<ExerciseScheduleItem> schedules) {
        public static ExerciseScheduleResponse from(MypageService.ExerciseScheduleUpdateResult result) {
            return new ExerciseScheduleResponse(result.schedules().stream()
                .map(ExerciseScheduleItem::from)
                .toList());
        }
    }

    public record ExerciseScheduleItem(
        @NotNull(message = "운동 요일은 필수입니다.")
        DayOfWeek dayOfWeek,
        @NotNull(message = "운동 시간은 필수입니다.")
        LocalTime time
    ) {
        public ExerciseScheduleItem {
        }

        public MypageService.ExerciseScheduleCommand toCommand() {
            return new MypageService.ExerciseScheduleCommand(dayOfWeek, time);
        }

        public static ExerciseScheduleItem from(MypageService.ExerciseScheduleResult result) {
            return new ExerciseScheduleItem(result.dayOfWeek(), result.time());
        }
    }

    public record MealScheduleItem(
        @NotNull(message = "식사 타입은 필수입니다.")
        MealType mealType,
        LocalTime time,
        boolean skipped
    ) {
        @JsonIgnore
        @AssertTrue(
            message = "먹지 않음이면 시간은 비워두고, 먹는 식사는 시간을 입력해주세요."
        )
        public boolean isTimeValid() {
            return skipped ? time == null : time != null;
        }

        public MypageService.MealScheduleCommand toCommand() {
            return new MypageService.MealScheduleCommand(mealType, time, skipped);
        }

        public static MealScheduleItem from(MypageService.MealScheduleResult result) {
            return new MealScheduleItem(result.mealType(), result.time(), result.skipped());
        }
    }

    public record MealTimeRequest(
        @NotNull(message = "식사 설정은 필수입니다.")
        @Size(min = 3, max = 3, message = "식사 설정은 아침, 점심, 저녁 3개를 입력해주세요.")
        List<@Valid MealScheduleItem> mealSchedules
    ) {
        @JsonIgnore
        @AssertTrue(message = "식사 설정은 아침, 점심, 저녁을 각각 1개씩 입력해주세요.")
        public boolean isMealTypesValid() {
            if (mealSchedules == null) {
                return true;
            }
            Set<MealType> mealTypes = mealSchedules.stream()
                .map(MealScheduleItem::mealType)
                .filter(Objects::nonNull)
                .collect(() -> EnumSet.noneOf(MealType.class), Set::add, Set::addAll);
            return mealTypes.equals(EnumSet.allOf(MealType.class));
        }

        public MealTimeUpdateCommand toCommand() {
            return new MealTimeUpdateCommand(mealSchedules.stream()
                .map(MealScheduleItem::toCommand)
                .toList());
        }
    }

    public record MealTimeResponse(List<MealScheduleItem> mealSchedules) {
        public static MealTimeResponse from(MypageService.MealTimeUpdateResult result) {
            return new MealTimeResponse(result.mealSchedules().stream()
                .map(MealScheduleItem::from)
                .toList());
        }
    }

    public record GroupMemberListResponse(List<GroupMemberResponse> members) {
        public static GroupMemberListResponse from(MypageService.GroupMemberListResult result) {
            return new GroupMemberListResponse(result.members().stream()
                .map(GroupMemberResponse::from)
                .toList());
        }
    }

    public record GroupMemberResponse(Long memberId, String nickname, String profileImageUrl) {
        public static GroupMemberResponse from(MypageService.GroupMemberResult result) {
            return new GroupMemberResponse(result.memberId(), result.nickname(), result.profileImageUrl());
        }
    }
}
