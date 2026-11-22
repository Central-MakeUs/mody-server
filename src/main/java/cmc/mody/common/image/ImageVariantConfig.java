package cmc.mody.common.image;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableConfigurationProperties(ImageVariantProperties.class)
public class ImageVariantConfig {
    /** 사진 디코딩은 장당 수십 MB를 쓴다. 동시 처리 수를 작게 묶어 API 요청이 쓸 메모리를 남긴다. */
    @Bean(name = "imageVariantExecutor")
    public ThreadPoolTaskExecutor imageVariantExecutor(ImageVariantProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int concurrency = Math.max(1, properties.getConcurrency());
        executor.setCorePoolSize(concurrency);
        executor.setMaxPoolSize(concurrency);
        executor.setQueueCapacity(Math.max(1, properties.getBatchSize()) * 2);
        executor.setThreadNamePrefix("image-variant-");
        executor.initialize();
        return executor;
    }
}
