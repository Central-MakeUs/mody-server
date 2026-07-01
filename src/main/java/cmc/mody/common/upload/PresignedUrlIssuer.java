package cmc.mody.common.upload;

public interface PresignedUrlIssuer {
    PresignedUrlIssueResult issue(String imageKey);

    record PresignedUrlIssueResult(
        String presignedUrl,
        long expiresInSeconds
    ) {
    }
}
