package cmc.mody.common.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cmc.mody.common.image.ImageVariantGenerator.GeneratedVariants;
import cmc.mody.common.image.ImageVariantGenerator.UnsupportedImageFormatException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ImageVariantGeneratorTest {
    private final ImageVariantGenerator generator = new ImageVariantGenerator(new ImageVariantProperties());

    @Test
    @DisplayName("긴 변 기준으로 목록용 480px, 상세용 1440px JPEG를 비율을 유지해 만든다.")
    void generateKeepsAspectRatio() throws IOException {
        byte[] original = encode(image(4032, 3024, BufferedImage.TYPE_INT_RGB), "jpg");

        GeneratedVariants variants = generator.generate(original);

        BufferedImage thumbnail = decode(variants.thumbnail());
        BufferedImage display = decode(variants.display());
        assertThat(thumbnail.getWidth()).isEqualTo(480);
        assertThat(thumbnail.getHeight()).isEqualTo(360);
        assertThat(display.getWidth()).isEqualTo(1440);
        assertThat(display.getHeight()).isEqualTo(1080);
        assertThat(variants.thumbnail().length).isLessThan(original.length);
    }

    @Test
    @DisplayName("원본이 기준보다 작으면 키우지 않는다.")
    void doesNotUpscale() throws IOException {
        GeneratedVariants variants = generator.generate(encode(image(300, 600, BufferedImage.TYPE_INT_RGB), "jpg"));

        assertThat(decode(variants.display()).getWidth()).isEqualTo(300);
        assertThat(decode(variants.display()).getHeight()).isEqualTo(600);
        assertThat(decode(variants.thumbnail()).getHeight()).isEqualTo(480);
    }

    @Test
    @DisplayName("투명 PNG는 흰 배경 JPEG로 만든다.")
    void flattensTransparentPng() throws IOException {
        BufferedImage transparent = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB);

        GeneratedVariants variants = generator.generate(encode(transparent, "png"));

        BufferedImage thumbnail = decode(variants.thumbnail());
        Color center = new Color(thumbnail.getRGB(100, 50));
        assertThat(center.getRed()).isGreaterThan(240);
        assertThat(center.getGreen()).isGreaterThan(240);
        assertThat(center.getBlue()).isGreaterThan(240);
    }

    @Test
    @DisplayName("읽을 수 없는 형식은 재시도하지 않을 실패로 구분한다.")
    void rejectsUnsupportedFormat() {
        assertThatThrownBy(() -> generator.generate("not an image".getBytes()))
            .isInstanceOf(UnsupportedImageFormatException.class);
    }

    @Test
    @DisplayName("EXIF 방향 6(시계 방향 90도)이면 가로·세로를 바꿔 세운다.")
    void appliesExifOrientation() {
        BufferedImage landscape = image(400, 200, BufferedImage.TYPE_INT_RGB);

        BufferedImage oriented = ExifOrientedImageReader.applyOrientation(landscape, 6);

        assertThat(oriented.getWidth()).isEqualTo(200);
        assertThat(oriented.getHeight()).isEqualTo(400);
    }

    @Test
    @DisplayName("최종 크기의 2배 이상 해상도는 남기고 디코딩 단계에서 건너뛴다.")
    void subsamplingKeepsTwiceTargetResolution() {
        assertThat(ExifOrientedImageReader.subsampling(4032, 3024, 1440)).isEqualTo(1);
        assertThat(ExifOrientedImageReader.subsampling(6000, 4000, 1440)).isEqualTo(2);
        assertThat(ExifOrientedImageReader.subsampling(4032, 3024, 480)).isEqualTo(4);
        assertThat(ExifOrientedImageReader.subsampling(4032, 3024, 0)).isEqualTo(1);
    }

    private static BufferedImage image(int width, int height, int type) {
        BufferedImage image = new BufferedImage(width, height, type);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(30, 120, 200));
            graphics.fillRect(0, 0, width, height);
            graphics.setColor(Color.ORANGE);
            graphics.fillOval(width / 4, height / 4, width / 2, height / 2);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static byte[] encode(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }

    private static BufferedImage decode(byte[] bytes) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }
}
