package com.cartandcook.selfhosted.configs;

import com.cartandcook.core.api.AiService;
import com.cartandcook.core.api.DisabledAiService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}

