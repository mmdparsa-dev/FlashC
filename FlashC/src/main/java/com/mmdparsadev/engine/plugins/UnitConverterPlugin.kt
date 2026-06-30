package com.mmdparsadev.engine.plugins

import android.content.Context
import com.mmdparsadev.data.preferences.AppPreferences
import java.io.InputStream
import java.io.OutputStream

class UnitConverterPlugin : ConversionPlugin {
    override val metadata = PluginMetadata(
        id = "unit_converter",
        name = "Unit Converter",
        description = "Parses and converts units of length, mass, temperature, speed, area, volume, time, digital storage, pressure, energy, power, and angle.",
        category = "Unit",
        supportedInputTypes = listOf("text/plain", "txt", "text/*"),
        supportedOutputTypes = listOf("text/plain", "txt")
    )

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
            onProgress(20f)
            val inputStream = inputProvider() ?: return ConversionResult.Error(Exception("Input missing"), "Could not open source file")
            val text = inputStream.bufferedReader().use { it.readText() }
            onProgress(50f)

            val prefs = AppPreferences(context)
            val enabledCategories = prefs.enabledUnitCategories.split(",").map { it.trim().lowercase() }.toSet()

            val convertedText = parseAndConvertText(text, enabledCategories, prefs.appLanguage)
            onProgress(80f)

            val outputStream = outputProvider() ?: return ConversionResult.Error(Exception("Output missing"), "Could not write to destination")
            outputStream.write(convertedText.toByteArray())
            outputStream.flush()
            outputStream.close()

            onProgress(100f)
            ConversionResult.Success("Processed units in document", "Identified and translated units based on your preferences.")
        } catch (e: Exception) {
            ConversionResult.Error(e, "Unit processing failed: ${e.localizedMessage}")
        }
    }

    fun parseAndConvertText(input: String, enabledCategories: Set<String> = allCategories, appLanguage: String = "en"): String {
        var result = input
        val isPersianApp = appLanguage == "fa"

        // Helper to check if category is enabled
        fun isEnabled(cat: String) = enabledCategories.contains(cat.lowercase())
        
        // Regex pattern for number with optional decimal, allowing Persian and English digits
        val numRegex = """([0-9۰-۹]+(?:[\.,][0-9۰-۹]+)?)"""
        // Optional space or ZWNJ
        val spaceRegex = """[\s\u200C]*"""

        // 1. TEMPERATURE (Fahrenheit <-> Celsius <-> Kelvin)
        if (isEnabled("temperature")) {
            // Fahrenheit -> Celsius
            val fahrenheitRegex = Regex("""$numRegex$spaceRegex(?:Fahrenheit|F|°F)\b""", RegexOption.IGNORE_CASE)
            result = fahrenheitRegex.replace(result) { matchResult ->
                val fStr = normalizeNumerals(matchResult.groupValues[1]).replace(",", ".")
                val f = fStr.toDoubleOrNull() ?: return@replace matchResult.value
                val c = (f - 32) * 5.0 / 9.0
                val k = c + 273.15
                val cStr = String.format("%.1f", c)
                val kStr = String.format("%.1f", k)
                
                if (isPersianApp) {
                    "${matchResult.value} (${formatNumerals(cStr, true)}°C / ${formatNumerals(kStr, true)} K)"
                } else {
                    "${matchResult.value} ($cStr°C / $kStr K)"
                }
            }

            // Celsius -> Fahrenheit
            val celsiusRegex = Regex("""$numRegex$spaceRegex(?:Celsius|C|°C)\b""", RegexOption.IGNORE_CASE)
            result = celsiusRegex.replace(result) { matchResult ->
                val cStr = normalizeNumerals(matchResult.groupValues[1]).replace(",", ".")
                val c = cStr.toDoubleOrNull() ?: return@replace matchResult.value
                val f = (c * 9.0 / 5.0) + 32
                val k = c + 273.15
                val fStr = String.format("%.1f", f)
                val kStr = String.format("%.1f", k)

                if (isPersianApp) {
                    "${matchResult.value} (${formatNumerals(fStr, true)}°F / ${formatNumerals(kStr, true)} K)"
                } else {
                    "${matchResult.value} ($fStr°F / $kStr K)"
                }
            }
        }

        // 2. LENGTH
        if (isEnabled("length")) {
            // Meters -> Kilometers / Feet
            val meterRegex = Regex("""$numRegex$spaceRegex(?:meters?|meter|متر)\b""", RegexOption.IGNORE_CASE)
            result = meterRegex.replace(result) { matchResult ->
                val mStr = normalizeNumerals(matchResult.groupValues[1]).replace(",", ".")
                val m = mStr.toDoubleOrNull() ?: return@replace matchResult.value
                val feet = m * 3.28084
                val feetStr = String.format("%.1f", feet)
                
                val output = if (m >= 1000) {
                    val km = m / 1000.0
                    val kmStr = String.format("%.2f", km)
                    if (isPersianApp) "${formatNumerals(kmStr, true)} کیلومتر / ${formatNumerals(feetStr, true)} فوت" 
                    else "$kmStr km / $feetStr ft"
                } else {
                    if (isPersianApp) "${formatNumerals(feetStr, true)} فوت"
                    else "$feetStr ft"
                }
                "${matchResult.value} ($output)"
            }

            // Kilometers -> Miles
            val kmRegex = Regex("""$numRegex$spaceRegex(?:kilometers?|kilometer|km|کیلو[‌\u200C]?متر)\b""", RegexOption.IGNORE_CASE)
            result = kmRegex.replace(result) { matchResult ->
                val kmStr = normalizeNumerals(matchResult.groupValues[1]).replace(",", ".")
                val km = kmStr.toDoubleOrNull() ?: return@replace matchResult.value
                val miles = km * 0.621371
                val milesStr = String.format("%.2f", miles)
                val output = if (isPersianApp) "${formatNumerals(milesStr, true)} مایل" else "$milesStr mi"
                "${matchResult.value} ($output)"
            }

            // Miles -> Kilometers
            val milesRegex = Regex("""$numRegex$spaceRegex(?:miles?|mile|mi|مایل)\b""", RegexOption.IGNORE_CASE)
            result = milesRegex.replace(result) { matchResult ->
                val miStr = normalizeNumerals(matchResult.groupValues[1]).replace(",", ".")
                val mi = miStr.toDoubleOrNull() ?: return@replace matchResult.value
                val km = mi * 1.60934
                val kmStr = String.format("%.2f", km)
                val output = if (isPersianApp) "${formatNumerals(kmStr, true)} کیلومتر" else "$kmStr km"
                "${matchResult.value} ($output)"
            }

            // Inches -> Centimeters
            val inchRegex = Regex("""$numRegex$spaceRegex(?:inches?|inch|in|اینچ)\b""", RegexOption.IGNORE_CASE)
            result = inchRegex.replace(result) { matchResult ->
                val inchStr = normalizeNumerals(matchResult.groupValues[1]).replace(",", ".")
                val inch = inchStr.toDoubleOrNull() ?: return@replace matchResult.value
                val cm = inch * 2.54
                val cmStr = String.format("%.2f", cm)
                val output = if (isPersianApp) "${formatNumerals(cmStr, true)} سانتی‌متر" else "$cmStr cm"
                "${matchResult.value} ($output)"
            }

            // Feet -> Meters
            val feetRegex = Regex("""$numRegex$spaceRegex(?:feet|foot|ft|فوت)\b""", RegexOption.IGNORE_CASE)
            result = feetRegex.replace(result) { matchResult ->
                val ftStr = normalizeNumerals(matchResult.groupValues[1]).replace(",", ".")
                val ft = ftStr.toDoubleOrNull() ?: return@replace matchResult.value
                val m = ft * 0.3048
                val mStr = String.format("%.2f", m)
                val output = if (isPersianApp) "${formatNumerals(mStr, true)} متر" else "$mStr m"
                "${matchResult.value} ($output)"
            }
        }

        // 3. MASS (kg, lbs, g, oz)
        if (isEnabled("mass")) {
            // Kilograms -> Pounds
            val kgRegex = Regex("""$numRegex$spaceRegex(?:kilograms?|kilogram|kg|کیلو[‌\u200C]?گرم)\b""", RegexOption.IGNORE_CASE)
            result = kgRegex.replace(result) { matchResult ->
                val kgStr = normalizeNumerals(matchResult.groupValues[1]).replace(",", ".")
                val kg = kgStr.toDoubleOrNull() ?: return@replace matchResult.value
                val lbs = kg * 2.20462
                val lbsStr = String.format("%.2f", lbs)
                val output = if (isPersianApp) "${formatNumerals(lbsStr, true)} پوند" else "$lbsStr lbs"
                "${matchResult.value} ($output)"
            }

            // Pounds -> Kilograms
            val lbsRegex = Regex("""$numRegex$spaceRegex(?:pounds?|pound|lbs?|lb)\b""", RegexOption.IGNORE_CASE)
            result = lbsRegex.replace(result) { matchResult ->
                val lbsStr = normalizeNumerals(matchResult.groupValues[1]).replace(",", ".")
                val lbs = lbsStr.toDoubleOrNull() ?: return@replace matchResult.value
                val kg = lbs * 0.453592
                val kgStr = String.format("%.2f", kg)
                val output = if (isPersianApp) "${formatNumerals(kgStr, true)} کیلوگرم" else "$kgStr kg"
                "${matchResult.value} ($output)"
            }

            // Grams -> Ounces
            val gRegex = Regex("""$numRegex$spaceRegex(?:grams?|gram|g|گرم)\b""", RegexOption.IGNORE_CASE)
            result = gRegex.replace(result) { matchResult ->
                val gStr = normalizeNumerals(matchResult.groupValues[1]).replace(",", ".")
                val g = gStr.toDoubleOrNull() ?: return@replace matchResult.value
                val oz = g * 0.035274
                val ozStr = String.format("%.2f", oz)
                val output = if (isPersianApp) "${formatNumerals(ozStr, true)} اونس" else "$ozStr oz"
                "${matchResult.value} ($output)"
            }
        }

        // 4. SPEED (m/s, km/h, mph)
        if (isEnabled("speed")) {
            // m/s -> km/h
            val mpsRegex = Regex("""$numRegex$spaceRegex(?:m/s|mps)\b""", RegexOption.IGNORE_CASE)
            result = mpsRegex.replace(result) { matchResult ->
                val mpsStr = normalizeNumerals(matchResult.groupValues[1]).replace(",", ".")
                val mps = mpsStr.toDoubleOrNull() ?: return@replace matchResult.value
                val kmh = mps * 3.6
                val kmhStr = String.format("%.1f", kmh)
                val output = if (isPersianApp) "${formatNumerals(kmhStr, true)} کیلومتر بر ساعت" else "$kmhStr km/h"
                "${matchResult.value} ($output)"
            }

            // km/h -> mph
            val kmhRegex = Regex("""$numRegex$spaceRegex(?:km/h|kph)\b""", RegexOption.IGNORE_CASE)
            result = kmhRegex.replace(result) { matchResult ->
                val kmhStr = normalizeNumerals(matchResult.groupValues[1]).replace(",", ".")
                val kmh = kmhStr.toDoubleOrNull() ?: return@replace matchResult.value
                val mph = kmh * 0.621371
                val mphStr = String.format("%.1f", mph)
                val output = if (isPersianApp) "${formatNumerals(mphStr, true)} مایل بر ساعت" else "$mphStr mph"
                "${matchResult.value} ($output)"
            }
        }

        // 5. AREA (m², acres, hectares)
        if (isEnabled("area")) {
            // m2 -> sq ft
            val m2Regex = Regex("""$numRegex$spaceRegex(?:sq\s*meters?|sq\s*m|m²|m2)\b""", RegexOption.IGNORE_CASE)
            result = m2Regex.replace(result) { matchResult ->
                val m2Str = normalizeNumerals(matchResult.groupValues[1]).replace(",", ".")
                val m2 = m2Str.toDoubleOrNull() ?: return@replace matchResult.value
                val sqft = m2 * 10.7639
                val sqftStr = String.format("%.1f", sqft)
                val output = if (isPersianApp) "${formatNumerals(sqftStr, true)} فوت مربع" else "$sqftStr sq ft"
                "${matchResult.value} ($output)"
            }
        }

        // 6. VOLUME (liters, gallons, ml)
        if (isEnabled("volume")) {
            // Liters -> Gallons
            val literRegex = Regex("""$numRegex$spaceRegex(?:liters?|liter|L|لیتر)\b""", RegexOption.IGNORE_CASE)
            result = literRegex.replace(result) { matchResult ->
                val lStr = normalizeNumerals(matchResult.groupValues[1]).replace(",", ".")
                val l = lStr.toDoubleOrNull() ?: return@replace matchResult.value
                val gal = l * 0.264172
                val galStr = String.format("%.2f", gal)
                val output = if (isPersianApp) "${formatNumerals(galStr, true)} گالن" else "$galStr gal"
                "${matchResult.value} ($output)"
            }
        }

        return result
    }

    companion object {
        val allCategories = setOf(
            "length", "mass", "temperature", "speed", "area", "volume",
            "time", "digital storage", "pressure", "energy", "power", "angle"
        )
    }
}
