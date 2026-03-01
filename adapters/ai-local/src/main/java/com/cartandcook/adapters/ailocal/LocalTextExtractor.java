package com.cartandcook.adapters.ailocal;

import com.cartandcook.core.ai.TextExtractionResult;
import com.cartandcook.core.ai.TextExtractor;
import jakarta.annotation.PostConstruct;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

@Component
@ConditionalOnProperty(name = "cartcook.ai.mode", havingValue = "local", matchIfMissing = true)
@ConfigurationProperties(prefix = "cartcook.ai.local")
public class LocalTextExtractor implements TextExtractor {

    private String tessdataPath;
    private Tesseract tesseract;

    public String getTessdataPath() { return tessdataPath; }
    public void setTessdataPath(String tessdataPath) { this.tessdataPath = tessdataPath; }

    @PostConstruct
    public void init() {
        tesseract = new Tesseract();
        tesseract.setDatapath(tessdataPath);
        tesseract.setLanguage("eng");
    }

    @Override
    public TextExtractionResult extract(byte[] imageBytes) throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
        String fullText = null;
        try {
            fullText = tesseract.doOCR(img);
        } catch (TesseractException e) {
            throw new RuntimeException(e);
        }

        // TODO: optionally split fullText into structured RecipeSegment objects
        List<TextExtractionResult.RecipeSegment> segments = List.of(); // placeholder

        return TextExtractionResult.builder()
                .fullText(fullText)
                .segments(segments)
                .build();
    }

}



