package cmc.mody.challenge.application;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

@Component
public class WeeklyChallengeShareImageGenerator {
    private static final int CELL_SIZE = 512;
    private static final String IMAGE_FORMAT = "jpg";

    public byte[] generate(List<byte[]> imageBytes, GridSize gridSize) {
        BufferedImage canvas = new BufferedImage(
            gridSize.columns() * CELL_SIZE,
            gridSize.rows() * CELL_SIZE,
            BufferedImage.TYPE_INT_RGB
        );
        Graphics2D graphics = canvas.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            for (int index = 0; index < imageBytes.size(); index++) {
                drawImage(graphics, readImage(imageBytes.get(index)), gridSize, index);
            }
            return toJpeg(canvas);
        } finally {
            graphics.dispose();
        }
    }

    public GridSize calculateGridSize(int imageCount) {
        int columns = (int) Math.ceil(Math.sqrt(imageCount));
        int rows = (int) Math.ceil((double) imageCount / columns);
        return new GridSize(rows, columns);
    }

    private void drawImage(Graphics2D graphics, BufferedImage source, GridSize gridSize, int index) {
        int row = index / gridSize.columns();
        int column = index % gridSize.columns();
        graphics.drawImage(
            cropSquare(source),
            column * CELL_SIZE,
            row * CELL_SIZE,
            CELL_SIZE,
            CELL_SIZE,
            null
        );
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

    private byte[] toJpeg(BufferedImage image) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, IMAGE_FORMAT, output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new GeneralException(ErrorStatus.UPLOAD_STORAGE_OPERATION_FAILED);
        }
    }

    public record GridSize(int rows, int columns) {
    }
}
