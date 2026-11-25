package cmc.mody.admin.presentation;

import cmc.mody.admin.application.AdminSessionService;
import cmc.mody.admin.application.SlackReportActionService;
import cmc.mody.admin.domain.AdminAccount;
import cmc.mody.common.api.ApiResponse;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.report.application.event.ReportKind;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Slack 버튼으로 하는 신고 숨김·기각(LLD-0040). 인증은 {@code AdminRequestInterceptor}가 봇 서명으로 하고,
 * 버튼을 누른 Slack 사용자에 연결된 관리자 계정을 principal로 넣는다. 작업 이력도 그 계정으로 남는다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/slack/reports")
public class AdminSlackReportController {
    private final SlackReportActionService actions;

    @PostMapping("/records/{recordId}/{action}")
    public ApiResponse<SlackReportActionService.Result> record(@PathVariable long recordId,
        @PathVariable String action, HttpServletRequest request) {
        return ApiResponse.ok(actions.resolve(ReportKind.RECORD, recordId,
            SlackReportActionService.Action.parse(action), actor(request)));
    }

    @PostMapping("/comments/{commentId}/{action}")
    public ApiResponse<SlackReportActionService.Result> comment(@PathVariable long commentId,
        @PathVariable String action, HttpServletRequest request) {
        return ApiResponse.ok(actions.resolve(ReportKind.COMMENT, commentId,
            SlackReportActionService.Action.parse(action), actor(request)));
    }

    private AdminAccount actor(HttpServletRequest request) {
        if (request.getAttribute(AdminSessionService.PRINCIPAL_ATTRIBUTE) instanceof AdminAccount account) {
            return account;
        }
        throw new GeneralException(ErrorStatus.UNAUTHORIZED);
    }
}
