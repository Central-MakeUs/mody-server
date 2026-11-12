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
    @DisplayName("기록을 삭제하면 그 기록의 버디 기록 알림만 소프트 삭제한다.")
    void deleteByRecord() {
        persistRecord(10L, WRITER_ID);
        insertBuddyNotification(100L, 1L, 10L, NotificationType.BUDDY_MEAL_RECORDED);
        insertBuddyNotification(101L, 4L, 10L, NotificationType.BUDDY_MEAL_RECORDED);
        insertNotification(102L, 1L, 10L, NotificationType.COMMENT_CREATED, "COMMENT_CREATED:COMMENT:30:1");

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
}
