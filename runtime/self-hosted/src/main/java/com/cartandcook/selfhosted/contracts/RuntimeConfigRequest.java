package com.cartandcook.selfhosted.contracts;

import lombok.Data;

@Data
public class RuntimeConfigRequest {
    private String aiProvider;
    private String ollamaBaseUrl;
    private String ollamaModel;
    private String openAiModel;
    private String awsRegion;
    private String bedrockModelId;
    private String huggingFaceModel;
}
