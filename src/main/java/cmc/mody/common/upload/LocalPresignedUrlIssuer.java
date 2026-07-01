package cmc.mody.common.upload;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "upload", name = "provider", havingValue = "local", matchIfMissing = true)
public class LocalPresignedUrlIssuer implements PresignedUrlIssuer {
    private final UploadProperties uploadProperties;

    @Override
    public PresignedUrlIssueResult issue(String imageKey) {
        long expiresInSeconds = uploadProperties.getPresignedUrlExpiresInSeconds();
        return new PresignedUrlIssueResult(
            normalizedBaseUrl() + "/" + encodePath(imageKey) + "?expiresIn=" + expiresInSeconds,
            expiresInSeconds
        );
    }

    private String normalizedBaseUrl() {
        String baseUrl = uploadProperties.getBaseUrl();
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    private String encodePath(String imageKey) {
        return URLEncoder.encode(imageKey, StandardCharsets.UTF_8)
            .replace("+", "%20")
            .replace("%2F", "/");
    }
}
