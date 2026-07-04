package cmc.mody.notification.application;

import java.util.concurrent.Executor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableConfigurationProperties({
    NotificationDispatchProperties.class,
    NotificationReminderProperties.class,
    NotificationRetentionProperties.class,
    NotificationStreakRiskProperties.class
})
public class NotificationConfig {
    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("notification-");
        executor.initialize();
        return executor;
    }
}
