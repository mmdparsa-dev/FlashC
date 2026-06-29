package com.mmdparsadev.engine

import com.mmdparsadev.engine.plugins.*

object ConversionEngine {
    val plugins = listOf(
        ImageConverterPlugin(),
        VideoConverterPlugin(),
        AudioConverterPlugin(),
        UnitConverterPlugin(),
        CurrencyConverterPlugin(),
        TextConverterPlugin()
    )

    fun findPluginFor(inputType: String, outputType: String): ConversionPlugin? {
        val cleanInput = getExtensionFromType(inputType)
        val cleanOutput = getExtensionFromType(outputType)
        return plugins.find { it.canHandle(cleanInput, cleanOutput) }
    }

    fun getSuggestedFormats(inputType: String): List<String> {
        val cleanInput = getExtensionFromType(inputType)
        val formats = mutableListOf<String>()
        plugins.forEach { plugin ->
            val matchesInput = plugin.metadata.supportedInputTypes.any { supported ->
                supported == cleanInput || (supported.endsWith("/*") && cleanInput.startsWith(supported.substring(0, supported.length - 2)))
            }
            if (matchesInput) {
                formats.addAll(plugin.metadata.supportedOutputTypes)
            }
        }
        return formats.distinct().filter { it != cleanInput && !it.contains("/") }
    }

    private fun getExtensionFromType(type: String): String {
        val lower = type.lowercase().trim()
        return when {
            lower.contains("/") -> {
                // Parse mime type e.g. "image/png" -> "png"
                val subtype = lower.substringAfter("/")
                if (subtype == "jpeg") "jpg" else subtype
            }
            lower.startsWith(".") -> lower.substring(1)
            else -> lower
        }
    }
}
