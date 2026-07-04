package cmc.mody.record.application;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.id.IdGenerator;
import cmc.mody.common.upload.UploadProperties;
import cmc.mody.grouping.domain.GroupMember;
import cmc.mody.grouping.domain.GroupMemberStatus;
import cmc.mody.grouping.domain.ModyGroup;
import cmc.mody.grouping.infrastructure.repository.GroupMemberRepository;
import cmc.mody.grouping.infrastructure.repository.ModyGroupRepository;
import cmc.mody.member.domain.Member;
import cmc.mody.member.infrastructure.repository.MemberRepository;
import cmc.mody.notification.application.NotificationRequestService;
import cmc.mody.record.domain.ActivityRecord;
import cmc.mody.record.domain.RecordComment;
import cmc.mody.record.domain.RecordType;
import cmc.mody.record.infrastructure.repository.ActivityRecordRepository;
import cmc.mody.record.infrastructure.repository.RecordCommentRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivityRecordService {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final IdGenerator idGenerator;
    private final MemberRepository memberRepository;
    private final ModyGroupRepository modyGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final RecordCommentRepository recordCommentRepository;
    private final UploadProperties uploadProperties;
    private final NotificationRequestService notificationRequestService;

    @Transactional(readOnly = true)
    public ActivityCalendarResult getActivityCalendar(Long memberId, Long groupId, YearMonth yearMonth) {
        getMember(memberId);
        validateGroupMembership(memberId, groupId);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth().plusDays(1);
        List<ActivityRecord> records = activityRecordRepository.findActiveGroupRecordsBetween(
            groupId,
            startDate.atStartOfDay(),
            endDate.atStartOfDay(),
            GroupMemberStatus.JOINED
        );

        Map<LocalDate, EnumMap<RecordType, Boolean>> recordedByDate = records.stream()
            .collect(Collectors.groupingBy(
                record -> record.getUploadedAt().toLocalDate(),
                Collectors.collectingAndThen(Collectors.toList(), this::toRecordTypeMap)
            ));

        List<ActivityDayResult> days = new ArrayList<>();
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = yearMonth.atDay(day);
            Map<RecordType, Boolean> recordedTypes = recordedByDate.getOrDefault(date, new EnumMap<>(RecordType.class));
            days.add(new ActivityDayResult(
                date,
                recordedTypes.getOrDefault(RecordType.MEAL, false),
                recordedTypes.getOrDefault(RecordType.EXERCISE, false)
            ));
        }
        return new ActivityCalendarResult(days);
    }

    @Transactional(readOnly = true)
    public RecordCursorResult getRecords(Long memberId, Long groupId, LocalDate date, Long cursor, int size) {
        getMember(memberId);
        validateGroupMembership(memberId, groupId);
        int pageSize = normalizeSize(size);
        List<ActivityRecord> records = activityRecordRepository.findActiveGroupRecordsByCursor(
            groupId,
            date.atStartOfDay(),
            date.plusDays(1).atStartOfDay(),
            cursor,
            GroupMemberStatus.JOINED,
            PageRequest.of(0, pageSize + 1)
        );
        boolean hasNext = records.size() > pageSize;
        List<ActivityRecord> pageRecords = hasNext ? records.subList(0, pageSize) : records;
        Map<Long, GroupMember> groupMembers = getJoinedGroupMembers(groupId);
        List<RecordSummaryResult> summaries = pageRecords.stream()
            .map(record -> toRecordSummary(record, groupMembers.get(record.getMemberId())))
            .toList();
        Long nextCursor = hasNext ? pageRecords.get(pageRecords.size() - 1).getId() : null;
        return new RecordCursorResult(summaries, nextCursor, hasNext);
    }

    @Transactional(readOnly = true)
    public RecordDetailResult getRecordDetail(Long memberId, Long recordId) {
        Member member = getMember(memberId);
        ActivityRecord record = getAccessibleRecord(memberId, recordId);

        if (record.getGroupId() == null) {
            return toPersonalRecordDetail(member, record);
        }

        Map<Long, GroupMember> groupMembers = getJoinedGroupMembers(record.getGroupId());
        GroupMember recordWriter = groupMembers.get(record.getMemberId());
        if (recordWriter == null) {
            throw new GeneralException(ErrorStatus.RECORD_NOT_FOUND);
        }

        List<CommentResult> comments = recordCommentRepository
            .findByRecordIdAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(record.getId())
            .stream()
            .filter(comment -> groupMembers.containsKey(comment.getMemberId()))
            .map(comment -> toCommentResult(comment, groupMembers.get(comment.getMemberId())))
            .toList();

        return toRecordDetail(
            record,
            recordWriter.getDisplayNickname(),
            recordWriter.getDisplayProfileImageKey(),
            comments
        );
    }

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

    @Transactional
    public CommentCreateResult createComment(Long memberId, Long recordId, CommentCreateCommand command) {
        getMember(memberId);
        ActivityRecord record = getAccessibleRecord(memberId, recordId);
        if (record.getGroupId() != null) {
            Map<Long, GroupMember> groupMembers = getJoinedGroupMembers(record.getGroupId());
            if (!groupMembers.containsKey(record.getMemberId())) {
                throw new GeneralException(ErrorStatus.RECORD_NOT_FOUND);
            }
        }

        Long commentId = idGenerator.nextId();
        RecordComment savedComment = recordCommentRepository.save(new RecordComment(
            commentId,
            recordId,
            memberId,
            command.content().trim()
        ));
        notificationRequestService.requestCommentCreated(recordId, memberId);
        return new CommentCreateResult(savedComment.getId(), recordId);
    }

    private ActivityRecord getAccessibleRecord(Long memberId, Long recordId) {
        ActivityRecord record = activityRecordRepository.findById(recordId)
            .filter(ActivityRecord::isActive)
            .orElseThrow(() -> new GeneralException(ErrorStatus.RECORD_NOT_FOUND));

        if (record.getGroupId() == null) {
            if (!record.getMemberId().equals(memberId)) {
                throw new GeneralException(ErrorStatus.RECORD_NOT_FOUND);
            }
            return record;
        }

        validateGroupMembership(memberId, record.getGroupId());
        return record;
    }

    private RecordDetailResult toPersonalRecordDetail(Member member, ActivityRecord record) {
        List<CommentResult> comments = recordCommentRepository
            .findByRecordIdAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(record.getId())
            .stream()
            .filter(comment -> comment.getMemberId().equals(member.getId()))
            .map(comment -> new CommentResult(
                comment.getId(),
                comment.getMemberId(),
                member.getNickname(),
                comment.getContent()
            ))
            .toList();

        return toRecordDetail(record, member.getNickname(), member.getProfileImageKey(), comments);
    }

    private RecordDetailResult toRecordDetail(
        ActivityRecord record,
        String nickname,
        String profileImageKey,
        List<CommentResult> comments
    ) {
        return new RecordDetailResult(
            record.getId(),
            record.getRecordType(),
            record.getMemberId(),
            nickname,
            toImageUrl(profileImageKey),
            resolveRecordedTime(record),
            record.getMenu(),
            record.getExerciseDurationMinutes(),
            record.getExerciseName(),
            toImageUrl(record.getImageKey()),
            comments
        );
    }

    private CommentResult toCommentResult(RecordComment comment, GroupMember groupMember) {
        return new CommentResult(
            comment.getId(),
            comment.getMemberId(),
            groupMember.getDisplayNickname(),
            comment.getContent()
        );
    }

    private EnumMap<RecordType, Boolean> toRecordTypeMap(List<ActivityRecord> records) {
        EnumMap<RecordType, Boolean> recordedTypes = new EnumMap<>(RecordType.class);
        for (ActivityRecord record : records) {
            recordedTypes.put(record.getRecordType(), true);
        }
        return recordedTypes;
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private Map<Long, GroupMember> getJoinedGroupMembers(Long groupId) {
        return groupMemberRepository
            .findByGroupIdAndGroupMemberStatusAndDeletedAtIsNullOrderByJoinedAtAsc(
                groupId,
                GroupMemberStatus.JOINED
            )
            .stream()
            .collect(Collectors.toMap(GroupMember::getMemberId, Function.identity(), (left, right) -> left));
    }

    private RecordSummaryResult toRecordSummary(ActivityRecord record, GroupMember groupMember) {
        return new RecordSummaryResult(
            record.getId(),
            record.getRecordType(),
            record.getMemberId(),
            groupMember == null ? null : groupMember.getDisplayNickname(),
            groupMember == null ? null : toImageUrl(groupMember.getDisplayProfileImageKey()),
            resolveRecordedTime(record),
            record.getMenu(),
            record.getExerciseDurationMinutes(),
            record.getExerciseName(),
            toImageUrl(record.getImageKey())
        );
    }

    private LocalTime resolveRecordedTime(ActivityRecord record) {
        if (record.getRecordType() == RecordType.MEAL) {
            return record.getMealTime();
        }
        return record.getUploadedAt().toLocalTime().withSecond(0).withNano(0);
    }

    private String toImageUrl(String imageKey) {
        if (imageKey == null || imageKey.isBlank()) {
            return null;
        }
        String baseUrl = uploadProperties.getBaseUrl();
        if (baseUrl.endsWith("/")) {
            return baseUrl + imageKey;
        }
        return baseUrl + "/" + imageKey;
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

    public record CommentCreateCommand(String content) {
    }

    public record CommentCreateResult(Long commentId, Long recordId) {
    }

    public record ActivityCalendarResult(List<ActivityDayResult> days) {
    }

    public record ActivityDayResult(LocalDate date, boolean mealRecorded, boolean exerciseRecorded) {
    }

    public record RecordCursorResult(List<RecordSummaryResult> records, Long nextCursor, boolean hasNext) {
    }

    public record RecordSummaryResult(
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
    }

    public record RecordDetailResult(
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
        List<CommentResult> comments
    ) {
    }

    public record CommentResult(Long commentId, Long memberId, String nickname, String content) {
    }
}
