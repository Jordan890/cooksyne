package com.cartandcook.core.ai;

public interface TextExtractor {
    TextExtractionResult extract(byte[] imageBytes);
}
