package com.mmdparsadev.service

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.mmdparsadev.data.preferences.AppPreferences
import com.mmdparsadev.engine.plugins.CurrencyConverterPlugin
import com.mmdparsadev.engine.plugins.UnitConverterPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FlashCAccessibilityService : AccessibilityService() {
    private lateinit var prefs: AppPreferences
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val unitConverter = UnitConverterPlugin()
    private val currencyConverter = CurrencyConverterPlugin()
    private var overlayController: OverlayController? = null

    private var lastAnalyzedText = ""
    private var lastAnalysisTime = 0L
    private var lastRenderedConversions = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        android.util.Log.d("FlashC", "FlashCAccessibilityService.onServiceConnected()")
    }

    override fun onCreate() {
        super.onCreate()
        prefs = AppPreferences(applicationContext)
        overlayController = OverlayController(applicationContext)

        if (prefs.isCurrencySyncEnabled) {
            serviceScope.launch {
                runCatching { currencyConverter.syncRate(prefs, force = false) }
            }
        }

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.addPrimaryClipChangedListener {
            if (prefs.isClipboardParseEnabled) {
                val clipData = clipboard.primaryClip
                if (clipData != null && clipData.itemCount > 0) {
                    val text = clipData.getItemAt(0).text?.toString() ?: ""
                    analyzeSingleText(text)
                }
            }
        }
    }

    private fun analyzeSingleText(text: String) {
        val trimmed = text.trim()
        if (trimmed == lastAnalyzedText && System.currentTimeMillis() - lastAnalysisTime < 3000) {
            return
        }

        lastAnalyzedText = trimmed
        lastAnalysisTime = System.currentTimeMillis()

        serviceScope.launch {
            if (prefs.isCurrencySyncEnabled) {
                runCatching { currencyConverter.syncRate(prefs, force = false) }
            }
            val rate = prefs.lastUsdRate
            val eurRate = prefs.lastEurRate
            val prefDisplay = prefs.preferredCurrencyDisplay
            var converted = if (prefs.isCurrencyEnabled) {
                currencyConverter.convertCurrencyInText(trimmed, rate, eurRate, prefDisplay)
            } else {
                trimmed
            }
            val enabledCategories = prefs.enabledUnitCategories.split(",").map { it.trim().lowercase() }.toSet()
            converted = if (prefs.isUnitsEnabled) {
                unitConverter.parseAndConvertText(converted, enabledCategories)
            } else {
                converted
            }

            if (converted != trimmed && trimmed.isNotBlank()) {
                overlayController?.showOverlay(listOf(trimmed to converted)) { convertedText ->
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("FlashC Converted", convertedText)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(applicationContext, "Copied converted text!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getNormalizedText(input: String): String {
        val persian = "۰۱۲۳۴۵۶۷۸۹"
        val arabic = "٠١٢٣٤٥٦٧٨٩"
        val english = "0123456789"
        val mapped = input.map { char ->
            val pIdx = persian.indexOf(char)
            val aIdx = arabic.indexOf(char)
            when {
                pIdx != -1 -> english[pIdx]
                aIdx != -1 -> english[aIdx]
                else -> char
            }
        }.joinToString("")
        return mapped.lowercase().replace(Regex("""[\s\u200C]+"""), "").trim()
    }

    private var searchJob: kotlinx.coroutines.Job? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) {
            return
        }
        if (!prefs.isOverlayEnabled) {
            return
        }
        if (!prefs.isCurrencyEnabled && !prefs.isUnitsEnabled) {
            return
        }

        val eventType = event.eventType
        if (eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED ||
            eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
            eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        ) {
            android.util.Log.d("FlashC", "Accessibility event received: $eventType")
            try {
                val rootNode = rootInActiveWindow ?: return

                val nodeBounds = mutableListOf<Pair<String, android.graphics.Rect>>()
                var nodeCount = 0

                fun collectTexts(n: android.view.accessibility.AccessibilityNodeInfo?) {
                    if (n == null) return
                    nodeCount++
                    val t = (n.text ?: n.contentDescription)?.toString()?.trim()
                    if (!t.isNullOrBlank() && t.length < 200 && t.any { it.isDigit() }) {
                        val rect = android.graphics.Rect()
                        n.getBoundsInScreen(rect)
                        nodeBounds.add(t to rect)
                    }
                    for (i in 0 until n.childCount) {
                        collectTexts(n.getChild(i))
                    }
                }

                collectTexts(rootNode)

                val allTexts = nodeBounds.map { it.first }
                android.util.Log.d("FlashC", "Processed $nodeCount nodes, found ${allTexts.size} texts")
                if (allTexts.isNotEmpty()) {
                    android.util.Log.d("FlashC", "Extracted text samples: ${allTexts.take(3).joinToString(", ")}")
                }

                searchJob?.cancel()
                searchJob = serviceScope.launch {
                    kotlinx.coroutines.delay(450) // Debounce for 450ms

                    if (allTexts.isNotEmpty()) {
                        val combinedForCheck = allTexts.joinToString("\n")

                        if (combinedForCheck != lastAnalyzedText || System.currentTimeMillis() - lastAnalysisTime > 3000) {
                            lastAnalyzedText = combinedForCheck
                            lastAnalysisTime = System.currentTimeMillis()

                            if (prefs.isCurrencySyncEnabled) {
                                runCatching { currencyConverter.syncRate(prefs, force = false) }
                            }

                            val extractedStrings = com.mmdparsadev.engine.TextExtractionEngine.extractConvertibleValues(allTexts)

                            if (extractedStrings.isEmpty()) {
                                return@launch
                            }

                            val textToRect = nodeBounds.associate { it.first to it.second }
                            val foundConversions = mutableListOf<Triple<String, String, android.graphics.Rect>>()
                            val rate = prefs.lastUsdRate
                            val eurRate = prefs.lastEurRate
                            val prefDisplay = prefs.preferredCurrencyDisplay
                            val enabledCategories = prefs.enabledUnitCategories.split(",").map { it.trim().lowercase() }.toSet()

                            val seenNormalized = mutableSetOf<String>()
                            for (t in extractedStrings) {
                                var converted = t
                                if (prefs.isCurrencyEnabled) {
                                    converted = currencyConverter.convertCurrencyInText(converted, rate, eurRate, prefDisplay)
                                }
                                if (prefs.isUnitsEnabled) {
                                    converted = unitConverter.parseAndConvertText(converted, enabledCategories)
                                }
                                if (converted != t) {
                                    val norm = getNormalizedText(t)
                                    if (seenNormalized.add(norm)) {
                                        val rect = textToRect[t] ?: android.graphics.Rect()
                                        foundConversions.add(Triple(t, converted, rect))
                                    }
                                }
                            }

                            if (foundConversions.isNotEmpty()) {
                                val conversionsList = foundConversions.map { it.first to it.second }
                                val firstRect = foundConversions.first().third

                                android.util.Log.d("FlashC", "Final conversion candidates: ${conversionsList.joinToString(", ") { "${it.first} -> ${it.second}" }}")
                                val thisRenderHash = conversionsList.joinToString("||") { "${it.first}->${it.second}" }

                                if (thisRenderHash != lastRenderedConversions) {
                                    lastRenderedConversions = thisRenderHash
                                    android.util.Log.d("FlashC", "Found ${conversionsList.size} conversions. Showing overlay.")
                                    overlayController?.showOverlay(conversionsList, firstRect) { convertedText ->
                                        val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("FlashC Converted", convertedText)
                                        clipboard.setPrimaryClip(clip)
                                        android.widget.Toast.makeText(applicationContext, "Copied converted text!", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                // Do not hide overlay for unrelated node events
                            }
                        }
                    } else {
                        // Do not hide overlay for unrelated node events
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FlashC", "Error processing accessibility event", e)
            }
        }
    }

    override fun onInterrupt() {
        overlayController?.hideOverlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        overlayController?.hideOverlay()
    }
}
