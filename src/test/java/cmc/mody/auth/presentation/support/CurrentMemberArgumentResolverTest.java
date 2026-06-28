package cmc.mody.auth.presentation.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import cmc.mody.auth.application.token.TokenProvider;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;

@ExtendWith(MockitoExtension.class)
class CurrentMemberArgumentResolverTest {
    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private NativeWebRequest webRequest;

    @Test
    void resolveCurrentMember() throws Exception {
        CurrentMemberArgumentResolver resolver = new CurrentMemberArgumentResolver(tokenProvider);
        given(webRequest.getHeader("Authorization")).willReturn("Bearer access-token");
        given(tokenProvider.getMemberIdByAccessToken("access-token")).willReturn(1L);

        Object result = resolver.resolveArgument(currentMemberParameter(), null, webRequest, null);

        assertThat(result).isEqualTo(1L);
        then(tokenProvider).should().getMemberIdByAccessToken("access-token");
    }

    @Test
    void throwNoAuthorizedWhenAuthorizationHeaderIsMissing() throws Exception {
        CurrentMemberArgumentResolver resolver = new CurrentMemberArgumentResolver(tokenProvider);
        given(webRequest.getHeader("Authorization")).willReturn(null);

        assertThatThrownBy(() -> resolver.resolveArgument(currentMemberParameter(), null, webRequest, null))
            .isInstanceOf(GeneralException.class)
            .extracting("status")
            .isEqualTo(ErrorStatus.NO_AUTHORIZED);
    }

    @Test
    void throwInvalidJwtWhenAuthorizationHeaderIsNotBearer() throws Exception {
        CurrentMemberArgumentResolver resolver = new CurrentMemberArgumentResolver(tokenProvider);
        given(webRequest.getHeader("Authorization")).willReturn("access-token");

        assertThatThrownBy(() -> resolver.resolveArgument(currentMemberParameter(), null, webRequest, null))
            .isInstanceOf(GeneralException.class)
            .extracting("status")
            .isEqualTo(ErrorStatus.INVALID_JWT);
    }

    @Test
    void throwEmptyJwtWhenBearerTokenIsBlank() throws Exception {
        CurrentMemberArgumentResolver resolver = new CurrentMemberArgumentResolver(tokenProvider);
        given(webRequest.getHeader("Authorization")).willReturn("Bearer ");

        assertThatThrownBy(() -> resolver.resolveArgument(currentMemberParameter(), null, webRequest, null))
            .isInstanceOf(GeneralException.class)
            .extracting("status")
            .isEqualTo(ErrorStatus.EMPTY_JWT);
    }

    private MethodParameter currentMemberParameter() throws NoSuchMethodException {
        return new MethodParameter(
            getClass().getDeclaredMethod("sample", Long.class),
            0
        );
    }

    @SuppressWarnings("unused")
    private void sample(@CurrentMember Long memberId) {
    }
}
