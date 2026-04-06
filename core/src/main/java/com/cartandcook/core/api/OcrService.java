package com.cartandcook.core.api;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Uses Tesseract OCR to extract text from recipe images.
 */
public class OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

    private final String tessDataPath;

    public OcrService(String tessDataPath) {
        this.tessDataPath = tessDataPath;
    }

    /**
     * Extract text from an image using Tesseract OCR.
     *
     * @param imageBytes raw image bytes (JPEG, PNG, etc.)
     * @return extracted text
     * @throws AiServiceException if OCR fails or returns empty text
     */
    public String extractText(byte[] imageBytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                throw new AiServiceException("Could not read uploaded image. Ensure it is a valid image file.");
            }

            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(tessDataPath);
            tesseract.setLanguage("eng");
            // Page segmentation mode 3 = fully automatic
            tesseract.setPageSegMode(3);

            String text = tesseract.doOCR(image);

            if (text == null || text.isBlank()) {
                throw new AiServiceException("OCR could not extract any text from the image. Try a clearer photo.");
            }

            log.debug("OCR extracted {} characters from image", text.length());
            return text.strip();
        } catch (IOException e) {
            throw new AiServiceException("Failed to read image for OCR", e);
        } catch (TesseractException e) {
            throw new AiServiceException("OCR text extraction failed: " + e.getMessage(), e);
        }
    }
}
