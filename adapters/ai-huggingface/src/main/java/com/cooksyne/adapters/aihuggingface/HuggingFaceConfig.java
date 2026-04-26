package com.cooksyne.adapters.aihuggingface;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@ConditionalOnProperty(name = "cooksyne.ai.provider", havingValue = "huggingface")
@EnableConfigurationProperties(HuggingFaceProperties.class)
public class HuggingFaceConfig {

    @Bean
    public WebClient huggingFaceWebClient(HuggingFaceProperties props) {
        return WebClient.builder()
                .baseUrl("https://api-inference.huggingface.co")
                .defaultHeader("Authorization", "Bearer " + props.getApiKey())
                .build();
    }
}

