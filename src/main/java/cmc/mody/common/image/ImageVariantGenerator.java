package cmc.mody.common.image;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 원본 사진에서 목록용·상세용 JPEG를 만든다.
 * 자르기(crop)는 하지 않고 비율을 유지한 채 줄이기만 한다. 앱이 저장된 crop 비율 좌표를 그대로 적용할 수 있게 하기 위해서다.
 */
@Component
@RequiredArgsConstructor
public class ImageVariantGenerator {
    private final ImageVariantProperties properties;

    public GeneratedVariants generate(byte[] original) throws IOException {
        int largest = Math.max(properties.getThumbnailMaxSize(), properties.getDisplayMaxSize());
        BufferedImage source = ExifOrientedImageReader.read(original, largest);
        if (source == null) {
            throw new UnsupportedImageFormatException();
        }
        BufferedImage display = resize(source, properties.getDisplayMaxSize());
        // 목록용은 이미 줄인 상세용에서 다시 줄인다. 원본에서 한 번에 크게 줄이면 계단 현상이 생기기 쉽다.
        BufferedImage thumbnail = resize(display, properties.getThumbnailMaxSize());
        return new GeneratedVariants(
            encodeJpeg(thumbnail),
            encodeJpeg(display),
            display.getWidth(),
            display.getHeight()
        );
    }

    static BufferedImage resize(BufferedImage source, int maxLongEdge) {
        int width = source.getWidth();
        int height = source.getHeight();
        double scale = Math.min(1.0, (double) maxLongEdge / Math.max(width, height));
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));

        BufferedImage current = toOpaqueRgb(source);
        // 한 번에 크게 줄이지 않고 절반씩 줄여 bilinear 보간의 품질을 확보한다.
        while (current.getWidth() / 2 >= targetWidth && current.getHeight() / 2 >= targetHeight) {
            current = draw(current, current.getWidth() / 2, current.getHeight() / 2);
        }
        if (current.getWidth() != targetWidth || current.getHeight() != targetHeight) {
            current = draw(current, targetWidth, targetHeight);
        }
        return current;
    }

    private static BufferedImage toOpaqueRgb(BufferedImage source) {
        if (!source.getColorModel().hasAlpha()) {
            return source;
        }
        // JPEG는 투명도를 담지 못한다. PNG 투명 영역은 흰 배경으로 채운다.
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return rgb;
    }

    private static BufferedImage draw(BufferedImage source, int width, int height) {
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    private byte[] encodeJpeg(BufferedImage image) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("jpeg writer not available");
        }
        ImageWriter writer = writers.next();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(stream);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(properties.getJpegQuality());
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return output.toByteArray();
    }

    public record GeneratedVariants(byte[] thumbnail, byte[] display, int displayWidth, int displayHeight) {
    }

    /** ImageIO가 읽지 못하는 형식(예: WebP). 재시도해도 결과가 같으므로 바로 실패 처리한다. */
    public static class UnsupportedImageFormatException extends IOException {
        public UnsupportedImageFormatException() {
            super("unsupported image format");
        }
    }
}
