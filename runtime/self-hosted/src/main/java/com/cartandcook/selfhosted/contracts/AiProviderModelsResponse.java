package com.cartandcook.selfhosted.contracts;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AiProviderModelsResponse {
    private Map<String, List<String>> models;
}
