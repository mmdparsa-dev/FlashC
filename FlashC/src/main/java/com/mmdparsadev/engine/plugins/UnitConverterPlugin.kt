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

            val convertedText = parseAndConvertText(text, enabledCategories)
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

    fun parseAndConvertText(input: String, enabledCategories: Set<String> = allCategories): String {
        var result = normalizeNumerals(input)

        // Helper to check if category is enabled
        fun isEnabled(cat: String) = enabledCategories.contains(cat.lowercase())
        
        // Regex pattern for number with optional decimal, allowing Persian and English digits
        val numRegex = """([0-9]+(?:[\.,][0-9]+)?)"""
        // Optional space or ZWNJ
        val spaceRegex = """[\s\u200C]*"""

        // 1. TEMPERATURE (Fahrenheit <-> Celsius <-> Kelvin)
        if (isEnabled("temperature")) {
            // Fahrenheit -> Celsius
            val fahrenheitRegex = Regex("""$numRegex$spaceRegex(?:Fahrenheit|F|°F)\b""", RegexOption.IGNORE_CASE)
            result = fahrenheitRegex.replace(result) { matchResult ->
                val f = matchResult.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@replace matchResult.value
                val c = (f - 32) * 5.0 / 9.0
                val k = c + 273.15
                "$f°F (${String.format("%.1f", c)}°C / ${String.format("%.1f", k)} K)"
            }

            // Celsius -> Fahrenheit
            val celsiusRegex = Regex("""$numRegex$spaceRegex(?:Celsius|C|°C)\b""", RegexOption.IGNORE_CASE)
            result = celsiusRegex.replace(result) { matchResult ->
                val c = matchResult.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@replace matchResult.value
                val f = (c * 9.0 / 5.0) + 32
                val k = c + 273.15
                "$c°C (${String.format("%.1f", f)}°F / ${String.format("%.1f", k)} K)"
            }
        }

        // 2. LENGTH
        if (isEnabled("length")) {
            // Meters -> Kilometers / Feet
            val meterRegex = Regex("""$numRegex$spaceRegex(?:meters?|meter|متر)\b""", RegexOption.IGNORE_CASE)
            result = meterRegex.replace(result) { matchResult ->
                val m = matchResult.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@replace matchResult.value
                val feet = m * 3.28084
                if (m >= 1000) {
                    val km = m / 1000.0
                    "$m meters (${String.format("%.2f", km)} km / ${String.format("%.1f", feet)} ft)"
                } else {
                    "$m meters (${String.format("%.1f", feet)} ft)"
                }
            }

            // Kilometers -> Miles
            val kmRegex = Regex("""$numRegex$spaceRegex(?:kilometers?|kilometer|km|کیلو[‌\u200C]?متر)\b""", RegexOption.IGNORE_CASE)
            result = kmRegex.replace(result) { matchResult ->
                val km = matchResult.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@replace matchResult.value
                val miles = km * 0.621371
                "$km km (${String.format("%.2f", miles)} mi)"
            }

            // Miles -> Kilometers
            val milesRegex = Regex("""$numRegex$spaceRegex(?:miles?|mile|mi|مایل)\b""", RegexOption.IGNORE_CASE)
            result = milesRegex.replace(result) { matchResult ->
                val mi = matchResult.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@replace matchResult.value
                val km = mi * 1.60934
                "$mi mi (${String.format("%.2f", km)} km)"
            }

            // Inches -> Centimeters
            val inchRegex = Regex("""$numRegex$spaceRegex(?:inches?|inch|in|اینچ)\b""", RegexOption.IGNORE_CASE)
            result = inchRegex.replace(result) { matchResult ->
                val inch = matchResult.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@replace matchResult.value
                val cm = inch * 2.54
                "$inch in (${String.format("%.2f", cm)} cm)"
            }

            // Feet -> Meters
            val feetRegex = Regex("""$numRegex$spaceRegex(?:feet|foot|ft|فوت)\b""", RegexOption.IGNORE_CASE)
            result = feetRegex.replace(result) { matchResult ->
                val ft = matchResult.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@replace matchResult.value
                val m = ft * 0.3048
                "$ft ft (${String.format("%.2f", m)} m)"
            }
        }

        // 3. MASS (kg, lbs, g, oz)
        if (isEnabled("mass")) {
            // Kilograms -> Pounds
            val kgRegex = Regex("""$numRegex$spaceRegex(?:kilograms?|kilogram|kg|کیلو[‌\u200C]?گرم)\b""", RegexOption.IGNORE_CASE)
            result = kgRegex.replace(result) { matchResult ->
                val kg = matchResult.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@replace matchResult.value
                val lbs = kg * 2.20462
                "$kg kg (${String.format("%.2f", lbs)} lbs)"
            }

            // Pounds -> Kilograms
            val lbsRegex = Regex("""$numRegex$spaceRegex(?:pounds?|pound|lbs?|lb)\b""", RegexOption.IGNORE_CASE)
            result = lbsRegex.replace(result) { matchResult ->
                val lbs = matchResult.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@replace matchResult.value
                val kg = lbs * 0.453592
                "$lbs lbs (${String.format("%.2f", kg)} kg)"
            }

            // Grams -> Ounces
            val gRegex = Regex("""$numRegex$spaceRegex(?:grams?|gram|g|گرم)\b""", RegexOption.IGNORE_CASE)
            result = gRegex.replace(result) { matchResult ->
                val g = matchResult.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@replace matchResult.value
                val oz = g * 0.035274
                "$g g (${String.format("%.2f", oz)} oz)"
            }
        }

        // 4. SPEED (m/s, km/h, mph)
        if (isEnabled("speed")) {
            // m/s -> km/h
            val mpsRegex = Regex("""$numRegex$spaceRegex(?:m/s|mps)\b""", RegexOption.IGNORE_CASE)
            result = mpsRegex.replace(result) { matchResult ->
                val mps = matchResult.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@replace matchResult.value
                val kmh = mps * 3.6
                "$mps m/s (${String.format("%.1f", kmh)} km/h)"
            }

            // km/h -> mph
            val kmhRegex = Regex("""$numRegex$spaceRegex(?:km/h|kph)\b""", RegexOption.IGNORE_CASE)
            result = kmhRegex.replace(result) { matchResult ->
                val kmh = matchResult.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@replace matchResult.value
                val mph = kmh * 0.621371
                "$kmh km/h (${String.format("%.1f", mph)} mph)"
            }

            // mph -> km/h
            val mphRegex = Regex("""$numRegex$spaceRegex(?:mph|miles per hour)\b""", RegexOption.IGNORE_CASE)
            result = mphRegex.replace(result) { matchResult ->
                val mph = matchResult.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@replace matchResult.value
                val kmh = mph * 1.60934
                "$mph mph (${String.format("%.1f", kmh)} km/h)"
            }
        }

        // 5. AREA (m², acres, hectares)
        if (isEnabled("area")) {
            // m2 -> sq ft
            val m2Regex = Regex("""$numRegex$spaceRegex(?:sq\s*meters?|sq\s*m|m²|m2)\b""", RegexOption.IGNORE_CASE)
            result = m2Regex.replace(result) { matchResult ->
                val m2 = matchResult.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@replace matchResult.value
                val sqft = m2 * 10.7639
                "$m2 m² (${String.format("%.1f", sqft)} sq ft)"
            }

            // Acres -> Hectares
            val acreRegex = Regex("""$numRegex$spaceRegex(?:acres?|ac)\b""", RegexOption.IGNORE_CASE)
            result = acreRegex.replace(result) { matchResult ->
                val ac = matchResult.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@replace matchResult.value
                val ha = ac * 0.404686
                "$ac acres (${String.format("%.2f", ha)} ha)"
            }
        }

        // 6. VOLUME (liters, gallons, ml)
        if (isEnabled("volume")) {
            // Liters -> Gallons
            val literRegex = Regex("""$numRegex$spaceRegex(?:liters?|liter|L|لیتر)\b""", RegexOption.IGNORE_CASE)
            result = literRegex.replace(result) { matchResult ->
                val l = matchResult.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@replace matchResult.value
                val gal = l * 0.264172
                "$l L (${String.format("%.2f", gal)} gal)"
            }

            // Gallons -> Liters
            val galRegex = Regex("""$numRegex$spaceRegex(?:gallons?|gallon|gal)\b""", RegexOption.IGNORE_CASE)
            result = galRegex.replace(result) { matchResult ->
                val gal = matchResult.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@replace matchResult.value
                val l = gal * 3.78541
                "$gal gal (${String.format("%.2f", l)} L)"
            }

            // ml -> cups
            val mlRegex = Regex("""$numRegex$spaceRegex(?:milliliters?|ml|میلی[‌\u200C]?لیتر)\b""", RegexOption.IGNORE_CASE)
            result = mlRegex.replace(result) { matchResult ->
                val ml = matchResult.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@replace matchResult.value
                val cups = ml * 0.00422675
                "$ml ml (${String.format("%.2f", cups)} cups)"
            }
        }

        // 7. TIME (hours, days)
        if (isEnabled("time")) {
            // Hours -> Minutes / Seconds
            val hrRegex = Regex("""$numRegex$spaceRegex(?:hours?|hour|hr|hrs)\b""", RegexOption.IGNORE_CASE)
            result = hrRegex.replace(result) { matchResult ->
                val hr = matchResult.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@replace matchResult.value
                val min = hr * 60
                val sec = min * 60
                "$hr hrs (${String.format("%.0f", min)} min / ${String.format("%.0f", sec)} sec)"
            }

            // Days -> Hours
            val daysRegex = Regex("""$numRegex$spaceRegex(?:days?|day)\b""", RegexOption.IGNORE_CASE)
            result = daysRegex.replace(result) { matchResult ->
                val d = matchResult.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@replace matchResult.value
                val hr = d * 24
                "$d days (${String.format("%.0f", hr)} hrs)"
            }
        }

        // 8. DIGITAL STORAGE
        if (isEnabled("digital storage")) {
            // GB -> MB
            val gbRegex = Regex("""$numRegex$spaceRegex(?:GB|gigabytes?)\b""", RegexOption.IGNORE_CASE)
            result = gbRegex.replace(result) { matchResult ->
                val gb = matchResult.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@replace matchResult.value
                val mb = gb * 1024
                val tb = gb / 1024.0
                "$gb GB (${mb.toInt()} MB / ${String.format("%.3f", tb)} TB)"
            }

            // MB -> GB
            val mbRegex = Regex("""$numRegex$spaceRegex(?:MB|megabytes?)\b""", RegexOption.IGNORE_CASE)
            result = mbRegex.replace(result) { matchResult ->
                val mb = matchResult.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@replace matchResult.value
                val gb = mb / 1024.0
                "$mb MB (${String.format("%.2f", gb)} GB)"
            }

            // TB -> GB
            val tbRegex = Regex("""$numRegex$spaceRegex(?:TB|terabytes?)\b""", RegexOption.IGNORE_CASE)
            result = tbRegex.replace(result) { matchResult ->
                val tb = matchResult.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@replace matchResult.value
                val gb = tb * 1024
                "$tb TB (${gb.toInt()} GB)"
            }
        }

        // 9. PRESSURE (bar, psi)
        if (isEnabled("pressure")) {
            // Bar -> PSI
            val barRegex = Regex("""$numRegex$spaceRegex(?:bar)\b""", RegexOption.IGNORE_CASE)
            result = barRegex.replace(result) { matchResult ->
                val bar = matchResult.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@replace matchResult.value
                val psi = bar * 14.5038
                "$bar bar (${String.format("%.1f", psi)} psi)"
            }

            // PSI -> Bar
            val psiRegex = Regex("""$numRegex$spaceRegex(?:psi)\b""", RegexOption.IGNORE_CASE)
            result = psiRegex.replace(result) { matchResult ->
                val psi = matchResult.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@replace matchResult.value
                val bar = psi * 0.0689476
                "$psi psi (${String.format("%.2f", bar)} bar)"
            }
        }

        // 10. ENERGY (cal, kcal, kWh)
        if (isEnabled("energy")) {
            // Calories -> Joules
            val calRegex = Regex("""$numRegex$spaceRegex(?:calories?|cal)\b""", RegexOption.IGNORE_CASE)
            result = calRegex.replace(result) { matchResult ->
                val cal = matchResult.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@replace matchResult.value
                val j = cal * 4.184
                "$cal cal (${String.format("%.1f", j)} J)"
            }

            // Kilocalories -> Kilojoules
            val kcalRegex = Regex("""$numRegex$spaceRegex(?:kilocalories?|kcal)\b""", RegexOption.IGNORE_CASE)
            result = kcalRegex.replace(result) { matchResult ->
                val kcal = matchResult.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@replace matchResult.value
                val kj = kcal * 4.184
                "$kcal kcal (${String.format("%.1f", kj)} kJ)"
            }

            // kWh -> MJ
            val kwhRegex = Regex("""$numRegex$spaceRegex(?:kWh|kilowatt-hours?)\b""", RegexOption.IGNORE_CASE)
            result = kwhRegex.replace(result) { matchResult ->
                val kwh = matchResult.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@replace matchResult.value
                val mj = kwh * 3.6
                "$kwh kWh (${String.format("%.1f", mj)} MJ)"
            }
        }

        // 11. POWER (kW, hp)
        if (isEnabled("power")) {
            // kW -> hp
            val kwRegex = Regex("""$numRegex$spaceRegex(?:kilowatts?|kW)\b""", RegexOption.IGNORE_CASE)
            result = kwRegex.replace(result) { matchResult ->
                val kw = matchResult.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@replace matchResult.value
                val hp = kw * 1.34102
                "$kw kW (${String.format("%.1f", hp)} hp)"
            }

            // hp -> kW
            val hpRegex = Regex("""$numRegex$spaceRegex(?:horsepower|hp)\b""", RegexOption.IGNORE_CASE)
            result = hpRegex.replace(result) { matchResult ->
                val hp = matchResult.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@replace matchResult.value
                val kw = hp * 0.7457
                "$hp hp (${String.format("%.2f", kw)} kW)"
            }
        }

        // 12. ANGLE (degrees, rad)
        if (isEnabled("angle")) {
            // Degrees -> Radians (ignoring °C / °F)
            val degRegex = Regex("""$numRegex$spaceRegex(?:degrees?|deg|°)(?![CFcf])\b""", RegexOption.IGNORE_CASE)
            result = degRegex.replace(result) { matchResult ->
                val deg = matchResult.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@replace matchResult.value
                val rad = deg * Math.PI / 180.0
                "$deg° (${String.format("%.4f", rad)} rad)"
            }

            // Radians -> Degrees
            val radRegex = Regex("""$numRegex$spaceRegex(?:radians?|rad)\b""", RegexOption.IGNORE_CASE)
            result = radRegex.replace(result) { matchResult ->
                val rad = matchResult.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return@replace matchResult.value
                val deg = rad * 180.0 / Math.PI
                "$rad rad (${String.format("%.1f", deg)}°)"
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
