package cmc.mody.notification.infrastructure.repository;

import cmc.mody.notification.domain.Notification;
import cmc.mody.notification.domain.NotificationDeliveryStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByReceiverMemberIdAndDeletedAtIsNull(Long receiverMemberId);

    List<Notification> findByReceiverMemberIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(Long receiverMemberId);

    Optional<Notification> findByIdAndDeletedAtIsNull(Long notificationId);

    List<Notification> findByCreatedAtBeforeAndDeletedAtIsNull(LocalDateTime createdAt, Pageable pageable);

    boolean existsByDedupeKeyAndDeletedAtIsNull(String dedupeKey);

    List<Notification> findByPickedByAndDeliveryStatusAndDeletedAtIsNull(
        String pickedBy,
        NotificationDeliveryStatus deliveryStatus,
        Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = """
        update notification
           set delivery_status = 'PROCESSING',
               picked_by = :serverId,
               picked_at = current_timestamp
         where delivery_status = 'PENDING'
           and deleted_at is null
           and (scheduled_at is null or scheduled_at <= current_timestamp)
           and (next_retry_at is null or next_retry_at <= current_timestamp)
         order by scheduled_at asc, id asc
         limit :batchSize
        """, nativeQuery = true)
    int pickPendingNotifications(@Param("serverId") String serverId, @Param("batchSize") int batchSize);
}
