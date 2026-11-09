package cmc.mody.record.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import cmc.mody.record.application.ActivityRecordService.ImageCropRegionCommand;
import cmc.mody.record.presentation.ActivityRecordController.ImageCropRegionRequest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ImageCropRegionRequestTest {

    @Test
    void 경계를_살짝_넘는_관심_영역은_허용하고_이미지_경계에_맞춘다() {
        ImageCropRegionRequest request = new ImageCropRegionRequest(
            new BigDecimal("0.00000000000000010"),
            new BigDecimal("0.3"),
            BigDecimal.ONE,
            new BigDecimal("0.70000000000000004")
        );

        ImageCropRegionCommand command = request.toCommand();

        assertThat(request.isRegionInsideImage()).isTrue();
        assertThat(command.x().add(command.width())).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(command.y().add(command.height())).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void 허용_오차보다_크게_벗어난_관심_영역은_거절한다() {
        ImageCropRegionRequest request = new ImageCropRegionRequest(
            new BigDecimal("0.5"),
            new BigDecimal("0.1"),
            new BigDecimal("0.6"),
            new BigDecimal("0.5")
        );

        assertThat(request.isRegionInsideImage()).isFalse();
    }

    @Test
    void 시작_좌표가_이미지_끝이면_거절한다() {
        ImageCropRegionRequest request = new ImageCropRegionRequest(
            BigDecimal.ONE,
            new BigDecimal("0.1"),
            new BigDecimal("0.005"),
            new BigDecimal("0.5")
        );

        assertThat(request.isRegionInsideImage()).isFalse();
    }

    @Test
    void 경계_안의_관심_영역은_그대로_저장한다() {
        ImageCropRegionRequest request = new ImageCropRegionRequest(
            new BigDecimal("0.1"),
            new BigDecimal("0.2"),
            new BigDecimal("0.5"),
            new BigDecimal("0.6")
        );

        ImageCropRegionCommand command = request.toCommand();

        assertThat(command.width()).isEqualByComparingTo("0.5");
        assertThat(command.height()).isEqualByComparingTo("0.6");
    }
}
