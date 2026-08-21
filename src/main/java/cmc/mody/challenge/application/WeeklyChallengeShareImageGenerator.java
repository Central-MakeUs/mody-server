package cmc.mody.challenge.application;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.springframework.stereotype.Component;

@Component
public class WeeklyChallengeShareImageGenerator {
    private static final int SCALE = 3;
    private static final int CANVAS_WIDTH = scaled(402);
    private static final int CANVAS_HEIGHT = scaled(874);
    private static final int TITLE_CENTER_X = CANVAS_WIDTH / 2;
    private static final int TITLE_TOP = scaled(88);
    private static final int TITLE_LINE_HEIGHT = scaled(24);
    private static final int TITLE_MAX_WIDTH = scaled(354);
    private static final int GRID_X = scaled(24);
    private static final int GRID_Y = scaled(125);
    private static final int CELL_SIZE = scaled(172);
    private static final int CELL_GAP = scaled(10);
    private static final int CELL_RADIUS = scaled(12);
    private static final int AVATAR_SIZE = scaled(30);
    private static final int AVATAR_MARGIN = scaled(16);
    private static final int PARTICIPANT_GAP = scaled(4);
    private static final int TITLE_FONT_SIZE = scaled(16);
    private static final int NICKNAME_FONT_SIZE = scaled(14);
    private static final String IMAGE_FORMAT = "jpg";
    private static final String FONT_PATH = "/fonts/";
    private static final String TEMPLATE_PATH = "/images/weekly-challenge-share-template.png";
    private static final Color BACKGROUND_COLOR = new Color(17, 17, 17);
    private static final Color TITLE_COLOR = Color.WHITE;
    private static final Color FALLBACK_AVATAR_COLOR = new Color(255, 231, 86);
    private static final Color AVATAR_BORDER_COLOR = new Color(246, 246, 246);
    private static final Font TITLE_FONT = loadFont("Pretendard-SemiBold.otf", Font.BOLD);
    private static final Font MEDIUM_FONT = loadFont("Pretendard-Medium.otf", Font.PLAIN);
    private static final BufferedImage TEMPLATE_IMAGE = loadTemplateImage();

    private static int scaled(int value) {
        return value * SCALE;
    }

    public byte[] generate(String title, String description, List<ShareImageSource> sources, GridSize gridSize) {
        BufferedImage canvas = new BufferedImage(
            CANVAS_WIDTH,
            CANVAS_HEIGHT,
            BufferedImage.TYPE_INT_RGB
        );
        Graphics2D graphics = canvas.createGraphics();
        try {
            configure(graphics);
            drawBackground(graphics);
            drawTitle(graphics, title);
            for (int index = 0; index < sources.size(); index++) {
                drawTile(graphics, sources.get(index), gridSize, index);
            }
            return toJpeg(canvas);
        } finally {
            graphics.dispose();
        }
    }

    public GridSize calculateGridSize(int imageCount) {
        if (imageCount <= 0) {
            return new GridSize(0, 0);
        }
        if (imageCount == 1) {
            return new GridSize(1, 1);
        }
        if (imageCount <= 8) {
            return new GridSize(Math.min(4, (int) Math.ceil(imageCount / 2.0)), 2);
        }
        int columns = (int) Math.ceil(Math.sqrt(imageCount));
        int rows = (int) Math.ceil((double) imageCount / columns);
        return new GridSize(rows, columns);
    }

    private void configure(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    private void drawBackground(Graphics2D graphics) {
        graphics.setColor(BACKGROUND_COLOR);
        graphics.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
        graphics.drawImage(TEMPLATE_IMAGE, 0, 0, CANVAS_WIDTH, CANVAS_HEIGHT, null);
    }

    private void drawTitle(Graphics2D graphics, String title) {
        graphics.setColor(TITLE_COLOR);
        graphics.setFont(TITLE_FONT.deriveFont((float) TITLE_FONT_SIZE));
        String text = trimText(graphics, (title == null ? "" : title) + " 챌린지 완료!", TITLE_MAX_WIDTH);
        FontMetrics metrics = graphics.getFontMetrics();
        int textX = TITLE_CENTER_X - metrics.stringWidth(text) / 2;
        graphics.drawString(text, textX, textBaseline(graphics, TITLE_TOP, TITLE_LINE_HEIGHT));
    }

    private void drawTile(Graphics2D graphics, ShareImageSource source, GridSize gridSize, int index) {
        int row = index / gridSize.columns();
        int column = index % gridSize.columns();
        int x = GRID_X + column * (CELL_SIZE + CELL_GAP);
        int y = GRID_Y + row * (CELL_SIZE + CELL_GAP);
        Shape previousClip = graphics.getClip();
        RoundRectangle2D tile = new RoundRectangle2D.Double(x, y, CELL_SIZE, CELL_SIZE, CELL_RADIUS, CELL_RADIUS);
        graphics.setClip(tile);
        graphics.drawImage(
            crop(sourceImage(source), source.cropRegion()),
            x,
            y,
            CELL_SIZE,
            CELL_SIZE,
            null
        );
        graphics.setClip(previousClip);

        drawTopScrim(graphics, x, y);
        drawParticipant(graphics, source, x, y);
    }

    private BufferedImage sourceImage(ShareImageSource source) {
        return readImage(source.imageBytes());
    }

    private void drawTopScrim(Graphics2D graphics, int x, int y) {
        Shape previousClip = graphics.getClip();
        RoundRectangle2D tile = new RoundRectangle2D.Double(x, y, CELL_SIZE, CELL_SIZE, CELL_RADIUS, CELL_RADIUS);
        graphics.setClip(tile);
        graphics.setPaint(new GradientPaint(x, y, new Color(0, 0, 0, 153), x, y + scaled(90), new Color(0, 0, 0, 0)));
        graphics.fillRect(x, y, CELL_SIZE, scaled(90));
        graphics.setClip(previousClip);
    }

    private void drawParticipant(Graphics2D graphics, ShareImageSource source, int x, int y) {
        int avatarX = x + AVATAR_MARGIN;
        int avatarY = y + AVATAR_MARGIN;
        drawAvatar(graphics, source, avatarX, avatarY);

        graphics.setColor(Color.WHITE);
        graphics.setFont(MEDIUM_FONT.deriveFont((float) NICKNAME_FONT_SIZE));
        String nickname = trimText(
            graphics,
            source.nickname(),
            CELL_SIZE - AVATAR_MARGIN * 2 - AVATAR_SIZE - PARTICIPANT_GAP
        );
        graphics.drawString(
            nickname,
            avatarX + AVATAR_SIZE + PARTICIPANT_GAP,
            textBaseline(graphics, avatarY + scaled(5), scaled(20))
        );
    }

    private void drawAvatar(Graphics2D graphics, ShareImageSource source, int x, int y) {
        Ellipse2D avatar = new Ellipse2D.Double(x, y, AVATAR_SIZE, AVATAR_SIZE);
        Shape previousClip = graphics.getClip();
        BufferedImage profileImage = source.profileImageBytes() == null ? null : readImage(source.profileImageBytes());
        if (profileImage != null) {
            graphics.setClip(avatar);
            graphics.drawImage(cropSquare(profileImage), x, y, AVATAR_SIZE, AVATAR_SIZE, null);
            graphics.setClip(previousClip);
        } else {
            graphics.setColor(FALLBACK_AVATAR_COLOR);
            graphics.fill(avatar);
            graphics.setColor(Color.BLACK);
            graphics.setFont(TITLE_FONT.deriveFont((float) scaled(13)));
            String initial = initial(source.nickname());
            FontMetrics metrics = graphics.getFontMetrics();
            graphics.drawString(
                initial,
                x + (AVATAR_SIZE - metrics.stringWidth(initial)) / 2,
                y + (AVATAR_SIZE + metrics.getAscent()) / 2 - 4
            );
        }
        graphics.setColor(AVATAR_BORDER_COLOR);
        Stroke previousStroke = graphics.getStroke();
        graphics.setStroke(new BasicStroke(0.818f * SCALE));
        graphics.draw(avatar);
        graphics.setStroke(previousStroke);
    }

    BufferedImage readImage(byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                throw new GeneralException(ErrorStatus.UPLOAD_STORAGE_OPERATION_FAILED);
            }
            return applyExifOrientation(image, readExifOrientation(bytes));
        } catch (IOException e) {
            throw new GeneralException(ErrorStatus.UPLOAD_STORAGE_OPERATION_FAILED);
        }
    }

    private int readExifOrientation(byte[] bytes) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(bytes));
            ExifIFD0Directory exifDirectory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            Integer orientation = exifDirectory == null ? null : exifDirectory.getInteger(ExifIFD0Directory.TAG_ORIENTATION);
            return orientation == null ? 1 : orientation;
        } catch (ImageProcessingException | IOException e) {
            return 1;
        }
    }

    private BufferedImage applyExifOrientation(BufferedImage source, int orientation) {
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

    private AffineTransform orientationTransform(int orientation, int width, int height) {
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

    private BufferedImage cropSquare(BufferedImage source) {
        int size = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - size) / 2;
        int y = (source.getHeight() - size) / 2;
        return source.getSubimage(x, y, size, size);
    }

    private BufferedImage crop(BufferedImage source, ImageCropRegion cropRegion) {
        if (cropRegion == null) {
            return cropSquare(source);
        }
        int x = cropRegion.x().multiply(BigDecimal.valueOf(source.getWidth())).intValue();
        int y = cropRegion.y().multiply(BigDecimal.valueOf(source.getHeight())).intValue();
        int width = cropRegion.width().multiply(BigDecimal.valueOf(source.getWidth())).intValue();
        int height = cropRegion.height().multiply(BigDecimal.valueOf(source.getHeight())).intValue();
        if (width <= 0 || height <= 0) {
            return cropSquare(source);
        }
        x = Math.max(0, Math.min(x, source.getWidth() - 1));
        y = Math.max(0, Math.min(y, source.getHeight() - 1));
        width = Math.min(width, source.getWidth() - x);
        height = Math.min(height, source.getHeight() - y);
        int size = Math.min(width, height);
        int squareX = x + (width - size) / 2;
        int squareY = y + (height - size) / 2;
        return source.getSubimage(squareX, squareY, size, size);
    }

    private String trimText(Graphics2D graphics, String value, int maxWidth) {
        String text = value == null || value.isBlank() ? "" : value;
        if (graphics.getFontMetrics().stringWidth(text) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        while (!text.isEmpty() && graphics.getFontMetrics().stringWidth(text + suffix) > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + suffix;
    }

    private int textBaseline(Graphics2D graphics, int top, int lineHeight) {
        FontMetrics metrics = graphics.getFontMetrics();
        return top + (lineHeight - metrics.getHeight()) / 2 + metrics.getAscent();
    }

    private String initial(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return "?";
        }
        return nickname.substring(0, 1);
    }

    private byte[] toJpeg(BufferedImage image) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageWriter writer = ImageIO.getImageWritersByFormatName(IMAGE_FORMAT).next();
            try (ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
                ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(0.95f);
                writer.setOutput(imageOutput);
                writer.write(null, new IIOImage(image, null, null), param);
            } finally {
                writer.dispose();
            }
            return output.toByteArray();
        } catch (IOException e) {
            throw new GeneralException(ErrorStatus.UPLOAD_STORAGE_OPERATION_FAILED);
        }
    }

    private static Font loadFont(String fileName, int fallbackStyle) {
        try (InputStream inputStream = WeeklyChallengeShareImageGenerator.class.getResourceAsStream(FONT_PATH + fileName)) {
            if (inputStream == null) {
                return new Font(Font.SANS_SERIF, fallbackStyle, 1);
            }
            return Font.createFont(Font.TRUETYPE_FONT, inputStream);
        } catch (java.awt.FontFormatException | IOException e) {
            return new Font(Font.SANS_SERIF, fallbackStyle, 1);
        }
    }

    private static BufferedImage loadTemplateImage() {
        try (InputStream inputStream = WeeklyChallengeShareImageGenerator.class.getResourceAsStream(TEMPLATE_PATH)) {
            if (inputStream == null) {
                throw new IllegalStateException("Weekly challenge share image template is missing.");
            }
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new IllegalStateException("Weekly challenge share image template cannot be read.");
            }
            return image;
        } catch (IOException e) {
            throw new IllegalStateException("Weekly challenge share image template cannot be loaded.", e);
        }
    }

    public record GridSize(int rows, int columns) {
    }

    public record ShareImageSource(
        byte[] imageBytes,
        ImageCropRegion cropRegion,
        String nickname,
        byte[] profileImageBytes
    ) {
    }

    public record ImageCropRegion(BigDecimal x, BigDecimal y, BigDecimal width, BigDecimal height) {
    }
}
