package com.cartandcook.adapters.aiollama;

import com.cartandcook.core.api.*;
import com.cartandcook.core.domain.RecipeAnalysis;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * AiService implementation for Ollama's native /api/chat endpoint.
 */
@Service
@ConditionalOnProperty(name = "cartandcook.ai.provider", havingValue = "ollama")
public class OllamaAiAdapter implements AiService {

    private static final Logger log = LoggerFactory.getLogger(OllamaAiAdapter.class);

    private final WebClient webClient;
    private final OllamaProperties properties;
    private final ObjectMapper objectMapper;

    public OllamaAiAdapter(WebClient ollamaWebClient, OllamaProperties properties, ObjectMapper objectMapper) {
        this.webClient = ollamaWebClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public RecipeAnalysis analyzeFoodByTitle(String dishTitle) {
        return analyzeTextOnly(AiPrompts.foodTitleOnlyPrompt(dishTitle));
    }

    @Override
    public RecipeAnalysis analyzeRecipeByText(String extractedText) {
        return analyzeTextOnly(AiPrompts.recipeTextPrompt(extractedText));
    }

    private RecipeAnalysis analyzeTextOnly(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "model", properties.getModel(),
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", prompt)),
                "stream", false,
                "options", Map.of("temperature", 0.2));

        String rawResponse = sendRequest(requestBody);
        String content = extractContent(rawResponse);

        return AiResponseParser.parseWithRetry(content, objectMapper, () -> {
            Map<String, Object> retryBody = Map.of(
                    "model", properties.getModel(),
                    "messages", List.of(Map.of(
                            "role", "user",
                            "content", AiPrompts.RETRY_PROMPT)),
                    "stream", false,
                    "options", Map.of("temperature", 0.2));
            return extractContent(sendRequest(retryBody));
        });
    }

    private String sendRequest(Map<String, Object> requestBody) {
        try {
            long start = System.currentTimeMillis();
            log.info("Ollama: sending request to model '{}'", properties.getModel());
            String response = webClient.post()
                    .uri("/api/chat")
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse -> clientResponse.bodyToMono(String.class)
                            .defaultIfEmpty("(no body)")
                            .map(body -> new AiServiceException(
                                    "Ollama returned " + clientResponse.statusCode().value()
                                            + ". Response: " + body)))
                    .bodyToMono(String.class)
                    .block();

            log.info("Ollama: response received in {} ms", System.currentTimeMillis() - start);
            if (response == null || response.isBlank()) {
                throw new AiServiceException("Empty response from Ollama");
            }
            return response;
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AiServiceException("Failed to call Ollama: " + e.getMessage(), e);
        }
    }

    private String extractContent(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode message = root.path("message");
            if (!message.isMissingNode() && message.has("content")) {
                String content = message.path("content").asText();
                if (content != null && !content.isBlank()) {
                    return content.strip();
                }
            }
            throw new AiServiceException("Invalid Ollama response: could not extract content");
        } catch (JsonProcessingException e) {
            throw new AiServiceException("Failed to parse Ollama response", e);
        }
    }
}
