package com.cooksyne.selfhosted.controller;

import com.cooksyne.selfhosted.contracts.AiProviderModelsResponse;
import com.cooksyne.selfhosted.contracts.RuntimeConfigRequest;
import com.cooksyne.selfhosted.contracts.RuntimeConfigResponse;
import com.cooksyne.selfhosted.service.RuntimeConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/config/runtime")
public class RuntimeConfigController {

    private final RuntimeConfigService runtimeConfigService;

    public RuntimeConfigController(RuntimeConfigService runtimeConfigService) {
        this.runtimeConfigService = runtimeConfigService;
    }

    @GetMapping
    public ResponseEntity<RuntimeConfigResponse> getConfig() {
        return ResponseEntity.ok(runtimeConfigService.get());
    }

    @PutMapping
    public ResponseEntity<RuntimeConfigResponse> saveConfig(@RequestBody RuntimeConfigRequest request) {
        return ResponseEntity.ok(runtimeConfigService.save(request));
    }

    @GetMapping("/models")
    public ResponseEntity<AiProviderModelsResponse> getModels() {
        AiProviderModelsResponse response = new AiProviderModelsResponse();
        response.setModels(Map.of(
                "ollama", List.of(
                        "llava-phi3",
                        "llava:7b",
                        "llava:13b",
                        "llava:34b",
                        "llava-llama3",
                        "bakllava",
                        "minicpm-v",
                        "moondream"),
                "openai", List.of(
                        "gpt-4o-mini",
                        "gpt-4o",
                        "gpt-4-turbo",
                        "gpt-4.1",
                        "gpt-4.1-mini",
                        "gpt-4.1-nano",
                        "o4-mini"),
                "bedrock", List.of(
                        "anthropic.claude-3-haiku-20240307-v1:0",
                        "anthropic.claude-3-sonnet-20240229-v1:0",
                        "anthropic.claude-3-5-sonnet-20241022-v2:0",
                        "anthropic.claude-3-opus-20240229-v1:0",
                        "amazon.nova-lite-v1:0",
                        "amazon.nova-pro-v1:0"),
                "huggingface", List.of(
                        "Salesforce/blip-image-captioning-large",
                        "Salesforce/blip-image-captioning-base",
                        "nlpconnect/vit-gpt2-image-captioning",
                        "microsoft/Florence-2-large")));
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }
}
