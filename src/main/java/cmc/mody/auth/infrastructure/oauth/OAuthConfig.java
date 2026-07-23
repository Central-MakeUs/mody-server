package cmc.mody.auth.infrastructure.oauth;

import cmc.mody.auth.application.oauth.DemoLoginProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({OAuthProperties.class, AppleIdentityTokenProperties.class, DemoLoginProperties.class})
public class OAuthConfig {
}
