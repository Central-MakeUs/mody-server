package cmc.mody.notification.infrastructure.repository;

import cmc.mody.notification.domain.ExerciseSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciseScheduleRepository extends JpaRepository<ExerciseSchedule, Long> {
}
