package com.cartandcook.selfhosted.contracts;

import lombok.Data;

import java.util.List;

@Data
public class RuntimeConfigResponse {
    private String dbUrl;
    private String dbUsername;
    private String dbPassword;
    private String oauth2IssuerUri;
    private String port;
    private boolean autoRestartOnConfigSave;
    private boolean dbSafeMode;
    private boolean lastKnownGoodDbConfigured;

    private String aiProvider;
    private String ollamaBaseUrl;
    private String ollamaModel;
    private String openAiApiKey;
    private String openAiModel;
    private String awsRegion;
    private String bedrockModelId;
    private String huggingFaceApiKey;
    private String huggingFaceModel;

    private boolean restartRequired;
    private List<String> restartRequiredKeys;
}
