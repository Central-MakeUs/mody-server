package cmc.mody.common.upload;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImageUrlResolver {
    private final UploadProperties uploadProperties;

    public String resolve(String imageKey) {
        if (imageKey == null || imageKey.isBlank()) {
            return null;
        }
        if (imageKey.startsWith("http://") || imageKey.startsWith("https://")) {
            return imageKey;
        }
        String baseUrl = uploadProperties.getBaseUrl();
        if (baseUrl.endsWith("/")) {
            return baseUrl + imageKey;
        }
        return baseUrl + "/" + imageKey;
    }
}
