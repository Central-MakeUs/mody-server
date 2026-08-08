package cmc.mody.record.presentation;

import cmc.mody.common.admin.AdminAccessService;
import cmc.mody.common.api.ApiResponse;
import cmc.mody.record.application.AdminRecordService;
import cmc.mody.record.domain.RecordType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/groups/{groupId}/records")
public class AdminRecordController {
    private static final String ADMIN_API_KEY_HEADER = "X-Admin-Api-Key";

    private final AdminAccessService adminAccessService;
    private final AdminRecordService adminRecordService;

    @GetMapping
    public ApiResponse<AdminRecordListResponse> getRecords(
        @RequestHeader(name = ADMIN_API_KEY_HEADER, required = false) String adminApiKey,
        @PathVariable Long groupId
    ) {
        adminAccessService.validate(adminApiKey);
        return ApiResponse.ok(AdminRecordListResponse.from(adminRecordService.getRecords(groupId)));
    }

    @PutMapping("/{recordId}")
    public ApiResponse<AdminRecordResponse> updateRecord(
        @RequestHeader(name = ADMIN_API_KEY_HEADER, required = false) String adminApiKey,
        @PathVariable Long groupId,
        @PathVariable Long recordId,
        @Valid @RequestBody AdminRecordUpdateRequest request
    ) {
        adminAccessService.validate(adminApiKey);
        return ApiResponse.ok(AdminRecordResponse.from(adminRecordService.updateRecord(groupId, recordId, request.toCommand())));
    }

    @DeleteMapping("/{recordId}")
    public ApiResponse<Void> deleteRecord(
        @RequestHeader(name = ADMIN_API_KEY_HEADER, required = false) String adminApiKey,
        @PathVariable Long groupId,
        @PathVariable Long recordId
    ) {
        adminAccessService.validate(adminApiKey);
        adminRecordService.deleteRecord(groupId, recordId);
        return ApiResponse.ok();
    }

    public record AdminRecordListResponse(List<AdminRecordResponse> records) {
        private static AdminRecordListResponse from(AdminRecordService.AdminRecordListResult result) {
            return new AdminRecordListResponse(result.records().stream().map(AdminRecordResponse::from).toList());
        }
    }

    public record AdminRecordResponse(
        Long recordId,
        Long memberId,
        String memberNickname,
        RecordType recordType,
        LocalTime mealTime,
        String menu,
        Integer exerciseDurationMinutes,
        String exerciseName,
        String imageKey,
        LocalDateTime uploadedAt
    ) {
        private static AdminRecordResponse from(AdminRecordService.AdminRecordResult result) {
            return new AdminRecordResponse(
                result.recordId(),
                result.memberId(),
                result.memberNickname(),
                result.recordType(),
                result.mealTime(),
                result.menu(),
                result.exerciseDurationMinutes(),
                result.exerciseName(),
                result.imageKey(),
                result.uploadedAt()
            );
        }
    }

    public record AdminRecordUpdateRequest(
        @NotNull(message = "기록 타입은 필수입니다.")
        RecordType recordType,
        LocalTime mealTime,
        String menu,
        @Min(value = 1, message = "운동 시간은 1분 이상이어야 합니다.")
        @Max(value = 1440, message = "운동 시간은 24시간 이하여야 합니다.")
        Integer exerciseDurationMinutes,
        String exerciseName
    ) {
        @AssertTrue(message = "식사 기록은 식사 시간과 메뉴를 입력하고 운동 정보는 비워주세요.")
        public boolean isMealValid() {
            return recordType != RecordType.MEAL
                || (mealTime != null && menu != null && !menu.isBlank()
                && exerciseDurationMinutes == null && (exerciseName == null || exerciseName.isBlank()));
        }

        @AssertTrue(message = "운동 기록은 운동 시간과 운동명을 입력하고 식사 정보는 비워주세요.")
        public boolean isExerciseValid() {
            return recordType != RecordType.EXERCISE
                || (exerciseDurationMinutes != null && exerciseName != null && !exerciseName.isBlank()
                && mealTime == null && (menu == null || menu.isBlank()));
        }

        private AdminRecordService.AdminRecordUpdateCommand toCommand() {
            return new AdminRecordService.AdminRecordUpdateCommand(
                recordType,
                mealTime,
                menu == null ? null : menu.trim(),
                exerciseDurationMinutes,
                exerciseName == null ? null : exerciseName.trim()
            );
        }
    }
}
