package cmc.mody.common.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.id.IdGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UploadServiceTest {
    @Mock
    private IdGenerator idGenerator;

    @Test
    @DisplayName("업로드 도메인과 파일명으로 image key와 presigned URL을 발급한다.")
    void createPresignedUrl() {
        given(idGenerator.nextId()).willReturn(123L);
        UploadService service = new UploadService(idGenerator, imageKey ->
            new PresignedUrlIssuer.PresignedUrlIssueResult("https://storage.example.com/" + imageKey, 300L));

        UploadService.PresignedUrlResult result = service.createPresignedUrl(1L, "record", "meal.JPG");

        assertThat(result.imageKey()).startsWith("records/1/");
        assertThat(result.imageKey()).endsWith("/123.jpg");
        assertThat(result.presignedUrl()).endsWith(result.imageKey());
        assertThat(result.expiresInSeconds()).isEqualTo(300L);
    }

    @Test
    @DisplayName("지원하지 않는 업로드 도메인은 예외 처리한다.")
    void throwUnsupportedUploadDomain() {
        UploadService service = new UploadService(idGenerator, imageKey ->
            new PresignedUrlIssuer.PresignedUrlIssueResult("https://storage.example.com/" + imageKey, 300L));

        assertThatThrownBy(() -> service.createPresignedUrl(1L, "unknown", "meal.jpg"))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getStatus()).isEqualTo(ErrorStatus.UPLOAD_UNSUPPORTED_DOMAIN));
    }

    @Test
    @DisplayName("지원하지 않는 파일 확장자는 예외 처리한다.")
    void throwUnsupportedFileExtension() {
        UploadService service = new UploadService(idGenerator, imageKey ->
            new PresignedUrlIssuer.PresignedUrlIssueResult("https://storage.example.com/" + imageKey, 300L));

        assertThatThrownBy(() -> service.createPresignedUrl(1L, "record", "meal.gif"))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getStatus()).isEqualTo(ErrorStatus.UPLOAD_UNSUPPORTED_EXTENSION));
    }
}
