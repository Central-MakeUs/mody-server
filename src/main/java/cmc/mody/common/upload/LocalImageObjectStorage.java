package cmc.mody.common.upload;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "upload", name = "provider", havingValue = "local", matchIfMissing = true)
public class LocalImageObjectStorage implements ImageObjectStorage {
    private final UploadProperties uploadProperties;
    private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

    @Override
    public boolean exists(String imageKey) {
        return objects.containsKey(imageKey);
    }

    @Override
    public byte[] read(String imageKey) {
        byte[] bytes = objects.get(imageKey);
        if (bytes == null) {
            throw new GeneralException(ErrorStatus.UPLOAD_STORAGE_OPERATION_FAILED);
        }
        return bytes;
    }

    @Override
    public void write(String imageKey, byte[] bytes, String contentType) {
        objects.put(imageKey, bytes);
    }

    @Override
    public String toUrl(String imageKey) {
        String baseUrl = uploadProperties.getBaseUrl();
        if (baseUrl.endsWith("/")) {
            return baseUrl + imageKey;
        }
        return baseUrl + "/" + imageKey;
    }
}
