package cmc.mody.record.application;

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
import cmc.mody.record.domain.RecordType;
import cmc.mody.record.infrastructure.repository.ActivityRecordRepository;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivityRecordService {
    private final IdGenerator idGenerator;
    private final MemberRepository memberRepository;
    private final ModyGroupRepository modyGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ActivityRecordRepository activityRecordRepository;

    @Transactional
    public RecordCreateResult createRecord(Long memberId, RecordCreateCommand command) {
        getMember(memberId);
        validateGroupMembership(memberId, command.groupId());

        Long recordId = idGenerator.nextId();
        LocalDateTime uploadedAt = LocalDateTime.now();
        ActivityRecord activityRecord = switch (command.recordType()) {
            case MEAL -> ActivityRecord.meal(
                recordId,
                memberId,
                command.groupId(),
                command.mealTime(),
                command.menu(),
                command.imageKey(),
                uploadedAt
            );
            case EXERCISE -> ActivityRecord.exercise(
                recordId,
                memberId,
                command.groupId(),
                command.exerciseDurationMinutes(),
                command.exerciseName(),
                command.imageKey(),
                uploadedAt
            );
        };

        ActivityRecord savedRecord = activityRecordRepository.save(activityRecord);
        return new RecordCreateResult(savedRecord.getId());
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
            .filter(Member::isActive)
            .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
    }

    private void validateGroupMembership(Long memberId, Long groupId) {
        if (groupId == null) {
            return;
        }

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

    public record RecordCreateCommand(
        Long groupId,
        RecordType recordType,
        String imageKey,
        LocalTime mealTime,
        String menu,
        Integer exerciseDurationMinutes,
        String exerciseName
    ) {
    }

    public record RecordCreateResult(Long recordId) {
    }
}
