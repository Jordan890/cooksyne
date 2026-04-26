package com.cooksyne.core.api;

import com.cooksyne.core.domain.RecipeAnalysis;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared JSON response parsing with retry support and markdown fence stripping.
 */
public final class AiResponseParser {

    private static final Logger log = LoggerFactory.getLogger(AiResponseParser.class);
    private static final int MAX_RETRIES = 2;

    private AiResponseParser() {
    }

    /**
     * Parse the LLM text content into RecipeAnalysis.
     * On failure, calls retryCallback to get a new response and retries up to
     * MAX_RETRIES times.
     */
    public static RecipeAnalysis parseWithRetry(String content, ObjectMapper objectMapper,
            RetryCallback retryCallback) {
        return parseWithRetry(content, objectMapper, retryCallback, 0);
    }

    private static RecipeAnalysis parseWithRetry(String content, ObjectMapper objectMapper,
            RetryCallback retryCallback, int attempt) {
        String cleaned = cleanJsonContent(content);
        ObjectMapper relaxedMapper = buildRelaxedMapper(objectMapper);

        try {
            return relaxedMapper.readValue(cleaned, RecipeAnalysis.class);
        } catch (JsonProcessingException e) {
            if (attempt < MAX_RETRIES) {
                log.warn("Failed to parse LLM JSON response (attempt {}), retrying...", attempt + 1, e);
                String retryContent = retryCallback.sendRetry();
                return parseWithRetry(retryContent, objectMapper, retryCallback, attempt + 1);
            }
            throw new AiServiceException("Failed to parse LLM response after " + (attempt + 1) + " attempts", e);
        }
    }

    /**
     * Strip markdown code fences from LLM output.
     */
    public static String cleanJsonContent(String content) {
        String cleaned = content.strip();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        cleaned = cleaned.strip();

        // Keep only the JSON object payload if extra text appears before/after it.
        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            cleaned = cleaned.substring(firstBrace, lastBrace + 1);
        }

        cleaned = cleaned.strip();

        // Attempt to repair truncated JSON (LLMs often hit token limits mid-array).
        cleaned = repairTruncatedJson(cleaned);

        return cleaned;
    }

    private static ObjectMapper buildRelaxedMapper(ObjectMapper baseMapper) {
        ObjectMapper relaxed = baseMapper.copy();
        relaxed.getFactory().configure(JsonReadFeature.ALLOW_JAVA_COMMENTS.mappedFeature(), true);
        relaxed.getFactory().configure(JsonReadFeature.ALLOW_YAML_COMMENTS.mappedFeature(), true);
        relaxed.getFactory().configure(JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature(), true);
        relaxed.getFactory().configure(JsonReadFeature.ALLOW_SINGLE_QUOTES.mappedFeature(), true);
        return relaxed;
    }

    /**
     * Attempt to repair JSON truncated by LLM token limits.
     * Trims back to the last complete value, then closes any unclosed
     * arrays/objects.
     */
    static String repairTruncatedJson(String json) {
        // Count unmatched open brackets/braces (skip content inside strings).
        int openBraces = 0;
        int openBrackets = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\' && inString) {
                escaped = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (!inString) {
                switch (c) {
                    case '{' -> openBraces++;
                    case '}' -> openBraces--;
                    case '[' -> openBrackets++;
                    case ']' -> openBrackets--;
                }
            }
        }

        // Already balanced — nothing to repair.
        if (openBraces == 0 && openBrackets == 0) {
            return json;
        }

        log.warn("Detected truncated JSON (unclosed braces={}, brackets={}), attempting repair", openBraces,
                openBrackets);

        // If we ended inside a string, close it.
        if (inString) {
            json = json + "\"";
        }

        // Trim back: remove a trailing incomplete element (partial key/value after last
        // comma).
        // Walk backward to find last structurally valid position.
        String trimmed = trimToLastCompleteElement(json);

        // Recount after trimming.
        openBraces = 0;
        openBrackets = 0;
        inString = false;
        escaped = false;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\' && inString) {
                escaped = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (!inString) {
                switch (c) {
                    case '{' -> openBraces++;
                    case '}' -> openBraces--;
                    case '[' -> openBrackets++;
                    case ']' -> openBrackets--;
                }
            }
        }

        // Append missing closers in reverse-open order.
        StringBuilder sb = new StringBuilder(trimmed);
        for (int i = 0; i < openBrackets; i++) {
            sb.append(']');
        }
        for (int i = 0; i < openBraces; i++) {
            sb.append('}');
        }
        return sb.toString();
    }

    /**
     * Walk backward and strip any trailing partial element after the last complete
     * value.
     * A complete value ends with: }, ], ", a digit, true, false, or null.
     */
    private static String trimToLastCompleteElement(String json) {
        // Find the last character that could be the end of a complete JSON value.
        for (int i = json.length() - 1; i >= 0; i--) {
            char c = json.charAt(i);
            if (c == '}' || c == ']' || c == '"' || Character.isDigit(c)
                    || c == 'e' || c == 'l') { // true/false/null endings
                // Check the surrounding substring for true/false/null.
                String tail = json.substring(Math.max(0, i - 4), i + 1);
                if (c == 'e' && !(tail.endsWith("true") || tail.endsWith("false"))) {
                    continue;
                }
                if (c == 'l' && !tail.endsWith("null")) {
                    continue;
                }
                // Strip trailing comma if present after this position.
                String result = json.substring(0, i + 1);
                String stripped = result.stripTrailing();
                if (stripped.endsWith(",")) {
                    stripped = stripped.substring(0, stripped.length() - 1);
                }
                return stripped;
            }
        }
        return json;
    }

    /**
     * Parse a simple calorie estimate JSON response ({"estimatedCalories": N}).
     */
    public static Integer parseCalorieEstimate(String content, ObjectMapper objectMapper) {
        String cleaned = cleanJsonContent(content);
        try {
            JsonNode root = objectMapper.readTree(cleaned);
            JsonNode calories = root.path("estimatedCalories");
            if (!calories.isMissingNode() && calories.isNumber()) {
                return calories.intValue();
            }
            throw new AiServiceException("Invalid calorie estimate response: missing estimatedCalories field");
        } catch (JsonProcessingException e) {
            throw new AiServiceException("Failed to parse calorie estimate response", e);
        }
    }

    /**
     * Callback interface for retrying a request when JSON parsing fails.
     */
    @FunctionalInterface
    public interface RetryCallback {
        /**
         * Send a retry request and return the extracted text content from the response.
         */
        String sendRetry();
    }
}
