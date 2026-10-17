package cmc.mody.record.application;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.upload.ImageUrlResolver;
import cmc.mody.grouping.domain.ModyGroup;
import cmc.mody.grouping.domain.GroupMemberStatus;
import cmc.mody.grouping.infrastructure.repository.GroupMemberRepository;
import cmc.mody.grouping.infrastructure.repository.ModyGroupRepository;
import cmc.mody.record.domain.ActivityRecord;
import cmc.mody.record.domain.ActivityRecordGroup;
import cmc.mody.record.domain.RecordComment;
import cmc.mody.record.domain.RecordType;
import cmc.mody.record.infrastructure.repository.ActivityRecordGroupRepository;
import cmc.mody.record.infrastructure.repository.ActivityRecordRepository;
import cmc.mody.record.infrastructure.repository.RecordCommentRepository;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminRecordService {
    private final ModyGroupRepository modyGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final ActivityRecordGroupRepository activityRecordGroupRepository;
    private final RecordCommentRepository recordCommentRepository;
    private final ImageUrlResolver imageUrlResolver;

    @Transactional(readOnly = true)
    public AdminRecordListResult getRecords(Long groupId) {
        validateGroup(groupId);
        Map<Long, String> nicknamesByMemberId = groupMemberRepository
            .findByGroupIdAndGroupMemberStatusAndDeletedAtIsNullOrderByJoinedAtAsc(groupId, GroupMemberStatus.JOINED)
            .stream()
            .filter(groupMember -> groupMember.getDisplayNickname() != null && !groupMember.getDisplayNickname().isBlank())
            .collect(Collectors.toMap(
                groupMember -> groupMember.getMemberId(),
                groupMember -> groupMember.getDisplayNickname(),
                (first, ignored) -> first
            ));
        return new AdminRecordListResult(activityRecordRepository.findActiveAdminRecordsByGroupId(groupId).stream()
            .map(record -> AdminRecordResult.from(record, nicknamesByMemberId.get(record.getMemberId()), imageUrlResolver))
            .toList());
    }

    @Transactional
    public AdminRecordResult updateRecord(Long groupId, Long recordId, AdminRecordUpdateCommand command) {
        ActivityRecord record = getGroupRecord(groupId, recordId);
        record.update(
            command.recordType(),
            command.mealTime(),
            command.menu(),
            command.exerciseDurationMinutes(),
            command.exerciseName()
        );
        return AdminRecordResult.from(record, null, imageUrlResolver);
    }

    @Transactional
    public void deleteRecord(Long groupId, Long recordId) {
        deleteWithShares(getGroupRecord(groupId, recordId));
    }

    /**
     * 그룹과 무관하게 기록을 삭제한다(신고 처리). 이미 삭제된 기록이면 아무것도 하지 않고 false를 돌려준다.
     */
    @Transactional
    public boolean deleteRecord(Long recordId) {
        return activityRecordRepository.findById(recordId)
            .filter(ActivityRecord::isActive)
            .map(record -> {
                deleteWithShares(record);
                return true;
            })
            .orElse(false);
    }

    private void deleteWithShares(ActivityRecord record) {
        Long recordId = record.getId();
        recordCommentRepository.findByRecordIdInAndDeletedAtIsNull(List.of(recordId))
            .forEach(RecordComment::delete);
        activityRecordGroupRepository.findByRecordIdAndDeletedAtIsNull(recordId)
            .forEach(ActivityRecordGroup::delete);
        record.delete();
    }

    private ActivityRecord getGroupRecord(Long groupId, Long recordId) {
        validateGroup(groupId);
        ActivityRecord record = activityRecordRepository.findById(recordId)
            .filter(ActivityRecord::isActive)
            .orElseThrow(() -> new GeneralException(ErrorStatus.RECORD_NOT_FOUND));
        activityRecordGroupRepository.findByRecordIdAndGroupIdAndDeletedAtIsNull(recordId, groupId)
            .orElseThrow(() -> new GeneralException(ErrorStatus.RECORD_NOT_FOUND));
        return record;
    }

    private void validateGroup(Long groupId) {
        modyGroupRepository.findById(groupId)
            .filter(ModyGroup::isActive)
            .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_NOT_FOUND));
    }

    public record AdminRecordListResult(List<AdminRecordResult> records) {
    }

    public record AdminRecordUpdateCommand(
        RecordType recordType,
        LocalTime mealTime,
        String menu,
        Integer exerciseDurationMinutes,
        String exerciseName
    ) {
    }

    public record AdminRecordResult(
        Long recordId,
        Long memberId,
        String memberNickname,
        RecordType recordType,
        LocalTime mealTime,
        String menu,
        Integer exerciseDurationMinutes,
        String exerciseName,
        String imageKey,
        String imageUrl,
        LocalDateTime uploadedAt
    ) {
        private static AdminRecordResult from(
            ActivityRecord record,
            String memberNickname,
            ImageUrlResolver imageUrlResolver
        ) {
            return new AdminRecordResult(
                record.getId(),
                record.getMemberId(),
                memberNickname,
                record.getRecordType(),
                record.getMealTime(),
                record.getMenu(),
                record.getExerciseDurationMinutes(),
                record.getExerciseName(),
                record.getImageKey(),
                imageUrlResolver.resolve(record.getImageKey()),
                record.getUploadedAt()
            );
        }
    }
}
