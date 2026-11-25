package cmc.mody.admin.application;

import static org.assertj.core.api.Assertions.assertThat;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.report.domain.RecordReport;
import cmc.mody.report.infrastructure.repository.RecordReportRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** 콘솔과 Slack에서 같은 신고를 동시에 처리해도 한쪽만 처리된다(LLD-0040). 실제 커밋이 필요해 트랜잭션 롤백을 쓰지 않는다. */
@SpringBootTest
class AdminReportConcurrencyTest {
    private static final long RECORD_ID = 9_810_100L;
    @Autowired private AdminReportService reports;
    @Autowired private RecordReportRepository repository;

    @AfterEach
    void cleanup() {
        repository.deleteAllById(List.of(9_810_001L, 9_810_002L));
    }

    @Test
    void concurrentDismissResolvesOnlyOnce() throws Exception {
        repository.save(new RecordReport(9_810_001L, 1L, RECORD_ID));
        repository.save(new RecordReport(9_810_002L, 2L, RECORD_ID));

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<String>> results = new ArrayList<>();
        for (String actor : List.of("console@example.com", "slack@example.com")) {
            results.add(pool.submit(() -> {
                start.await();
                try {
                    reports.dismiss(RECORD_ID, actor);
                    return "resolved";
                } catch (GeneralException exception) {
                    return exception.getStatus() == ErrorStatus.NOT_FOUND ? "already" : exception.toString();
                }
            }));
        }
        start.countDown();
        List<String> outcomes = new ArrayList<>();
        for (Future<String> result : results) {
            outcomes.add(result.get(20, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertThat(outcomes).containsExactlyInAnyOrder("resolved", "already");
        List<String> resolvers = repository.findAllById(List.of(9_810_001L, 9_810_002L)).stream()
            .map(RecordReport::getResolvedBy).distinct().toList();
        assertThat(resolvers).hasSize(1);
    }
}
