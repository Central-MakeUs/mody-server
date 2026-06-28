package cmc.mody.grouping.infrastructure.repository;

import cmc.mody.grouping.domain.ModyGroup;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModyGroupRepository extends JpaRepository<ModyGroup, Long> {
    boolean existsByCodeAndDeletedAtIsNull(String code);

    Optional<ModyGroup> findByCodeAndDeletedAtIsNull(String code);
}
