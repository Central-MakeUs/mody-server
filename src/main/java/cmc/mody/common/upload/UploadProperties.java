package cmc.mody.common.upload;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "upload")
public class UploadProperties {
    private String provider = "local";
    private String gcpBucket = "";
    private String baseUrl = "https://storage.example.com";
    private long presignedUrlExpiresInSeconds = 300L;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getGcpBucket() {
        return gcpBucket;
    }

    public void setGcpBucket(String gcpBucket) {
        this.gcpBucket = gcpBucket;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public long getPresignedUrlExpiresInSeconds() {
        return presignedUrlExpiresInSeconds;
    }

    public void setPresignedUrlExpiresInSeconds(long presignedUrlExpiresInSeconds) {
        this.presignedUrlExpiresInSeconds = presignedUrlExpiresInSeconds;
    }
}
