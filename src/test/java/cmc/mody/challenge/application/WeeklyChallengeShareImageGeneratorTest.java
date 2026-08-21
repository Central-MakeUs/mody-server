package cmc.mody.challenge.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
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
        assertDominantColor(image.getRGB(100 * 3, 220 * 3), Color.RED);
        Color background = new Color(image.getRGB(200 * 3, 115 * 3));
        assertThat(background.getRed()).isLessThan(30);
        assertThat(background.getGreen()).isLessThan(30);
        assertThat(background.getBlue()).isLessThan(30);
    }

    @Test
    void readImageAppliesExifOrientationBeforeCropping() throws IOException {
        BufferedImage image = quadrantImage();
        Map<Integer, List<Color>> expectedCornersByOrientation = Map.of(
            2, List.of(Color.GREEN, Color.RED, Color.YELLOW, Color.BLUE),
            3, List.of(Color.YELLOW, Color.BLUE, Color.GREEN, Color.RED),
            4, List.of(Color.BLUE, Color.YELLOW, Color.RED, Color.GREEN),
            5, List.of(Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW),
            6, List.of(Color.BLUE, Color.RED, Color.YELLOW, Color.GREEN),
            7, List.of(Color.YELLOW, Color.GREEN, Color.BLUE, Color.RED),
            8, List.of(Color.GREEN, Color.YELLOW, Color.RED, Color.BLUE)
        );

        for (Map.Entry<Integer, List<Color>> expected : expectedCornersByOrientation.entrySet()) {
            BufferedImage oriented = generator.readImage(jpegWithExifOrientation(image, expected.getKey()));

            assertThat(oriented.getWidth()).isEqualTo(expected.getKey() >= 5 ? 40 : 80);
            assertThat(oriented.getHeight()).isEqualTo(expected.getKey() >= 5 ? 80 : 40);
            assertCorners(oriented, expected.getValue());
        }
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

    private byte[] jpegWithExifOrientation(BufferedImage image, int orientation) throws IOException {
        ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", jpeg);
        byte[] source = jpeg.toByteArray();
        byte[] exifSegment = {
            (byte) 0xFF, (byte) 0xE1, 0x00, 0x22,
            'E', 'x', 'i', 'f', 0x00, 0x00,
            'M', 'M', 0x00, 0x2A, 0x00, 0x00, 0x00, 0x08,
            0x00, 0x01,
            0x01, 0x12, 0x00, 0x03, 0x00, 0x00, 0x00, 0x01,
            0x00, (byte) orientation, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00
        };
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(source, 0, 2);
        output.write(exifSegment);
        output.write(source, 2, source.length - 2);
        return output.toByteArray();
    }

    private BufferedImage quadrantImage() {
        BufferedImage image = new BufferedImage(80, 40, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.RED);
            graphics.fillRect(0, 0, 40, 20);
            graphics.setColor(Color.GREEN);
            graphics.fillRect(40, 0, 40, 20);
            graphics.setColor(Color.BLUE);
            graphics.fillRect(0, 20, 40, 20);
            graphics.setColor(Color.YELLOW);
            graphics.fillRect(40, 20, 40, 20);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private void assertCorners(BufferedImage image, List<Color> expectedCorners) {
        int right = image.getWidth() - 6;
        int bottom = image.getHeight() - 6;
        assertDominantColor(image.getRGB(5, 5), expectedCorners.get(0));
        assertDominantColor(image.getRGB(right, 5), expectedCorners.get(1));
        assertDominantColor(image.getRGB(5, bottom), expectedCorners.get(2));
        assertDominantColor(image.getRGB(right, bottom), expectedCorners.get(3));
    }

    private void assertDominantColor(int rgb, Color expected) {
        Color actual = new Color(rgb);
        if (expected == Color.RED) {
            assertThat(actual.getRed()).isGreaterThan(180);
        } else if (expected == Color.GREEN) {
            assertThat(actual.getGreen()).isGreaterThan(100);
        } else if (expected == Color.BLUE) {
            assertThat(actual.getBlue()).isGreaterThan(180);
        } else {
            assertThat(actual.getRed()).isGreaterThan(180);
            assertThat(actual.getGreen()).isGreaterThan(180);
        }
    }
}
