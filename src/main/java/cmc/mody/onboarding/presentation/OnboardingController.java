package cmc.mody.onboarding.presentation;

import cmc.mody.auth.presentation.support.CurrentMember;
import cmc.mody.common.api.ApiResponse;
import cmc.mody.grouping.application.GroupService;
import cmc.mody.grouping.application.GroupService.GroupCreateCommand;
import cmc.mody.grouping.application.GroupService.GroupJoinCommand;
import cmc.mody.notification.domain.MealType;
import cmc.mody.onboarding.application.OnboardingService;
import cmc.mody.onboarding.application.OnboardingService.ExerciseScheduleCommand;
import cmc.mody.onboarding.application.OnboardingService.HealthConnectionCommand;
import cmc.mody.onboarding.application.OnboardingService.MealScheduleCommand;
import cmc.mody.onboarding.application.OnboardingService.NotificationSetupCommand;
import cmc.mody.onboarding.application.OnboardingService.ProfileSetupCommand;
import cmc.mody.onboarding.application.OnboardingService.WeightSetupCommand;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
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
    private final GroupService groupService;

    @PostMapping("/profile")
    public ApiResponse<ProfileSetupResponse> setupProfile(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @Valid @RequestBody ProfileSetupRequest request
    ) {
        OnboardingService.ProfileSetupResult result = onboardingService.setupProfile(memberId, request.toCommand());
        return ApiResponse.ok(ProfileSetupResponse.from(result));
    }

    @PostMapping("/weight")
    public ApiResponse<WeightSetupResponse> setupWeight(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @Valid @RequestBody WeightSetupRequest request
    ) {
        OnboardingService.WeightSetupResult result = onboardingService.setupWeight(memberId, request.toCommand());
        return ApiResponse.ok(WeightSetupResponse.from(result));
    }

    @PutMapping("/notifications")
    public ApiResponse<NotificationSetupResponse> setupNotifications(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @Valid @RequestBody NotificationSetupRequest request
    ) {
        OnboardingService.NotificationSetupResult result = onboardingService.setupNotifications(
            memberId,
            request.toCommand()
        );
        return ApiResponse.ok(NotificationSetupResponse.from(result));
    }

    @PutMapping("/health-connection")
    public ApiResponse<HealthConnectionResponse> updateHealthConnection(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @Valid @RequestBody HealthConnectionRequest request
    ) {
        OnboardingService.HealthConnectionResult result = onboardingService.updateHealthConnection(
            memberId,
            request.toCommand()
        );
        return ApiResponse.ok(HealthConnectionResponse.from(result));
    }

    @PostMapping("/groups/join")
    public ApiResponse<GroupJoinResponse> joinGroup(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @Valid @RequestBody GroupJoinRequest request
    ) {
        GroupService.GroupJoinResult result = groupService.joinGroup(memberId, request.toCommand());
        return ApiResponse.ok(GroupJoinResponse.from(result));
    }

    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GroupCreateResponse> createGroup(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @Valid @RequestBody GroupCreateRequest request
    ) {
        GroupService.GroupCreateResult result = groupService.createGroup(memberId, request.toCommand());
        return ApiResponse.created(GroupCreateResponse.from(result));
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
        @Digits(
            integer = 3,
            fraction = 2,
            message = "현재 체중은 소수점 둘째 자리까지 입력해주세요."
        )
        BigDecimal currentWeightKg,
        @NotNull(message = "목표 체중은 필수입니다.")
        @DecimalMin(value = "20.0", message = "목표 체중은 20kg 이상이어야 합니다.")
        @DecimalMax(value = "300.0", message = "목표 체중은 300kg 이하여야 합니다.")
        @Digits(
            integer = 3,
            fraction = 2,
            message = "목표 체중은 소수점 둘째 자리까지 입력해주세요."
        )
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
        @AssertTrue(
            message = "먹지 않음이면 시간은 비워두고, 먹는 식사는 시간을 입력해주세요."
        )
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
        @NotNull(message = "현재 체중은 필수입니다.")
        @DecimalMin(value = "20.0", message = "현재 체중은 20kg 이상이어야 합니다.")
        @DecimalMax(value = "300.0", message = "현재 체중은 300kg 이하여야 합니다.")
        @Digits(
            integer = 3,
            fraction = 2,
            message = "현재 체중은 소수점 둘째 자리까지 입력해주세요."
        )
        BigDecimal currentWeightKg,
        @NotNull(message = "목표 체중은 필수입니다.")
        @DecimalMin(value = "20.0", message = "목표 체중은 20kg 이상이어야 합니다.")
        @DecimalMax(value = "300.0", message = "목표 체중은 300kg 이하여야 합니다.")
        @Digits(
            integer = 3,
            fraction = 2,
            message = "목표 체중은 소수점 둘째 자리까지 입력해주세요."
        )
        BigDecimal targetWeightKg
    ) {
        public WeightSetupCommand toCommand() {
            return new WeightSetupCommand(currentWeightKg, targetWeightKg);
        }
    }

    public record WeightSetupResponse(
        Long weightRecordId,
        LocalDate recordedOn,
        BigDecimal weightKg,
        BigDecimal changeFromPreviousKg
    ) {
        public static WeightSetupResponse from(OnboardingService.WeightSetupResult result) {
            return new WeightSetupResponse(
                result.weightRecordId(),
                result.recordedOn(),
                result.weightKg(),
                result.changeFromPreviousKg()
            );
        }
    }

    public record NotificationSetupRequest(
        boolean recordReminderEnabled,
        boolean commentNotificationEnabled,
        boolean challengeNotificationEnabled
    ) {
        public NotificationSetupCommand toCommand() {
            return new NotificationSetupCommand(
                recordReminderEnabled,
                commentNotificationEnabled,
                challengeNotificationEnabled
            );
        }
    }

    public record NotificationSetupResponse(
        Long notificationSettingId,
        boolean enabled
    ) {
        public static NotificationSetupResponse from(OnboardingService.NotificationSetupResult result) {
            return new NotificationSetupResponse(result.notificationSettingId(), result.enabled());
        }
    }

    public record HealthConnectionRequest(
        boolean connected
    ) {
        public HealthConnectionCommand toCommand() {
            return new HealthConnectionCommand(connected);
        }
    }

    public record HealthConnectionResponse(
        boolean connected
    ) {
        public static HealthConnectionResponse from(OnboardingService.HealthConnectionResult result) {
            return new HealthConnectionResponse(result.connected());
        }
    }

    public record GroupJoinRequest(
        @NotBlank(message = "그룹 코드는 필수입니다.")
        @Pattern(
            regexp = "^[A-Za-z0-9]{6}$",
            message = "그룹 코드는 영문 또는 숫자 6자리여야 합니다."
        )
        String code
    ) {
        public GroupJoinCommand toCommand() {
            return new GroupJoinCommand(code.toUpperCase(Locale.ROOT));
        }
    }

    public record GroupJoinResponse(
        Long groupId,
        String code,
        String name,
        int memberCount
    ) {
        public static GroupJoinResponse from(GroupService.GroupJoinResult result) {
            return new GroupJoinResponse(result.groupId(), result.code(), result.name(), result.memberCount());
        }
    }

    public record GroupCreateRequest(
        @NotBlank(message = "그룹명은 필수입니다.")
        @Size(max = 30, message = "그룹명은 30자 이하로 입력해주세요.")
        String name
    ) {
        public GroupCreateCommand toCommand() {
            return new GroupCreateCommand(name);
        }
    }

    public record GroupCreateResponse(
        Long groupId,
        String code,
        String name
    ) {
        public static GroupCreateResponse from(GroupService.GroupCreateResult result) {
            return new GroupCreateResponse(result.groupId(), result.code(), result.name());
        }
    }
}
