package com.cooksyne.adapters.aiollama;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
@ConditionalOnProperty(name = "cooksyne.ai.provider", havingValue = "ollama")
@EnableConfigurationProperties(OllamaProperties.class)
public class OllamaConfig {

    @Bean
    public WebClient ollamaWebClient(OllamaProperties props) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMinutes(5));
        return WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }
}
