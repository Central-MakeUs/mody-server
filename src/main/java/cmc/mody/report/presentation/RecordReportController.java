package cmc.mody.report.presentation;

import cmc.mody.auth.presentation.support.CurrentMember;
import cmc.mody.common.api.ApiResponse;
import cmc.mody.report.application.CommentReportService;
import cmc.mody.report.application.RecordReportService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class RecordReportController {
    private final RecordReportService recordReportService;
    private final CommentReportService commentReportService;

    @PostMapping("/groups/{groupId}/records/{recordId}/report")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RecordReportResponse> reportRecord(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @PathVariable Long groupId,
        @PathVariable Long recordId
    ) {
        RecordReportService.RecordReportResult result = recordReportService.reportRecord(memberId, groupId, recordId);
        return ApiResponse.created(RecordReportResponse.from(result));
    }

    @PostMapping("/groups/{groupId}/records/{recordId}/comments/{commentId}/report")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommentReportResponse> reportComment(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @PathVariable Long groupId,
        @PathVariable Long recordId,
        @PathVariable Long commentId
    ) {
        CommentReportService.CommentReportResult result = commentReportService.reportComment(
            memberId,
            groupId,
            recordId,
            commentId
        );
        return ApiResponse.created(CommentReportResponse.from(result));
    }

    public record CommentReportResponse(Long reportId, Long commentId) {
        public static CommentReportResponse from(CommentReportService.CommentReportResult result) {
            return new CommentReportResponse(result.reportId(), result.commentId());
        }
    }

    public record RecordReportResponse(Long reportId, Long recordId) {
        public static RecordReportResponse from(RecordReportService.RecordReportResult result) {
            return new RecordReportResponse(result.reportId(), result.recordId());
        }
    }
}
