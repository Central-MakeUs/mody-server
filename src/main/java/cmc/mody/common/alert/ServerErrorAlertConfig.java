package cmc.mody.common.alert;

import java.util.concurrent.Executor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableConfigurationProperties(ServerErrorAlertProperties.class)
public class ServerErrorAlertConfig {
    @Bean(name = "serverErrorAlertExecutor")
    public Executor serverErrorAlertExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("server-error-alert-");
        executor.initialize();
        return executor;
    }
}
