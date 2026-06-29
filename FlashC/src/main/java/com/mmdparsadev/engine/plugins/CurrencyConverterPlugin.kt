package com.mmdparsadev.engine.plugins

import android.content.Context
import com.mmdparsadev.data.preferences.AppPreferences
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CurrencyConverterPlugin : ConversionPlugin {

    override val metadata = PluginMetadata(
        id = "currency_converter",
        name = "Currency Converter",
        description = "Converts between Iranian Rials, Tomans, USD, and EUR with real-time exchange rates.",
        category = "Currency",
        supportedInputTypes = listOf("text/plain", "txt", "text/*"),
        supportedOutputTypes = listOf("text/plain", "txt")
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    override suspend fun convert(
        context: Context,
        inputName: String,
        inputType: String,
        outputType: String,
        inputProvider: () -> InputStream?,
        outputProvider: () -> OutputStream?,
        onProgress: (Float) -> Unit,
        isCancelled: () -> Boolean
    ): ConversionResult {
        return try {
            val text = inputProvider()?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (text.isBlank()) return ConversionResult.Success("Nothing to convert", "")

            val prefs = AppPreferences(context)
            if (!prefs.isCurrencyEnabled) {
                outputProvider()?.use { it.write(text.toByteArray()) }
                return ConversionResult.Success("Skipped", "Disabled in settings")
            }

            if (prefs.isCurrencySyncEnabled) {
                runCatching { syncRate(prefs) }
            }

            val convertedText = convertCurrencyInText(
                input = text,
                usdRateInTomans = prefs.lastUsdRate.takeIf { it > 0f } ?: 70000f,
                eurRateInTomans = prefs.lastEurRate.takeIf { it > 0f } ?: ( (prefs.lastUsdRate.takeIf { it > 0f } ?: 70000f) * 1.09f),
                prefDisplay = prefs.preferredCurrencyDisplay
            )

            outputProvider()?.use { it.write(convertedText.toByteArray()) }
            ConversionResult.Success("Converted", "Success")
        } catch (e: Exception) {
            ConversionResult.Error(e, "Failed: ${e.localizedMessage}")
        }
    }

    suspend fun syncRate(prefs: AppPreferences) {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            if (now - prefs.lastRateFetchTime < 3_600_000L) return@withContext

            try {
                val latest = fetchRatesFromApi() ?: fetchRatesFromHtml()
                
                if (latest == null) {
                    android.util.Log.e("CurrencyConverter", "Failed to fetch rates from both API and HTML source")
                    // Update fetch time to avoid hammering the server, but maybe retry sooner
                    prefs.lastRateFetchTime = now - 3_000_000L // Retry in 5 minutes
                    return@withContext
                }

                latest.usd?.takeIf { it > 0f }?.let { prefs.lastUsdRate = it }
                latest.eur?.takeIf { it > 0f }?.let { prefs.lastEurRate = it }

                if (prefs.lastEurRate <= 0f && prefs.lastUsdRate > 0f) {
                    prefs.lastEurRate = prefs.lastUsdRate * 1.09f
                }
                prefs.lastRateFetchTime = now
            } catch (e: Exception) {
                android.util.Log.e("CurrencyConverter", "Error syncing rates: ${e.message}", e)
                prefs.lastRateFetchTime = now - 3_000_000L // Retry in 5 minutes
            }
        }
    }

    private data class RateSnapshot(val usd: Float?, val eur: Float?)

    private fun fetchRatesFromApi(): RateSnapshot? = try {
        val request = Request.Builder().url("https://api.tgju.org/v1/market/indicator/summary").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) null else parseRatesFromText(response.body?.string().orEmpty())
        }
    } catch (e: Exception) { null }

    private fun fetchRatesFromHtml(): RateSnapshot? = try {
        val request = Request.Builder().url("https://www.tgju.org/").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) null else parseRatesFromText(response.body?.string().orEmpty())
        }
    } catch (e: Exception) { null }

    private fun parseRatesFromText(text: String): RateSnapshot {
        val normalized = normalizeNumerals(text)
        fun find(keys: List<String>): Float? {
            for (key in keys) {
                val regex = Regex("""$key.*?(\d[\d,.]*)""", RegexOption.IGNORE_CASE)
                regex.find(normalized)?.groupValues?.get(1)?.let {
                    val clean = it.replace(",", "").toFloatOrNull()
                    if (clean != null && clean > 1000f) return if (clean > 500000f) clean / 10f else clean
                }
            }
            return null
        }
        return RateSnapshot(find(listOf("price_dollar", "usd")), find(listOf("price_eur", "eur", "euro")))
    }

    fun convertCurrencyInText(input: String, usdRateInTomans: Float, eurRateInTomans: Float, prefDisplay: String): String {
        var result = normalizeNumerals(input)
        val usdRate = usdRateInTomans.takeIf { it > 0f } ?: 70000f
        val eurRate = eurRateInTomans.takeIf { it > 0f } ?: (usdRate * 1.09f)

        // Regex to match numbers with currency units (mandatory)
        val numRegex = """([0-9]+(?:[\.,][0-9]+)?)"""
        val spaceRegex = """[\s\u200C]*"""
        val regex = Regex("""$numRegex$spaceRegex(تومان|Tomans?|ریال|Rials?|\$|USD|€|EUR|دلار|یورو)""", RegexOption.IGNORE_CASE)
        
        return try {
            regex.replace(result) { match ->
                try {
                    val numberStr = match.groupValues[1].replace(",", "").replace(".", "")
                    val value = numberStr.toDoubleOrNull() ?: return@replace match.value
                    val unit = match.groupValues[2]
                    if (unit.isNullOrBlank()) return@replace match.value

                    val tomansValue = when {
                        unit.contains("تومان", true) || unit.contains("Toman", true) -> value
                        unit.contains("ریال", true) || unit.contains("Rial", true) -> value / 10.0
                        unit.contains("$") || unit.contains("USD", true) || unit.contains("دلار", true) -> value * usdRate
                        unit.contains("€") || unit.contains("EUR", true) || unit.contains("یورو", true) -> value * eurRate
                        else -> return@replace match.value
                    }

                    val convertedUsd = if (usdRate != 0f) tomansValue / usdRate else 0.0
                    val convertedEur = if (eurRate != 0f) tomansValue / eurRate else 0.0
                    
                    // Format result based on preferred display
                    when (prefDisplay.lowercase()) {
                        "usd" -> String.format("%.2f USD", convertedUsd)
                        "eur" -> String.format("%.2f EUR", convertedEur)
                        else -> String.format("%,.0f Tomans", tomansValue)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CurrencyConverter", "Error parsing match: ${match.value}", e)
                    match.value
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CurrencyConverter", "Error replacing currency in text", e)
            result
        }
    }
}
