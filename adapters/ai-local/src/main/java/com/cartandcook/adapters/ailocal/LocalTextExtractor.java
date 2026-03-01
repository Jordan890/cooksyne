package com.cartandcook.adapters.ailocal;

import com.cartandcook.core.ai.TextExtractionResult;
import com.cartandcook.core.ai.TextExtractor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "cartcook.ai.mode", havingValue = "local", matchIfMissing = true)
public class LocalTextExtractor implements TextExtractor {

    private String tessdataPath; // bound from application.yml

    public LocalTextExtractor() { }

    public String getTessdataPath() {
        return tessdataPath;
    }

    public void setTessdataPath(String tessdataPath) {
        this.tessdataPath = tessdataPath;
    }

    @Override
    public TextExtractionResult extract(byte[] imageBytes) {
        // TODO: implement real OCR
        return TextExtractionResult.builder()
                .fullText("Sample recipe text")
                .segment(TextExtractionResult.RecipeSegment.builder()
                        .type("title")
                        .content("Sample Recipe")
                        .build())
                .build();
    }
}

