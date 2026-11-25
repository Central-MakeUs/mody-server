package cmc.mody.admin.application;

import cmc.mody.admin.domain.AdminAccount;
import cmc.mody.admin.infrastructure.AdminAccountRepository;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.report.application.event.ReportKind;
import cmc.mody.report.infrastructure.repository.CommentReportRepository;
import cmc.mody.report.infrastructure.repository.RecordReportRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Slack 버튼의 숨김·기각. 콘솔과 같은 처리(AdminReportService, AdminCommentReportService)를 부르고,
 * 이미 처리된 신고면 다시 처리하지 않고 마지막 처리 결과를 돌려준다(LLD-0040).
 * 처리마다 각 서비스의 트랜잭션이 따로 열리도록 이 클래스는 트랜잭션을 걸지 않는다.
 */
@Service
@RequiredArgsConstructor
public class SlackReportActionService {
    private final AdminReportService recordReports;
    private final AdminCommentReportService commentReports;
    private final RecordReportRepository recordReportRepository;
    private final CommentReportRepository commentReportRepository;
    private final AdminAccountRepository accounts;

    public Result resolve(ReportKind kind, long targetId, Action action, AdminAccount actor) {
        int resolved;
        try {
            resolved = switch (kind) {
                case RECORD -> action == Action.HIDE ? recordReports.hide(targetId, actor.getEmail()).resolvedReports()
                    : recordReports.dismiss(targetId, actor.getEmail()).resolvedReports();
                case COMMENT -> action == Action.HIDE
                    ? commentReports.hide(targetId, actor.getEmail()).resolvedReports()
                    : commentReports.dismiss(targetId, actor.getEmail()).resolvedReports();
            };
        } catch (GeneralException exception) {
            if (exception.getStatus() != ErrorStatus.NOT_FOUND) {
                throw exception;
            }
            return last(kind, targetId).map(found -> found.as(Outcome.ALREADY_RESOLVED, 0))
                .orElseThrow(() -> exception);
        }
        return last(kind, targetId).map(found -> found.as(Outcome.RESOLVED, resolved))
            .orElseThrow(() -> new GeneralException(ErrorStatus.NOT_FOUND));
    }

    private Optional<Result> last(ReportKind kind, long targetId) {
        return switch (kind) {
            case RECORD -> recordReportRepository
                .findFirstByRecordIdAndResolutionIsNotNullAndDeletedAtIsNullOrderByResolvedAtDesc(targetId)
                .map(report -> result(kind, targetId, report.getResolution().name(), report.getResolvedBy(),
                    report.getResolvedAt()));
            case COMMENT -> commentReportRepository
                .findFirstByCommentIdAndResolutionIsNotNullAndDeletedAtIsNullOrderByResolvedAtDesc(targetId)
                .map(report -> result(kind, targetId, report.getResolution().name(), report.getResolvedBy(),
                    report.getResolvedAt()));
        };
    }

    private Result result(ReportKind kind, long targetId, String resolution, String resolvedBy,
        LocalDateTime resolvedAt) {
        String slackUserId = resolvedBy == null ? null : accounts.findFirstByEmailAndSlackUserIdIsNotNull(resolvedBy)
            .map(AdminAccount::getSlackUserId).orElse(null);
        return new Result(null, kind.name(), Long.toString(targetId), resolution, resolvedBy, slackUserId,
            resolvedAt, 0);
    }

    public enum Action {
        HIDE, DISMISS;

        public static Action parse(String value) {
            return switch (value) {
                case "hide" -> HIDE;
                case "dismiss" -> DISMISS;
                default -> throw new GeneralException(ErrorStatus.BAD_REQUEST);
            };
        }
    }

    public enum Outcome { RESOLVED, ALREADY_RESOLVED }

    public record Result(Outcome outcome, String kind, String targetId, String resolution, String resolvedBy,
        String resolverSlackUserId, LocalDateTime resolvedAt, int resolvedReports) {
        Result as(Outcome outcome, int resolvedReports) {
            return new Result(outcome, kind, targetId, resolution, resolvedBy, resolverSlackUserId, resolvedAt,
                resolvedReports);
        }
    }
}
