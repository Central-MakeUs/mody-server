package cmc.mody.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.member.domain.Member;
import cmc.mody.record.domain.ActivityRecord;
import cmc.mody.record.domain.ActivityRecordGroup;
import cmc.mody.record.domain.RecordComment;
import cmc.mody.record.domain.RecordType;
import cmc.mody.report.domain.RecordReport;
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
class AdminReportServiceTest {
    private static final LocalDateTime AT = LocalDateTime.of(2026, 10, 11, 12, 0);
    @Autowired private EntityManager em;
    @Autowired private AdminReportService reports;

    @BeforeEach
    void setup() {
        em.persist(new Member(10L, "작성자", null, null));
        em.persist(new Member(11L, "신고자1", null, null));
        em.persist(new Member(12L, "신고자2", null, null));
        em.persist(record(100L));
        em.persist(record(101L));
        em.persist(new ActivityRecordGroup(200L, 100L, 30L, 10L, AT));
        em.persist(new RecordComment(300L, 100L, 30L, 11L, "댓글"));
        em.persist(new RecordReport(400L, 11L, 100L));
        em.persist(new RecordReport(401L, 12L, 100L));
        em.persist(new RecordReport(402L, 11L, 101L));
        em.flush();
    }

    @Test
    void listGroupsReportsByRecordWithReporters() {
        var page = reports.list("PENDING", null, 20);
        assertThat(page.items()).extracting(AdminReportService.Item::recordId).containsExactlyInAnyOrder("100", "101");
        var target = page.items().stream().filter(item -> item.recordId().equals("100")).findFirst().orElseThrow();
        assertThat(target.reportCount()).isEqualTo(2);
        assertThat(target.pendingCount()).isEqualTo(2);
        assertThat(target.authorNickname()).isEqualTo("작성자");
        assertThat(target.reporters()).extracting(AdminReportService.Reporter::nickname)
            .containsExactlyInAnyOrder("신고자1", "신고자2");
        assertThat(target.recordActive()).isTrue();

        var first = reports.list("ALL", null, 1);
        assertThat(first.hasMore()).isTrue();
        assertThat(reports.list("ALL", first.nextCursor(), 1).items()).hasSize(1);
        assertThat(reports.summary().pendingRecords()).isEqualTo(2);
        assertThat(reports.summary().pendingReports()).isEqualTo(3);
    }

    @Test
    void hideDeletesRecordWithSharesAndResolvesReports() {
        var result = reports.hide(100L, "ops@example.com");
        em.flush();
        em.clear();
        assertThat(result.resolvedReports()).isEqualTo(2);
        assertThat(result.recordDeleted()).isTrue();
        assertThat(em.find(ActivityRecord.class, 100L).isActive()).isFalse();
        assertThat(em.find(ActivityRecordGroup.class, 200L).getDeletedAt()).isNotNull();
        assertThat(em.find(RecordComment.class, 300L).getDeletedAt()).isNotNull();
        RecordReport report = em.find(RecordReport.class, 400L);
        assertThat(report.getResolution()).isEqualTo(RecordReportResolution.HIDDEN);
        assertThat(report.getResolvedBy()).isEqualTo("ops@example.com");

        assertThat(reports.list("PENDING", null, 20).items()).extracting(AdminReportService.Item::recordId)
            .containsExactly("101");
        var resolved = reports.list("RESOLVED", null, 20).items().getFirst();
        assertThat(resolved.resolution()).isEqualTo("HIDDEN");
        assertThat(resolved.recordActive()).isFalse();
        assertThatThrownBy(() -> reports.hide(100L, "ops@example.com")).isInstanceOf(GeneralException.class);
    }

    @Test
    void dismissKeepsRecordAndNewReportAfterwardIsPendingAgain() {
        reports.dismiss(101L, "machine");
        em.flush();
        em.clear();
        assertThat(em.find(ActivityRecord.class, 101L).isActive()).isTrue();
        assertThat(em.find(RecordReport.class, 402L).getResolution()).isEqualTo(RecordReportResolution.DISMISSED);
        assertThat(reports.list("PENDING", null, 20).items()).extracting(AdminReportService.Item::recordId)
            .containsExactly("100");
        assertThatThrownBy(() -> reports.list("WRONG", null, 20)).isInstanceOf(GeneralException.class);
    }

    private ActivityRecord record(long id) {
        return new ActivityRecord(id, 10L, 30L, RecordType.MEAL, LocalTime.NOON, "샐러드", null, null,
            "records/" + id + ".jpg", AT);
    }
}
