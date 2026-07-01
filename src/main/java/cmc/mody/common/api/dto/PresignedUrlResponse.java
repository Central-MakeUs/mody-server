package cmc.mody.common.api.dto;

import cmc.mody.common.upload.UploadService;

public record PresignedUrlResponse(
    String presignedUrl,
    String imageKey,
    long expiresInSeconds
) {
    public static PresignedUrlResponse from(UploadService.PresignedUrlResult result) {
        return new PresignedUrlResponse(
            result.presignedUrl(),
            result.imageKey(),
            result.expiresInSeconds()
        );
    }
}
