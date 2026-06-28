package cmc.mody.auth.application.token;

import cmc.mody.auth.presentation.dto.TokenDto;

public interface TokenProvider {
    TokenDto createToken(Long memberId);

    void validateToken(String token);

    Long getMemberIdByToken(String token);

    Long getMemberIdByAccessToken(String token);
}
