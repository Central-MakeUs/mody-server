package cmc.mody.notification.infrastructure.repository;

import cmc.mody.notification.domain.NotificationSetting;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {
    Optional<NotificationSetting> findByMemberIdAndDeletedAtIsNull(Long memberId);
}
