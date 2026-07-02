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
import cmc.mody.record.application.ActivityRecordService.RecordCreateCommand;
import cmc.mody.record.application.ActivityRecordService.RecordCreateResult;
import cmc.mody.record.domain.ActivityRecord;
import cmc.mody.record.domain.RecordComment;
import cmc.mody.record.domain.RecordType;
import cmc.mody.record.infrastructure.repository.ActivityRecordRepository;
import cmc.mody.record.infrastructure.repository.RecordCommentRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
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
    private RecordCommentRepository recordCommentRepository;

    @Captor
    private ArgumentCaptor<ActivityRecord> activityRecordCaptor;

    @Captor
    private ArgumentCaptor<RecordComment> recordCommentCaptor;

    @Test
    @DisplayName("월별 활동 여부는 해당 월 전체 날짜의 기록 여부를 반환한다.")
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
            LocalDateTime.of(2026, 7, 1, 0, 0),
            LocalDateTime.of(2026, 8, 1, 0, 0),
            GroupMemberStatus.JOINED
        )).willReturn(List.of(
            mealRecord(100L, LocalDateTime.of(2026, 7, 1, 12, 30)),
            exerciseRecord(101L, LocalDateTime.of(2026, 7, 1, 20, 0)),
            exerciseRecord(102L, LocalDateTime.of(2026, 7, 3, 21, 0))
        ));

        ActivityRecordService.ActivityCalendarResult result = service.getActivityCalendar(
            1L,
            10L,
            YearMonth.of(2026, 7)
        );

        assertThat(result.days()).hasSize(31);
        assertThat(result.days().get(0).date()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(result.days().get(0).mealRecorded()).isTrue();
        assertThat(result.days().get(0).exerciseRecorded()).isTrue();
        assertThat(result.days().get(1).mealRecorded()).isFalse();
        assertThat(result.days().get(1).exerciseRecorded()).isFalse();
        assertThat(result.days().get(2).mealRecorded()).isFalse();
        assertThat(result.days().get(2).exerciseRecorded()).isTrue();
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
    }

    @Test
    @DisplayName("기록 상세는 현재 그룹원의 댓글만 포함해 반환한다.")
    void getRecordDetail() {
        ActivityRecordService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(activityRecordRepository.findById(100L))
            .willReturn(Optional.of(mealRecord(100L, LocalDateTime.of(2026, 7, 1, 12, 30))));
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
        given(recordCommentRepository.findByRecordIdAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(100L))
            .willReturn(List.of(
                new RecordComment(200L, 100L, 2L, "좋다"),
                new RecordComment(201L, 100L, 3L, "탈퇴 회원 댓글")
            ));

        ActivityRecordService.RecordDetailResult result = service.getRecordDetail(1L, 100L);

        assertThat(result.recordId()).isEqualTo(100L);
        assertThat(result.nickname()).isEqualTo("민석");
        assertThat(result.profileImageUrl()).isEqualTo("https://storage.example.com/profiles/member-1.jpg");
        assertThat(result.imageUrl()).isEqualTo("https://storage.example.com/records/1/2026/07/meal.jpg");
        assertThat(result.comments()).hasSize(1);
        assertThat(result.comments().get(0).memberId()).isEqualTo(2L);
        assertThat(result.comments().get(0).nickname()).isEqualTo("친구");
    }

    @Test
    @DisplayName("기록 작성자가 그룹을 나갔으면 상세를 조회할 수 없다.")
    void getRecordDetailRecordWriterLeft() {
        ActivityRecordService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(activityRecordRepository.findById(100L))
            .willReturn(Optional.of(mealRecord(100L, LocalDateTime.of(2026, 7, 1, 12, 30))));
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

        assertThatThrownBy(() -> service.getRecordDetail(1L, 100L))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getStatus()).isEqualTo(ErrorStatus.RECORD_NOT_FOUND));
    }

    @Test
    @DisplayName("식사 기록을 생성하면 식사 필드만 저장한다.")
    void createMealRecord() {
        ActivityRecordService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(modyGroupRepository.findById(10L)).willReturn(Optional.of(group()));
        given(groupMemberRepository.existsByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            1L,
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(true);
        given(idGenerator.nextId()).willReturn(100L);
        given(activityRecordRepository.save(any(ActivityRecord.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        RecordCreateResult result = service.createRecord(1L, new RecordCreateCommand(
            10L,
            RecordType.MEAL,
            "records/1/2026/07/4111584723968.jpg",
            LocalTime.of(12, 30),
            "샐러드",
            null,
            null
        ));

        assertThat(result.recordId()).isEqualTo(100L);
        then(activityRecordRepository).should().save(activityRecordCaptor.capture());
        ActivityRecord savedRecord = activityRecordCaptor.getValue();
        assertThat(savedRecord.getMemberId()).isEqualTo(1L);
        assertThat(savedRecord.getGroupId()).isEqualTo(10L);
        assertThat(savedRecord.getRecordType()).isEqualTo(RecordType.MEAL);
        assertThat(savedRecord.getMealTime()).isEqualTo(LocalTime.of(12, 30));
        assertThat(savedRecord.getMenu()).isEqualTo("샐러드");
        assertThat(savedRecord.getExerciseDurationMinutes()).isNull();
        assertThat(savedRecord.getExerciseName()).isNull();
        assertThat(savedRecord.getImageKey()).isEqualTo("records/1/2026/07/4111584723968.jpg");
        assertThat(savedRecord.getUploadedAt()).isNotNull();
    }

    @Test
    @DisplayName("운동 기록을 생성하면 운동 필드만 저장한다.")
    void createExerciseRecord() {
        ActivityRecordService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(idGenerator.nextId()).willReturn(101L);
        given(activityRecordRepository.save(any(ActivityRecord.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        RecordCreateResult result = service.createRecord(1L, new RecordCreateCommand(
            null,
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
            100L,
            new ActivityRecordService.CommentCreateCommand("  좋다  ")
        );

        assertThat(result.commentId()).isEqualTo(200L);
        then(recordCommentRepository).should().save(recordCommentCaptor.capture());
        RecordComment savedComment = recordCommentCaptor.getValue();
        assertThat(savedComment.getRecordId()).isEqualTo(100L);
        assertThat(savedComment.getMemberId()).isEqualTo(1L);
        assertThat(savedComment.getContent()).isEqualTo("좋다");
    }

    @Test
    @DisplayName("없는 기록에는 댓글을 작성할 수 없다.")
    void createCommentRecordNotFound() {
        ActivityRecordService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(activityRecordRepository.findById(100L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.createComment(
            1L,
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
    @DisplayName("그룹이 없으면 그룹 기록을 생성할 수 없다.")
    void createRecordGroupNotFound() {
        ActivityRecordService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(modyGroupRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.createRecord(1L, mealCommand()))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getStatus()).isEqualTo(ErrorStatus.GROUP_NOT_FOUND));
    }

    @Test
    @DisplayName("참여하지 않은 그룹에는 기록을 생성할 수 없다.")
    void createRecordGroupMemberNotFound() {
        ActivityRecordService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(modyGroupRepository.findById(10L)).willReturn(Optional.of(group()));
        given(groupMemberRepository.existsByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            1L,
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(false);

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
            recordCommentRepository,
            new UploadProperties()
        );
    }

    private RecordCreateCommand mealCommand() {
        return new RecordCreateCommand(
            10L,
            RecordType.MEAL,
            "records/1/2026/07/4111584723968.jpg",
            LocalTime.of(12, 30),
            "샐러드",
            null,
            null
        );
    }

    private Member member() {
        return new Member(1L, "민석", LocalDate.of(2000, 1, 1), BigDecimal.valueOf(68.0));
    }

    private ModyGroup group() {
        return new ModyGroup(10L, "ABC123", "모디 그룹");
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
