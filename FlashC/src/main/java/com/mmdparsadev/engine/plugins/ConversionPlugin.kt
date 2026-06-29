package com.mmdparsadev.engine.plugins

import android.content.Context
import java.io.InputStream
import java.io.OutputStream

data class PluginMetadata(
    val id: String,
    val name: String,
    val description: String,
    val category: String, // "Image", "Video", "Audio", "Unit", "Currency", "Text"
    val supportedInputTypes: List<String>,
    val supportedOutputTypes: List<String>
)

interface ConversionPlugin {
    val metadata: PluginMetadata

    fun normalizeNumerals(input: String): String {
        val persian = "۰۱۲۳۴۵۶۷۸۹"
        val arabic = "٠١٢٣٤٥٦٧٨٩"
        val english = "0123456789"
        
        return input.map { char ->
            val pIdx = persian.indexOf(char)
            val aIdx = arabic.indexOf(char)
            when {
                pIdx != -1 -> english[pIdx]
                aIdx != -1 -> english[aIdx]
                else -> char
            }
        }.joinToString("")
    }

    fun canHandle(inputType: String, outputType: String): Boolean {
        val inType = inputType.lowercase().trim()
        val outType = outputType.lowercase().trim()
        
        // Match specific extension or category mime wildcard (e.g. image/*)
        val matchesInput = metadata.supportedInputTypes.any { supported ->
            supported == inType || (supported.endsWith("/*") && inType.startsWith(supported.substring(0, supported.length - 2)))
        }
        val matchesOutput = metadata.supportedOutputTypes.any { supported ->
            supported == outType || (supported.endsWith("/*") && outType.startsWith(supported.substring(0, supported.length - 2)))
        }
        return matchesInput && matchesOutput
    }

    suspend fun convert(
        context: Context,
        inputName: String,
        inputType: String,
        outputType: String,
        inputProvider: () -> InputStream?,
        outputProvider: () -> OutputStream?,
        onProgress: (Float) -> Unit,
        isCancelled: () -> Boolean
    ): ConversionResult
}

sealed class ConversionResult {
    data class Success(val message: String, val details: String = "") : ConversionResult()
    data class Error(val exception: Throwable, val message: String) : ConversionResult()
}
