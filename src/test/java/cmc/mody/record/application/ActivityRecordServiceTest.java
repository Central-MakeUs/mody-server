package cmc.mody.record.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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
import cmc.mody.record.application.ActivityRecordService.RecordCreateCommand;
import cmc.mody.record.application.ActivityRecordService.RecordCreateResult;
import cmc.mody.record.domain.ActivityRecord;
import cmc.mody.record.domain.ActivityRecordGroup;
import cmc.mody.record.domain.RecordComment;
import cmc.mody.record.domain.RecordType;
import cmc.mody.record.domain.RecordViewHistory;
import cmc.mody.record.infrastructure.repository.ActivityRecordGroupRepository;
import cmc.mody.record.infrastructure.repository.ActivityRecordRepository;
import cmc.mody.record.infrastructure.repository.RecordCommentRepository;
import cmc.mody.record.infrastructure.repository.RecordViewHistoryRepository;
import org.springframework.data.domain.PageRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActivityRecordServiceTest {
    @Mock
    private IdGenerator idGenerator;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ModyGroupRepository modyGroupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private ActivityRecordRepository activityRecordRepository;

    @Mock
    private ActivityRecordGroupRepository activityRecordGroupRepository;

    @Mock
    private RecordCommentRepository recordCommentRepository;

    @Mock
    private NotificationRequestService notificationRequestService;

    @Mock
    private RecordViewHistoryRepository recordViewHistoryRepository;

    @Captor
    private ArgumentCaptor<ActivityRecord> activityRecordCaptor;

    @Captor
    private ArgumentCaptor<ActivityRecordGroup> activityRecordGroupCaptor;

    @Captor
    private ArgumentCaptor<RecordComment> recordCommentCaptor;

    @Captor
    private ArgumentCaptor<RecordViewHistory> recordViewHistoryCaptor;

    @Test
    @DisplayName("주간 활동 여부는 기준 날짜가 속한 일요일부터 토요일까지 그룹 전체 기록 여부를 반환한다.")
    void getActivityCalendar() {
        ActivityRecordService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(modyGroupRepository.findById(10L)).willReturn(Optional.of(group()));
        given(groupMemberRepository.existsByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            1L,
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(true);
        given(activityRecordRepository.findActiveGroupRecordsBetween(
            10L,
            LocalDateTime.of(2026, 7, 12, 0, 0),
            LocalDateTime.of(2026, 7, 19, 0, 0),
            GroupMemberStatus.JOINED
        )).willReturn(List.of(
            mealRecord(100L, LocalDateTime.of(2026, 7, 13, 12, 30)),
            exerciseRecord(101L, LocalDateTime.of(2026, 7, 13, 20, 0)),
            exerciseRecord(102L, LocalDateTime.of(2026, 7, 15, 21, 0))
        ));

        ActivityRecordService.ActivityCalendarResult result = service.getActivityCalendar(
            1L,
            10L,
            LocalDate.of(2026, 7, 13)
        );

        assertThat(result.weekStartDate()).isEqualTo(LocalDate.of(2026, 7, 12));
        assertThat(result.weekEndDate()).isEqualTo(LocalDate.of(2026, 7, 18));
        assertThat(result.days()).hasSize(7);
        assertThat(result.days())
            .extracting(ActivityRecordService.ActivityDayResult::date)
            .containsExactly(
                LocalDate.of(2026, 7, 12),
                LocalDate.of(2026, 7, 13),
                LocalDate.of(2026, 7, 14),
                LocalDate.of(2026, 7, 15),
                LocalDate.of(2026, 7, 16),
                LocalDate.of(2026, 7, 17),
                LocalDate.of(2026, 7, 18)
            );
        assertThat(result.days())
            .extracting(ActivityRecordService.ActivityDayResult::hasRecord)
            .containsExactly(false, true, false, true, false, false, false);
    }

    @Test
    @DisplayName("날짜별 기록은 커서 기반으로 조회하고 작성자 표시 정보를 함께 반환한다.")
    void getRecords() {
        ActivityRecordService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(modyGroupRepository.findById(10L)).willReturn(Optional.of(group()));
        given(groupMemberRepository.existsByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            1L,
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(true);
        given(activityRecordRepository.findActiveGroupRecordsByCursor(
            org.mockito.ArgumentMatchers.eq(10L),
            org.mockito.ArgumentMatchers.eq(LocalDateTime.of(2026, 7, 1, 0, 0)),
            org.mockito.ArgumentMatchers.eq(LocalDateTime.of(2026, 7, 2, 0, 0)),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.eq(GroupMemberStatus.JOINED),
            any()
        )).willReturn(List.of(
            mealRecord(103L, LocalDateTime.of(2026, 7, 1, 12, 30)),
            exerciseRecord(102L, LocalDateTime.of(2026, 7, 1, 20, 0)),
            mealRecord(101L, LocalDateTime.of(2026, 7, 1, 8, 0))
        ));
        given(groupMemberRepository.findByGroupIdAndGroupMemberStatusAndDeletedAtIsNullOrderByJoinedAtAsc(
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(List.of(new GroupMember(
            20L,
            1L,
            10L,
            "민석",
            "profiles/member-1.jpg",
            LocalDateTime.of(2026, 6, 1, 0, 0)
        )));
        given(activityRecordRepository.findActiveGroupRecordsByMemberBefore(
            10L,
            1L,
            LocalDateTime.of(2026, 7, 2, 0, 0),
            GroupMemberStatus.JOINED
        )).willReturn(List.of(
            mealRecord(103L, LocalDateTime.of(2026, 7, 1, 12, 30)),
            exerciseRecord(90L, LocalDateTime.of(2026, 6, 30, 20, 0))
        ));

        ActivityRecordService.RecordCursorResult result = service.getRecords(
            1L,
            10L,
            LocalDate.of(2026, 7, 1),
            null,
            2
        );

        assertThat(result.records()).hasSize(2);
        assertThat(result.nextCursor()).isEqualTo(102L);
        assertThat(result.hasNext()).isTrue();
        ActivityRecordService.RecordSummaryResult firstRecord = result.records().get(0);
        assertThat(firstRecord.recordId()).isEqualTo(103L);
        assertThat(firstRecord.nickname()).isEqualTo("민석");
        assertThat(firstRecord.profileImageUrl()).isEqualTo("https://storage.example.com/profiles/member-1.jpg");
        assertThat(firstRecord.imageUrl()).isEqualTo("https://storage.example.com/records/1/2026/07/meal.jpg");
        assertThat(firstRecord.recordingStreakDays()).isEqualTo(2);
    }

    @Test
    @DisplayName("기록 상세는 작성자의 해당 날짜 기록 목록과 현재 위치를 반환한다.")
    void getRecordDetail() {
        ActivityRecordService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(activityRecordRepository.findById(100L))
            .willReturn(Optional.of(mealRecord(100L, LocalDateTime.of(2026, 7, 1, 12, 30))));
        given(activityRecordGroupRepository.findByRecordIdAndGroupIdAndDeletedAtIsNull(100L, 10L))
            .willReturn(Optional.of(recordGroup(100L, 10L, 1L, LocalDateTime.of(2026, 7, 1, 12, 30))));
        given(activityRecordGroupRepository.findByRecordIdAndGroupIdAndDeletedAtIsNull(100L, 10L))
            .willReturn(Optional.of(recordGroup(100L, 10L, 1L, LocalDateTime.of(2026, 7, 1, 12, 30))));
        given(activityRecordGroupRepository.findByRecordIdAndGroupIdAndDeletedAtIsNull(100L, 10L))
            .willReturn(Optional.of(recordGroup(100L, 10L, 1L, LocalDateTime.of(2026, 7, 1, 12, 30))));
        given(modyGroupRepository.findById(10L)).willReturn(Optional.of(group()));
        given(groupMemberRepository.existsByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            1L,
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(true);
        given(groupMemberRepository.findByGroupIdAndGroupMemberStatusAndDeletedAtIsNullOrderByJoinedAtAsc(
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(List.of(
            new GroupMember(20L, 1L, 10L, "민석", "profiles/member-1.jpg", LocalDateTime.of(2026, 6, 1, 0, 0)),
            new GroupMember(21L, 2L, 10L, "친구", "profiles/member-2.jpg", LocalDateTime.of(2026, 6, 2, 0, 0))
        ));
        given(activityRecordRepository.findActiveRecordsForDetailCarousel(
            10L,
            1L,
            LocalDateTime.of(2026, 7, 1, 0, 0),
            LocalDateTime.of(2026, 7, 2, 0, 0),
            99L,
            GroupMemberStatus.JOINED,
            PageRequest.of(0, 21)
        ))
            .willReturn(List.of(
                mealRecord(99L, LocalDateTime.of(2026, 7, 1, 8, 0)),
                mealRecord(100L, LocalDateTime.of(2026, 7, 1, 12, 30)),
                exerciseRecord(101L, LocalDateTime.of(2026, 7, 1, 20, 0))
            ));
        given(activityRecordRepository.countActiveRecordsForDetailCarousel(
            10L,
            1L,
            LocalDateTime.of(2026, 7, 1, 0, 0),
            LocalDateTime.of(2026, 7, 2, 0, 0),
            GroupMemberStatus.JOINED
        )).willReturn(3L);
        given(recordViewHistoryRepository.findByViewerMemberIdAndGroupIdAndWriterMemberIdAndDeletedAtIsNull(
            1L,
            10L,
            1L
        )).willReturn(Optional.empty());
        given(idGenerator.nextId()).willReturn(300L);
        given(recordViewHistoryRepository.save(any(RecordViewHistory.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        ActivityRecordService.RecordDetailPageResult result = service.getRecordDetail(1L, 10L, 100L, null, 20);

        assertThat(result.totalCount()).isEqualTo(3);
        assertThat(result.currentIndex()).isEqualTo(1);
        assertThat(result.records()).hasSize(3);
        assertThat(result.records().get(1).recordId()).isEqualTo(100L);
        assertThat(result.records().get(1).nickname()).isEqualTo("민석");
        assertThat(result.records().get(1).profileImageUrl())
            .isEqualTo("https://storage.example.com/profiles/member-1.jpg");
        assertThat(result.records().get(1).imageUrl()).isEqualTo("https://storage.example.com/records/1/2026/07/meal.jpg");
        assertThat(result.records().get(2).recordType()).isEqualTo(RecordType.EXERCISE);
        then(recordViewHistoryRepository).should().save(recordViewHistoryCaptor.capture());
        assertThat(recordViewHistoryCaptor.getValue().getViewerMemberId()).isEqualTo(1L);
        assertThat(recordViewHistoryCaptor.getValue().getGroupId()).isEqualTo(10L);
        assertThat(recordViewHistoryCaptor.getValue().getWriterMemberId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("기록 댓글은 커서 기반으로 조회하고 작성자 프로필과 내 댓글 여부를 반환한다.")
    void getRecordComments() {
        ActivityRecordService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(activityRecordRepository.findById(100L))
            .willReturn(Optional.of(mealRecord(100L, LocalDateTime.of(2026, 7, 1, 12, 30))));
        given(activityRecordGroupRepository.findByRecordIdAndGroupIdAndDeletedAtIsNull(100L, 10L))
            .willReturn(Optional.of(recordGroup(100L, 10L, 1L, LocalDateTime.of(2026, 7, 1, 12, 30))));
        given(modyGroupRepository.findById(10L)).willReturn(Optional.of(group()));
        given(groupMemberRepository.existsByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            1L,
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(true);
        given(groupMemberRepository.findByGroupIdAndGroupMemberStatusAndDeletedAtIsNullOrderByJoinedAtAsc(
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(List.of(
            new GroupMember(20L, 1L, 10L, "민석", "profiles/member-1.jpg", LocalDateTime.of(2026, 6, 1, 0, 0)),
            new GroupMember(21L, 2L, 10L, "친구", "profiles/member-2.jpg", LocalDateTime.of(2026, 6, 2, 0, 0))
        ));
        given(recordCommentRepository.findActiveCommentsByCursor(
            org.mockito.ArgumentMatchers.eq(100L),
            org.mockito.ArgumentMatchers.eq(10L),
            org.mockito.ArgumentMatchers.eq(1L),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.eq(GroupMemberStatus.JOINED),
            any()
        )).willReturn(List.of(
            new RecordComment(200L, 100L, 10L, 2L, "좋다"),
            new RecordComment(201L, 100L, 10L, 1L, "고마워"),
            new RecordComment(202L, 100L, 10L, 2L, "또 올려줘")
        ));

        ActivityRecordService.CommentCursorResult result = service.getRecordComments(1L, 10L, 100L, null, 2);

        assertThat(result.comments()).hasSize(2);
        assertThat(result.nextCursor()).isEqualTo(201L);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.comments().get(0).memberId()).isEqualTo(2L);
        assertThat(result.comments().get(0).nickname()).isEqualTo("친구");
        assertThat(result.comments().get(0).profileImageUrl())
            .isEqualTo("https://storage.example.com/profiles/member-2.jpg");
        assertThat(result.comments().get(0).isMine()).isFalse();
        assertThat(result.comments().get(1).memberId()).isEqualTo(1L);
        assertThat(result.comments().get(1).isMine()).isTrue();
    }

    @Test
    @DisplayName("기록 작성자가 그룹을 나갔으면 상세를 조회할 수 없다.")
    void getRecordDetailRecordWriterLeft() {
        ActivityRecordService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(activityRecordRepository.findById(100L))
            .willReturn(Optional.of(mealRecord(100L, LocalDateTime.of(2026, 7, 1, 12, 30))));
        given(activityRecordGroupRepository.findByRecordIdAndGroupIdAndDeletedAtIsNull(100L, 10L))
            .willReturn(Optional.of(recordGroup(100L, 10L, 1L, LocalDateTime.of(2026, 7, 1, 12, 30))));
        given(modyGroupRepository.findById(10L)).willReturn(Optional.of(group()));
        given(groupMemberRepository.existsByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            1L,
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(true);
        given(groupMemberRepository.findByGroupIdAndGroupMemberStatusAndDeletedAtIsNullOrderByJoinedAtAsc(
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(List.of());

        assertThatThrownBy(() -> service.getRecordDetail(1L, 10L, 100L, null, 20))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getStatus()).isEqualTo(ErrorStatus.RECORD_NOT_FOUND));
    }

    @Test
    @DisplayName("식사 기록을 생성하면 원본 기록과 가입한 모든 그룹 노출 매핑을 저장한다.")
    void createMealRecord() {
        ActivityRecordService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(groupMemberRepository.findByMemberIdAndGroupMemberStatusAndDeletedAtIsNull(
            1L,
            GroupMemberStatus.JOINED
        )).willReturn(List.of(
            new GroupMember(20L, 1L, 10L, "민석", null, LocalDateTime.of(2026, 6, 1, 0, 0)),
            new GroupMember(21L, 1L, 11L, "민석", null, LocalDateTime.of(2026, 6, 2, 0, 0))
        ));
        given(idGenerator.nextId()).willReturn(100L, 1000L, 1001L);
        given(activityRecordRepository.save(any(ActivityRecord.class)))
            .willAnswer(invocation -> invocation.getArgument(0));
        given(activityRecordGroupRepository.save(any(ActivityRecordGroup.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        RecordCreateResult result = service.createRecord(1L, new RecordCreateCommand(
            RecordType.MEAL,
            "records/1/2026/07/4111584723968.jpg",
            LocalTime.of(12, 30),
            "샐러드",
            null,
            null
        ));

        assertThat(result.recordId()).isEqualTo(100L);
        assertThat(result.groupIds()).containsExactly(10L, 11L);
        then(activityRecordRepository).should().save(activityRecordCaptor.capture());
        ActivityRecord savedRecord = activityRecordCaptor.getValue();
        assertThat(savedRecord.getMemberId()).isEqualTo(1L);
        assertThat(savedRecord.getGroupId()).isNull();
        assertThat(savedRecord.getRecordType()).isEqualTo(RecordType.MEAL);
        assertThat(savedRecord.getMealTime()).isEqualTo(LocalTime.of(12, 30));
        assertThat(savedRecord.getMenu()).isEqualTo("샐러드");
        assertThat(savedRecord.getExerciseDurationMinutes()).isNull();
        assertThat(savedRecord.getExerciseName()).isNull();
        assertThat(savedRecord.getImageKey()).isEqualTo("records/1/2026/07/4111584723968.jpg");
        assertThat(savedRecord.getUploadedAt()).isNotNull();
        then(activityRecordGroupRepository).should(org.mockito.Mockito.times(2))
            .save(activityRecordGroupCaptor.capture());
        assertThat(activityRecordGroupCaptor.getAllValues())
            .extracting(ActivityRecordGroup::getGroupId)
            .containsExactly(10L, 11L);
    }

    @Test
    @DisplayName("운동 기록을 생성하면 운동 필드만 저장한다.")
    void createExerciseRecord() {
        ActivityRecordService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(groupMemberRepository.findByMemberIdAndGroupMemberStatusAndDeletedAtIsNull(
            1L,
            GroupMemberStatus.JOINED
        )).willReturn(List.of(new GroupMember(20L, 1L, 10L, "민석", null, LocalDateTime.of(2026, 6, 1, 0, 0))));
        given(idGenerator.nextId()).willReturn(101L, 1002L);
        given(activityRecordRepository.save(any(ActivityRecord.class)))
            .willAnswer(invocation -> invocation.getArgument(0));
        given(activityRecordGroupRepository.save(any(ActivityRecordGroup.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        RecordCreateResult result = service.createRecord(1L, new RecordCreateCommand(
            RecordType.EXERCISE,
            "records/1/2026/07/4111584723969.jpg",
            null,
            null,
            40,
            "러닝"
        ));

        assertThat(result.recordId()).isEqualTo(101L);
        then(activityRecordRepository).should().save(activityRecordCaptor.capture());
        ActivityRecord savedRecord = activityRecordCaptor.getValue();
        assertThat(savedRecord.getGroupId()).isNull();
        assertThat(savedRecord.getRecordType()).isEqualTo(RecordType.EXERCISE);
        assertThat(savedRecord.getMealTime()).isNull();
        assertThat(savedRecord.getMenu()).isNull();
        assertThat(savedRecord.getExerciseDurationMinutes()).isEqualTo(40);
        assertThat(savedRecord.getExerciseName()).isEqualTo("러닝");
    }

    @Test
    @DisplayName("접근 가능한 기록에 댓글을 작성한다.")
    void createComment() {
        ActivityRecordService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(activityRecordRepository.findById(100L))
            .willReturn(Optional.of(mealRecord(100L, LocalDateTime.of(2026, 7, 1, 12, 30))));
        given(activityRecordGroupRepository.findByRecordIdAndGroupIdAndDeletedAtIsNull(100L, 10L))
            .willReturn(Optional.of(recordGroup(100L, 10L, 1L, LocalDateTime.of(2026, 7, 1, 12, 30))));
        given(modyGroupRepository.findById(10L)).willReturn(Optional.of(group()));
        given(groupMemberRepository.existsByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            1L,
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(true);
        given(groupMemberRepository.findByGroupIdAndGroupMemberStatusAndDeletedAtIsNullOrderByJoinedAtAsc(
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(List.of(new GroupMember(
            20L,
            1L,
            10L,
            "민석",
            "profiles/member-1.jpg",
            LocalDateTime.of(2026, 6, 1, 0, 0)
        )));
        given(idGenerator.nextId()).willReturn(200L);
        given(recordCommentRepository.save(any(RecordComment.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        ActivityRecordService.CommentCreateResult result = service.createComment(
            1L,
            10L,
            100L,
            new ActivityRecordService.CommentCreateCommand("  좋다  ")
        );

        assertThat(result.commentId()).isEqualTo(200L);
        then(recordCommentRepository).should().save(recordCommentCaptor.capture());
        RecordComment savedComment = recordCommentCaptor.getValue();
        assertThat(savedComment.getRecordId()).isEqualTo(100L);
        assertThat(savedComment.getGroupId()).isEqualTo(10L);
        assertThat(savedComment.getMemberId()).isEqualTo(1L);
        assertThat(savedComment.getContent()).isEqualTo("좋다");
        then(notificationRequestService).should().requestCommentCreated(100L, 1L);
    }

    @Test
    @DisplayName("없는 기록에는 댓글을 작성할 수 없다.")
    void createCommentRecordNotFound() {
        ActivityRecordService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(modyGroupRepository.findById(10L)).willReturn(Optional.of(group()));
        given(groupMemberRepository.existsByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            1L,
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(true);
        given(activityRecordRepository.findById(100L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.createComment(
            1L,
            10L,
            100L,
            new ActivityRecordService.CommentCreateCommand("좋다")
        ))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getStatus()).isEqualTo(ErrorStatus.RECORD_NOT_FOUND));
    }

    @Test
    @DisplayName("회원이 없으면 기록을 생성할 수 없다.")
    void createRecordMemberNotFound() {
        ActivityRecordService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.createRecord(1L, mealCommand()))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getStatus()).isEqualTo(ErrorStatus.MEMBER_NOT_FOUND));
    }

    @Test
    @DisplayName("참여 중인 그룹이 없으면 기록을 생성할 수 없다.")
    void createRecordGroupMemberNotFound() {
        ActivityRecordService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(groupMemberRepository.findByMemberIdAndGroupMemberStatusAndDeletedAtIsNull(
            1L,
            GroupMemberStatus.JOINED
        )).willReturn(List.of());

        assertThatThrownBy(() -> service.createRecord(1L, mealCommand()))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getStatus()).isEqualTo(ErrorStatus.GROUP_MEMBER_NOT_FOUND));
    }

    private ActivityRecordService service() {
        return new ActivityRecordService(
            idGenerator,
            memberRepository,
            modyGroupRepository,
            groupMemberRepository,
            activityRecordRepository,
            activityRecordGroupRepository,
            recordCommentRepository,
            recordViewHistoryRepository,
            new UploadProperties(),
            notificationRequestService
        );
    }

    private RecordCreateCommand mealCommand() {
        return new RecordCreateCommand(
            RecordType.MEAL,
            "records/1/2026/07/4111584723968.jpg",
            LocalTime.of(12, 30),
            "샐러드",
            null,
            null
        );
    }

    private ActivityRecordGroup recordGroup(Long recordId, Long groupId, Long memberId, LocalDateTime uploadedAt) {
        return new ActivityRecordGroup(1000L + recordId + groupId, recordId, groupId, memberId, uploadedAt);
    }

    private Member member() {
        return new Member(1L, "민석", LocalDate.of(2000, 1, 1), BigDecimal.valueOf(68.0));
    }

    private ModyGroup group() {
        return new ModyGroup(10L, "ABCD2345", "모디 그룹");
    }

    private ActivityRecord mealRecord(Long id, LocalDateTime uploadedAt) {
        return ActivityRecord.meal(
            id,
            1L,
            10L,
            LocalTime.of(12, 30),
            "샐러드",
            "records/1/2026/07/meal.jpg",
            uploadedAt
        );
    }

    private ActivityRecord exerciseRecord(Long id, LocalDateTime uploadedAt) {
        return ActivityRecord.exercise(
            id,
            1L,
            10L,
            40,
            "러닝",
            "records/1/2026/07/exercise.jpg",
            uploadedAt
        );
    }
}
