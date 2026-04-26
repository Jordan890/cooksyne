package com.cooksyne.core.domain;

/**
 * Utility class for converting between different cooking measurement units.
 * Supports conversion between weight and volume measurements.
 */
public class UnitConverter {

    /**
     * Convert a quantity from one unit to another.
     *
     * @param amount the amount to convert
     * @param fromUnit the source unit
     * @param toUnit the target unit
     * @return the converted amount
     * @throws IllegalArgumentException if units are incompatible (e.g., weight to volume)
     */
    public static double convert(double amount, Unit fromUnit, Unit toUnit) {
        if (fromUnit == toUnit) {
            return amount;
        }

        // Convert to base unit (grams for weight, ml for volume)
        double baseAmount = convertToBaseUnit(amount, fromUnit);

        // Convert from base unit to target unit
        return convertFromBaseUnit(baseAmount, toUnit);
    }

    /**
     * Convert a Quantity to a different unit.
     *
     * @param quantity the quantity to convert
     * @param targetUnit the target unit
     * @return a new Quantity with the converted amount and target unit
     */
    public static Quantity convert(Quantity quantity, Unit targetUnit) {
        double convertedAmount = convert(quantity.amount(), quantity.unit(), targetUnit);
        return new Quantity(convertedAmount, targetUnit);
    }

    /**
     * Convert an amount to the base unit (grams for weight, ml for volume).
     */
    private static double convertToBaseUnit(double amount, Unit unit) {
        return switch (unit) {
            // Weight to grams
            case GRAMS -> amount;
            case OUNCES -> amount * 28.3495;  // 1 oz = 28.3495g
            case POUNDS -> amount * 453.592;  // 1 lb = 453.592g

            // Volume to ml
            case ML -> amount;
            case LITERS -> amount * 1000;     // 1L = 1000ml
            case CUPS -> amount * 236.588;    // 1 cup = 236.588ml
            case TBSP -> amount * 14.7868;    // 1 tbsp = 14.7868ml
            case TSP -> amount * 4.92892;     // 1 tsp = 4.92892ml

            case COUNT -> throw new IllegalArgumentException("Cannot convert COUNT unit to a base unit");
        };
    }

    /**
     * Convert an amount from the base unit to the target unit.
     */
    private static double convertFromBaseUnit(double baseAmount, Unit unit) {
        return switch (unit) {
            // From grams
            case GRAMS -> baseAmount;
            case OUNCES -> baseAmount / 28.3495;
            case POUNDS -> baseAmount / 453.592;

            // From ml
            case ML -> baseAmount;
            case LITERS -> baseAmount / 1000;
            case CUPS -> baseAmount / 236.588;
            case TBSP -> baseAmount / 14.7868;
            case TSP -> baseAmount / 4.92892;

            case COUNT -> throw new IllegalArgumentException("Cannot convert COUNT unit to a base unit");
        };
    }

    /**
     * Check if two units are compatible (both weight or both volume).
     *
     * @param unit1 the first unit
     * @param unit2 the second unit
     * @return true if units are compatible, false otherwise
     */
    public static boolean isCompatible(Unit unit1, Unit unit2) {
        return isWeightUnit(unit1) == isWeightUnit(unit2);
    }

    /**
     * Check if a unit is a weight measurement.
     *
     * @param unit the unit to check
     * @return true if the unit is a weight measurement, false if volume
     */
    public static boolean isWeightUnit(Unit unit) {
        return switch (unit) {
            case GRAMS, OUNCES, POUNDS -> true;
            case ML, COUNT, LITERS, CUPS, TBSP, TSP -> false;
        };
    }
}

