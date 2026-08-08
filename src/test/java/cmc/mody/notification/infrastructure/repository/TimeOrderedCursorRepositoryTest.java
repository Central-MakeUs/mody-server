package cmc.mody.notification.infrastructure.repository;

import static org.assertj.core.api.Assertions.assertThat;

import cmc.mody.grouping.domain.GroupMember;
import cmc.mody.grouping.domain.GroupMemberStatus;
import cmc.mody.grouping.infrastructure.repository.GroupMemberRepository;
import cmc.mody.notification.domain.Notification;
import cmc.mody.notification.domain.NotificationType;
import cmc.mody.record.domain.ActivityRecord;
import cmc.mody.record.domain.ActivityRecordGroup;
import cmc.mody.record.domain.RecordComment;
import cmc.mody.record.infrastructure.repository.ActivityRecordGroupRepository;
import cmc.mody.record.infrastructure.repository.ActivityRecordRepository;
import cmc.mody.record.infrastructure.repository.RecordCommentRepository;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TimeOrderedCursorRepositoryTest {
    private static final LocalDateTime NINE_DAYS_AGO = LocalDateTime.of(2026, 7, 30, 12, 0);
    private static final LocalDateTime RECENT = LocalDateTime.of(2026, 8, 8, 11, 34);

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ActivityRecordRepository activityRecordRepository;

    @Autowired
    private ActivityRecordGroupRepository activityRecordGroupRepository;

    @Autowired
    private RecordCommentRepository recordCommentRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Test
    void notificationsUseCreatedAtBeforeIdForCursorPaging() {
        persistNotification(300L, NINE_DAYS_AGO);
        persistNotification(100L, NINE_DAYS_AGO);
        persistNotification(10L, RECENT);

        assertThat(notificationRepository.findByReceiverMemberIdByCursor(1L, null, PageRequest.of(0, 2)))
            .extracting(Notification::getId)
            .containsExactly(10L, 300L);
        assertThat(notificationRepository.findByReceiverMemberIdByCursor(1L, 10L, PageRequest.of(0, 2)))
            .extracting(Notification::getId)
            .containsExactly(300L, 100L);
    }

    @Test
    void recordsUseUploadedAtBeforeIdForCursorPaging() {
        persistJoinedMember();
        persistRecord(300L, NINE_DAYS_AGO);
        persistRecord(100L, NINE_DAYS_AGO);
        persistRecord(10L, RECENT);

        assertThat(findRecords(null)).extracting(ActivityRecord::getId).containsExactly(10L, 300L);
        assertThat(findRecords(10L)).extracting(ActivityRecord::getId).containsExactly(300L, 100L);
    }

    @Test
    void commentsUseCreatedAtBeforeIdForCursorPaging() {
        persistJoinedMember();
        persistComment(100L, NINE_DAYS_AGO);
        persistComment(300L, NINE_DAYS_AGO);
        persistComment(10L, RECENT);

        assertThat(findComments(null)).extracting(RecordComment::getId).containsExactly(100L, 300L);
        assertThat(findComments(100L)).extracting(RecordComment::getId).containsExactly(300L, 10L);
    }

    private void persistNotification(Long id, LocalDateTime createdAt) {
        entityManager.persist(new Notification(id, 1L, NotificationType.COMMENT, "댓글", "새 댓글"));
        entityManager.flush();
        entityManager.getEntityManager().createNativeQuery("update notification set created_at = :createdAt where id = :id")
            .setParameter("createdAt", createdAt)
            .setParameter("id", id)
            .executeUpdate();
        entityManager.clear();
    }

    private void persistJoinedMember() {
        if (groupMemberRepository.existsByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(1L, 1L, GroupMemberStatus.JOINED)) {
            return;
        }
        entityManager.persist(new GroupMember(1L, 1L, 1L, LocalDateTime.now()));
        entityManager.flush();
    }

    private void persistRecord(Long id, LocalDateTime uploadedAt) {
        entityManager.persist(ActivityRecord.meal(id, 1L, null, LocalTime.NOON, "샐러드", "records/" + id, uploadedAt));
        entityManager.persist(new ActivityRecordGroup(id + 1000L, id, 1L, 1L, uploadedAt));
        entityManager.flush();
        entityManager.clear();
    }

    private List<ActivityRecord> findRecords(Long cursor) {
        return activityRecordRepository.findActiveGroupRecordsByCursor(
            1L,
            NINE_DAYS_AGO.toLocalDate().atStartOfDay(),
            RECENT.toLocalDate().plusDays(1).atStartOfDay(),
            cursor,
            GroupMemberStatus.JOINED,
            PageRequest.of(0, 2)
        );
    }

    private void persistComment(Long id, LocalDateTime createdAt) {
        entityManager.persist(new RecordComment(id, 1L, 1L, 1L, "댓글"));
        entityManager.flush();
        entityManager.getEntityManager().createNativeQuery("update record_comment set created_at = :createdAt where id = :id")
            .setParameter("createdAt", createdAt)
            .setParameter("id", id)
            .executeUpdate();
        entityManager.clear();
    }

    private List<RecordComment> findComments(Long cursor) {
        return recordCommentRepository.findActiveCommentsByCursor(
            1L,
            1L,
            1L,
            cursor,
            GroupMemberStatus.JOINED,
            PageRequest.of(0, 2)
        );
    }
}
