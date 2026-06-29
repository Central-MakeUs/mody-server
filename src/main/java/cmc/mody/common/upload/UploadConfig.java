package cmc.mody.common.upload;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(UploadProperties.class)
public class UploadConfig {
    @Bean
    @ConditionalOnProperty(prefix = "upload", name = "provider", havingValue = "gcs")
    public Storage storage() {
        return StorageOptions.getDefaultInstance().getService();
    }
}
