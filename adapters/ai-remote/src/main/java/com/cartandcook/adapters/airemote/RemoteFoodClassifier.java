package com.cartandcook.adapters.airemote;

import com.cartandcook.core.ai.FoodClassifier;
import com.cartandcook.core.ai.VisionResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@ConditionalOnProperty(
        name = "cartcook.ai.mode",
        havingValue = "remote"
)
public class RemoteFoodClassifier implements FoodClassifier {

    private String baseUrl;
    private String apiKey;
    private int timeoutSeconds;

    private final WebClient webClient;

    public RemoteFoodClassifier(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public VisionResult classify(byte[] imageBytes) {
        // Call remote AI endpoint
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return "image.jpg";
            }
        });

        return webClient.post()
                .uri(baseUrl + "/classify")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .headers(h -> {
                    if (apiKey != null && !apiKey.isEmpty()) {
                        h.set("x-api-key", apiKey);
                    }
                })
                .bodyValue(body)
                .retrieve()
                .bodyToMono(VisionResult.class)
                .block();
    }
}

