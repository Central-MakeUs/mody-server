package cmc.mody.notification.application;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cmc.mody.notification.domain.ExerciseSchedule;
import cmc.mody.notification.domain.NotificationSetting;
import cmc.mody.notification.infrastructure.repository.ExerciseScheduleRepository;
import cmc.mody.notification.infrastructure.repository.NotificationSettingRepository;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationReminderSchedulerTest {
    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @Mock
    private ExerciseScheduleRepository exerciseScheduleRepository;

    @Mock
    private NotificationRequestService notificationRequestService;

    @Test
    @DisplayName("현재 시간과 일치하는 식사 알림 설정으로 리마인더 이벤트를 생성한다.")
    void sendMealReminder() {
        NotificationReminderScheduler scheduler = scheduler();
        LocalTime time = LocalTime.of(8, 0);
        given(notificationSettingRepository.findByMealReminderEnabledTrueAndBreakfastTimeAndDeletedAtIsNull(time))
            .willReturn(List.of(notificationSetting(1L, time, null, null, true)));
        given(notificationSettingRepository.findByMealReminderEnabledTrueAndLunchTimeAndDeletedAtIsNull(time))
            .willReturn(List.of());
        given(notificationSettingRepository.findByMealReminderEnabledTrueAndDinnerTimeAndDeletedAtIsNull(time))
            .willReturn(List.of());
        given(exerciseScheduleRepository.findByDayOfWeekAndScheduledTimeAndDeletedAtIsNull(DayOfWeek.SATURDAY, time))
            .willReturn(List.of());

        scheduler.sendDueReminders(LocalDateTime.of(2026, 7, 4, 8, 0));

        verify(notificationRequestService).requestMealReminder(1L, "BREAKFAST", "2026-07-04");
    }

    @Test
    @DisplayName("현재 요일과 시간이 일치하고 운동 알림이 켜진 회원에게 운동 리마인더 이벤트를 생성한다.")
    void sendExerciseReminder() {
        NotificationReminderScheduler scheduler = scheduler();
        LocalTime time = LocalTime.of(20, 0);
        given(notificationSettingRepository.findByMealReminderEnabledTrueAndBreakfastTimeAndDeletedAtIsNull(time))
            .willReturn(List.of());
        given(notificationSettingRepository.findByMealReminderEnabledTrueAndLunchTimeAndDeletedAtIsNull(time))
            .willReturn(List.of());
        given(notificationSettingRepository.findByMealReminderEnabledTrueAndDinnerTimeAndDeletedAtIsNull(time))
            .willReturn(List.of());
        given(exerciseScheduleRepository.findByDayOfWeekAndScheduledTimeAndDeletedAtIsNull(DayOfWeek.MONDAY, time))
            .willReturn(List.of(
                new ExerciseSchedule(10L, 1L, DayOfWeek.MONDAY, time),
                new ExerciseSchedule(11L, 2L, DayOfWeek.MONDAY, time)
            ));
        given(notificationSettingRepository.findByMemberIdAndDeletedAtIsNull(1L))
            .willReturn(java.util.Optional.of(notificationSetting(1L, null, null, null, true)));
        given(notificationSettingRepository.findByMemberIdAndDeletedAtIsNull(2L))
            .willReturn(java.util.Optional.of(notificationSetting(2L, null, null, null, false)));

        scheduler.sendDueReminders(LocalDateTime.of(2026, 7, 6, 20, 0));

        verify(notificationRequestService).requestExerciseReminder(1L, "20:00", "2026-07-06");
        verify(notificationRequestService, never()).requestExerciseReminder(2L, "20:00", "2026-07-06");
    }

    private NotificationReminderScheduler scheduler() {
        return new NotificationReminderScheduler(
            notificationSettingRepository,
            exerciseScheduleRepository,
            notificationRequestService
        );
    }

    private NotificationSetting notificationSetting(
        Long memberId,
        LocalTime breakfastTime,
        LocalTime lunchTime,
        LocalTime dinnerTime,
        boolean exerciseReminderEnabled
    ) {
        return new NotificationSetting(
            memberId,
            memberId,
            true,
            breakfastTime,
            lunchTime,
            dinnerTime,
            exerciseReminderEnabled,
            null
        );
    }
}
