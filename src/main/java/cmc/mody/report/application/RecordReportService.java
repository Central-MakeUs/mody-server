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
import cmc.mody.record.infrastructure.repository.ActivityRecordGroupRepository;
import cmc.mody.record.infrastructure.repository.ActivityRecordRepository;
import cmc.mody.report.domain.RecordReport;
import cmc.mody.report.infrastructure.repository.RecordReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecordReportService {
    private final IdGenerator idGenerator;
    private final MemberRepository memberRepository;
    private final ModyGroupRepository modyGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final ActivityRecordGroupRepository activityRecordGroupRepository;
    private final RecordReportRepository recordReportRepository;

    @Transactional
    public RecordReportResult reportRecord(Long memberId, Long groupId, Long recordId) {
        getMember(memberId);
        ActivityRecord record = getReportableRecord(memberId, groupId, recordId);
        return recordReportRepository
            .findByReporterMemberIdAndRecordIdAndDeletedAtIsNull(memberId, record.getId())
            .map(RecordReportResult::from)
            .orElseGet(() -> saveReport(memberId, record.getId()));
    }

    private RecordReportResult saveReport(Long memberId, Long recordId) {
        RecordReport savedReport = recordReportRepository.save(new RecordReport(
            idGenerator.nextId(),
            memberId,
            recordId
        ));
        return RecordReportResult.from(savedReport);
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
            .filter(Member::isActive)
            .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
    }

    private ActivityRecord getReportableRecord(Long memberId, Long groupId, Long recordId) {
        validateJoinedMember(memberId, groupId);
        ActivityRecord record = activityRecordRepository.findById(recordId)
            .filter(ActivityRecord::isActive)
            .orElseThrow(() -> new GeneralException(ErrorStatus.RECORD_NOT_FOUND));
        activityRecordGroupRepository.findByRecordIdAndGroupIdAndDeletedAtIsNull(recordId, groupId)
            .orElseThrow(() -> new GeneralException(ErrorStatus.RECORD_NOT_FOUND));

        boolean writerJoined = groupMemberRepository.existsByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            record.getMemberId(),
            groupId,
            GroupMemberStatus.JOINED
        );
        if (!writerJoined) {
            throw new GeneralException(ErrorStatus.RECORD_NOT_FOUND);
        }
        return record;
    }

    private void validateJoinedMember(Long memberId, Long groupId) {
        modyGroupRepository.findById(groupId)
            .filter(ModyGroup::isActive)
            .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_NOT_FOUND));

        boolean joined = groupMemberRepository.existsByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            memberId,
            groupId,
            GroupMemberStatus.JOINED
        );
        if (!joined) {
            throw new GeneralException(ErrorStatus.GROUP_MEMBER_NOT_FOUND);
        }
    }

    public record RecordReportResult(Long reportId, Long recordId) {
        public static RecordReportResult from(RecordReport report) {
            return new RecordReportResult(report.getId(), report.getRecordId());
        }
    }
}
