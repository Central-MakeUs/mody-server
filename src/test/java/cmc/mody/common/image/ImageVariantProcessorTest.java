package cmc.mody.common.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import cmc.mody.common.image.ImageVariantGenerator.GeneratedVariants;
import cmc.mody.common.image.ImageVariantGenerator.UnsupportedImageFormatException;
import cmc.mody.common.upload.ImageObjectStorage;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class ImageVariantProcessorTest {
    private static final String IMAGE_KEY = "records/1/2026/11/100.jpeg";

    @Mock
    private ImageVariantRepository imageVariantRepository;
    @Mock
    private ImageObjectStorage imageObjectStorage;
    @Mock
    private ImageVariantGenerator imageVariantGenerator;
    @Mock
    private PlatformTransactionManager transactionManager;

    private ImageVariantProcessor processor;
    private ImageVariant variant;

    @BeforeEach
    void setUp() {
        given(transactionManager.getTransaction(any())).willReturn(new SimpleTransactionStatus());
        processor = new ImageVariantProcessor(
            imageVariantRepository,
            imageObjectStorage,
            imageVariantGenerator,
            new ImageVariantProperties(),
            new TransactionTemplate(transactionManager)
        );
        variant = new ImageVariant(10L, IMAGE_KEY);
        variant.markProcessing("server", LocalDateTime.now());
        given(imageVariantRepository.findById(10L)).willReturn(Optional.of(variant));
    }

    @Test
    @DisplayName("원본을 읽어 두 축소본을 원본 키에서 정해진 위치에 쓰고 READY로 둔다.")
    void processWritesVariants() throws IOException {
        byte[] original = new byte[2_000];
        given(imageObjectStorage.read(IMAGE_KEY)).willReturn(original);
        given(imageVariantGenerator.generate(original))
            .willReturn(new GeneratedVariants(new byte[50], new byte[300], 1440, 1080));

        processor.process(10L);

        then(imageObjectStorage).should().write(eq("variants/w480/records/1/2026/11/100.jpg"), any(), eq("image/jpeg"));
        then(imageObjectStorage).should().write(eq("variants/w1440/records/1/2026/11/100.jpg"), any(), eq("image/jpeg"));
        assertThat(variant.isReady()).isTrue();
        assertThat(variant.getOriginalBytes()).isEqualTo(2_000);
        assertThat(variant.getThumbnailBytes()).isEqualTo(50);
    }

    @Test
    @DisplayName("저장소 오류는 재시도 대상으로 PENDING에 되돌린다.")
    void storageFailureIsRetried() {
        given(imageObjectStorage.read(IMAGE_KEY)).willThrow(new RuntimeException("gcs timeout"));

        processor.process(10L);

        assertThat(variant.getVariantStatus()).isEqualTo(ImageVariantStatus.PENDING);
        assertThat(variant.getLastError()).contains("gcs timeout");
    }

    @Test
    @DisplayName("읽을 수 없는 형식은 바로 FAILED로 두고 아무것도 쓰지 않는다.")
    void unsupportedFormatFails() throws IOException {
        byte[] original = new byte[10];
        given(imageObjectStorage.read(IMAGE_KEY)).willReturn(original);
        given(imageVariantGenerator.generate(original)).willThrow(new UnsupportedImageFormatException());

        processor.process(10L);

        assertThat(variant.getVariantStatus()).isEqualTo(ImageVariantStatus.FAILED);
        then(imageObjectStorage).should(never()).write(any(), any(), any());
    }
}
