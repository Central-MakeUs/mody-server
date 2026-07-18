package cmc.mody.auth.infrastructure.repository;

import cmc.mody.auth.domain.RefreshToken;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<RefreshToken> findAllByMemberIdAndDeletedAtIsNullOrderByIdAsc(Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<RefreshToken> findAllByTokenAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(String token);
}
