package cmc.mody.onboarding.presentation;

import cmc.mody.auth.presentation.support.CurrentMember;
import cmc.mody.common.api.ApiResponse;
import cmc.mody.notification.domain.MealType;
import cmc.mody.onboarding.application.OnboardingService;
import cmc.mody.onboarding.application.OnboardingService.ExerciseScheduleCommand;
import cmc.mody.onboarding.application.OnboardingService.MealScheduleCommand;
import cmc.mody.onboarding.application.OnboardingService.ProfileSetupCommand;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/onboarding")
public class OnboardingController {
    private final OnboardingService onboardingService;

    @PostMapping("/profile")
    public ApiResponse<ProfileSetupResponse> setupProfile(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @Valid @RequestBody ProfileSetupRequest request
    ) {
        OnboardingService.ProfileSetupResult result = onboardingService.setupProfile(memberId, request.toCommand());
        return ApiResponse.ok(ProfileSetupResponse.from(result));
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
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = 14, message = "닉네임은 14자 이하로 입력해주세요.")
        String nickname,
        @NotNull(message = "생년월일은 필수입니다.")
        @Past(message = "생년월일은 과거 날짜여야 합니다.")
        LocalDate birthDate,
        @NotNull(message = "현재 체중은 필수입니다.")
        @DecimalMin(value = "20.0", message = "현재 체중은 20kg 이상이어야 합니다.")
        @DecimalMax(value = "300.0", message = "현재 체중은 300kg 이하여야 합니다.")
        @Digits(integer = 3, fraction = 2, message = "현재 체중은 소수점 둘째 자리까지 입력해주세요.")
        BigDecimal currentWeightKg,
        @NotNull(message = "목표 체중은 필수입니다.")
        @DecimalMin(value = "20.0", message = "목표 체중은 20kg 이상이어야 합니다.")
        @DecimalMax(value = "300.0", message = "목표 체중은 300kg 이하여야 합니다.")
        @Digits(integer = 3, fraction = 2, message = "목표 체중은 소수점 둘째 자리까지 입력해주세요.")
        BigDecimal targetWeightKg,
        @NotNull(message = "식사 설정은 필수입니다.")
        @Size(min = 3, max = 3, message = "식사 설정은 아침, 점심, 저녁 3개를 입력해주세요.")
        List<@Valid MealScheduleRequest> mealSchedules,
        @NotNull(message = "운동 일정은 필수입니다.")
        @Size(min = 3, message = "운동 일정은 주 3회 이상 입력해주세요.")
        List<@Valid ExerciseScheduleRequest> exerciseSchedules
    ) {
        @JsonIgnore
        @AssertTrue(message = "식사 설정은 아침, 점심, 저녁을 각각 1개씩 입력해주세요.")
        public boolean isMealTypesValid() {
            if (mealSchedules == null) {
                return true;
            }
            Set<MealType> mealTypes = mealSchedules.stream()
                .map(MealScheduleRequest::mealType)
                .filter(Objects::nonNull)
                .collect(() -> EnumSet.noneOf(MealType.class), Set::add, Set::addAll);
            return mealTypes.equals(EnumSet.allOf(MealType.class));
        }

        public ProfileSetupCommand toCommand() {
            return new ProfileSetupCommand(
                nickname,
                birthDate,
                currentWeightKg,
                targetWeightKg,
                mealSchedules.stream()
                    .map(MealScheduleRequest::toCommand)
                    .toList(),
                exerciseSchedules.stream()
                    .map(ExerciseScheduleRequest::toCommand)
                    .toList()
            );
        }
    }

    public record MealScheduleRequest(
        @NotNull(message = "식사 타입은 필수입니다.")
        MealType mealType,
        LocalTime time,
        boolean skipped
    ) {
        @JsonIgnore
        @AssertTrue(message = "먹지 않음이면 시간은 비워두고, 먹는 식사는 시간을 입력해주세요.")
        public boolean isTimeValid() {
            return skipped ? time == null : time != null;
        }

        public MealScheduleCommand toCommand() {
            return new MealScheduleCommand(mealType, time, skipped);
        }
    }

    public record ExerciseScheduleRequest(
        @NotNull(message = "운동 요일은 필수입니다.")
        DayOfWeek dayOfWeek,
        @NotNull(message = "운동 시간은 필수입니다.")
        LocalTime time
    ) {
        public ExerciseScheduleCommand toCommand() {
            return new ExerciseScheduleCommand(dayOfWeek, time);
        }
    }

    public record ProfileSetupResponse(
        Long memberId,
        Long weightRecordId,
        boolean personalInfoCompleted
    ) {
        public static ProfileSetupResponse from(OnboardingService.ProfileSetupResult result) {
            return new ProfileSetupResponse(
                result.memberId(),
                result.weightRecordId(),
                result.personalInfoCompleted()
            );
        }
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
        List<MealScheduleRequest> mealSchedules,
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
