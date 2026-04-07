package com.cartandcook.adapters.aiopenai;

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
 * AiService implementation for OpenAI's /v1/chat/completions endpoint.
 */
@Service
@ConditionalOnProperty(name = "cartandcook.ai.provider", havingValue = "openai")
public class OpenAiAdapter implements AiService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiAdapter.class);

    private final WebClient webClient;
    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;

    public OpenAiAdapter(WebClient openAiWebClient, OpenAiProperties properties, ObjectMapper objectMapper) {
        this.webClient = openAiWebClient;
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

    @Override
    public Integer estimateCalories(String recipeName, String ingredientsSummary, String servingSize) {
        String prompt = AiPrompts.estimateCaloriesPrompt(recipeName, ingredientsSummary, servingSize);
        Map<String, Object> requestBody = Map.of(
                "model", properties.getModel(),
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", List.of(Map.of("type", "text", "text", prompt)))),
                "temperature", 0.2);
        String content = extractContent(sendRequest(requestBody));
        return AiResponseParser.parseCalorieEstimate(content, objectMapper);
    }

    private RecipeAnalysis analyzeTextOnly(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "model", properties.getModel(),
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", List.of(Map.of("type", "text", "text", prompt)))),
                "temperature", 0.2);

        String rawResponse = sendRequest(requestBody);
        String content = extractContent(rawResponse);

        return AiResponseParser.parseWithRetry(content, objectMapper, () -> {
            Map<String, Object> retryBody = Map.of(
                    "model", properties.getModel(),
                    "messages", List.of(Map.of(
                            "role", "user",
                            "content", List.of(Map.of("type", "text", "text", AiPrompts.RETRY_PROMPT)))),
                    "temperature", 0.2);
            return extractContent(sendRequest(retryBody));
        });
    }

    private String sendRequest(Map<String, Object> requestBody) {
        try {
            String response = webClient.post()
                    .uri("/v1/chat/completions")
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse -> clientResponse.bodyToMono(String.class)
                            .defaultIfEmpty("(no body)")
                            .map(body -> new AiServiceException(
                                    "OpenAI returned " + clientResponse.statusCode().value()
                                            + ". Response: " + body)))
                    .bodyToMono(String.class)
                    .block();

            if (response == null || response.isBlank()) {
                throw new AiServiceException("Empty response from OpenAI");
            }
            return response;
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AiServiceException("Failed to call OpenAI: " + e.getMessage(), e);
        }
    }

    private String extractContent(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode choices = root.path("choices");
            if (!choices.isMissingNode() && choices.isArray() && !choices.isEmpty()) {
                String content = choices.get(0).path("message").path("content").asText();
                if (content != null && !content.isBlank()) {
                    return content.strip();
                }
            }
            throw new AiServiceException("Invalid OpenAI response: could not extract content");
        } catch (JsonProcessingException e) {
            throw new AiServiceException("Failed to parse OpenAI response", e);
        }
    }
}
