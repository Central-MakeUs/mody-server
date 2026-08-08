package cmc.mody.record.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import cmc.mody.common.upload.ImageUrlResolver;
import cmc.mody.grouping.domain.ModyGroup;
import cmc.mody.grouping.infrastructure.repository.GroupMemberRepository;
import cmc.mody.grouping.infrastructure.repository.ModyGroupRepository;
import cmc.mody.record.application.AdminRecordService.AdminRecordUpdateCommand;
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
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminRecordServiceTest {
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
    private ImageUrlResolver imageUrlResolver;

    @Test
    @DisplayName("운영자가 그룹에 노출된 식사 기록을 수정한다.")
    void updateRecord() {
        AdminRecordService service = service();
        ActivityRecord record = ActivityRecord.exercise(
            20L, 30L, 10L, 30, "러닝", "records/20.jpg", LocalDateTime.of(2026, 8, 8, 12, 0)
        );
        givenGroupAndRecord(record);

        service.updateRecord(10L, 20L, new AdminRecordUpdateCommand(
            RecordType.MEAL, LocalTime.of(12, 30), "비빔밥", null, null
        ));

        assertThat(record.getRecordType()).isEqualTo(RecordType.MEAL);
        assertThat(record.getMealTime()).isEqualTo(LocalTime.of(12, 30));
        assertThat(record.getMenu()).isEqualTo("비빔밥");
        assertThat(record.getExerciseDurationMinutes()).isNull();
        assertThat(record.getExerciseName()).isNull();
    }

    @Test
    @DisplayName("기록을 삭제하면 모든 그룹 연결과 댓글도 함께 삭제한다.")
    void deleteRecordGlobally() {
        AdminRecordService service = service();
        ActivityRecord record = ActivityRecord.meal(
            20L, 30L, 10L, LocalTime.NOON, "샐러드", "records/20.jpg", LocalDateTime.of(2026, 8, 8, 12, 0)
        );
        ActivityRecordGroup firstGroup = new ActivityRecordGroup(1L, 20L, 10L, 30L, LocalDateTime.now());
        ActivityRecordGroup secondGroup = new ActivityRecordGroup(2L, 20L, 11L, 30L, LocalDateTime.now());
        RecordComment comment = new RecordComment(3L, 20L, 10L, 31L, "좋아요");
        givenGroupAndRecord(record);
        given(activityRecordGroupRepository.findByRecordIdAndDeletedAtIsNull(20L))
            .willReturn(List.of(firstGroup, secondGroup));
        given(recordCommentRepository.findByRecordIdInAndDeletedAtIsNull(List.of(20L))).willReturn(List.of(comment));

        service.deleteRecord(10L, 20L);

        assertThat(record.isActive()).isFalse();
        assertThat(firstGroup.isActive()).isFalse();
        assertThat(secondGroup.isActive()).isFalse();
        assertThat(comment.isActive()).isFalse();
    }

    private void givenGroupAndRecord(ActivityRecord record) {
        given(modyGroupRepository.findById(10L)).willReturn(Optional.of(new ModyGroup(10L, "ABCD2345", "모디")));
        given(activityRecordRepository.findById(20L)).willReturn(Optional.of(record));
        given(activityRecordGroupRepository.findByRecordIdAndGroupIdAndDeletedAtIsNull(20L, 10L))
            .willReturn(Optional.of(new ActivityRecordGroup(1L, 20L, 10L, 30L, LocalDateTime.now())));
    }

    private AdminRecordService service() {
        return new AdminRecordService(
            modyGroupRepository,
            groupMemberRepository,
            activityRecordRepository,
            activityRecordGroupRepository,
            recordCommentRepository,
            imageUrlResolver
        );
    }
}
