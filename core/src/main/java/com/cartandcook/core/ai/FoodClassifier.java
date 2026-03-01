package com.cartandcook.core.ai;

import java.io.IOException;

public interface FoodClassifier {
    VisionResult classify(byte[] imageBytes) throws IOException;
}
