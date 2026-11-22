package cmc.mody.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.grouping.domain.ModyGroup;
import cmc.mody.member.domain.Member;
import cmc.mody.record.domain.ActivityRecord;
import cmc.mody.record.domain.RecordComment;
import cmc.mody.record.domain.RecordType;
import cmc.mody.report.domain.CommentReport;
import cmc.mody.report.domain.RecordReportResolution;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AdminCommentReportServiceTest {
    private static final LocalDateTime AT = LocalDateTime.of(2026, 11, 21, 12, 0);
    @Autowired private EntityManager em;
    @Autowired private AdminCommentReportService reports;

    @BeforeEach
    void setup() {
        em.persist(new Member(10L, "작성자", null, null));
        em.persist(new Member(11L, "신고자1", null, null));
        em.persist(new Member(12L, "신고자2", null, null));
        em.persist(new ModyGroup(30L, "AAAABBBB", "아침 산책"));
        em.persist(new ActivityRecord(100L, 10L, 30L, RecordType.MEAL, LocalTime.NOON, "샐러드", null, null,
            "records/100.jpg", AT));
        em.persist(new RecordComment(300L, 100L, 30L, 10L, "나쁜 댓글"));
        em.persist(new RecordComment(301L, 100L, 30L, 10L, "", "clap"));
        em.persist(new CommentReport(400L, 11L, 300L, 100L, 30L));
        em.persist(new CommentReport(401L, 12L, 300L, 100L, 30L));
        em.persist(new CommentReport(402L, 11L, 301L, 100L, 30L));
        em.flush();
    }

    @Test
    void listGroupsReportsByCommentWithContext() {
        var page = reports.list("PENDING", null, 20);
        assertThat(page.items()).extracting(AdminCommentReportService.Item::commentId)
            .containsExactlyInAnyOrder("300", "301");
        var target = page.items().stream().filter(item -> item.commentId().equals("300")).findFirst().orElseThrow();
        assertThat(target.reportCount()).isEqualTo(2);
        assertThat(target.content()).isEqualTo("나쁜 댓글");
        assertThat(target.recordTitle()).isEqualTo("샐러드");
        assertThat(target.groupName()).isEqualTo("아침 산책");
        assertThat(target.authorNickname()).isEqualTo("작성자");
        assertThat(target.commentActive()).isTrue();
        assertThat(target.reporters()).extracting(AdminReportService.Reporter::nickname)
            .containsExactlyInAnyOrder("신고자1", "신고자2");

        var first = reports.list("ALL", null, 1);
        assertThat(first.hasMore()).isTrue();
        assertThat(reports.list("ALL", first.nextCursor(), 1).items()).hasSize(1);
        assertThat(reports.summary().pendingComments()).isEqualTo(2);
        assertThat(reports.summary().pendingReports()).isEqualTo(3);
    }

    @Test
    void hideDeletesOnlyTheCommentAndResolvesReports() {
        var result = reports.hide(300L, "ops@example.com");
        em.flush();
        em.clear();
        assertThat(result.resolvedReports()).isEqualTo(2);
        assertThat(result.commentDeleted()).isTrue();
        assertThat(em.find(RecordComment.class, 300L).getDeletedAt()).isNotNull();
        assertThat(em.find(RecordComment.class, 301L).getDeletedAt()).isNull();
        assertThat(em.find(ActivityRecord.class, 100L).isActive()).isTrue();
        CommentReport report = em.find(CommentReport.class, 400L);
        assertThat(report.getResolution()).isEqualTo(RecordReportResolution.HIDDEN);
        assertThat(report.getResolvedBy()).isEqualTo("ops@example.com");

        assertThat(reports.list("PENDING", null, 20).items()).extracting(AdminCommentReportService.Item::commentId)
            .containsExactly("301");
        var resolved = reports.list("RESOLVED", null, 20).items().getFirst();
        assertThat(resolved.resolution()).isEqualTo("HIDDEN");
        assertThat(resolved.commentActive()).isFalse();
        assertThat(reports.summary().hiddenLast7Days()).isEqualTo(2);
        assertThatThrownBy(() -> reports.hide(300L, "ops@example.com")).isInstanceOf(GeneralException.class);
    }

    @Test
    void dismissKeepsTheComment() {
        var result = reports.dismiss(301L, "ops@example.com");
        em.flush();
        em.clear();
        assertThat(result.resolvedReports()).isEqualTo(1);
        assertThat(em.find(RecordComment.class, 301L).getDeletedAt()).isNull();
        assertThat(em.find(CommentReport.class, 402L).getResolution()).isEqualTo(RecordReportResolution.DISMISSED);
        assertThatThrownBy(() -> reports.list("WRONG", null, 20)).isInstanceOf(GeneralException.class);
        assertThatThrownBy(() -> reports.dismiss(999L, "ops@example.com")).isInstanceOf(GeneralException.class);
    }
}
