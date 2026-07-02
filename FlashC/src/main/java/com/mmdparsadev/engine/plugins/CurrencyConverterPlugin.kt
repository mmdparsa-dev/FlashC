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
        description = "Converts between Iranian Rials, Tomans, USD, and EUR offline.",
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
                eurRateInTomans = prefs.lastEurRate.takeIf { it > 0f } ?: 76000f,
                showUsd = prefs.showUsdConversion,
                showEur = prefs.showEurConversion
            )

            outputProvider()?.use { it.write(convertedText.toByteArray()) }
            ConversionResult.Success("Converted", "Success")
        } catch (e: Exception) {
            ConversionResult.Error(e, "Failed: ${e.localizedMessage}")
        }
    }

    companion object {
        private var lastAttemptTime = 0L
    }

    suspend fun syncRate(prefs: AppPreferences, force: Boolean = false) {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            if (!force && now - prefs.lastRateFetchTime < 3_600_000L) return@withContext
            if (!force && now - lastAttemptTime < 300_000L) return@withContext
            lastAttemptTime = now

            try {
                val latest = fetchRatesFromApi() ?: fetchRatesFromHtml()

                if (latest == null) {
                    android.util.Log.e("CurrencyConverter", "Failed to fetch rates from both API and HTML source")
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

    fun convertCurrencyInText(
        input: String,
        usdRateInTomans: Float,
        eurRateInTomans: Float,
        showUsd: Boolean = false,
        showEur: Boolean = false
    ): String {
        val usdRate = usdRateInTomans.takeIf { it > 0f } ?: 70000f
        val eurRate = eurRateInTomans.takeIf { it > 0f } ?: 76000f

        val numRegex = """([0-9۰-۹][0-9۰-۹\.,،/\s\u200C]*)"""
        val spaceRegex = """[\s\u200C]*"""
        val regex = Regex("""$numRegex$spaceRegex(تومان|Tomans?|ریال|Rials?|\$|USD|€|EUR|دلار|یورو)\b""", RegexOption.IGNORE_CASE)

        return try {
            regex.replace(input) { match ->
                try {
                    val originalText = match.value
                    val originalNumberStr = match.groupValues[1]
                    val unit = match.groupValues[2]

                    // Detect script from original input to decide output script
                    val isPersianScript = originalText.any { it in '۰'..'۹' || it == 'ت' || it == 'ر' || it == 'د' || it == 'ی' }
                    
                    val normalizedNumberStr = normalizeNumerals(originalNumberStr)
                        .replace(" ", "")
                        .replace("\u200C", "")
                        .replace("،", ",") // Normalize Arabic comma to English comma
                        .replace("/", ".") // Normalize Persian slash to dot

                    // Improved number parsing:
                    // If multiple separators of same type, they are group separators.
                    val value = try {
                        val dots = normalizedNumberStr.count { it == '.' }
                        val commas = normalizedNumberStr.count { it == ',' }
                        
                        val cleanStr = when {
                            dots > 1 && commas == 0 -> normalizedNumberStr.replace(".", "")
                            commas > 1 && dots == 0 -> normalizedNumberStr.replace(",", "")
                            dots == 1 && commas == 1 -> {
                                // e.g. 1,000.50 or 1.000,50
                                val dotIdx = normalizedNumberStr.indexOf('.')
                                val commaIdx = normalizedNumberStr.indexOf(',')
                                if (dotIdx < commaIdx) {
                                    // 1.000,50 -> comma is decimal
                                    normalizedNumberStr.replace(".", "").replace(",", ".")
                                } else {
                                    // 1,000.50 -> dot is decimal
                                    normalizedNumberStr.replace(",", "")
                                }
                            }
                            dots == 1 -> normalizedNumberStr
                            commas == 1 -> {
                                // In Iran, comma is usually group separator.
                                // But if it's the only one and near the end, could it be decimal?
                                // Let's check the distance from end.
                                val idx = normalizedNumberStr.indexOf(',')
                                if (normalizedNumberStr.length - idx <= 3) {
                                    // treat as decimal e.g. 10,5
                                    normalizedNumberStr.replace(",", ".")
                                } else {
                                    normalizedNumberStr.replace(",", "")
                                }
                            }
                            else -> normalizedNumberStr
                        }
                        cleanStr.toDoubleOrNull() ?: return@replace originalText
                    } catch (e: Exception) {
                        return@replace originalText
                    }

                    var tomansValue = 0.0
                    var detectedType = "" // toman, rial, usd, eur

                    when {
                        unit.contains("تومان", true) || unit.contains("Toman", true) -> {
                            tomansValue = value
                            detectedType = "toman"
                        }
                        unit.contains("ریال", true) || unit.contains("Rial", true) -> {
                            tomansValue = value / 10.0
                            detectedType = "rial"
                        }
                        unit.contains("$") || unit.contains("USD", true) || unit.contains("دلار", true) -> {
                            tomansValue = value * usdRate
                            detectedType = "usd"
                        }
                        unit.contains("€") || unit.contains("EUR", true) || unit.contains("یورو", true) -> {
                            tomansValue = value * eurRate
                            detectedType = "eur"
                        }
                        else -> return@replace originalText
                    }

                    val results = mutableListOf<String>()

                    // Always show Toman and Rial
                    if (detectedType != "toman") {
                        val tomanVal = String.format("%,.0f", tomansValue)
                        results.add("≈ ${formatNumerals(tomanVal, isPersianScript)}${if (isPersianScript) " تومان" else " Toman"}")
                    }
                    if (detectedType != "rial") {
                        val rialVal = String.format("%,.0f", tomansValue * 10)
                        results.add("≈ ${formatNumerals(rialVal, isPersianScript)}${if (isPersianScript) " ریال" else " Rial"}")
                    }

                    // 2. USD Conversion
                    if (showUsd && detectedType != "usd") {
                        val usdVal = String.format("%.2f", tomansValue / usdRate)
                        results.add("≈ ${formatNumerals(usdVal, isPersianScript)}${if (isPersianScript) " دلار" else " USD"}")
                    }

                    // 3. EUR Conversion
                    if (showEur && detectedType != "eur") {
                        val eurVal = String.format("%.2f", tomansValue / eurRate)
                        results.add("≈ ${formatNumerals(eurVal, isPersianScript)}${if (isPersianScript) " یورو" else " EUR"}")
                    }

                    // Grouped result: Original input \n ≈ Conversion 1 \n ≈ Conversion 2...
                    if (results.isEmpty()) return@replace originalText
                    "${originalText}\n${results.joinToString("\n")}"
                } catch (e: Exception) {
                    match.value
                }
            }
        } catch (e: Exception) {
            input
        }
    }
}
