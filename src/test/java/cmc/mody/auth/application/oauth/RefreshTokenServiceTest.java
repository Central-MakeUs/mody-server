package cmc.mody.auth.application.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import cmc.mody.auth.domain.RefreshToken;
import cmc.mody.auth.infrastructure.repository.RefreshTokenRepository;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.domain.Status;
import cmc.mody.common.id.IdGenerator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {
    @Mock
    private IdGenerator idGenerator;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Captor
    private ArgumentCaptor<RefreshToken> refreshTokenCaptor;

    @Test
    @DisplayName("기존 refresh token을 비활성화하고 새 refresh token을 저장한다.")
    void replaceRefreshToken() {
        RefreshToken oldToken = new RefreshToken(1L, 10L, "old-refresh");
        RefreshTokenService service = new RefreshTokenService(idGenerator, refreshTokenRepository);

        given(refreshTokenRepository.findAllByMemberIdAndDeletedAtIsNullOrderByIdAsc(10L)).willReturn(List.of(oldToken));
        given(refreshTokenRepository.findAllByTokenAndDeletedAtIsNullOrderByCreatedAtDescIdDesc("new-refresh"))
            .willReturn(List.of());
        given(idGenerator.nextId()).willReturn(2L);
        given(refreshTokenRepository.save(any(RefreshToken.class))).willAnswer(invocation -> invocation.getArgument(0));

        service.replace(10L, "new-refresh");

        assertThat(oldToken.getStatus()).isEqualTo(Status.INACTIVE);
        then(refreshTokenRepository).should()
            .findAllByTokenAndDeletedAtIsNullOrderByCreatedAtDescIdDesc("new-refresh");
        then(refreshTokenRepository).should()
            .findAllByMemberIdAndDeletedAtIsNullOrderByIdAsc(10L);
        then(refreshTokenRepository).should().save(refreshTokenCaptor.capture());
        assertThat(refreshTokenCaptor.getValue().getId()).isEqualTo(2L);
        assertThat(refreshTokenCaptor.getValue().getToken()).isEqualTo("new-refresh");
    }

    @Test
    @DisplayName("저장된 활성 refresh token이면 검증을 통과한다.")
    void validateRefreshToken() {
        RefreshTokenService service = new RefreshTokenService(idGenerator, refreshTokenRepository);
        given(refreshTokenRepository.findAllByTokenAndDeletedAtIsNullOrderByCreatedAtDescIdDesc("refresh"))
            .willReturn(List.of(new RefreshToken(1L, 10L, "refresh")));

        service.validate(10L, "refresh");

        then(refreshTokenRepository).should().findAllByTokenAndDeletedAtIsNullOrderByCreatedAtDescIdDesc("refresh");
    }

    @Test
    @DisplayName("중복 저장된 활성 refresh token은 최신 1개만 사용하고 나머지는 비활성화한다.")
    void validateRefreshTokenWithDuplicatedActiveTokens() {
        RefreshToken latestToken = new RefreshToken(2L, 10L, "refresh");
        RefreshToken duplicatedToken = new RefreshToken(1L, 10L, "refresh");
        RefreshTokenService service = new RefreshTokenService(idGenerator, refreshTokenRepository);
        given(refreshTokenRepository.findAllByTokenAndDeletedAtIsNullOrderByCreatedAtDescIdDesc("refresh"))
            .willReturn(List.of(latestToken, duplicatedToken));

        service.validate(10L, "refresh");

        assertThat(latestToken.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(duplicatedToken.getStatus()).isEqualTo(Status.INACTIVE);
    }

    @Test
    @DisplayName("저장되지 않은 refresh token은 검증할 수 없다.")
    void throwInvalidRefreshTokenWhenNotStored() {
        RefreshTokenService service = new RefreshTokenService(idGenerator, refreshTokenRepository);
        given(refreshTokenRepository.findAllByTokenAndDeletedAtIsNullOrderByCreatedAtDescIdDesc("refresh"))
            .willReturn(List.of());

        assertThatThrownBy(() -> service.validate(10L, "refresh"))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getStatus()).isEqualTo(ErrorStatus.INVALID_REFRESH_TOKEN));
    }

    @Test
    @DisplayName("회원 id가 다른 refresh token은 검증할 수 없다.")
    void throwInvalidRefreshTokenWhenMemberDiffers() {
        RefreshTokenService service = new RefreshTokenService(idGenerator, refreshTokenRepository);
        given(refreshTokenRepository.findAllByTokenAndDeletedAtIsNullOrderByCreatedAtDescIdDesc("refresh"))
            .willReturn(List.of(new RefreshToken(1L, 20L, "refresh")));

        assertThatThrownBy(() -> service.validate(10L, "refresh"))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getStatus()).isEqualTo(ErrorStatus.INVALID_REFRESH_TOKEN));
    }

    @Test
    @DisplayName("로그아웃 시 refresh token을 비활성화한다.")
    void deleteRefreshToken() {
        RefreshToken token = new RefreshToken(1L, 10L, "refresh");
        RefreshTokenService service = new RefreshTokenService(idGenerator, refreshTokenRepository);
        given(refreshTokenRepository.findAllByTokenAndDeletedAtIsNullOrderByCreatedAtDescIdDesc("refresh"))
            .willReturn(List.of(token));

        service.delete(10L, "refresh");

        assertThat(token.getStatus()).isEqualTo(Status.INACTIVE);
    }
}
