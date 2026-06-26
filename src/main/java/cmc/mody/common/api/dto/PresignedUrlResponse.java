package cmc.mody.common.api.dto;

public record PresignedUrlResponse(
    String presignedUrl,
    String imageKey,
    long expiresInSeconds
) {
}
