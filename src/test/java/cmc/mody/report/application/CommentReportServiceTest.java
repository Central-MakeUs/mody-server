package cmc.mody.report.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

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
import cmc.mody.record.domain.RecordComment;
import cmc.mody.record.infrastructure.repository.ActivityRecordGroupRepository;
import cmc.mody.record.infrastructure.repository.ActivityRecordRepository;
import cmc.mody.record.infrastructure.repository.RecordCommentRepository;
import cmc.mody.report.domain.CommentReport;
import cmc.mody.report.infrastructure.repository.CommentReportRepository;
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
class CommentReportServiceTest {
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
    private CommentReportRepository commentReportRepository;

    @Captor
    private ArgumentCaptor<CommentReport> commentReportCaptor;

    @Test
    @DisplayName("댓글을 신고하면 신고 내역을 저장한다.")
    void reportComment() {
        givenVisibleRecord();
        given(recordCommentRepository.findById(200L)).willReturn(Optional.of(comment(100L, 10L)));
        givenJoined(3L, true);
        given(commentReportRepository.findByReporterMemberIdAndCommentIdAndDeletedAtIsNull(1L, 200L))
            .willReturn(Optional.empty());
        given(idGenerator.nextId()).willReturn(300L);
        given(commentReportRepository.save(any(CommentReport.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        CommentReportService.CommentReportResult result = service().reportComment(1L, 10L, 100L, 200L);

        assertThat(result.reportId()).isEqualTo(300L);
        assertThat(result.commentId()).isEqualTo(200L);
        then(commentReportRepository).should().save(commentReportCaptor.capture());
        CommentReport saved = commentReportCaptor.getValue();
        assertThat(saved.getReporterMemberId()).isEqualTo(1L);
        assertThat(saved.getCommentId()).isEqualTo(200L);
        assertThat(saved.getRecordId()).isEqualTo(100L);
        assertThat(saved.getGroupId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("이미 신고한 댓글은 기존 신고 내역을 반환한다.")
    void reportCommentAlreadyReported() {
        givenVisibleRecord();
        given(recordCommentRepository.findById(200L)).willReturn(Optional.of(comment(100L, 10L)));
        givenJoined(3L, true);
        given(commentReportRepository.findByReporterMemberIdAndCommentIdAndDeletedAtIsNull(1L, 200L))
            .willReturn(Optional.of(new CommentReport(300L, 1L, 200L, 100L, 10L)));

        CommentReportService.CommentReportResult result = service().reportComment(1L, 10L, 100L, 200L);

        assertThat(result.reportId()).isEqualTo(300L);
        then(commentReportRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("다른 기록의 댓글은 신고할 수 없다.")
    void reportCommentOfOtherRecord() {
        givenVisibleRecord();
        given(recordCommentRepository.findById(200L)).willReturn(Optional.of(comment(999L, 10L)));

        assertCommentNotFound();
    }

    @Test
    @DisplayName("작성자가 그룹을 나가 목록에서 보이지 않는 댓글은 신고할 수 없다.")
    void reportCommentOfLeftMember() {
        givenVisibleRecord();
        given(recordCommentRepository.findById(200L)).willReturn(Optional.of(comment(100L, 10L)));
        givenJoined(3L, false);

        assertCommentNotFound();
    }

    @Test
    @DisplayName("삭제된 댓글은 신고할 수 없다.")
    void reportDeletedComment() {
        givenVisibleRecord();
        RecordComment comment = comment(100L, 10L);
        comment.delete();
        given(recordCommentRepository.findById(200L)).willReturn(Optional.of(comment));

        assertCommentNotFound();
    }

    private void assertCommentNotFound() {
        assertThatThrownBy(() -> service().reportComment(1L, 10L, 100L, 200L))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getStatus()).isEqualTo(ErrorStatus.RECORD_COMMENT_NOT_FOUND));
        then(commentReportRepository).should(never()).save(any());
    }

    private void givenVisibleRecord() {
        given(memberRepository.findById(1L)).willReturn(Optional.of(member()));
        given(modyGroupRepository.findById(10L)).willReturn(Optional.of(group()));
        givenJoined(1L, true);
        given(activityRecordRepository.findById(100L)).willReturn(Optional.of(record()));
        given(activityRecordGroupRepository.findByRecordIdAndGroupIdAndDeletedAtIsNull(100L, 10L))
            .willReturn(Optional.of(new ActivityRecordGroup(1000L, 100L, 10L, 2L, LocalDateTime.now())));
        givenJoined(2L, true);
    }

    private void givenJoined(Long memberId, boolean joined) {
        given(groupMemberRepository.existsByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            memberId,
            10L,
            GroupMemberStatus.JOINED
        )).willReturn(joined);
    }

    private CommentReportService service() {
        return new CommentReportService(
            idGenerator,
            memberRepository,
            modyGroupRepository,
            groupMemberRepository,
            activityRecordRepository,
            activityRecordGroupRepository,
            recordCommentRepository,
            commentReportRepository
        );
    }

    private RecordComment comment(Long recordId, Long groupId) {
        return new RecordComment(200L, recordId, groupId, 3L, "별로다");
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
