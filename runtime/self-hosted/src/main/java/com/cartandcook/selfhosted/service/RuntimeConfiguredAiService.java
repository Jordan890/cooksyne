package com.cartandcook.selfhosted.service;

import com.cartandcook.core.api.AiImageProcessor;
import com.cartandcook.core.api.AiPrompts;
import com.cartandcook.core.api.AiResponseParser;
import com.cartandcook.core.api.AiService;
import com.cartandcook.core.api.AiServiceException;
import com.cartandcook.core.domain.RecipeAnalysis;
import com.cartandcook.selfhosted.contracts.RuntimeConfigResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Runtime AI service that reads provider + credentials from persisted runtime
 * config.
 */
@Service
@Primary
public class RuntimeConfiguredAiService implements AiService {

    private final RuntimeConfigService runtimeConfigService;
    private final ObjectMapper objectMapper;

    public RuntimeConfiguredAiService(RuntimeConfigService runtimeConfigService, ObjectMapper objectMapper) {
        this.runtimeConfigService = runtimeConfigService;
        this.objectMapper = objectMapper;
    }

    @Override
    public RecipeAnalysis analyzeFoodImage(byte[] image) {
        RuntimeConfigResponse cfg = runtimeConfigService.get();
        return analyze(image, AiPrompts.FOOD_IMAGE_PROMPT, cfg);
    }

    @Override
    public RecipeAnalysis analyzeRecipeImage(byte[] image) {
        RuntimeConfigResponse cfg = runtimeConfigService.get();
        return analyze(image, AiPrompts.RECIPE_IMAGE_PROMPT, cfg);
    }

    private RecipeAnalysis analyze(byte[] image, String prompt, RuntimeConfigResponse cfg) {
        String provider = normalizeProvider(cfg.getAiProvider());
        return switch (provider) {
            case "ollama" -> analyzeWithOllama(image, prompt, cfg);
            case "openai" -> analyzeWithOpenAi(image, prompt, cfg);
            case "huggingface" -> analyzeWithHuggingFace(image, prompt, cfg);
            case "bedrock" -> analyzeWithBedrock(image, prompt, cfg);
            default -> throw new AiServiceException(
                    "AI provider is not configured. Set AI provider in Runtime Settings (ollama/openai/huggingface/bedrock).");
        };
    }

    private RecipeAnalysis analyzeWithOllama(byte[] image, String prompt, RuntimeConfigResponse cfg) {
        byte[] processed = AiImageProcessor.preprocessImage(image);
        String base64 = Base64.getEncoder().encodeToString(processed);
        WebClient webClient = WebClient.builder().baseUrl(cfg.getOllamaBaseUrl()).build();

        Map<String, Object> requestBody = Map.of(
                "model", cfg.getOllamaModel(),
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", prompt,
                        "images", List.of(base64))),
                "stream", false,
                "options", Map.of("temperature", 0.2));

        String content = extractOllamaContent(sendWebClientRequest(webClient, "/api/chat", requestBody, "Ollama"));

        return AiResponseParser.parseWithRetry(content, objectMapper, () -> {
            Map<String, Object> retryBody = Map.of(
                    "model", cfg.getOllamaModel(),
                    "messages", List.of(Map.of("role", "user", "content", AiPrompts.RETRY_PROMPT)),
                    "stream", false,
                    "options", Map.of("temperature", 0.2));
            return extractOllamaContent(sendWebClientRequest(webClient, "/api/chat", retryBody, "Ollama"));
        });
    }

    private RecipeAnalysis analyzeWithOpenAi(byte[] image, String prompt, RuntimeConfigResponse cfg) {
        byte[] processed = AiImageProcessor.preprocessImage(image);
        String base64 = Base64.getEncoder().encodeToString(processed);
        String imageDataUrl = "data:image/jpeg;base64," + base64;

        WebClient webClient = WebClient.builder()
                .baseUrl("https://api.openai.com")
                .defaultHeader("Authorization", "Bearer " + cfg.getOpenAiApiKey())
                .build();

        Map<String, Object> requestBody = Map.of(
                "model", cfg.getOpenAiModel(),
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of("type", "text", "text", prompt),
                                Map.of("type", "image_url", "image_url", Map.of("url", imageDataUrl))))),
                "temperature", 0.2);

        String content = extractOpenAiLikeContent(
                sendWebClientRequest(webClient, "/v1/chat/completions", requestBody, "OpenAI"),
                "OpenAI");

        return AiResponseParser.parseWithRetry(content, objectMapper, () -> {
            Map<String, Object> retryBody = Map.of(
                    "model", cfg.getOpenAiModel(),
                    "messages", List.of(Map.of(
                            "role", "user",
                            "content", List.of(Map.of("type", "text", "text", AiPrompts.RETRY_PROMPT)))),
                    "temperature", 0.2);
            return extractOpenAiLikeContent(
                    sendWebClientRequest(webClient, "/v1/chat/completions", retryBody, "OpenAI"),
                    "OpenAI");
        });
    }

    private RecipeAnalysis analyzeWithHuggingFace(byte[] image, String prompt, RuntimeConfigResponse cfg) {
        byte[] processed = AiImageProcessor.preprocessImage(image);
        String base64 = Base64.getEncoder().encodeToString(processed);
        String imageDataUrl = "data:image/jpeg;base64," + base64;

        WebClient webClient = WebClient.builder()
                .baseUrl("https://api-inference.huggingface.co")
                .defaultHeader("Authorization", "Bearer " + cfg.getHuggingFaceApiKey())
                .build();

        String uri = "/models/" + cfg.getHuggingFaceModel() + "/v1/chat/completions";

        Map<String, Object> requestBody = Map.of(
                "model", cfg.getHuggingFaceModel(),
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of("type", "text", "text", prompt),
                                Map.of("type", "image_url", "image_url", Map.of("url", imageDataUrl))))),
                "temperature", 0.2);

        String content = extractOpenAiLikeContent(
                sendWebClientRequest(webClient, uri, requestBody, "Hugging Face"),
                "Hugging Face");

        return AiResponseParser.parseWithRetry(content, objectMapper, () -> {
            Map<String, Object> retryBody = Map.of(
                    "model", cfg.getHuggingFaceModel(),
                    "messages", List.of(Map.of(
                            "role", "user",
                            "content", List.of(Map.of("type", "text", "text", AiPrompts.RETRY_PROMPT)))),
                    "temperature", 0.2);
            return extractOpenAiLikeContent(
                    sendWebClientRequest(webClient, uri, retryBody, "Hugging Face"),
                    "Hugging Face");
        });
    }

    private RecipeAnalysis analyzeWithBedrock(byte[] image, String prompt, RuntimeConfigResponse cfg) {
        byte[] processed = AiImageProcessor.preprocessImage(image);
        String base64 = Base64.getEncoder().encodeToString(processed);

        String content = sendBedrockImageRequest(base64, prompt, cfg);

        return AiResponseParser.parseWithRetry(content, objectMapper,
                () -> sendBedrockTextRequest(AiPrompts.RETRY_PROMPT, cfg));
    }

    private String sendBedrockImageRequest(String base64Image, String prompt, RuntimeConfigResponse cfg) {
        Map<String, Object> requestBody = Map.of(
                "anthropic_version", "bedrock-2023-05-31",
                "max_tokens", 4096,
                "temperature", 0.2,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of("type", "image",
                                        "source", Map.of(
                                                "type", "base64",
                                                "media_type", "image/jpeg",
                                                "data", base64Image)),
                                Map.of("type", "text", "text", prompt)))));
        return invokeBedrock(requestBody, cfg);
    }

    private String sendBedrockTextRequest(String prompt, RuntimeConfigResponse cfg) {
        Map<String, Object> requestBody = Map.of(
                "anthropic_version", "bedrock-2023-05-31",
                "max_tokens", 4096,
                "temperature", 0.2,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", List.of(Map.of("type", "text", "text", prompt)))));
        return invokeBedrock(requestBody, cfg);
    }

    private String invokeBedrock(Map<String, Object> requestBody, RuntimeConfigResponse cfg) {
        try (BedrockRuntimeClient bedrockClient = BedrockRuntimeClient.builder()
                .region(Region.of(cfg.getAwsRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build()) {

            String json = objectMapper.writeValueAsString(requestBody);

            InvokeModelResponse response = bedrockClient.invokeModel(InvokeModelRequest.builder()
                    .modelId(cfg.getBedrockModelId())
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromUtf8String(json))
                    .build());

            return extractBedrockContent(response.body().asUtf8String());
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AiServiceException("Failed to call AWS Bedrock: " + e.getMessage(), e);
        }
    }

    private String sendWebClientRequest(WebClient webClient, String uri, Map<String, Object> requestBody,
            String providerName) {
        try {
            String response = webClient.post()
                    .uri(uri)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse -> clientResponse.bodyToMono(String.class)
                            .defaultIfEmpty("(no body)")
                            .map(body -> new AiServiceException(
                                    providerName + " returned " + clientResponse.statusCode().value()
                                            + ". Response: " + body)))
                    .bodyToMono(String.class)
                    .block();

            if (response == null || response.isBlank()) {
                throw new AiServiceException("Empty response from " + providerName);
            }
            return response;
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AiServiceException("Failed to call " + providerName + ": " + e.getMessage(), e);
        }
    }

    private String extractOllamaContent(String rawResponse) {
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

    private String extractOpenAiLikeContent(String rawResponse, String providerName) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode choices = root.path("choices");
            if (!choices.isMissingNode() && choices.isArray() && !choices.isEmpty()) {
                String content = choices.get(0).path("message").path("content").asText();
                if (content != null && !content.isBlank()) {
                    return content.strip();
                }
            }
            throw new AiServiceException("Invalid " + providerName + " response: could not extract content");
        } catch (JsonProcessingException e) {
            throw new AiServiceException("Failed to parse " + providerName + " response", e);
        }
    }

    private String extractBedrockContent(String responseBody) {
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

    private String normalizeProvider(String provider) {
        return provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
    }
}
