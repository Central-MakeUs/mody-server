package cmc.mody.report.application;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.id.IdGenerator;
import cmc.mody.grouping.domain.GroupMemberStatus;
import cmc.mody.grouping.domain.ModyGroup;
import cmc.mody.grouping.infrastructure.repository.GroupMemberRepository;
import cmc.mody.grouping.infrastructure.repository.ModyGroupRepository;
import cmc.mody.member.domain.Member;
import cmc.mody.member.infrastructure.repository.MemberRepository;
import cmc.mody.record.domain.ActivityRecord;
import cmc.mody.record.domain.RecordComment;
import cmc.mody.record.infrastructure.repository.ActivityRecordGroupRepository;
import cmc.mody.record.infrastructure.repository.ActivityRecordRepository;
import cmc.mody.record.infrastructure.repository.RecordCommentRepository;
import cmc.mody.report.application.event.ReportFiledEvent;
import cmc.mody.report.domain.CommentReport;
import cmc.mody.report.infrastructure.repository.CommentReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기록 댓글 신고. 댓글 목록에서 보이는 댓글만 신고할 수 있고, 같은 댓글을 다시 신고하면 기존 신고를 돌려준다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentReportService {
    private final IdGenerator idGenerator;
    private final MemberRepository memberRepository;
    private final ModyGroupRepository modyGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final ActivityRecordGroupRepository activityRecordGroupRepository;
    private final RecordCommentRepository recordCommentRepository;
    private final CommentReportRepository commentReportRepository;
    private final ApplicationEventPublisher events;

    @Transactional
    public CommentReportResult reportComment(Long memberId, Long groupId, Long recordId, Long commentId) {
        getMember(memberId);
        validateJoinedMember(memberId, groupId);
        validateVisibleRecord(groupId, recordId);
        RecordComment comment = getVisibleComment(groupId, recordId, commentId);
        return commentReportRepository
            .findByReporterMemberIdAndCommentIdAndDeletedAtIsNull(memberId, comment.getId())
            .map(CommentReportResult::from)
            .orElseGet(() -> saveReport(memberId, comment));
    }

    private CommentReportResult saveReport(Long memberId, RecordComment comment) {
        CommentReport savedReport = commentReportRepository.save(new CommentReport(
            idGenerator.nextId(),
            memberId,
            comment.getId(),
            comment.getRecordId(),
            comment.getGroupId()
        ));
        log.info("comment reported reportId={} commentId={} recordId={}",
            savedReport.getId(), comment.getId(), comment.getRecordId());
        events.publishEvent(ReportFiledEvent.comment(comment.getId(), comment.getRecordId(), savedReport.getId()));
        return CommentReportResult.from(savedReport);
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
            .filter(Member::isActive)
            .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
    }

    private void validateJoinedMember(Long memberId, Long groupId) {
        modyGroupRepository.findById(groupId)
            .filter(ModyGroup::isActive)
            .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_NOT_FOUND));
        if (!isJoined(memberId, groupId)) {
            throw new GeneralException(ErrorStatus.GROUP_MEMBER_NOT_FOUND);
        }
    }

    private void validateVisibleRecord(Long groupId, Long recordId) {
        ActivityRecord record = activityRecordRepository.findById(recordId)
            .filter(ActivityRecord::isActive)
            .orElseThrow(() -> new GeneralException(ErrorStatus.RECORD_NOT_FOUND));
        activityRecordGroupRepository.findByRecordIdAndGroupIdAndDeletedAtIsNull(recordId, groupId)
            .orElseThrow(() -> new GeneralException(ErrorStatus.RECORD_NOT_FOUND));
        if (!isJoined(record.getMemberId(), groupId)) {
            throw new GeneralException(ErrorStatus.RECORD_NOT_FOUND);
        }
    }

    /** 댓글 목록과 같은 기준: 삭제되지 않았고, 이 기록·그룹의 댓글이며, 작성자가 그룹에 남아 있다. */
    private RecordComment getVisibleComment(Long groupId, Long recordId, Long commentId) {
        RecordComment comment = recordCommentRepository.findById(commentId)
            .filter(RecordComment::isActive)
            .filter(found -> found.getRecordId().equals(recordId) && found.getGroupId().equals(groupId))
            .orElseThrow(() -> new GeneralException(ErrorStatus.RECORD_COMMENT_NOT_FOUND));
        if (!isJoined(comment.getMemberId(), groupId)) {
            throw new GeneralException(ErrorStatus.RECORD_COMMENT_NOT_FOUND);
        }
        return comment;
    }

    private boolean isJoined(Long memberId, Long groupId) {
        return groupMemberRepository.existsByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            memberId,
            groupId,
            GroupMemberStatus.JOINED
        );
    }

    public record CommentReportResult(Long reportId, Long commentId) {
        public static CommentReportResult from(CommentReport report) {
            return new CommentReportResult(report.getId(), report.getCommentId());
        }
    }
}
