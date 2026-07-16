package cmc.mody.auth.infrastructure.repository;

import cmc.mody.auth.domain.RefreshToken;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    List<RefreshToken> findAllByMemberIdAndDeletedAtIsNull(Long memberId);

    List<RefreshToken> findAllByTokenAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(String token);
}
