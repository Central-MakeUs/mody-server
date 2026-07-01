package cmc.mody.common.upload;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.id.IdGenerator;
import cmc.mody.common.upload.PresignedUrlIssuer.PresignedUrlIssueResult;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UploadService {
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final IdGenerator idGenerator;
    private final PresignedUrlIssuer presignedUrlIssuer;

    public PresignedUrlResult createPresignedUrl(Long memberId, String domain, String fileName) {
        UploadDomain uploadDomain = UploadDomain.from(domain);
        String extension = extractExtension(fileName);
        String imageKey = createImageKey(memberId, uploadDomain, extension);
        PresignedUrlIssueResult issued = presignedUrlIssuer.issue(imageKey);
        return new PresignedUrlResult(issued.presignedUrl(), imageKey, issued.expiresInSeconds());
    }

    private String extractExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new GeneralException(ErrorStatus.UPLOAD_UNSUPPORTED_EXTENSION);
        }
        String normalized = fileName.substring(fileName.lastIndexOf('/') + 1);
        int dotIndex = normalized.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == normalized.length() - 1) {
            throw new GeneralException(ErrorStatus.UPLOAD_UNSUPPORTED_EXTENSION);
        }
        String extension = normalized.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new GeneralException(ErrorStatus.UPLOAD_UNSUPPORTED_EXTENSION);
        }
        return extension;
    }

    private String createImageKey(Long memberId, UploadDomain domain, String extension) {
        LocalDate now = LocalDate.now();
        return "%s/%d/%04d/%02d/%d.%s".formatted(
            domain.getDirectory(),
            memberId,
            now.getYear(),
            now.getMonthValue(),
            idGenerator.nextId(),
            extension
        );
    }

    public record PresignedUrlResult(
        String presignedUrl,
        String imageKey,
        long expiresInSeconds
    ) {
    }
}
