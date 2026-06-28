package cmc.mody.auth.application.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import cmc.mody.auth.domain.RefreshToken;
import cmc.mody.auth.infrastructure.repository.RefreshTokenRepository;
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

        given(refreshTokenRepository.findAllByMemberIdAndDeletedAtIsNull(10L)).willReturn(List.of(oldToken));
        given(idGenerator.nextId()).willReturn(2L);
        given(refreshTokenRepository.save(any(RefreshToken.class))).willAnswer(invocation -> invocation.getArgument(0));

        service.replace(10L, "new-refresh");

        assertThat(oldToken.getStatus()).isEqualTo(Status.INACTIVE);
        then(refreshTokenRepository).should().save(refreshTokenCaptor.capture());
        assertThat(refreshTokenCaptor.getValue().getId()).isEqualTo(2L);
        assertThat(refreshTokenCaptor.getValue().getToken()).isEqualTo("new-refresh");
    }
}
