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
import cmc.mody.record.domain.ActivityRecordGroup;
import cmc.mody.record.domain.RecordComment;
import cmc.mody.record.domain.RecordType;
import cmc.mody.record.domain.RecordViewHistory;
import cmc.mody.record.infrastructure.repository.ActivityRecordGroupRepository;
import cmc.mody.record.infrastructure.repository.ActivityRecordRepository;
import cmc.mody.record.infrastructure.repository.RecordCommentRepository;
import cmc.mody.record.infrastructure.repository.RecordViewHistoryRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final ActivityRecordGroupRepository activityRecordGroupRepository;
    private final RecordCommentRepository recordCommentRepository;
    private final RecordViewHistoryRepository recordViewHistoryRepository;
    private final UploadProperties uploadProperties;
    private final NotificationRequestService notificationRequestService;

    @Transactional(readOnly = true)
    public ActivityCalendarResult getActivityCalendar(Long memberId, Long groupId, LocalDate baseDate) {
        getMember(memberId);
        validateGroupMembership(memberId, groupId);
        LocalDate weekStartDate = baseDate.minusDays(baseDate.getDayOfWeek().getValue() % 7L);
        LocalDate weekEndDate = weekStartDate.plusDays(6);
        List<ActivityRecord> records = activityRecordRepository.findActiveGroupRecordsBetween(
            groupId,
            weekStartDate.atStartOfDay(),
            weekEndDate.plusDays(1).atStartOfDay(),
            GroupMemberStatus.JOINED
        );

        Set<LocalDate> recordedDates = records.stream()
            .map(record -> record.getUploadedAt().toLocalDate())
            .collect(Collectors.toSet());

        List<ActivityDayResult> days = new ArrayList<>();
        for (int offset = 0; offset < 7; offset++) {
            LocalDate date = weekStartDate.plusDays(offset);
            days.add(new ActivityDayResult(date, recordedDates.contains(date)));
        }
        return new ActivityCalendarResult(weekStartDate, weekEndDate, days);
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
        Map<Long, Integer> streakDaysByMember = pageRecords.stream()
            .map(ActivityRecord::getMemberId)
            .distinct()
            .collect(Collectors.toMap(
                Function.identity(),
                writerId -> calculateRecordingStreakDays(groupId, writerId, date)
            ));
        List<RecordSummaryResult> summaries = pageRecords.stream()
            .map(record -> toRecordSummary(
                record,
                groupMembers.get(record.getMemberId()),
                streakDaysByMember.getOrDefault(record.getMemberId(), 0)
            ))
            .toList();
        Long nextCursor = hasNext ? pageRecords.get(pageRecords.size() - 1).getId() : null;
        return new RecordCursorResult(summaries, nextCursor, hasNext);
    }

    @Transactional
    public RecordDetailPageResult getRecordDetail(Long memberId, Long groupId, Long recordId, Long cursor, int size) {
        getMember(memberId);
        ActivityRecord record = getAccessibleRecord(memberId, groupId, recordId);

        Map<Long, GroupMember> groupMembers = getJoinedGroupMembers(groupId);
        GroupMember recordWriter = groupMembers.get(record.getMemberId());
        if (recordWriter == null) {
            throw new GeneralException(ErrorStatus.RECORD_NOT_FOUND);
        }
        updateRecordViewHistory(memberId, groupId, record);

        return toRecordDetailPage(
            record,
            groupId,
            cursor,
            size,
            recordWriter.getDisplayNickname(),
            recordWriter.getDisplayProfileImageKey()
        );
    }

    @Transactional(readOnly = true)
    public CommentCursorResult getRecordComments(Long memberId, Long groupId, Long recordId, Long cursor, int size) {
        Member member = getMember(memberId);
        ActivityRecord record = getAccessibleRecord(memberId, groupId, recordId);
        int pageSize = normalizeSize(size);
        Map<Long, GroupMember> groupMembers = getJoinedGroupMembers(groupId);
        if (!groupMembers.containsKey(record.getMemberId())) {
            throw new GeneralException(ErrorStatus.RECORD_NOT_FOUND);
        }

        List<RecordComment> comments = recordCommentRepository.findActiveCommentsByCursor(
            record.getId(),
            groupId,
            memberId,
            cursor,
            GroupMemberStatus.JOINED,
            PageRequest.of(0, pageSize + 1)
        );
        boolean hasNext = comments.size() > pageSize;
        List<RecordComment> pageComments = hasNext ? comments.subList(0, pageSize) : comments;
        List<CommentResult> results = pageComments.stream()
            .map(comment -> toCommentResult(comment, member, groupMembers))
            .toList();
        Long nextCursor = hasNext ? pageComments.get(pageComments.size() - 1).getId() : null;
        return new CommentCursorResult(results, nextCursor, hasNext);
    }

    @Transactional
    public RecordCreateResult createRecord(Long memberId, RecordCreateCommand command) {
        getMember(memberId);
        List<GroupMember> joinedGroups = groupMemberRepository
            .findByMemberIdAndGroupMemberStatusAndDeletedAtIsNull(memberId, GroupMemberStatus.JOINED);
        if (joinedGroups.isEmpty()) {
            throw new GeneralException(ErrorStatus.GROUP_MEMBER_NOT_FOUND);
        }

        Long recordId = idGenerator.nextId();
        LocalDateTime uploadedAt = LocalDateTime.now();
        ActivityRecord activityRecord = switch (command.recordType()) {
            case MEAL -> ActivityRecord.meal(
                recordId,
                memberId,
                null,
                command.mealTime(),
                command.menu(),
                command.imageKey(),
                uploadedAt
            );
            case EXERCISE -> ActivityRecord.exercise(
                recordId,
                memberId,
                null,
                command.exerciseDurationMinutes(),
                command.exerciseName(),
                command.imageKey(),
                uploadedAt
            );
        };

        ActivityRecord savedRecord = activityRecordRepository.save(activityRecord);
        List<Long> groupIds = joinedGroups.stream()
            .map(GroupMember::getGroupId)
            .toList();
        for (Long groupId : groupIds) {
            activityRecordGroupRepository.save(new ActivityRecordGroup(
                idGenerator.nextId(),
                savedRecord.getId(),
                groupId,
                memberId,
                uploadedAt
            ));
        }
        return new RecordCreateResult(savedRecord.getId(), groupIds);
    }

    @Transactional
    public CommentCreateResult createComment(Long memberId, Long groupId, Long recordId, CommentCreateCommand command) {
        Member member = getMember(memberId);
        ActivityRecord record = getAccessibleRecord(memberId, groupId, recordId);
        Map<Long, GroupMember> groupMembers = getJoinedGroupMembers(groupId);
        if (!groupMembers.containsKey(record.getMemberId())) {
            throw new GeneralException(ErrorStatus.RECORD_NOT_FOUND);
        }
        String commenterNickname = groupMembers.containsKey(memberId)
            ? groupMembers.get(memberId).getDisplayNickname()
            : member.getNickname();

        Long commentId = idGenerator.nextId();
        RecordComment savedComment = recordCommentRepository.save(new RecordComment(
            commentId,
            recordId,
            groupId,
            memberId,
            command.content().trim()
        ));
        notificationRequestService.requestCommentCreated(recordId, memberId, commenterNickname);
        return new CommentCreateResult(savedComment.getId(), recordId);
    }

    private ActivityRecord getAccessibleRecord(Long memberId, Long groupId, Long recordId) {
        validateGroupMembership(memberId, groupId);
        ActivityRecord record = activityRecordRepository.findById(recordId)
            .filter(ActivityRecord::isActive)
            .orElseThrow(() -> new GeneralException(ErrorStatus.RECORD_NOT_FOUND));
        activityRecordGroupRepository.findByRecordIdAndGroupIdAndDeletedAtIsNull(recordId, groupId)
            .orElseThrow(() -> new GeneralException(ErrorStatus.RECORD_NOT_FOUND));
        return record;
    }

    private RecordDetailPageResult toRecordDetailPage(
        ActivityRecord record,
        Long groupId,
        Long cursor,
        int size,
        String nickname,
        String profileImageKey
    ) {
        LocalDate recordDate = record.getUploadedAt().toLocalDate();
        int pageSize = normalizeSize(size);
        Long queryCursor = (cursor == null) ? record.getId() - 1 : cursor;

        List<ActivityRecord> foundRecords = activityRecordRepository.findActiveRecordsForDetailCarousel(
            groupId,
            record.getMemberId(),
            recordDate.atStartOfDay(),
            recordDate.plusDays(1).atStartOfDay(),
            queryCursor,
            GroupMemberStatus.JOINED,
            PageRequest.of(0, pageSize + 1)
        );

        boolean hasNext = foundRecords.size() > pageSize;
        List<ActivityRecord> pageRecords = hasNext ? foundRecords.subList(0, pageSize) : foundRecords;

        List<RecordDetailResult> records = pageRecords.stream()
            .map(found -> toRecordDetail(found, nickname, profileImageKey))
            .toList();

        long totalCount = activityRecordRepository.countActiveRecordsForDetailCarousel(
            groupId,
            record.getMemberId(),
            recordDate.atStartOfDay(),
            recordDate.plusDays(1).atStartOfDay(),
            GroupMemberStatus.JOINED
        );

        int currentIndex = -1;
        for (int index = 0; index < records.size(); index++) {
            if (records.get(index).recordId().equals(record.getId())) {
                currentIndex = index;
                break;
            }
        }
        if (currentIndex < 0) {
            currentIndex = 0;
        }

        Long nextCursor = hasNext ? records.get(records.size() - 1).recordId() : null;

        return new RecordDetailPageResult((int) totalCount, currentIndex, records, nextCursor, hasNext);
    }

    private RecordDetailResult toRecordDetail(ActivityRecord record, String nickname, String profileImageKey) {
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
            toImageUrl(record.getImageKey())
        );
    }

    private CommentResult toCommentResult(
        RecordComment comment,
        Member currentMember,
        Map<Long, GroupMember> groupMembers
    ) {
        if (groupMembers.isEmpty()) {
            return new CommentResult(
                comment.getId(),
                comment.getMemberId(),
                currentMember.getNickname(),
                toImageUrl(currentMember.getProfileImageKey()),
                comment.getContent(),
                true
            );
        }

        GroupMember groupMember = groupMembers.get(comment.getMemberId());
        if (groupMember == null) {
            throw new GeneralException(ErrorStatus.RECORD_NOT_FOUND);
        }
        return new CommentResult(
            comment.getId(),
            comment.getMemberId(),
            groupMember.getDisplayNickname(),
            toImageUrl(groupMember.getDisplayProfileImageKey()),
            comment.getContent(),
            comment.getMemberId().equals(currentMember.getId())
        );
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

    private int calculateRecordingStreakDays(Long groupId, Long writerMemberId, LocalDate baseDate) {
        List<LocalDate> recordedDates = activityRecordRepository.findActiveGroupRecordsByMemberBefore(
                groupId,
                writerMemberId,
                baseDate.plusDays(1).atStartOfDay(),
                GroupMemberStatus.JOINED
            )
            .stream()
            .map(found -> found.getUploadedAt().toLocalDate())
            .distinct()
            .toList();

        int streakDays = 0;
        LocalDate expectedDate = baseDate;
        for (LocalDate recordedDate : recordedDates) {
            if (recordedDate.isAfter(expectedDate)) {
                continue;
            }
            if (!recordedDate.equals(expectedDate)) {
                break;
            }
            streakDays++;
            expectedDate = expectedDate.minusDays(1);
        }
        return streakDays;
    }

    private void updateRecordViewHistory(Long viewerMemberId, Long groupId, ActivityRecord record) {
        recordViewHistoryRepository
            .findByViewerMemberIdAndGroupIdAndWriterMemberIdAndDeletedAtIsNull(
                viewerMemberId,
                groupId,
                record.getMemberId()
            )
            .ifPresentOrElse(
                history -> history.updateLastViewedAt(LocalDateTime.now()),
                () -> recordViewHistoryRepository.save(new RecordViewHistory(
                    idGenerator.nextId(),
                    viewerMemberId,
                    groupId,
                    record.getMemberId(),
                    LocalDateTime.now()
                ))
            );
    }

    private RecordSummaryResult toRecordSummary(
        ActivityRecord record,
        GroupMember groupMember,
        int recordingStreakDays
    ) {
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
            toImageUrl(record.getImageKey()),
            recordingStreakDays
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
        RecordType recordType,
        String imageKey,
        LocalTime mealTime,
        String menu,
        Integer exerciseDurationMinutes,
        String exerciseName
    ) {
    }

    public record RecordCreateResult(Long recordId, List<Long> groupIds) {
    }

    public record CommentCreateCommand(String content) {
    }

    public record CommentCreateResult(Long commentId, Long recordId) {
    }

    public record ActivityCalendarResult(
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        List<ActivityDayResult> days
    ) {
    }

    public record ActivityDayResult(LocalDate date, boolean hasRecord) {
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
        String imageUrl,
        int recordingStreakDays
    ) {
    }

    public record RecordDetailPageResult(
        int totalCount,
        int currentIndex,
        List<RecordDetailResult> records,
        Long nextCursor,
        boolean hasNext
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
        String imageUrl
    ) {
    }

    public record CommentCursorResult(List<CommentResult> comments, Long nextCursor, boolean hasNext) {
    }

    public record CommentResult(
        Long commentId,
        Long memberId,
        String nickname,
        String profileImageUrl,
        String content,
        boolean isMine
    ) {
    }
}
