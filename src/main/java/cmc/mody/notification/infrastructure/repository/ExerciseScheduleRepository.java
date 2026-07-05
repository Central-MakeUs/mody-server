package cmc.mody.notification.infrastructure.repository;

import cmc.mody.notification.domain.ExerciseSchedule;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciseScheduleRepository extends JpaRepository<ExerciseSchedule, Long> {
    List<ExerciseSchedule> findByMemberIdAndDeletedAtIsNull(Long memberId);

    List<ExerciseSchedule> findByDayOfWeekAndScheduledTimeAndDeletedAtIsNull(
        DayOfWeek dayOfWeek,
        LocalTime scheduledTime
    );
}
