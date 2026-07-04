package cmc.mody.notification.infrastructure.repository;

import cmc.mody.notification.domain.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByReceiverMemberIdAndDeletedAtIsNull(Long receiverMemberId);
}
