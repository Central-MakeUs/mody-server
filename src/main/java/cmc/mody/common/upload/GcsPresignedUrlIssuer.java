package cmc.mody.common.upload;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "upload", name = "provider", havingValue = "gcs")
public class GcsPresignedUrlIssuer implements PresignedUrlIssuer {
    private final Storage storage;
    private final UploadProperties uploadProperties;

    @Override
    public PresignedUrlIssueResult issue(String imageKey) {
        String bucket = uploadProperties.getGcpBucket();
        if (!StringUtils.hasText(bucket)) {
            throw new GeneralException(ErrorStatus.UPLOAD_STORAGE_CONFIG_INVALID);
        }

        BlobInfo blobInfo = BlobInfo.newBuilder(bucket, imageKey).build();
        URL signedUrl = signUrl(blobInfo);
        return new PresignedUrlIssueResult(
            signedUrl.toString(),
            uploadProperties.getPresignedUrlExpiresInSeconds()
        );
    }

    private URL signUrl(BlobInfo blobInfo) {
        try {
            return storage.signUrl(
                blobInfo,
                uploadProperties.getPresignedUrlExpiresInSeconds(),
                TimeUnit.SECONDS,
                Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
                Storage.SignUrlOption.withV4Signature()
            );
        } catch (RuntimeException e) {
            throw new GeneralException(ErrorStatus.UPLOAD_PRESIGNED_URL_ISSUE_FAILED);
        }
    }
}
