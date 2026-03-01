package com.cartandcook.core.ai;


import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class VisionResult {

    @Singular
    List<Prediction> predictions;

    public Prediction topPrediction() {
        return predictions.isEmpty() ? null : predictions.get(0);
    }

    @Value
    @Builder
    public static class Prediction {
        String label;
        double confidence; // 0.0 – 1.0
    }
}
