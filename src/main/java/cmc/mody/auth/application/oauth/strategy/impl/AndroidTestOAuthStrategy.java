package cmc.mody.auth.application.oauth.strategy.impl;

import cmc.mody.auth.application.oauth.DemoLoginProperties;
import cmc.mody.member.domain.LoginType;
import org.springframework.stereotype.Component;

@Component
public class AndroidTestOAuthStrategy extends DemoOAuthStrategy {
    public AndroidTestOAuthStrategy(DemoLoginProperties properties) {
        super(properties, LoginType.ANDROIDTEST);
    }
}
