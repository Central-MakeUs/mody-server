package cmc.mody.common.upload;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "upload", name = "provider", havingValue = "gcs")
public class GcsImageObjectStorage implements ImageObjectStorage {
    private final Storage storage;
    private final UploadProperties uploadProperties;

    @Override
    public boolean exists(String imageKey) {
        try {
            return storage.get(blobId(imageKey)) != null;
        } catch (RuntimeException e) {
            throw new GeneralException(ErrorStatus.UPLOAD_STORAGE_OPERATION_FAILED);
        }
    }

    @Override
    public byte[] read(String imageKey) {
        try {
            return storage.readAllBytes(blobId(imageKey));
        } catch (RuntimeException e) {
            throw new GeneralException(ErrorStatus.UPLOAD_STORAGE_OPERATION_FAILED);
        }
    }

    @Override
    public void write(String imageKey, byte[] bytes, String contentType) {
        try {
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId(imageKey))
                .setContentType(contentType)
                .build();
            storage.create(blobInfo, bytes);
        } catch (RuntimeException e) {
            throw new GeneralException(ErrorStatus.UPLOAD_STORAGE_OPERATION_FAILED);
        }
    }

    @Override
    public String toUrl(String imageKey) {
        String baseUrl = uploadProperties.getBaseUrl();
        if (baseUrl.endsWith("/")) {
            return baseUrl + imageKey;
        }
        return baseUrl + "/" + imageKey;
    }

    private BlobId blobId(String imageKey) {
        String bucket = uploadProperties.getGcpBucket();
        if (!StringUtils.hasText(bucket)) {
            throw new GeneralException(ErrorStatus.UPLOAD_STORAGE_CONFIG_INVALID);
        }
        return BlobId.of(bucket, imageKey);
    }
}
