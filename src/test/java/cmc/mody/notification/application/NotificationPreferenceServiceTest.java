package cmc.mody.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import cmc.mody.common.id.IdGenerator;
import cmc.mody.notification.domain.ExerciseSchedule;
import cmc.mody.notification.domain.MealType;
import cmc.mody.notification.domain.NotificationSetting;
import cmc.mody.notification.infrastructure.repository.ExerciseScheduleRepository;
import cmc.mody.notification.infrastructure.repository.NotificationSettingRepository;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {
    @Mock
    private IdGenerator idGenerator;

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @Mock
    private ExerciseScheduleRepository exerciseScheduleRepository;

    @Captor
    private ArgumentCaptor<NotificationSetting> notificationSettingCaptor;

    @Captor
    private ArgumentCaptor<ExerciseSchedule> exerciseScheduleCaptor;

    @Test
    @DisplayName("알림 설정이 없으면 기본 설정과 운동 일정을 반환한다.")
    void getDefaultPreferences() {
        NotificationPreferenceService service = service();
        given(notificationSettingRepository.findByMemberIdAndDeletedAtIsNull(1L)).willReturn(Optional.empty());
        given(exerciseScheduleRepository.findByMemberIdAndDeletedAtIsNull(1L))
            .willReturn(List.of(new ExerciseSchedule(10L, 1L, DayOfWeek.MONDAY, LocalTime.of(7, 30))));

        NotificationPreferenceService.NotificationPreferenceResult result = service.getPreferences(1L);

        assertThat(result.recordReminderEnabled()).isTrue();
        assertThat(result.commentNotificationEnabled()).isTrue();
        assertThat(result.challengeNotificationEnabled()).isTrue();
        assertThat(result.mealSchedules()).hasSize(3);
        assertThat(result.exerciseSchedules()).hasSize(1);
    }

    @Test
    @DisplayName("알림 on/off 설정을 저장한다.")
    void updateReminderFlags() {
        NotificationPreferenceService service = service();
        NotificationSetting notificationSetting = new NotificationSetting(10L, 1L);
        given(notificationSettingRepository.findByMemberIdAndDeletedAtIsNull(1L))
            .willReturn(Optional.of(notificationSetting));
        given(notificationSettingRepository.save(any(NotificationSetting.class)))
            .willAnswer(invocation -> invocation.getArgument(0));
        given(exerciseScheduleRepository.findByMemberIdAndDeletedAtIsNull(1L)).willReturn(List.of());

        service.updateReminderFlags(1L, new NotificationPreferenceService.ReminderFlagCommand(
            false,
            false,
            true
        ));

        then(notificationSettingRepository).should().save(notificationSettingCaptor.capture());
        assertThat(notificationSettingCaptor.getValue().isMealReminderEnabled()).isFalse();
        assertThat(notificationSettingCaptor.getValue().isExerciseReminderEnabled()).isFalse();
        assertThat(notificationSettingCaptor.getValue().isCommentNotificationEnabled()).isFalse();
        assertThat(notificationSettingCaptor.getValue().isChallengeNotificationEnabled()).isTrue();
    }

    @Test
    @DisplayName("식사 시간과 먹지 않음 설정을 저장한다.")
    void updateMealTimes() {
        NotificationPreferenceService service = service();
        NotificationSetting notificationSetting = new NotificationSetting(10L, 1L);
        given(notificationSettingRepository.findByMemberIdAndDeletedAtIsNull(1L))
            .willReturn(Optional.of(notificationSetting));
        given(notificationSettingRepository.save(any(NotificationSetting.class)))
            .willAnswer(invocation -> invocation.getArgument(0));
        given(exerciseScheduleRepository.findByMemberIdAndDeletedAtIsNull(1L)).willReturn(List.of());

        NotificationPreferenceService.NotificationPreferenceResult result = service.updateMealTimes(1L, List.of(
            new NotificationPreferenceService.MealScheduleCommand(MealType.BREAKFAST, LocalTime.of(8, 0), false),
            new NotificationPreferenceService.MealScheduleCommand(MealType.LUNCH, null, true),
            new NotificationPreferenceService.MealScheduleCommand(MealType.DINNER, LocalTime.of(18, 0), false)
        ));

        assertThat(result.mealSchedules())
            .extracting(NotificationPreferenceService.MealScheduleResult::time)
            .containsExactly(LocalTime.of(8, 0), null, LocalTime.of(18, 0));
    }

    @Test
    @DisplayName("운동 일정 수정 시 기존 일정을 삭제 처리하고 새 일정을 저장한다.")
    void updateExerciseSchedules() {
        NotificationPreferenceService service = service();
        ExerciseSchedule previous = new ExerciseSchedule(10L, 1L, DayOfWeek.TUESDAY, LocalTime.of(21, 0));
        given(exerciseScheduleRepository.findByMemberIdAndDeletedAtIsNull(1L)).willReturn(List.of(previous));
        given(idGenerator.nextId()).willReturn(11L, 12L, 13L);
        given(exerciseScheduleRepository.save(any(ExerciseSchedule.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        NotificationPreferenceService.ExerciseScheduleUpdateResult result = service.updateExerciseSchedules(1L, List.of(
            new NotificationPreferenceService.ExerciseScheduleCommand(DayOfWeek.MONDAY, LocalTime.of(7, 30)),
            new NotificationPreferenceService.ExerciseScheduleCommand(DayOfWeek.WEDNESDAY, LocalTime.of(20, 0)),
            new NotificationPreferenceService.ExerciseScheduleCommand(DayOfWeek.FRIDAY, LocalTime.of(9, 0))
        ));

        assertThat(previous.isActive()).isFalse();
        then(exerciseScheduleRepository).should(times(3)).save(exerciseScheduleCaptor.capture());
        assertThat(exerciseScheduleCaptor.getAllValues())
            .extracting(ExerciseSchedule::getDayOfWeek)
            .containsExactly(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);
        assertThat(result.schedules()).hasSize(3);
    }

    private NotificationPreferenceService service() {
        return new NotificationPreferenceService(
            idGenerator,
            notificationSettingRepository,
            exerciseScheduleRepository
        );
    }
}
