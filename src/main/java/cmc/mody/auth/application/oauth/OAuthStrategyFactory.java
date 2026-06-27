package cmc.mody.auth.application.oauth;

import cmc.mody.auth.application.oauth.strategy.OAuthStrategy;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.member.domain.LoginType;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OAuthStrategyFactory {
    private final Map<LoginType, OAuthStrategy> strategies;

    public OAuthStrategyFactory(List<OAuthStrategy> strategies) {
        this.strategies = Collections.unmodifiableMap(strategies.stream()
            .collect(Collectors.toMap(
                OAuthStrategy::getType,
                strategy -> strategy,
                (left, right) -> {
                    throw new IllegalStateException("Duplicated OAuth strategy: " + left.getType());
                },
                () -> new EnumMap<>(LoginType.class)
            )));
        log.info("Registered OAuth strategies: {}", this.strategies.keySet());
    }

    public OAuthStrategy getStrategy(LoginType loginType) {
        return Optional.ofNullable(strategies.get(loginType))
            .orElseThrow(() -> new GeneralException(ErrorStatus.UNSUPPORTED_LOGIN_TYPE));
    }
}
