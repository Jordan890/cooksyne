package com.cartandcook.adapters.ailocal;

import com.cartandcook.core.ai.FoodClassifier;
import com.cartandcook.core.ai.VisionResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "cartcook.ai.mode", havingValue = "local", matchIfMissing = true)
public class LocalFoodClassifier implements FoodClassifier {

    private String modelPath; // bound from application.yml

    // Explicit constructor
    public LocalFoodClassifier() { }

    // Getter and setter for Spring binding
    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

    @Override
    public VisionResult classify(byte[] imageBytes) {
        // TODO: implement real ONNX inference
        // Stub for now
        return VisionResult.builder()
                .prediction(VisionResult.Prediction.builder()
                        .label("pizza")
                        .confidence(0.95)
                        .build())
                .build();
    }
}


