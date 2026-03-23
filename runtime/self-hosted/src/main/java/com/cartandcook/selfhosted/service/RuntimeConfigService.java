package com.cartandcook.selfhosted.service;

import com.cartandcook.adapters.persistencejpa.SpringDataUserRuntimeConfigRepository;
import com.cartandcook.adapters.persistencejpa.UserRuntimeConfigEntity;
import com.cartandcook.selfhosted.contracts.RuntimeConfigRequest;
import com.cartandcook.selfhosted.contracts.RuntimeConfigResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class RuntimeConfigService {

    private static final String DEFAULT_DB_URL = "jdbc:postgresql://localhost:5432/cart_and_cook";
    private static final String DEFAULT_DB_USERNAME = "postgres";
    private static final String DEFAULT_DB_PASSWORD = "";
    private static final String DEFAULT_OAUTH_ISSUER = "http://localhost:8080/realms/cart_and_cook";
    private static final String DEFAULT_PORT = "8081";
    private static final boolean DEFAULT_AUTO_RESTART_ON_CONFIG_SAVE = true;

    private static final String DEFAULT_AI_PROVIDER = "";
    private static final String DEFAULT_OLLAMA_BASE_URL = "http://localhost:11434";
    private static final String DEFAULT_OLLAMA_MODEL = "llava-phi3";
    private static final String DEFAULT_OPENAI_API_KEY = "";
    private static final String DEFAULT_OPENAI_MODEL = "gpt-4o-mini";
    private static final String DEFAULT_AWS_REGION = "us-east-1";
    private static final String DEFAULT_BEDROCK_MODEL_ID = "anthropic.claude-3-haiku-20240307-v1:0";
    private static final String DEFAULT_HF_API_KEY = "";
    private static final String DEFAULT_HF_MODEL = "Salesforce/blip-image-captioning-large";

    private static final List<String> RESTART_REQUIRED_KEYS = List.of(
            "dbUrl",
            "dbUsername",
            "dbPassword",
            "oauth2IssuerUri",
            "port");

    private final SpringDataUserRuntimeConfigRepository repository;
    private final ConfigCryptoService cryptoService;

    public RuntimeConfigService(
            SpringDataUserRuntimeConfigRepository repository,
            ConfigCryptoService cryptoService) {
        this.repository = repository;
        this.cryptoService = cryptoService;
    }

    public RuntimeConfigResponse get() {
        UserRuntimeConfigEntity entity = repository.findTopByOrderByIdAsc()
                .orElseGet(UserRuntimeConfigEntity::new);
        RuntimeConfigResponse response = toResponse(entity);
        response.setRestartRequired(false);
        response.setRestartRequiredKeys(RESTART_REQUIRED_KEYS);
        return response;
    }

    public RuntimeConfigResponse save(RuntimeConfigRequest request) {
        UserRuntimeConfigEntity entity = repository.findTopByOrderByIdAsc()
                .orElseGet(UserRuntimeConfigEntity::new);

        RuntimeConfigResponse previous = toResponse(entity);

        entity.setDbUrl(normalize(request.getDbUrl(), DEFAULT_DB_URL));
        entity.setDbUsername(normalize(request.getDbUsername(), DEFAULT_DB_USERNAME));
        entity.setDbPassword(encryptSecret(normalize(request.getDbPassword(), DEFAULT_DB_PASSWORD)));
        entity.setOauth2IssuerUri(normalize(request.getOauth2IssuerUri(), DEFAULT_OAUTH_ISSUER));
        entity.setPort(normalize(request.getPort(), DEFAULT_PORT));
        entity.setAutoRestartOnConfigSave(request.isAutoRestartOnConfigSave());

        entity.setAiProvider(normalize(request.getAiProvider(), DEFAULT_AI_PROVIDER));
        entity.setOllamaBaseUrl(normalize(request.getOllamaBaseUrl(), DEFAULT_OLLAMA_BASE_URL));
        entity.setOllamaModel(normalize(request.getOllamaModel(), DEFAULT_OLLAMA_MODEL));
        entity.setOpenAiApiKey(encryptSecret(normalize(request.getOpenAiApiKey(), DEFAULT_OPENAI_API_KEY)));
        entity.setOpenAiModel(normalize(request.getOpenAiModel(), DEFAULT_OPENAI_MODEL));
        entity.setAwsRegion(normalize(request.getAwsRegion(), DEFAULT_AWS_REGION));
        entity.setBedrockModelId(normalize(request.getBedrockModelId(), DEFAULT_BEDROCK_MODEL_ID));
        entity.setHuggingFaceApiKey(encryptSecret(normalize(request.getHuggingFaceApiKey(), DEFAULT_HF_API_KEY)));
        entity.setHuggingFaceModel(normalize(request.getHuggingFaceModel(), DEFAULT_HF_MODEL));

        UserRuntimeConfigEntity saved = repository.save(entity);
        RuntimeConfigResponse current = toResponse(saved);
        List<String> changedRestartKeys = getChangedRestartKeys(previous, current);
        current.setRestartRequired(!changedRestartKeys.isEmpty());
        current.setRestartRequiredKeys(changedRestartKeys);
        return current;
    }

    private RuntimeConfigResponse toResponse(UserRuntimeConfigEntity e) {
        RuntimeConfigResponse response = new RuntimeConfigResponse();

        response.setDbUrl(coalesce(e.getDbUrl(), DEFAULT_DB_URL));
        response.setDbUsername(coalesce(e.getDbUsername(), DEFAULT_DB_USERNAME));
        response.setDbPassword(coalesce(decryptSecret(e.getDbPassword()), DEFAULT_DB_PASSWORD));
        response.setOauth2IssuerUri(coalesce(e.getOauth2IssuerUri(), DEFAULT_OAUTH_ISSUER));
        response.setPort(coalesce(e.getPort(), DEFAULT_PORT));
        response.setAutoRestartOnConfigSave(
                coalesceBoolean(e.getAutoRestartOnConfigSave(), DEFAULT_AUTO_RESTART_ON_CONFIG_SAVE));

        response.setAiProvider(coalesce(e.getAiProvider(), DEFAULT_AI_PROVIDER));
        response.setOllamaBaseUrl(coalesce(e.getOllamaBaseUrl(), DEFAULT_OLLAMA_BASE_URL));
        response.setOllamaModel(coalesce(e.getOllamaModel(), DEFAULT_OLLAMA_MODEL));
        response.setOpenAiApiKey(coalesce(decryptSecret(e.getOpenAiApiKey()), DEFAULT_OPENAI_API_KEY));
        response.setOpenAiModel(coalesce(e.getOpenAiModel(), DEFAULT_OPENAI_MODEL));
        response.setAwsRegion(coalesce(e.getAwsRegion(), DEFAULT_AWS_REGION));
        response.setBedrockModelId(coalesce(e.getBedrockModelId(), DEFAULT_BEDROCK_MODEL_ID));
        response.setHuggingFaceApiKey(coalesce(decryptSecret(e.getHuggingFaceApiKey()), DEFAULT_HF_API_KEY));
        response.setHuggingFaceModel(coalesce(e.getHuggingFaceModel(), DEFAULT_HF_MODEL));

        return response;
    }

    private List<String> getChangedRestartKeys(RuntimeConfigResponse previous, RuntimeConfigResponse current) {
        List<String> changed = new ArrayList<>();

        if (!Objects.equals(previous.getDbUrl(), current.getDbUrl()))
            changed.add("dbUrl");
        if (!Objects.equals(previous.getDbUsername(), current.getDbUsername()))
            changed.add("dbUsername");
        if (!Objects.equals(previous.getDbPassword(), current.getDbPassword()))
            changed.add("dbPassword");
        if (!Objects.equals(previous.getOauth2IssuerUri(), current.getOauth2IssuerUri()))
            changed.add("oauth2IssuerUri");
        if (!Objects.equals(previous.getPort(), current.getPort()))
            changed.add("port");

        return changed;
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

    private boolean coalesceBoolean(Boolean value, boolean defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String encryptSecret(String value) {
        if (value == null)
            return null;
        return cryptoService.encrypt(value);
    }

    private String decryptSecret(String value) {
        if (value == null)
            return null;
        return cryptoService.decrypt(value);
    }
}
