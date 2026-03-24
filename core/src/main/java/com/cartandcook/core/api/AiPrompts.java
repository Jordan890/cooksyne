package com.cartandcook.core.api;

/**
 * Shared AI prompt constants used by all provider adapters.
 */
public final class AiPrompts {

  private AiPrompts() {
  }

  public static final String FOOD_IMAGE_PROMPT = """
      You are a food analysis AI. The provided image is a photo of a prepared dish.

      Analyze the image and return a JSON object describing the recipe.

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
      - title: Name of the dish
      - category: A single-word or short food category (e.g. "seafood", "pasta", "salad", "dessert", "soup")
      - description: A brief 1-2 sentence description of the dish
      - ingredients: List of ingredients detected in the dish
      - name: Normalized common food name (e.g. "fresh diced roma tomatoes" becomes "tomato")
      - amount: Numeric quantity (e.g. 1, 2.5, 0.5)
      - unit: One of the following enum values ONLY: GRAMS, OUNCES, POUNDS, CUPS, TBSP, TSP, ML, LITERS, COUNT
        - Use GRAMS/OUNCES/POUNDS for weight-based ingredients (meats, cheeses, flour, etc.)
        - Use CUPS/TBSP/TSP/ML/LITERS for volume-based ingredients (liquids, oils, sauces, etc.)
        - Use COUNT for countable items (eggs, cloves of garlic, bay leaves, etc.)
      - calories: Estimated calories for that ingredient amount
      - estimatedCalories: Total calories for the entire dish (must equal the sum of all ingredient calories)

      Ingredient detection rules:
      - Detect vegetables, meats, grains, oils, spices, and sauces
      - Use normalized common food names, not descriptions
      - Estimate realistic cooking quantities for each ingredient
      - If an ingredient is unclear, choose the most likely common cooking ingredient

      Calorie estimation rules:
      - Use standard nutritional knowledge (e.g. 1 cup cooked rice = 200 cal, 100g chicken breast = 165 cal, 1 tbsp olive oil = 120 cal)
      - estimatedCalories must equal the sum of all ingredient calories

      Strict output rules:
      - Return ONLY valid JSON
      - Do NOT include markdown, code fences, or backticks
      - Do NOT include any text outside the JSON object
      - Do NOT include explanations or commentary

      If the image cannot be analyzed, return:
      {"title":"Unknown Dish","category":"unknown","description":"","ingredients":[],"estimatedCalories":0}
      """;

  public static final String RECIPE_IMAGE_PROMPT = """
      You are a food analysis AI. The provided image is a recipe page or screenshot.

      Extract the recipe information and return a JSON object.

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
      - title: Name of the recipe as written
      - category: A single-word or short food category (e.g. "seafood", "pasta", "salad", "dessert", "soup")
      - description: A brief 1-2 sentence description of the recipe
      - ingredients: List of ingredients from the recipe
      - name: Normalized common food name (e.g. "fresh diced roma tomatoes" becomes "tomato")
      - amount: Numeric quantity (e.g. 1, 2.5, 0.5)
      - unit: One of the following enum values ONLY: GRAMS, OUNCES, POUNDS, CUPS, TBSP, TSP, ML, LITERS, COUNT
        - Use GRAMS/OUNCES/POUNDS for weight-based ingredients (meats, cheeses, flour, etc.)
        - Use CUPS/TBSP/TSP/ML/LITERS for volume-based ingredients (liquids, oils, sauces, etc.)
        - Use COUNT for countable items (eggs, cloves of garlic, bay leaves, etc.)
      - amount: Amount exactly as listed in the recipe (e.g. 1, 2.5, 0.5)
      - calories: Estimated calories for that ingredient amount
      - estimatedCalories: Total calories for the entire recipe (must equal the sum of all ingredient calories)

      Ingredient extraction rules:
      - Determine the title first by reading the top of the page/screenshot
      - Prefer the most prominent heading near the top (H1/title-style text) as the dish name
      - Ignore site branding, author names, section labels, and navigation text when choosing the title
      - If multiple candidates exist, choose the top-most, dish-specific heading
      - Extract all ingredients listed in the recipe
      - Preserve the amounts as written in the recipe
      - Use normalized common food names, not descriptions
      - Detect vegetables, meats, grains, oils, spices, and sauces

      Calorie estimation rules:
      - Use standard nutritional knowledge (e.g. 1 cup cooked rice = 200 cal, 100g chicken breast = 165 cal, 1 tbsp olive oil = 120 cal)
      - estimatedCalories must equal the sum of all ingredient calories

      Strict output rules:
      - Return ONLY valid JSON
      - Do NOT include markdown, code fences, or backticks
      - Do NOT include any text outside the JSON object
      - Do NOT include explanations or commentary

      If the image cannot be analyzed, return:
      {"title":"Unknown Dish","category":"unknown","description":"","ingredients":[],"estimatedCalories":0}
      """;

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
}
