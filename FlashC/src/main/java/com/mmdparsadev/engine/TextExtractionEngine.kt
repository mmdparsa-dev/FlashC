package com.mmdparsadev.engine

object TextExtractionEngine {
    // Stricter whitelist of supported units and currencies to reduce false positives
    private val SUPPORTED_KEYWORDS = listOf(
        "تومان", "Tomans?", "ریال", "Rials?", "USD", "EUR", "دلار", "یورو",
        "meters?", "meter", "متر", "km", "کیلو[‌\\u200C]?متر", "miles?", "mile", "mi", "مایل",
        "inches?", "inch", "in", "اینچ", "feet", "foot", "ft", "فوت",
        "kg", "کیلو[‌\\u200C]?گرم", "lbs?", "lb", "grams?", "gram", "g", "گرم",
        "m/s", "mps", "km/h", "kph", "mph", "sq\\s*m", "m²", "acres?", "ac", "ha",
        "liters?", "liter", "L", "لیتر", "gallons?", "gallon", "gal", "ml", "میلی[‌\\u200C]?لیتر",
        "GB", "MB", "TB", "bar", "psi", "cal", "kcal", "kWh", "kW", "hp", "rad", "deg", "°"
    ).joinToString("|")

    private val CONVERTIBLE_PATTERN = Regex(
        """([0-9۰-۹]+(?:[\.,][0-9۰-۹]+)?)[\s\u200C]*($SUPPORTED_KEYWORDS|[\$€])""",
        RegexOption.IGNORE_CASE
    )

    fun extractConvertibleValues(texts: List<String>): List<String> {
        return texts.filter { text ->
            // Ensure it's not just a standalone number and matches our whitelist
            CONVERTIBLE_PATTERN.containsMatchIn(text)
        }
    }
}
