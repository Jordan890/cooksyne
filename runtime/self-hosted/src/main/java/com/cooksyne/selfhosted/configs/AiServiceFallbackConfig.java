package com.cooksyne.selfhosted.configs;

import com.cooksyne.core.api.AiService;
import com.cooksyne.core.api.DisabledAiService;
import com.cooksyne.core.api.OcrService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiServiceFallbackConfig {

    @Bean
    @ConditionalOnMissingBean(AiService.class)
    public AiService disabledAiService() {
        return new DisabledAiService();
    }

    @Bean
    public OcrService ocrService(
            @Value("${TESSDATA_PREFIX:/usr/share/tesseract-ocr/5/tessdata}") String tessDataPath,
            @Value("${JNA_LIBRARY_PATH:}") String jnaLibraryPath) {
        if (jnaLibraryPath != null && !jnaLibraryPath.isBlank()) {
            System.setProperty("jna.library.path", jnaLibraryPath);
        }
        return new OcrService(tessDataPath);
    }

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
