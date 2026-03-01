package com.cartandcook.core.ai;

public interface FoodClassifier {
    VisionResult classify(byte[] imageBytes);
}
