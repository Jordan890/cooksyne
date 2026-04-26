package com.cooksyne.adapters.aihuggingface;

import com.cooksyne.core.api.*;
import com.cooksyne.core.domain.RecipeAnalysis;
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
 * AiService implementation for Hugging Face Inference API.
 * Uses the /models/{model} endpoint with OpenAI-compatible chat completions
 * format.
 */
@Service
@ConditionalOnProperty(name = "cooksyne.ai.provider", havingValue = "huggingface")
public class HuggingFaceAiAdapter implements AiService {

    private static final Logger log = LoggerFactory.getLogger(HuggingFaceAiAdapter.class);

    private final WebClient webClient;
    private final HuggingFaceProperties properties;
    private final ObjectMapper objectMapper;

    public HuggingFaceAiAdapter(WebClient huggingFaceWebClient, HuggingFaceProperties properties,
            ObjectMapper objectMapper) {
        this.webClient = huggingFaceWebClient;
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
                    .uri("/models/" + properties.getModel() + "/v1/chat/completions")
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse -> clientResponse.bodyToMono(String.class)
                            .defaultIfEmpty("(no body)")
                            .map(body -> new AiServiceException(
                                    "Hugging Face returned " + clientResponse.statusCode().value()
                                            + ". Response: " + body)))
                    .bodyToMono(String.class)
                    .block();

            if (response == null || response.isBlank()) {
                throw new AiServiceException("Empty response from Hugging Face");
            }
            return response;
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AiServiceException("Failed to call Hugging Face: " + e.getMessage(), e);
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
            throw new AiServiceException("Invalid Hugging Face response: could not extract content");
        } catch (JsonProcessingException e) {
            throw new AiServiceException("Failed to parse Hugging Face response", e);
        }
    }
}
