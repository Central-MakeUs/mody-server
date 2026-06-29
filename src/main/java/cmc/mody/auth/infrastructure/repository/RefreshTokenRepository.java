package cmc.mody.auth.infrastructure.repository;

import cmc.mody.auth.domain.RefreshToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    List<RefreshToken> findAllByMemberIdAndDeletedAtIsNull(Long memberId);

    Optional<RefreshToken> findByTokenAndDeletedAtIsNull(String token);
}
