package cmc.mody.auth.infrastructure.oauth;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({OAuthProperties.class, AppleIdentityTokenProperties.class})
public class OAuthConfig {
}
