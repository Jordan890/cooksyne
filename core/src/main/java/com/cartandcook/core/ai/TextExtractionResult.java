package com.cartandcook.core.ai;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class TextExtractionResult {

    String fullText; // raw text

    @Singular
    List<RecipeSegment> segments; // structured pieces of the recipe

    @Value
    @Builder
    public static class RecipeSegment {
        String type;    // e.g., "title", "ingredient", "step"
        String content; // text content
    }
}
