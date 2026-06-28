package cmc.mody.common.config;

import cmc.mody.auth.application.token.TokenProvider;
import cmc.mody.auth.presentation.support.CurrentMemberArgumentResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final ObjectProvider<CurrentMemberArgumentResolver> currentMemberArgumentResolver;

    @Bean
    @ConditionalOnBean(TokenProvider.class)
    public CurrentMemberArgumentResolver currentMemberArgumentResolver(TokenProvider tokenProvider) {
        return new CurrentMemberArgumentResolver(tokenProvider);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        currentMemberArgumentResolver.ifAvailable(resolvers::add);
    }
}
