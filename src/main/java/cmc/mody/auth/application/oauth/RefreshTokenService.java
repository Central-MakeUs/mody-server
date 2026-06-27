package cmc.mody.auth.application.oauth;

import cmc.mody.auth.domain.RefreshToken;
import cmc.mody.auth.infrastructure.repository.RefreshTokenRepository;
import cmc.mody.common.id.IdGenerator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final IdGenerator idGenerator;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public void replace(Long memberId, String refreshToken) {
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllByMemberIdAndDeletedAtIsNull(memberId);
        activeTokens.forEach(RefreshToken::delete);
        refreshTokenRepository.save(new RefreshToken(idGenerator.nextId(), memberId, refreshToken));
    }
}
