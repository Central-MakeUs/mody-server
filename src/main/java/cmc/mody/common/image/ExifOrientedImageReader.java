package cmc.mody.common.image;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * 이미지 바이트를 읽어 EXIF 방향을 적용한 BufferedImage로 만든다.
 * 휴대폰 사진은 픽셀은 가로로 저장하고 EXIF로 회전을 표시하는 경우가 많아, 방향을 적용하지 않으면 앱 화면과 다르게 보인다.
 */
public final class ExifOrientedImageReader {
    private ExifOrientedImageReader() {
    }

    /** 원본 크기로 읽는다. 읽을 수 없는 형식이면 null. */
    public static BufferedImage read(byte[] bytes) throws IOException {
        return read(bytes, 0);
    }

    /**
     * 긴 변이 targetLongEdge의 두 배 이상이면 디코딩 단계에서 픽셀을 건너뛰어(subsampling) 읽는다.
     * 4032px 사진을 480px로 줄일 때 원본 전체를 메모리에 올리지 않기 위해서다. targetLongEdge가 0 이하면 원본 크기.
     */
    public static BufferedImage read(byte[] bytes, int targetLongEdge) throws IOException {
        BufferedImage decoded = decode(bytes, targetLongEdge);
        if (decoded == null) {
            return null;
        }
        return applyOrientation(decoded, readOrientation(bytes));
    }

    private static BufferedImage decode(byte[] bytes, int targetLongEdge) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) {
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                ImageReadParam param = reader.getDefaultReadParam();
                int subsampling = subsampling(reader.getWidth(0), reader.getHeight(0), targetLongEdge);
                if (subsampling > 1) {
                    param.setSourceSubsampling(subsampling, subsampling, 0, 0);
                }
                return reader.read(0, param);
            } finally {
                reader.dispose();
            }
        }
    }

    static int subsampling(int width, int height, int targetLongEdge) {
        if (targetLongEdge <= 0) {
            return 1;
        }
        // 최종 크기의 2배 이상 해상도는 남겨 두고, 그 위로는 건너뛴다. 남은 축소는 보간으로 처리해 화질을 지킨다.
        return Math.max(1, Math.max(width, height) / (targetLongEdge * 2));
    }

    static int readOrientation(byte[] bytes) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(bytes));
            ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            Integer orientation = directory == null ? null : directory.getInteger(ExifIFD0Directory.TAG_ORIENTATION);
            return orientation == null ? 1 : orientation;
        } catch (ImageProcessingException | IOException e) {
            return 1;
        }
    }

    public static BufferedImage applyOrientation(BufferedImage source, int orientation) {
        if (orientation < 2 || orientation > 8) {
            return source;
        }
        boolean swapsDimensions = orientation >= 5;
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        BufferedImage oriented = new BufferedImage(
            swapsDimensions ? sourceHeight : sourceWidth,
            swapsDimensions ? sourceWidth : sourceHeight,
            source.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB
        );
        Graphics2D graphics = oriented.createGraphics();
        try {
            graphics.drawImage(source, orientationTransform(orientation, sourceWidth, sourceHeight), null);
        } finally {
            graphics.dispose();
        }
        return oriented;
    }

    private static AffineTransform orientationTransform(int orientation, int width, int height) {
        AffineTransform transform = new AffineTransform();
        switch (orientation) {
            case 2 -> {
                transform.translate(width, 0);
                transform.scale(-1, 1);
            }
            case 3 -> {
                transform.translate(width, height);
                transform.rotate(Math.PI);
            }
            case 4 -> {
                transform.translate(0, height);
                transform.scale(1, -1);
            }
            case 5 -> {
                transform.rotate(Math.PI / 2);
                transform.scale(1, -1);
            }
            case 6 -> {
                transform.translate(height, 0);
                transform.rotate(Math.PI / 2);
            }
            case 7 -> {
                transform.translate(height, width);
                transform.rotate(-Math.PI / 2);
                transform.scale(1, -1);
            }
            case 8 -> {
                transform.translate(0, width);
                transform.rotate(-Math.PI / 2);
            }
            default -> {
            }
        }
        return transform;
    }
}
