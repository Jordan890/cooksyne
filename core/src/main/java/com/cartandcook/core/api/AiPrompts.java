package com.cartandcook.core.api;

/**
 * Shared AI prompt constants used by all provider adapters.
 */
public final class AiPrompts {

  private AiPrompts() {
  }

  public static final String RETRY_PROMPT = """
      Your previous response was not valid JSON.

      You MUST return ONLY a valid JSON object matching this exact schema:

      {
        "title": "string",
        "category": "string",
        "description": "string",
        "ingredients": [
          {
            "name": "string",
            "amount": number,
            "unit": "string",
            "calories": number
          }
        ],
        "estimatedCalories": number
      }

      Rules:
      - Return ONLY valid JSON
      - unit must be one of: GRAMS, OUNCES, POUNDS, CUPS, TBSP, TSP, ML, LITERS, COUNT
      - Do NOT include markdown, code fences, or backticks
      - Do NOT include any text outside the JSON object
      - Do NOT include explanations or commentary
      - estimatedCalories must equal the sum of all ingredient calories

      If you cannot produce the result, return:
      {"title":"Unknown Dish","category":"unknown","description":"","ingredients":[],"estimatedCalories":0}
      """;

  /**
   * Build a food analysis prompt for text-only (no image).
   * The user provides only a dish name and the AI generates ingredients from
   * knowledge.
   */
  public static String foodTitleOnlyPrompt(String dishTitle) {
    return """
        You are a food analysis AI. The user has provided the name of a dish: "%s"

        Based on your knowledge of this dish, return a JSON object describing a standard recipe for it.

        You MUST return ONLY valid JSON matching this exact schema:

        {
          "title": "string",
          "category": "string",
          "description": "string",
          "ingredients": [
            {
              "name": "string",
              "amount": number,
              "unit": "string",
              "calories": number
            }
          ],
          "estimatedCalories": number
        }

        Field definitions:
        - title: Name of the dish (use the provided dish name)
        - category: A single-word or short food category (e.g. "seafood", "pasta", "salad", "dessert", "soup")
        - description: A brief 1-2 sentence description of the dish
        - ingredients: List of ingredients typically used in this dish
        - name: Normalized common food name (e.g. "fresh diced roma tomatoes" becomes "tomato")
        - amount: Numeric quantity (e.g. 1, 2.5, 0.5)
        - unit: One of the following enum values ONLY: GRAMS, OUNCES, POUNDS, CUPS, TBSP, TSP, ML, LITERS, COUNT
        - calories: Estimated calories for that ingredient amount
        - estimatedCalories: Total calories for the entire dish (must equal the sum of all ingredient calories)

        CRITICAL unit selection rules — you MUST follow these guidelines:
        - GRAMS: Use for solid ingredients measured by weight (e.g. chicken breast → 200 GRAMS, cheese → 100 GRAMS, flour → 250 GRAMS)
        - OUNCES: Use for smaller weight-based measurements (e.g. cream cheese → 8 OUNCES)
        - POUNDS: Use for larger weight-based measurements (e.g. ground beef → 1 POUNDS)
        - CUPS: Use for volume-based dry or liquid ingredients (e.g. rice → 1 CUPS, milk → 0.5 CUPS, broth → 2 CUPS)
        - TBSP: Use for tablespoon-sized amounts (e.g. olive oil → 2 TBSP, soy sauce → 1 TBSP, butter → 1 TBSP)
        - TSP: Use for teaspoon-sized amounts (e.g. salt → 1 TSP, pepper → 0.5 TSP, vanilla extract → 1 TSP)
        - ML: Use for precise liquid measurements in milliliters (e.g. wine → 120 ML)
        - LITERS: Use for large liquid volumes (e.g. water → 1 LITERS, stock → 0.5 LITERS)
        - COUNT: Use ONLY for whole countable items (e.g. eggs → 2 COUNT, garlic cloves → 3 COUNT, bay leaves → 2 COUNT)
        - DO NOT default to COUNT. Most ingredients should use GRAMS, CUPS, TBSP, or TSP.
        - If unsure, prefer GRAMS for solids and CUPS for liquids over COUNT.

        Strict output rules:
        - Return ONLY valid JSON
        - Do NOT include markdown, code fences, or backticks
        - Do NOT include any text outside the JSON object
        - Do NOT include explanations or commentary
        - Keep JSON compact: use short descriptions and concise ingredient names to stay within output limits
        - Include ALL ingredients — do NOT stop mid-array

        If the dish cannot be identified, return:
        {"title":"Unknown Dish","category":"unknown","description":"","ingredients":[],"estimatedCalories":0}
        """
        .formatted(dishTitle);
  }

  /**
   * Build a recipe analysis prompt from OCR-extracted text.
   * The user uploaded a recipe image, Tesseract extracted the text, and now
   * the AI parses the extracted text into structured recipe JSON.
   */
  public static String recipeTextPrompt(String extractedText) {
    return """
        You are a food analysis AI. The user uploaded a photo of a recipe and OCR was used to extract the following text:

        --- BEGIN EXTRACTED TEXT ---
        %s
        --- END EXTRACTED TEXT ---

        Parse this text and return a JSON object describing the recipe.

        You MUST return ONLY valid JSON matching this exact schema:

        {
          "title": "string",
          "category": "string",
          "description": "string",
          "ingredients": [
            {
              "name": "string",
              "amount": number,
              "unit": "string",
              "calories": number
            }
          ],
          "estimatedCalories": number
        }

        Field definitions:
        - title: Name of the recipe as found in the extracted text
        - category: A single-word or short food category (e.g. "seafood", "pasta", "salad", "dessert", "soup")
        - description: A brief 1-2 sentence description of the recipe
        - ingredients: List of ingredients from the recipe
        - name: Normalized common food name (e.g. "fresh diced roma tomatoes" becomes "tomato")
        - amount: Numeric quantity exactly as listed in the recipe (e.g. 1, 2.5, 0.5)
        - unit: One of the following enum values ONLY: GRAMS, OUNCES, POUNDS, CUPS, TBSP, TSP, ML, LITERS, COUNT
        - calories: Estimated calories for that ingredient amount
        - estimatedCalories: Total calories for the entire recipe (must equal the sum of all ingredient calories)

        CRITICAL unit selection rules — you MUST follow these guidelines:
        - GRAMS: Use for solid ingredients measured by weight (e.g. chicken breast → 200 GRAMS, cheese → 100 GRAMS, flour → 250 GRAMS)
        - OUNCES: Use for smaller weight-based measurements (e.g. cream cheese → 8 OUNCES)
        - POUNDS: Use for larger weight-based measurements (e.g. ground beef → 1 POUNDS)
        - CUPS: Use for volume-based dry or liquid ingredients (e.g. rice → 1 CUPS, milk → 0.5 CUPS, broth → 2 CUPS)
        - TBSP: Use for tablespoon-sized amounts (e.g. olive oil → 2 TBSP, soy sauce → 1 TBSP, butter → 1 TBSP)
        - TSP: Use for teaspoon-sized amounts (e.g. salt → 1 TSP, pepper → 0.5 TSP, vanilla extract → 1 TSP)
        - ML: Use for precise liquid measurements in milliliters (e.g. wine → 120 ML)
        - LITERS: Use for large liquid volumes (e.g. water → 1 LITERS, stock → 0.5 LITERS)
        - COUNT: Use ONLY for whole countable items (e.g. eggs → 2 COUNT, garlic cloves → 3 COUNT, bay leaves → 2 COUNT)
        - DO NOT default to COUNT. Most ingredients should use GRAMS, CUPS, TBSP, or TSP.
        - If the recipe text specifies a unit (e.g. "2 cups flour"), map it to the matching enum value.
        - If unsure, prefer GRAMS for solids and CUPS for liquids over COUNT.

        Ingredient extraction rules:
        - Extract all ingredients listed in the extracted text
        - Preserve the amounts as written in the text
        - Use normalized common food names, not descriptions
        - If the OCR text is garbled or incomplete, do your best to interpret it

        Calorie estimation rules:
        - Use standard nutritional knowledge
        - estimatedCalories must equal the sum of all ingredient calories

        Strict output rules:
        - Return ONLY valid JSON
        - Do NOT include markdown, code fences, or backticks
        - Do NOT include any text outside the JSON object
        - Do NOT include explanations or commentary
        - Keep JSON compact: use short descriptions and concise ingredient names to stay within output limits
        - Include ALL ingredients — do NOT stop mid-array

        If the text cannot be parsed into a recipe, return:
        {"title":"Unknown Dish","category":"unknown","description":"","ingredients":[],"estimatedCalories":0}
        """
        .formatted(extractedText);
  }
}
