package cmc.mody.auth.presentation.support;

import static cmc.mody.auth.infrastructure.constants.JwtConstants.AUTHORIZATION_HEADER;
import static cmc.mody.auth.infrastructure.constants.JwtConstants.BEARER_PREFIX;

import cmc.mody.auth.application.token.TokenProvider;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@RequiredArgsConstructor
public class CurrentMemberArgumentResolver implements HandlerMethodArgumentResolver {
    private final TokenProvider tokenProvider;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentMember.class)
            && parameter.getParameterType().equals(Long.class);
    }

    @Override
    public Object resolveArgument(
        MethodParameter parameter,
        ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest,
        WebDataBinderFactory binderFactory
    ) {
        return tokenProvider.getMemberIdByAccessToken(extractToken(webRequest));
    }

    private String extractToken(NativeWebRequest webRequest) {
        String bearer = webRequest.getHeader(AUTHORIZATION_HEADER);
        if (!StringUtils.hasText(bearer)) {
            throw new GeneralException(ErrorStatus.NO_AUTHORIZED);
        }
        if (!bearer.startsWith(BEARER_PREFIX)) {
            throw new GeneralException(ErrorStatus.INVALID_JWT);
        }

        String token = bearer.substring(BEARER_PREFIX.length());
        if (!StringUtils.hasText(token)) {
            throw new GeneralException(ErrorStatus.EMPTY_JWT);
        }
        return token;
    }
}
