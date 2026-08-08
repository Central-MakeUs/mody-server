package cmc.mody.challenge.application;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import java.awt.AlphaComposite;
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
    private static final int OUTER_RADIUS = scaled(20);
    private static final int OUTER_BORDER_WIDTH = scaled(2);
    private static final int HEADER_X = scaled(24);
    private static final int HEADER_Y = scaled(31);
    private static final int HEADER_WIDTH = scaled(354);
    private static final int HEADER_HEIGHT = scaled(81);
    private static final int HEADER_RADIUS = scaled(12);
    private static final int HEADER_PADDING = scaled(16);
    private static final int HEADER_TEXT_GAP = scaled(4);
    private static final int GRID_X = scaled(24);
    private static final int GRID_Y = scaled(125);
    private static final int CELL_SIZE = scaled(172);
    private static final int CELL_GAP = scaled(10);
    private static final int CELL_RADIUS = scaled(12);
    private static final int AVATAR_SIZE = scaled(30);
    private static final int AVATAR_MARGIN = scaled(16);
    private static final int PARTICIPANT_GAP = scaled(4);
    private static final int TITLE_FONT_SIZE = scaled(18);
    private static final int DESCRIPTION_FONT_SIZE = scaled(14);
    private static final int NICKNAME_FONT_SIZE = scaled(14);
    private static final String IMAGE_FORMAT = "jpg";
    private static final String FONT_PATH = "/fonts/";
    private static final Color BACKGROUND_COLOR = new Color(30, 30, 30);
    private static final Color OUTER_BORDER_COLOR = new Color(132, 132, 132);
    private static final Color HEADER_COLOR = new Color(255, 252, 235);
    private static final Color TITLE_COLOR = new Color(20, 20, 20);
    private static final Color DESCRIPTION_COLOR = new Color(54, 54, 54);
    private static final Color FALLBACK_AVATAR_COLOR = new Color(255, 231, 86);
    private static final Color AVATAR_BORDER_COLOR = new Color(255, 255, 245);
    private static final Font TITLE_FONT = loadFont("Pretendard-SemiBold.otf", Font.BOLD);
    private static final Font MEDIUM_FONT = loadFont("Pretendard-Medium.otf", Font.PLAIN);

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
            drawHeader(graphics, title, description);
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
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
        RoundRectangle2D background = new RoundRectangle2D.Double(
            1,
            1,
            CANVAS_WIDTH - 2,
            CANVAS_HEIGHT - 2,
            OUTER_RADIUS,
            OUTER_RADIUS
        );
        graphics.setColor(BACKGROUND_COLOR);
        graphics.fill(background);
        Stroke previousStroke = graphics.getStroke();
        graphics.setStroke(new BasicStroke(OUTER_BORDER_WIDTH));
        graphics.setColor(OUTER_BORDER_COLOR);
        graphics.draw(background);
        graphics.setStroke(previousStroke);
    }

    private void drawHeader(Graphics2D graphics, String title, String description) {
        RoundRectangle2D header = new RoundRectangle2D.Double(
            HEADER_X,
            HEADER_Y,
            HEADER_WIDTH,
            HEADER_HEIGHT,
            HEADER_RADIUS,
            HEADER_RADIUS
        );
        graphics.setColor(HEADER_COLOR);
        graphics.fill(header);

        graphics.setColor(TITLE_COLOR);
        graphics.setFont(TITLE_FONT.deriveFont((float) TITLE_FONT_SIZE));
        int textX = HEADER_X + HEADER_PADDING;
        int maxTextWidth = HEADER_WIDTH - HEADER_PADDING * 2;
        int titleTop = HEADER_Y + HEADER_PADDING;
        graphics.drawString(trimText(graphics, title, maxTextWidth), textX, textBaseline(graphics, titleTop, scaled(25)));

        graphics.setColor(DESCRIPTION_COLOR);
        graphics.setFont(MEDIUM_FONT.deriveFont((float) DESCRIPTION_FONT_SIZE));
        graphics.drawString(
            trimText(graphics, description, maxTextWidth),
            textX,
            textBaseline(graphics, titleTop + scaled(25) + HEADER_TEXT_GAP, scaled(20))
        );
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
        graphics.setComposite(AlphaComposite.SrcOver.derive(0.46f));
        graphics.setPaint(new GradientPaint(x, y, Color.BLACK, x, y + scaled(58), new Color(0, 0, 0, 0)));
        graphics.fillRect(x, y, CELL_SIZE, scaled(72));
        graphics.setComposite(AlphaComposite.SrcOver);
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
            graphics.setColor(TITLE_COLOR);
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
        graphics.setStroke(new BasicStroke(1));
        graphics.draw(avatar);
        graphics.setStroke(previousStroke);
    }

    private BufferedImage readImage(byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                throw new GeneralException(ErrorStatus.UPLOAD_STORAGE_OPERATION_FAILED);
            }
            return image;
        } catch (IOException e) {
            throw new GeneralException(ErrorStatus.UPLOAD_STORAGE_OPERATION_FAILED);
        }
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
