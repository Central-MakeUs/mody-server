package cmc.mody.notification.infrastructure.repository;

import cmc.mody.notification.domain.Notification;
import cmc.mody.notification.domain.NotificationType;
import java.time.LocalDateTime;
import java.util.Collection;
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

    @Query("""
        select notification
        from Notification notification
        where notification.receiverMemberId = :receiverMemberId
          and notification.deletedAt is null
          and (
              :cursor is null
              or notification.createdAt < (
                  select cursorNotification.createdAt
                  from Notification cursorNotification
                  where cursorNotification.id = :cursor
              )
              or (
                  notification.createdAt = (
                      select cursorNotification.createdAt
                      from Notification cursorNotification
                      where cursorNotification.id = :cursor
                  )
                  and notification.id < :cursor
              )
          )
        order by notification.createdAt desc, notification.id desc
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

    @Modifying
    @Transactional
    @Query(value = """
        insert ignore into notification (
            content, created_at, dedupe_key, delivery_status, image_key, link, max_retry,
            notification_status, notification_type, receiver_member_id, reference_id,
            reference_type, retry_count, scheduled_at, status, title, updated_at, id
        ) values (
            :content, current_timestamp, :dedupeKey, 'PENDING', :imageKey, :link, :maxRetry,
            'PENDING', :notificationType, :receiverMemberId, :referenceId,
            :referenceType, 0, :scheduledAt, 'ACTIVE', :title, current_timestamp, :id
        )
        """, nativeQuery = true)
    int insertIfDedupeKeyAbsent(
        @Param("id") Long id,
        @Param("receiverMemberId") Long receiverMemberId,
        @Param("notificationType") String notificationType,
        @Param("title") String title,
        @Param("content") String content,
        @Param("imageKey") String imageKey,
        @Param("referenceType") String referenceType,
        @Param("referenceId") Long referenceId,
        @Param("link") String link,
        @Param("scheduledAt") LocalDateTime scheduledAt,
        @Param("maxRetry") int maxRetry,
        @Param("dedupeKey") String dedupeKey
    );

    boolean existsByDedupeKeyAndReferenceIdAndDeletedAtIsNull(String dedupeKey, Long referenceId);

    List<Notification> findByNotificationTypeAndReferenceIdAndReceiverMemberIdInAndCreatedAtGreaterThanEqualAndCreatedAtLessThanAndDeletedAtIsNull(
        NotificationType notificationType,
        Long referenceId,
        Collection<Long> receiverMemberIds,
        LocalDateTime createdAtStart,
        LocalDateTime createdAtEnd
    );

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
