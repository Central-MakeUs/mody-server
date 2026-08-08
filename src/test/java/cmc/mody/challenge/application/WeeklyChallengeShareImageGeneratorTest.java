package cmc.mody.challenge.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class WeeklyChallengeShareImageGeneratorTest {
    private final WeeklyChallengeShareImageGenerator generator = new WeeklyChallengeShareImageGenerator();

    @Test
    void calculateGridSizeUsesTwoColumnsForEightMembers() {
        WeeklyChallengeShareImageGenerator.GridSize gridSize = generator.calculateGridSize(8);

        assertThat(gridSize.rows()).isEqualTo(4);
        assertThat(gridSize.columns()).isEqualTo(2);
    }

    @Test
    void generateCreatesHighResolutionCollageImage() throws IOException {
        List<WeeklyChallengeShareImageGenerator.ShareImageSource> sources = List.of(
            source(Color.RED, "모나"),
            source(Color.ORANGE, "키드"),
            source(Color.YELLOW, "도모"),
            source(Color.GREEN, "마커스"),
            source(Color.CYAN, "리아"),
            source(Color.BLUE, "하루"),
            source(Color.MAGENTA, "제이"),
            source(Color.PINK, "민")
        );

        byte[] bytes = generator.generate(
            "엘레베이터 안 타고 집가기",
            "집까지 계단으로 이동한 사진을 모두 인증했어요!",
            sources,
            generator.calculateGridSize(sources.size())
        );

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));

        assertThat(image.getWidth()).isEqualTo(1206);
        assertThat(image.getHeight()).isEqualTo(2622);
    }

    private WeeklyChallengeShareImageGenerator.ShareImageSource source(Color color, String nickname) throws IOException {
        return new WeeklyChallengeShareImageGenerator.ShareImageSource(imageBytes(color), null, nickname, null);
    }

    private byte[] imageBytes(Color color) throws IOException {
        BufferedImage image = new BufferedImage(320, 320, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(color);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        } finally {
            graphics.dispose();
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", output);
        return output.toByteArray();
    }
}
