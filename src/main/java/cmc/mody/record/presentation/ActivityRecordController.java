package cmc.mody.record.presentation;

import cmc.mody.common.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ActivityRecordController {
    @GetMapping("/groups/{groupId}/activities/calendar")
    public ApiResponse<ActivityCalendarResponse> getActivityCalendar(
        @PathVariable Long groupId,
        @RequestParam String yearMonth
    ) {
        return ApiResponse.ok(new ActivityCalendarResponse(List.of(
            new ActivityDayResponse("2026-06-27", true, true)
        )));
    }

    @GetMapping("/groups/{groupId}/records")
    public ApiResponse<RecordCursorResponse> getRecords(
        @PathVariable Long groupId,
        @RequestParam String date,
        @RequestParam(required = false) Long cursor,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(new RecordCursorResponse(
            List.of(new RecordSummaryResponse(
                1L,
                "MEAL",
                1L,
                "민석",
                "profiles/member-1.jpg",
                "12:30",
                "샐러드",
                "records/meal-1.jpg"
            )),
            1L,
            false
        ));
    }

    @GetMapping("/records/{recordId}")
    public ApiResponse<RecordDetailResponse> getRecordDetail(@PathVariable Long recordId) {
        return ApiResponse.ok(new RecordDetailResponse(
            recordId,
            "MEAL",
            1L,
            "민석",
            "profiles/member-1.jpg",
            "12:30",
            "샐러드",
            "records/meal-1.jpg",
            List.of(new CommentResponse(1L, 2L, "친구", "좋다"))
        ));
    }

    @PostMapping("/records")
    public ApiResponse<RecordCreateResponse> createRecord(@RequestBody RecordCreateRequest request) {
        return ApiResponse.created(new RecordCreateResponse(1L));
    }

    @PostMapping("/records/{recordId}/comments")
    public ApiResponse<CommentCreateResponse> createComment(
        @PathVariable Long recordId,
        @RequestBody CommentCreateRequest request
    ) {
        return ApiResponse.created(new CommentCreateResponse(1L, recordId));
    }

    public record ActivityCalendarResponse(List<ActivityDayResponse> days) {
    }

    public record ActivityDayResponse(String date, boolean mealRecorded, boolean exerciseRecorded) {
    }

    public record RecordCursorResponse(List<RecordSummaryResponse> records, Long nextCursor, boolean hasNext) {
    }

    public record RecordSummaryResponse(
        Long recordId,
        String recordType,
        Long memberId,
        String nickname,
        String profileImageUrl,
        String recordedTime,
        String menu,
        String imageUrl
    ) {
    }

    public record RecordDetailResponse(
        Long recordId,
        String recordType,
        Long memberId,
        String nickname,
        String profileImageUrl,
        String recordedTime,
        String menu,
        String imageUrl,
        List<CommentResponse> comments
    ) {
    }

    public record CommentResponse(Long commentId, Long memberId, String nickname, String content) {
    }

    public record RecordCreateRequest(
        Long groupId,
        String recordType,
        String imageKey,
        String mealTime,
        String menu,
        Integer exerciseDurationMinutes,
        String exerciseName
    ) {
    }

    public record RecordCreateResponse(Long recordId) {
    }

    public record CommentCreateRequest(String content) {
    }

    public record CommentCreateResponse(Long commentId, Long recordId) {
    }
}
