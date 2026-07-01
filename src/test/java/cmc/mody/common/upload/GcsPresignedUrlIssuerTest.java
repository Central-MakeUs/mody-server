package cmc.mody.common.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GcsPresignedUrlIssuerTest {
    @Mock
    private Storage storage;

    @Test
    @DisplayName("GCS bucket 설정이 없으면 발급할 수 없다.")
    void throwStorageConfigInvalidWhenBucketBlank() {
        UploadProperties properties = new UploadProperties();
        properties.setProvider("gcs");
        properties.setGcpBucket("");

        GcsPresignedUrlIssuer issuer = new GcsPresignedUrlIssuer(storage, properties);

        assertThatThrownBy(() -> issuer.issue("records/1/2026/06/123.jpg"))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getStatus()).isEqualTo(ErrorStatus.UPLOAD_STORAGE_CONFIG_INVALID));
    }

    @Test
    @DisplayName("GCS signed URL 발급 실패를 업로드 예외로 변환한다.")
    void throwIssueFailedWhenStorageFails() {
        UploadProperties properties = properties();
        given(storage.signUrl(
            any(BlobInfo.class),
            eq(300L),
            eq(TimeUnit.SECONDS),
            any(Storage.SignUrlOption.class),
            any(Storage.SignUrlOption.class)
        )).willThrow(new IllegalStateException("failed"));

        GcsPresignedUrlIssuer issuer = new GcsPresignedUrlIssuer(storage, properties);

        assertThatThrownBy(() -> issuer.issue("records/1/2026/06/123.jpg"))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getStatus()).isEqualTo(ErrorStatus.UPLOAD_PRESIGNED_URL_ISSUE_FAILED));
    }

    @Test
    @DisplayName("GCS signed URL을 발급한다.")
    void issueSignedUrl() throws Exception {
        UploadProperties properties = properties();
        URL signedUrl = URI.create("https://storage.googleapis.com/mody/records/1/image.jpg").toURL();
        given(storage.signUrl(
            any(BlobInfo.class),
            eq(300L),
            eq(TimeUnit.SECONDS),
            any(Storage.SignUrlOption.class),
            any(Storage.SignUrlOption.class)
        )).willReturn(signedUrl);

        GcsPresignedUrlIssuer issuer = new GcsPresignedUrlIssuer(storage, properties);

        PresignedUrlIssuer.PresignedUrlIssueResult result = issuer.issue("records/1/image.jpg");

        assertThat(result.presignedUrl()).isEqualTo(signedUrl.toString());
        assertThat(result.expiresInSeconds()).isEqualTo(300L);
    }

    private UploadProperties properties() {
        UploadProperties properties = new UploadProperties();
        properties.setProvider("gcs");
        properties.setGcpBucket("mody");
        properties.setPresignedUrlExpiresInSeconds(300L);
        return properties;
    }
}
