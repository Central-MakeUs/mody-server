package cmc.mody.report.application;

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
import cmc.mody.record.domain.ActivityRecord;
import cmc.mody.record.domain.ActivityRecordGroup;
import cmc.mody.record.infrastructure.repository.ActivityRecordGroupRepository;
import cmc.mody.record.infrastructure.repository.ActivityRecordRepository;
import cmc.mody.report.domain.RecordReport;
import cmc.mody.report.infrastructure.repository.RecordReportRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
class RecordReportServiceTest {
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
    private RecordReportRepository recordReportRepository;

    @Captor
    private ArgumentCaptor<RecordReport> recordReportCaptor;

    @Test
    @DisplayName("피드 기록을 신고하면 신고 내역을 저장한다.")
    void reportRecord() {
        RecordReportService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(modyGroupRepository.findById(10L)).willReturn(Optional.of(group()));
        given(groupMemberRepository.existsByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            1L,
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(true);
        given(activityRecordRepository.findById(100L)).willReturn(Optional.of(record()));
        given(activityRecordGroupRepository.findByRecordIdAndGroupIdAndDeletedAtIsNull(100L, 10L))
            .willReturn(Optional.of(new ActivityRecordGroup(1000L, 100L, 10L, 2L, LocalDateTime.now())));
        given(groupMemberRepository.existsByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            2L,
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(true);
        given(recordReportRepository.findByReporterMemberIdAndRecordIdAndDeletedAtIsNull(1L, 100L))
            .willReturn(Optional.empty());
        given(idGenerator.nextId()).willReturn(300L);
        given(recordReportRepository.save(any(RecordReport.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        RecordReportService.RecordReportResult result = service.reportRecord(1L, 10L, 100L);

        assertThat(result.reportId()).isEqualTo(300L);
        assertThat(result.recordId()).isEqualTo(100L);
        then(recordReportRepository).should().save(recordReportCaptor.capture());
        assertThat(recordReportCaptor.getValue().getReporterMemberId()).isEqualTo(1L);
        assertThat(recordReportCaptor.getValue().getRecordId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("이미 신고한 피드 기록은 기존 신고 내역을 반환한다.")
    void reportRecordAlreadyReported() {
        RecordReportService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(modyGroupRepository.findById(10L)).willReturn(Optional.of(group()));
        given(groupMemberRepository.existsByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            1L,
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(true);
        given(activityRecordRepository.findById(100L)).willReturn(Optional.of(record()));
        given(activityRecordGroupRepository.findByRecordIdAndGroupIdAndDeletedAtIsNull(100L, 10L))
            .willReturn(Optional.of(new ActivityRecordGroup(1000L, 100L, 10L, 2L, LocalDateTime.now())));
        given(groupMemberRepository.existsByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            2L,
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(true);
        given(recordReportRepository.findByReporterMemberIdAndRecordIdAndDeletedAtIsNull(1L, 100L))
            .willReturn(Optional.of(new RecordReport(300L, 1L, 100L)));

        RecordReportService.RecordReportResult result = service.reportRecord(1L, 10L, 100L);

        assertThat(result.reportId()).isEqualTo(300L);
        assertThat(result.recordId()).isEqualTo(100L);
        then(recordReportRepository).should(org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("그룹에서 볼 수 없는 기록은 신고할 수 없다.")
    void reportRecordNotFound() {
        RecordReportService service = service();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(modyGroupRepository.findById(10L)).willReturn(Optional.of(group()));
        given(groupMemberRepository.existsByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            1L,
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(true);
        given(activityRecordRepository.findById(100L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.reportRecord(1L, 10L, 100L))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getStatus()).isEqualTo(ErrorStatus.RECORD_NOT_FOUND));
    }

    private RecordReportService service() {
        return new RecordReportService(
            idGenerator,
            memberRepository,
            modyGroupRepository,
            groupMemberRepository,
            activityRecordRepository,
            activityRecordGroupRepository,
            recordReportRepository
        );
    }

    private Member member() {
        return new Member(1L, "민석", LocalDate.of(2000, 1, 1), BigDecimal.valueOf(68.0));
    }

    private ModyGroup group() {
        return new ModyGroup(10L, "ABCD2345", "모디 그룹");
    }

    private ActivityRecord record() {
        return ActivityRecord.meal(
            100L,
            2L,
            null,
            LocalTime.of(12, 30),
            "샐러드",
            "records/2/2026/07/meal.jpg",
            LocalDateTime.of(2026, 7, 1, 12, 30)
        );
    }
}
