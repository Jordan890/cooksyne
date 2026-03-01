package com.cartandcook.core.ai;

import java.io.IOException;

public interface TextExtractor {
    TextExtractionResult extract(byte[] imageBytes) throws IOException;
}
