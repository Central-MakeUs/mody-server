package cmc.mody.auth.application.oauth;

import cmc.mody.auth.domain.RefreshToken;
import cmc.mody.auth.infrastructure.repository.RefreshTokenRepository;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
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
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllByMemberIdAndDeletedAtIsNullOrderByIdAsc(memberId);
        activeTokens.forEach(RefreshToken::delete);
        List<RefreshToken> duplicatedTokens = refreshTokenRepository
            .findAllByTokenAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(refreshToken);
        duplicatedTokens.forEach(RefreshToken::delete);
        refreshTokenRepository.save(new RefreshToken(idGenerator.nextId(), memberId, refreshToken));
    }

    @Transactional
    public void validate(Long memberId, String refreshToken) {
        RefreshToken token = getActiveRefreshToken(refreshToken);
        if (!token.getMemberId().equals(memberId)) {
            throw new GeneralException(ErrorStatus.INVALID_REFRESH_TOKEN);
        }
    }

    @Transactional
    public void delete(Long memberId, String refreshToken) {
        RefreshToken token = getActiveRefreshToken(refreshToken);
        if (!token.getMemberId().equals(memberId)) {
            throw new GeneralException(ErrorStatus.INVALID_REFRESH_TOKEN);
        }
        token.delete();
    }

    private RefreshToken getActiveRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new GeneralException(ErrorStatus.INVALID_REFRESH_TOKEN);
        }
        List<RefreshToken> tokens = refreshTokenRepository
            .findAllByTokenAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(refreshToken);
        if (tokens.isEmpty()) {
            throw new GeneralException(ErrorStatus.INVALID_REFRESH_TOKEN);
        }
        tokens.stream()
            .skip(1)
            .forEach(RefreshToken::delete);
        return tokens.get(0);
    }
}
