package cmc.mody.common.image;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ImageVariantTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 22, 12, 0);

    @Test
    @DisplayName("재시도 가능한 실패는 다음 시도 시각과 함께 PENDING으로 되돌린다.")
    void retryableFailureGoesBackToPending() {
        ImageVariant variant = new ImageVariant(1L, "records/1/2026/11/1.jpeg");
        variant.markProcessing("server", NOW);

        variant.markAttemptFailed("IOException: timeout", true, 3, NOW.plusMinutes(1));

        assertThat(variant.getVariantStatus()).isEqualTo(ImageVariantStatus.PENDING);
        assertThat(variant.getNextAttemptAt()).isEqualTo(NOW.plusMinutes(1));
        assertThat(variant.getAttemptCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("시도 횟수를 다 쓰면 FAILED로 둔다.")
    void failsAfterMaxAttempts() {
        ImageVariant variant = new ImageVariant(1L, "records/1/2026/11/1.jpeg");
        for (int i = 0; i < 3; i++) {
            variant.markProcessing("server", NOW);
        }

        variant.markAttemptFailed("IOException: timeout", true, 3, NOW.plusMinutes(1));

        assertThat(variant.getVariantStatus()).isEqualTo(ImageVariantStatus.FAILED);
        assertThat(variant.getNextAttemptAt()).isNull();
    }

    @Test
    @DisplayName("재시도해도 결과가 같은 실패는 바로 FAILED로 둔다.")
    void nonRetryableFailureFailsImmediately() {
        ImageVariant variant = new ImageVariant(1L, "records/1/2026/11/1.webp");
        variant.markProcessing("server", NOW);

        variant.markAttemptFailed("unsupported image format", false, 3, NOW.plusMinutes(1));

        assertThat(variant.getVariantStatus()).isEqualTo(ImageVariantStatus.FAILED);
    }

    @Test
    @DisplayName("완료하면 축소본 위치와 크기를 남기고 오류를 지운다.")
    void markReady() {
        ImageVariant variant = new ImageVariant(1L, "records/1/2026/11/1.jpeg");
        variant.markProcessing("server", NOW);
        variant.markAttemptFailed("IOException: timeout", true, 3, NOW.plusMinutes(1));
        variant.markProcessing("server", NOW.plusMinutes(1));

        variant.markReady("variants/w480/a.jpg", "variants/w1440/a.jpg", 2_500_000, 50_000, 300_000, NOW);

        assertThat(variant.isReady()).isTrue();
        assertThat(variant.getLastError()).isNull();
        assertThat(variant.getThumbnailBytes()).isEqualTo(50_000);
    }
}
