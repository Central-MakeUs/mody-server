package cmc.mody.record.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.id.IdGenerator;
import cmc.mody.grouping.domain.GroupMemberStatus;
import cmc.mody.grouping.domain.ModyGroup;
import cmc.mody.grouping.infrastructure.repository.GroupMemberRepository;
import cmc.mody.grouping.infrastructure.repository.ModyGroupRepository;
import cmc.mody.member.domain.Member;
import cmc.mody.member.infrastructure.repository.MemberRepository;
import cmc.mody.record.application.ActivityRecordService.RecordCreateCommand;
import cmc.mody.record.application.ActivityRecordService.RecordCreateResult;
import cmc.mody.record.domain.ActivityRecord;
import cmc.mody.record.domain.RecordType;
import cmc.mody.record.infrastructure.repository.ActivityRecordRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
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

    @Captor
    private ArgumentCaptor<ActivityRecord> activityRecordCaptor;

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
            activityRecordRepository
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
}
