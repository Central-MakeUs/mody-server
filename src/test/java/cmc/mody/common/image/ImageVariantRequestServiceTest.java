package cmc.mody.common.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import cmc.mody.common.id.IdGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImageVariantRequestServiceTest {
    @Mock
    private IdGenerator idGenerator;
    @Mock
    private ImageVariantRepository imageVariantRepository;
    @InjectMocks
    private ImageVariantRequestService service;

    @Test
    @DisplayName("처음 보는 사진이면 PENDING 작업을 만든다.")
    void requestCreatesPendingVariant() {
        given(imageVariantRepository.existsByImageKey("records/1/2026/11/1.jpeg")).willReturn(false);
        given(idGenerator.nextId()).willReturn(5L);

        boolean requested = service.request("records/1/2026/11/1.jpeg");

        assertThat(requested).isTrue();
        then(imageVariantRepository).should().saveAndFlush(any(ImageVariant.class));
    }

    @Test
    @DisplayName("이미 요청된 사진이나 외부 URL은 작업을 만들지 않는다.")
    void skipsExistingOrExternal() {
        given(imageVariantRepository.existsByImageKey("records/1/2026/11/1.jpeg")).willReturn(true);

        assertThat(service.request("records/1/2026/11/1.jpeg")).isFalse();
        assertThat(service.request("https://example.com/a.jpg")).isFalse();
        assertThat(service.request(" ")).isFalse();
        then(imageVariantRepository).should(never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("작업 생성이 실패해도 예외를 밖으로 던지지 않는다. 기록 저장은 이미 커밋됐다.")
    void handleSwallowsFailure() {
        given(imageVariantRepository.existsByImageKey(any())).willThrow(new RuntimeException("db down"));

        assertThatCode(() -> service.handle(new ImageVariantRequestedEvent("records/1/2026/11/1.jpeg")))
            .doesNotThrowAnyException();
    }
}
