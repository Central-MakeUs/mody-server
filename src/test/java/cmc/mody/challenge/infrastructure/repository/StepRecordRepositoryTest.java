package cmc.mody.challenge.infrastructure.repository;

import static org.assertj.core.api.Assertions.assertThat;

import cmc.mody.challenge.domain.GroupChallenge;
import cmc.mody.challenge.domain.GroupChallengeStatus;
import cmc.mody.challenge.domain.StepRecord;
import cmc.mody.challenge.domain.StepSource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class StepRecordRepositoryTest {
    private static final long GROUP_CHALLENGE_ID = 100L;
    private static final long MEMBER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 11);

    private final AtomicLong ids = new AtomicLong(1_000L);

    @Autowired
    private StepRecordRepository stepRecordRepository;

    @Autowired
    private GroupChallengeRepository groupChallengeRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void tearDown() {
        stepRecordRepository.deleteAllInBatch();
        groupChallengeRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("기록이 없으면 새로 저장한다.")
    void insertWhenAbsent() {
        upsert(3_000);

        assertThat(activeStepCount()).isEqualTo(3_000);
    }

    @Test
    @DisplayName("같은 날짜에 더 큰 걸음 수가 오면 갱신하고, 더 작은 값이 오면 유지한다.")
    void keepGreaterStepCount() {
        upsert(3_000);
        upsert(8_200);
        upsert(1_000);

        assertThat(activeStepCount()).isEqualTo(8_200);
        assertThat(stepRecordRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("그룹을 나가며 삭제된 기록은 되살리고 요청 값으로 덮어쓴다.")
    void restoreSoftDeletedRecord() {
        StepRecord deleted = new StepRecord(ids.incrementAndGet(), GROUP_CHALLENGE_ID, MEMBER_ID, TODAY, 9_000, StepSource.API);
        deleted.delete();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> stepRecordRepository.save(deleted));

        upsert(500);

        assertThat(activeStepCount()).isEqualTo(500);
        assertThat(stepRecordRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 날짜 요청이 동시에 들어와도 실패 없이 한 건만 저장하고 가장 큰 값을 남긴다.")
    void concurrentUpsertsDoNotFail() throws Exception {
        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Callable<Void>> tasks = new java.util.ArrayList<>();
            for (int i = 1; i <= threads; i++) {
                int stepCount = i * 1_000;
                tasks.add(() -> {
                    start.await();
                    upsert(stepCount);
                    return null;
                });
            }
            List<Future<Void>> futures = tasks.stream().map(executor::submit).toList();
            start.countDown();
            for (Future<Void> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(stepRecordRepository.count()).isEqualTo(1);
        assertThat(activeStepCount()).isEqualTo(threads * 1_000);
    }

    @Test
    @DisplayName("upsert 뒤에도 같은 트랜잭션에서 먼저 읽은 엔티티의 변경이 반영된다.")
    void keepPersistenceContextAfterUpsert() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> groupChallengeRepository.save(
            new GroupChallenge(GROUP_CHALLENGE_ID, 10L, 1L, TODAY.minusDays(1), TODAY.plusDays(30))));

        // 서비스와 같은 순서: 그룹 챌린지를 읽고 → 걸음 수를 upsert하고 → 완료 처리한다.
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            GroupChallenge groupChallenge = groupChallengeRepository.findById(GROUP_CHALLENGE_ID).orElseThrow();
            stepRecordRepository.upsertDailyStepCount(
                ids.incrementAndGet(), GROUP_CHALLENGE_ID, MEMBER_ID, TODAY, 15_000,
                StepSource.API.name(), LocalDateTime.now());
            groupChallenge.complete(LocalDateTime.now());
        });

        assertThat(groupChallengeRepository.findById(GROUP_CHALLENGE_ID).orElseThrow().getGroupChallengeStatus())
            .isEqualTo(GroupChallengeStatus.COMPLETED);
    }

    private void upsert(int stepCount) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
            stepRecordRepository.upsertDailyStepCount(
                ids.incrementAndGet(),
                GROUP_CHALLENGE_ID,
                MEMBER_ID,
                TODAY,
                stepCount,
                StepSource.API.name(),
                LocalDateTime.now()
            ));
    }

    private int activeStepCount() {
        return stepRecordRepository
            .findByGroupChallengeIdAndMemberIdAndRecordedOnAndDeletedAtIsNull(GROUP_CHALLENGE_ID, MEMBER_ID, TODAY)
            .orElseThrow()
            .getStepCount();
    }
}
