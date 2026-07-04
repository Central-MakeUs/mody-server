package cmc.mody.record.presentation;

import cmc.mody.auth.presentation.support.CurrentMember;
import cmc.mody.common.api.ApiResponse;
import cmc.mody.record.application.ActivityRecordService;
import cmc.mody.record.application.ActivityRecordService.CommentCreateCommand;
import cmc.mody.record.application.ActivityRecordService.RecordCreateCommand;
import cmc.mody.record.domain.RecordType;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ActivityRecordController {
    private static final String MEAL_RECORD_PAYLOAD_MESSAGE =
        "식사 기록은 식사 시간과 메뉴를 입력하고 운동 정보는 비워주세요.";
    private static final String EXERCISE_RECORD_PAYLOAD_MESSAGE =
        "운동 기록은 운동 시간과 운동명을 입력하고 식사 정보는 비워주세요.";

    private final ActivityRecordService activityRecordService;

    @GetMapping("/groups/{groupId}/activities/calendar")
    public ApiResponse<ActivityCalendarResponse> getActivityCalendar(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @PathVariable Long groupId,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth
    ) {
        ActivityRecordService.ActivityCalendarResult result = activityRecordService.getActivityCalendar(
            memberId,
            groupId,
            yearMonth
        );
        return ApiResponse.ok(ActivityCalendarResponse.from(result));
    }

    @GetMapping("/groups/{groupId}/records")
    public ApiResponse<RecordCursorResponse> getRecords(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @PathVariable Long groupId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(required = false) Long cursor,
        @RequestParam(defaultValue = "20") int size
    ) {
        ActivityRecordService.RecordCursorResult result = activityRecordService.getRecords(
            memberId,
            groupId,
            date,
            cursor,
            size
        );
        return ApiResponse.ok(RecordCursorResponse.from(result));
    }

    @GetMapping("/records/{recordId}")
    public ApiResponse<RecordDetailResponse> getRecordDetail(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @PathVariable Long recordId
    ) {
        ActivityRecordService.RecordDetailPageResult result = activityRecordService.getRecordDetail(memberId, recordId);
        return ApiResponse.ok(RecordDetailResponse.from(result));
    }

    @GetMapping("/records/{recordId}/comments")
    public ApiResponse<CommentCursorResponse> getRecordComments(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @PathVariable Long recordId,
        @RequestParam(required = false) Long cursor,
        @RequestParam(defaultValue = "20") int size
    ) {
        ActivityRecordService.CommentCursorResult result = activityRecordService.getRecordComments(
            memberId,
            recordId,
            cursor,
            size
        );
        return ApiResponse.ok(CommentCursorResponse.from(result));
    }

    @PostMapping("/records")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RecordCreateResponse> createRecord(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @Valid @RequestBody RecordCreateRequest request
    ) {
        ActivityRecordService.RecordCreateResult result = activityRecordService.createRecord(
            memberId,
            request.toCommand()
        );
        return ApiResponse.created(RecordCreateResponse.from(result));
    }

    @PostMapping("/records/{recordId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentCreateResponse> createComment(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @PathVariable Long recordId,
        @Valid @RequestBody CommentCreateRequest request
    ) {
        ActivityRecordService.CommentCreateResult result = activityRecordService.createComment(
            memberId,
            recordId,
            request.toCommand()
        );
        return ApiResponse.created(CommentCreateResponse.from(result));
    }

    public record ActivityCalendarResponse(List<ActivityDayResponse> days) {
        public static ActivityCalendarResponse from(ActivityRecordService.ActivityCalendarResult result) {
            return new ActivityCalendarResponse(result.days().stream()
                .map(ActivityDayResponse::from)
                .toList());
        }
    }

    public record ActivityDayResponse(LocalDate date, boolean mealRecorded, boolean exerciseRecorded) {
        public static ActivityDayResponse from(ActivityRecordService.ActivityDayResult result) {
            return new ActivityDayResponse(result.date(), result.mealRecorded(), result.exerciseRecorded());
        }
    }

    public record RecordCursorResponse(List<RecordSummaryResponse> records, Long nextCursor, boolean hasNext) {
        public static RecordCursorResponse from(ActivityRecordService.RecordCursorResult result) {
            return new RecordCursorResponse(result.records().stream()
                .map(RecordSummaryResponse::from)
                .toList(), result.nextCursor(), result.hasNext());
        }
    }

    public record RecordSummaryResponse(
        Long recordId,
        RecordType recordType,
        Long memberId,
        String nickname,
        String profileImageUrl,
        LocalTime recordedTime,
        String menu,
        Integer exerciseDurationMinutes,
        String exerciseName,
        String imageUrl,
        int recordingStreakDays
    ) {
        public static RecordSummaryResponse from(ActivityRecordService.RecordSummaryResult result) {
            return new RecordSummaryResponse(
                result.recordId(),
                result.recordType(),
                result.memberId(),
                result.nickname(),
                result.profileImageUrl(),
                result.recordedTime(),
                result.menu(),
                result.exerciseDurationMinutes(),
                result.exerciseName(),
                result.imageUrl(),
                result.recordingStreakDays()
            );
        }
    }

    public record RecordDetailResponse(
        int totalCount,
        int currentIndex,
        List<RecordDetailItemResponse> records
    ) {
        public static RecordDetailResponse from(ActivityRecordService.RecordDetailPageResult result) {
            return new RecordDetailResponse(
                result.totalCount(),
                result.currentIndex(),
                result.records().stream()
                    .map(RecordDetailItemResponse::from)
                    .toList()
            );
        }
    }

    public record RecordDetailItemResponse(
        Long recordId,
        RecordType recordType,
        Long memberId,
        String nickname,
        String profileImageUrl,
        LocalTime recordedTime,
        String menu,
        Integer exerciseDurationMinutes,
        String exerciseName,
        String imageUrl
    ) {
        public static RecordDetailItemResponse from(ActivityRecordService.RecordDetailResult result) {
            return new RecordDetailItemResponse(
                result.recordId(),
                result.recordType(),
                result.memberId(),
                result.nickname(),
                result.profileImageUrl(),
                result.recordedTime(),
                result.menu(),
                result.exerciseDurationMinutes(),
                result.exerciseName(),
                result.imageUrl()
            );
        }
    }

    public record CommentCursorResponse(List<CommentResponse> comments, Long nextCursor, boolean hasNext) {
        public static CommentCursorResponse from(ActivityRecordService.CommentCursorResult result) {
            return new CommentCursorResponse(result.comments().stream()
                .map(CommentResponse::from)
                .toList(), result.nextCursor(), result.hasNext());
        }
    }

    public record CommentResponse(
        Long commentId,
        Long memberId,
        String nickname,
        String profileImageUrl,
        String content,
        boolean isMine
    ) {
        public static CommentResponse from(ActivityRecordService.CommentResult result) {
            return new CommentResponse(
                result.commentId(),
                result.memberId(),
                result.nickname(),
                result.profileImageUrl(),
                result.content(),
                result.isMine()
            );
        }
    }

    public record RecordCreateRequest(
        @Positive(message = "그룹 id는 양수여야 합니다.")
        Long groupId,
        @NotNull(message = "기록 타입은 필수입니다.")
        RecordType recordType,
        @NotBlank(message = "이미지 키는 필수입니다.")
        @Size(max = 500, message = "이미지 키는 500자 이하로 입력해주세요.")
        @Pattern(
            regexp = "^records/[A-Za-z0-9/_-]+\\.(jpg|jpeg|png|webp)$",
            message = "기록 이미지는 records 도메인으로 발급된 이미지 키여야 합니다."
        )
        String imageKey,
        LocalTime mealTime,
        @Size(max = 100, message = "메뉴는 100자 이하로 입력해주세요.")
        String menu,
        @jakarta.validation.constraints.PositiveOrZero(message = "운동 시간은 0시간 이상이어야 합니다.")
        @Max(value = 24, message = "운동 시간은 24시간 이하로 입력해주세요.")
        Integer exerciseDurationHours,
        @jakarta.validation.constraints.PositiveOrZero(message = "운동 분은 0분 이상이어야 합니다.")
        @Max(value = 59, message = "운동 분은 59분 이하로 입력해주세요.")
        Integer exerciseDurationMinutes,
        @Size(max = 30, message = "운동명은 30자 이하로 입력해주세요.")
        String exerciseName
    ) {
        @AssertTrue(message = MEAL_RECORD_PAYLOAD_MESSAGE)
        public boolean isMealRecordPayloadValid() {
            if (recordType != RecordType.MEAL) {
                return true;
            }
            return mealTime != null
                && hasText(menu)
                && exerciseDurationHours == null
                && exerciseDurationMinutes == null
                && !hasText(exerciseName);
        }

        @AssertTrue(message = EXERCISE_RECORD_PAYLOAD_MESSAGE)
        public boolean isExerciseRecordPayloadValid() {
            if (recordType != RecordType.EXERCISE) {
                return true;
            }
            return totalExerciseDurationMinutes() != null
                && totalExerciseDurationMinutes() > 0
                && totalExerciseDurationMinutes() <= 1440
                && hasText(exerciseName)
                && mealTime == null
                && !hasText(menu);
        }

        public RecordCreateCommand toCommand() {
            return new RecordCreateCommand(
                groupId,
                recordType,
                imageKey,
                mealTime,
                menu,
                totalExerciseDurationMinutes(),
                exerciseName
            );
        }

        private boolean hasText(String value) {
            return value != null && !value.isBlank();
        }

        private Integer totalExerciseDurationMinutes() {
            if (exerciseDurationHours == null && exerciseDurationMinutes == null) {
                return null;
            }
            int hours = exerciseDurationHours == null ? 0 : exerciseDurationHours;
            int minutes = exerciseDurationMinutes == null ? 0 : exerciseDurationMinutes;
            return hours * 60 + minutes;
        }
    }

    public record RecordCreateResponse(Long recordId) {
        public static RecordCreateResponse from(ActivityRecordService.RecordCreateResult result) {
            return new RecordCreateResponse(result.recordId());
        }
    }

    public record CommentCreateRequest(
        @NotBlank(message = "댓글 내용은 필수입니다.")
        @Size(max = 100, message = "댓글은 100자 이하로 입력해주세요.")
        String content
    ) {
        public CommentCreateCommand toCommand() {
            return new CommentCreateCommand(content);
        }
    }

    public record CommentCreateResponse(Long commentId, Long recordId) {
        public static CommentCreateResponse from(ActivityRecordService.CommentCreateResult result) {
            return new CommentCreateResponse(result.commentId(), result.recordId());
        }
    }
}
