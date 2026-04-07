package com.cartandcook.core.api;

/**
 * Shared AI prompt constants used by all provider adapters.
 */
public final class AiPrompts {

  private AiPrompts() {
  }

  public static final String RETRY_PROMPT = """
      Your previous response was not valid JSON. Return ONLY a JSON object like this example:
      {"title":"Pasta","category":"Italian","description":"A simple pasta dish","ingredients":[{"name":"spaghetti","amount":200,"unit":"GRAMS","calories":310}],"estimatedCalories":310}
      No markdown, no explanation. JSON only.
      """;

  public static String foodTitleOnlyPrompt(String dishTitle) {
    return """
        Given the dish "%s", return a JSON object with its ingredients.

        Example output:
        {"title":"Spaghetti Bolognese","category":"Italian","description":"Classic meat sauce pasta","ingredients":[{"name":"spaghetti","amount":200,"unit":"GRAMS","calories":310},{"name":"ground beef","amount":250,"unit":"GRAMS","calories":575},{"name":"tomato sauce","amount":1,"unit":"CUPS","calories":78},{"name":"onion","amount":1,"unit":"COUNT","calories":44},{"name":"garlic","amount":2,"unit":"COUNT","calories":9},{"name":"olive oil","amount":2,"unit":"TBSP","calories":238}],"estimatedCalories":1254}

        Rules:
        - unit must be one of: GRAMS, OUNCES, POUNDS, CUPS, TBSP, TSP, ML, LITERS, COUNT
        - amount and calories are numbers, not strings
        - estimatedCalories = sum of all ingredient calories
        - Return ONLY JSON. No markdown, no explanation.

        If the dish is unknown: {"title":"Unknown Dish","category":"unknown","description":"","ingredients":[],"estimatedCalories":0}
        """
        .formatted(dishTitle);
  }

  public static String recipeTextPrompt(String extractedText) {
    return """
        Parse this recipe text into a JSON object with ingredients.

        --- RECIPE TEXT ---
        %s
        --- END ---

        Example output:
        {"title":"Banana Bread","category":"Baking","description":"Moist banana bread","ingredients":[{"name":"banana","amount":3,"unit":"COUNT","calories":315},{"name":"flour","amount":2,"unit":"CUPS","calories":910},{"name":"sugar","amount":0.5,"unit":"CUPS","calories":387},{"name":"butter","amount":3,"unit":"TBSP","calories":306},{"name":"egg","amount":1,"unit":"COUNT","calories":72}],"estimatedCalories":1990}

        Rules:
        - unit must be one of: GRAMS, OUNCES, POUNDS, CUPS, TBSP, TSP, ML, LITERS, COUNT
        - amount and calories are numbers, not strings
        - estimatedCalories = sum of all ingredient calories
        - Extract ALL ingredients from the text
        - Return ONLY JSON. No markdown, no explanation.

        If unparseable: {"title":"Unknown Dish","category":"unknown","description":"","ingredients":[],"estimatedCalories":0}
        """
        .formatted(extractedText);
  }

  public static String estimateCaloriesPrompt(String recipeName, String ingredientsSummary, String servingSize) {
    return """
        Estimate the total calories for one serving of the dish "%s".

        Serving size: %s

        Ingredients:
        %s

        Return ONLY a JSON object with one field:
        {"estimatedCalories": <number>}

        Rules:
        - estimatedCalories is the total calorie count for one serving of the given serving size
        - Consider proportion — if the serving size is a fraction of the total recipe, scale calories accordingly
        - Return ONLY JSON. No markdown, no explanation.
        """
        .formatted(recipeName, servingSize, ingredientsSummary);
  }
}
