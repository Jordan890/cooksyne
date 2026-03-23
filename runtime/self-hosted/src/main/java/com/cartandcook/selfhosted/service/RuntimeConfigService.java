package com.cartandcook.selfhosted.service;

import com.cartandcook.adapters.persistencejpa.SpringDataUserRuntimeConfigRepository;
import com.cartandcook.adapters.persistencejpa.UserRuntimeConfigEntity;
import com.cartandcook.selfhosted.contracts.RuntimeConfigRequest;
import com.cartandcook.selfhosted.contracts.RuntimeConfigResponse;
import org.springframework.stereotype.Service;

@Service
public class RuntimeConfigService {

    private static final String DEFAULT_AI_PROVIDER = "";
    private static final String DEFAULT_OLLAMA_BASE_URL = "http://localhost:11434";
    private static final String DEFAULT_OLLAMA_MODEL = "llava-phi3";
    private static final String DEFAULT_OPENAI_MODEL = "gpt-4o-mini";
    private static final String DEFAULT_AWS_REGION = "us-east-1";
    private static final String DEFAULT_BEDROCK_MODEL_ID = "anthropic.claude-3-haiku-20240307-v1:0";
    private static final String DEFAULT_HF_MODEL = "Salesforce/blip-image-captioning-large";

    private final SpringDataUserRuntimeConfigRepository repository;

    public RuntimeConfigService(SpringDataUserRuntimeConfigRepository repository) {
        this.repository = repository;
    }

    public RuntimeConfigResponse get() {
        UserRuntimeConfigEntity entity = repository.findTopByOrderByIdAsc()
                .orElseGet(UserRuntimeConfigEntity::new);
        return toResponse(entity);
    }

    public RuntimeConfigResponse save(RuntimeConfigRequest request) {
        UserRuntimeConfigEntity entity = repository.findTopByOrderByIdAsc()
                .orElseGet(UserRuntimeConfigEntity::new);

        entity.setAiProvider(normalize(request.getAiProvider(), DEFAULT_AI_PROVIDER));
        entity.setOllamaBaseUrl(normalize(request.getOllamaBaseUrl(), DEFAULT_OLLAMA_BASE_URL));
        entity.setOllamaModel(normalize(request.getOllamaModel(), DEFAULT_OLLAMA_MODEL));
        entity.setOpenAiModel(normalize(request.getOpenAiModel(), DEFAULT_OPENAI_MODEL));
        entity.setAwsRegion(normalize(request.getAwsRegion(), DEFAULT_AWS_REGION));
        entity.setBedrockModelId(normalize(request.getBedrockModelId(), DEFAULT_BEDROCK_MODEL_ID));
        entity.setHuggingFaceModel(normalize(request.getHuggingFaceModel(), DEFAULT_HF_MODEL));

        UserRuntimeConfigEntity saved = repository.save(entity);
        return toResponse(saved);
    }

    private RuntimeConfigResponse toResponse(UserRuntimeConfigEntity e) {
        RuntimeConfigResponse response = new RuntimeConfigResponse();

        response.setAiProvider(coalesce(e.getAiProvider(), DEFAULT_AI_PROVIDER));
        response.setOllamaBaseUrl(coalesce(e.getOllamaBaseUrl(), DEFAULT_OLLAMA_BASE_URL));
        response.setOllamaModel(coalesce(e.getOllamaModel(), DEFAULT_OLLAMA_MODEL));
        response.setOpenAiModel(coalesce(e.getOpenAiModel(), DEFAULT_OPENAI_MODEL));
        response.setAwsRegion(coalesce(e.getAwsRegion(), DEFAULT_AWS_REGION));
        response.setBedrockModelId(coalesce(e.getBedrockModelId(), DEFAULT_BEDROCK_MODEL_ID));
        response.setHuggingFaceModel(coalesce(e.getHuggingFaceModel(), DEFAULT_HF_MODEL));

        return response;
    }

    private String normalize(String value, String defaultValue) {
        if (value == null)
            return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty())
            return null;
        if (trimmed.equals(defaultValue))
            return null;
        return trimmed;
    }

    private String coalesce(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }

}
