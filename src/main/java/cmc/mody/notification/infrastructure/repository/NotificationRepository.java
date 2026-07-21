package cmc.mody.notification.infrastructure.repository;

import cmc.mody.notification.domain.Notification;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByReceiverMemberIdAndDeletedAtIsNull(Long receiverMemberId);

    List<Notification> findByReceiverMemberIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(Long receiverMemberId);

    @Query("""
        select notification
        from Notification notification
        where notification.receiverMemberId = :receiverMemberId
          and notification.deletedAt is null
          and (:cursor is null or notification.id < :cursor)
        order by notification.id desc
        """)
    List<Notification> findByReceiverMemberIdByCursor(
        @Param("receiverMemberId") Long receiverMemberId,
        @Param("cursor") Long cursor,
        Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Notification notification
           set notification.notificationStatus = cmc.mody.notification.domain.NotificationStatus.READ,
               notification.readAt = :readAt
         where notification.receiverMemberId = :receiverMemberId
           and notification.deletedAt is null
           and notification.createdAt <= :readAt
           and notification.readAt is null
           and notification.notificationStatus <> cmc.mody.notification.domain.NotificationStatus.READ
        """)
    int markAllUnreadAsRead(
        @Param("receiverMemberId") Long receiverMemberId,
        @Param("readAt") LocalDateTime readAt
    );

    @Query("""
        select count(notification) > 0
        from Notification notification
        where notification.receiverMemberId = :receiverMemberId
          and notification.deletedAt is null
          and notification.readAt is null
          and notification.notificationStatus <> cmc.mody.notification.domain.NotificationStatus.READ
        """)
    boolean existsUnreadByReceiverMemberId(@Param("receiverMemberId") Long receiverMemberId);

    Optional<Notification> findByIdAndDeletedAtIsNull(Long notificationId);

    List<Notification> findByCreatedAtBeforeAndDeletedAtIsNull(LocalDateTime createdAt, Pageable pageable);

    boolean existsByDedupeKeyAndDeletedAtIsNull(String dedupeKey);

    @Query(value = """
        select *
          from notification
         where delivery_status = 'PENDING'
           and deleted_at is null
           and (scheduled_at is null or scheduled_at <= current_timestamp)
           and (next_retry_at is null or next_retry_at <= current_timestamp)
         order by scheduled_at asc, id asc
         limit :batchSize
         for update skip locked
        """, nativeQuery = true)
    List<Notification> findDueNotificationsForUpdateSkipLocked(@Param("batchSize") int batchSize);
}
