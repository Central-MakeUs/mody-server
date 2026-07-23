package cmc.mody.auth.application.oauth.strategy.impl;

import cmc.mody.auth.application.oauth.DemoLoginProperties;
import cmc.mody.member.domain.LoginType;
import org.springframework.stereotype.Component;

@Component
public class IosTestOAuthStrategy extends DemoOAuthStrategy {
    public IosTestOAuthStrategy(DemoLoginProperties properties) {
        super(properties, LoginType.IOSTEST);
    }
}
