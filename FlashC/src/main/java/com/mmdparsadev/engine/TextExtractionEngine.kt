package com.mmdparsadev.engine

object TextExtractionEngine {
    // Regex for potential measurement: number + optional space/zwnj + unit/currency
    // [0-9۰-۹] matches both English and Persian digits
    private val CONVERTIBLE_PATTERN = Regex(
        """([0-9۰-۹]+(?:[\.,][0-9۰-۹]+)?)[\s\u200C]*([a-zA-Z\u0600-\u06FF]+|[\$€])""",
        RegexOption.IGNORE_CASE
    )

    fun extractConvertibleValues(texts: List<String>): List<String> {
        return texts.filter { text ->
            CONVERTIBLE_PATTERN.containsMatchIn(text)
        }
    }
}
