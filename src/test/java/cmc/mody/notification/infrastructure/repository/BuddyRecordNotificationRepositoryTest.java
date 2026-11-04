package cmc.mody.notification.infrastructure.repository;

import static org.assertj.core.api.Assertions.assertThat;

import cmc.mody.notification.domain.NotificationType;
import cmc.mody.record.domain.ActivityRecord;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BuddyRecordNotificationRepositoryTest {
    private static final Long WRITER_ID = 2L;
    private static final List<NotificationType> BUDDY_RECORD_TYPES = List.of(
        NotificationType.BUDDY_MEAL_RECORDED,
        NotificationType.BUDDY_EXERCISE_RECORDED
    );

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("같은 작성자의 다른 기록 알림이 기준 시각 이후에 있으면 수신자를 반환한다.")
    void findRecentBuddyRecordReceiverMemberIds() {
        persistRecord(10L, WRITER_ID);
        persistRecord(11L, 3L);
        insertBuddyNotification(100L, 1L, 10L, NotificationType.BUDDY_MEAL_RECORDED);
        insertBuddyNotification(101L, 4L, 11L, NotificationType.BUDDY_EXERCISE_RECORDED);
        insertBuddyNotification(102L, 5L, 10L, NotificationType.BUDDY_EXERCISE_RECORDED);
        setCreatedAt(102L, LocalDateTime.now().minusHours(1));

        List<Long> receiverIds = notificationRepository.findRecentBuddyRecordReceiverMemberIds(
            List.of(1L, 4L, 5L, 6L),
            WRITER_ID,
            20L,
            LocalDateTime.now().minusMinutes(10)
        );

        assertThat(receiverIds).containsExactly(1L);
    }

    @Test
    @DisplayName("같은 기록의 알림과 발송 전에 취소된 알림은 묶음 기준으로 보지 않는다.")
    void ignoreSameRecordAndCanceledUnsentNotification() {
        persistRecord(10L, WRITER_ID);
        persistRecord(11L, WRITER_ID);
        insertBuddyNotification(100L, 1L, 10L, NotificationType.BUDDY_MEAL_RECORDED);
        insertBuddyNotification(101L, 4L, 11L, NotificationType.BUDDY_MEAL_RECORDED);
        notificationRepository.deleteByRecord(11L, BUDDY_RECORD_TYPES, LocalDateTime.now());

        List<Long> receiverIds = notificationRepository.findRecentBuddyRecordReceiverMemberIds(
            List.of(1L, 4L),
            WRITER_ID,
            10L,
            LocalDateTime.now().minusMinutes(10)
        );

        assertThat(receiverIds).isEmpty();
    }

    @Test
    @DisplayName("이미 발송한 알림은 기록이 삭제돼도 묶음 기준으로 본다.")
    void keepSentNotificationAfterRecordDeleted() {
        persistRecord(10L, WRITER_ID);
        insertBuddyNotification(100L, 1L, 10L, NotificationType.BUDDY_EXERCISE_RECORDED);
        entityManager.getEntityManager()
            .createNativeQuery("update notification set sent_at = current_timestamp where id = 100")
            .executeUpdate();
        notificationRepository.deleteByRecord(10L, BUDDY_RECORD_TYPES, LocalDateTime.now());

        List<Long> receiverIds = notificationRepository.findRecentBuddyRecordReceiverMemberIds(
            List.of(1L),
            WRITER_ID,
            20L,
            LocalDateTime.now().minusMinutes(10)
        );

        assertThat(receiverIds).containsExactly(1L);
    }

    @Test
    @DisplayName("기록을 삭제하면 그 기록의 버디 기록 알림만 소프트 삭제한다.")
    void deleteByRecord() {
        persistRecord(10L, WRITER_ID);
        insertBuddyNotification(100L, 1L, 10L, NotificationType.BUDDY_MEAL_RECORDED);
        insertBuddyNotification(101L, 4L, 10L, NotificationType.BUDDY_MEAL_RECORDED);
        insertNotification(102L, 1L, 10L, NotificationType.COMMENT_CREATED, "COMMENT_CREATED:RECORD:10:1");

        int deleted = notificationRepository.deleteByRecord(10L, BUDDY_RECORD_TYPES, LocalDateTime.now());

        assertThat(deleted).isEqualTo(2);
        assertThat(notificationRepository.findByIdAndDeletedAtIsNull(100L)).isEmpty();
        assertThat(notificationRepository.findByIdAndDeletedAtIsNull(102L)).isPresent();
    }

    private void persistRecord(Long id, Long memberId) {
        entityManager.persist(ActivityRecord.meal(
            id, memberId, null, LocalTime.NOON, "샐러드", "records/" + id, LocalDateTime.now()
        ));
        entityManager.flush();
    }

    private void insertBuddyNotification(Long id, Long receiverId, Long recordId, NotificationType type) {
        insertNotification(id, receiverId, recordId, type, type.name() + ":RECORD:" + recordId + ":" + receiverId);
    }

    private void insertNotification(Long id, Long receiverId, Long recordId, NotificationType type, String dedupeKey) {
        notificationRepository.insertIfDedupeKeyAbsent(
            id, receiverId, type.name(), "제목", "내용", null, "RECORD", recordId,
            "/records/" + recordId, LocalDateTime.now(), 3, dedupeKey
        );
    }

    private void setCreatedAt(Long id, LocalDateTime createdAt) {
        entityManager.getEntityManager().createNativeQuery("update notification set created_at = :createdAt where id = :id")
            .setParameter("createdAt", createdAt)
            .setParameter("id", id)
            .executeUpdate();
    }
}
