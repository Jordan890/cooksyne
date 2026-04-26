package com.cooksyne.core.api;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Shared image preprocessing: resize to max 1024px and convert to JPEG.
 */
public final class AiImageProcessor {

    private static final int MAX_IMAGE_DIMENSION = 1024;

    private AiImageProcessor() {
    }

    /**
     * Resize the image so neither dimension exceeds 1024 and convert to JPEG bytes.
     */
    public static byte[] preprocessImage(byte[] imageBytes) {
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (original == null) {
                throw new AiServiceException("Could not read uploaded image. Ensure it is a valid image file.");
            }

            int width = original.getWidth();
            int height = original.getHeight();

            if (width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION) {
                double scale = Math.min(
                        (double) MAX_IMAGE_DIMENSION / width,
                        (double) MAX_IMAGE_DIMENSION / height
                );
                width = (int) (width * scale);
                height = (int) (height * scale);

                BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = resized.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(original, 0, 0, width, height, null);
                g.dispose();
                original = resized;
            } else if (original.getType() != BufferedImage.TYPE_INT_RGB) {
                BufferedImage rgb = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = rgb.createGraphics();
                g.drawImage(original, 0, 0, null);
                g.dispose();
                original = rgb;
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(original, "jpg", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new AiServiceException("Failed to preprocess image", e);
        }
    }
}

