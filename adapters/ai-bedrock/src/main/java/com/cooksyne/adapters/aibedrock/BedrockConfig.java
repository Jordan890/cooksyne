package com.cooksyne.adapters.aibedrock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

@Configuration
@ConditionalOnProperty(name = "cooksyne.ai.provider", havingValue = "bedrock")
@EnableConfigurationProperties(BedrockProperties.class)
public class BedrockConfig {

    @Bean
    public BedrockRuntimeClient bedrockRuntimeClient(BedrockProperties props) {
        return BedrockRuntimeClient.builder()
                .region(Region.of(props.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}

