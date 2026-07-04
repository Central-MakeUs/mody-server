package cmc.mody.notification.infrastructure.repository;

import cmc.mody.notification.domain.NotificationSetting;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {
    Optional<NotificationSetting> findByMemberIdAndDeletedAtIsNull(Long memberId);

    List<NotificationSetting> findAllByMemberIdAndDeletedAtIsNull(Long memberId);

    List<NotificationSetting> findByMealReminderEnabledTrueAndBreakfastTimeAndDeletedAtIsNull(LocalTime breakfastTime);

    List<NotificationSetting> findByMealReminderEnabledTrueAndLunchTimeAndDeletedAtIsNull(LocalTime lunchTime);

    List<NotificationSetting> findByMealReminderEnabledTrueAndDinnerTimeAndDeletedAtIsNull(LocalTime dinnerTime);
}
