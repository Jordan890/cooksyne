package com.cooksyne.adapters.aibedrock;

import com.cooksyne.core.api.*;
import com.cooksyne.core.domain.RecipeAnalysis;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.List;
import java.util.Map;

/**
 * AiService implementation for AWS Bedrock using the InvokeModel API.
 * Sends Claude Messages-format JSON directly.
 * Uses DefaultCredentialsProvider (env vars, ~/.aws/credentials, IAM role,
 * etc.).
 */
@Service
@ConditionalOnProperty(name = "cooksyne.ai.provider", havingValue = "bedrock")
public class BedrockAiAdapter implements AiService {

    private static final Logger log = LoggerFactory.getLogger(BedrockAiAdapter.class);

    private final BedrockRuntimeClient bedrockClient;
    private final BedrockProperties properties;
    private final ObjectMapper objectMapper;

    public BedrockAiAdapter(BedrockRuntimeClient bedrockClient, BedrockProperties properties,
            ObjectMapper objectMapper) {
        this.bedrockClient = bedrockClient;
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
        String content = sendTextRequest(prompt);
        return AiResponseParser.parseCalorieEstimate(content, objectMapper);
    }

    private RecipeAnalysis analyzeTextOnly(String prompt) {
        String content = sendTextRequest(prompt);

        return AiResponseParser.parseWithRetry(content, objectMapper, () -> sendTextRequest(AiPrompts.RETRY_PROMPT));
    }

    private String sendTextRequest(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "anthropic_version", "bedrock-2023-05-31",
                "max_tokens", 4096,
                "temperature", 0.2,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of("type", "text", "text", prompt)))));
        return invokeModel(requestBody);
    }

    private String invokeModel(Map<String, Object> requestBody) {
        try {
            String json = objectMapper.writeValueAsString(requestBody);

            InvokeModelResponse response = bedrockClient.invokeModel(InvokeModelRequest.builder()
                    .modelId(properties.getModelId())
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromUtf8String(json))
                    .build());

            String responseBody = response.body().asUtf8String();
            return extractContent(responseBody);
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AiServiceException("Failed to call AWS Bedrock: " + e.getMessage(), e);
        }
    }

    private String extractContent(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode content = root.path("content");
            if (content.isArray() && !content.isEmpty()) {
                String text = content.get(0).path("text").asText();
                if (text != null && !text.isBlank()) {
                    return text.strip();
                }
            }
            throw new AiServiceException("Invalid Bedrock response: could not extract content");
        } catch (JsonProcessingException e) {
            throw new AiServiceException("Failed to parse Bedrock response", e);
        }
    }
}
