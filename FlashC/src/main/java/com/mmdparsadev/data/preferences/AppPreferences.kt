package com.mmdparsadev.data.preferences

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("flashc_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_OVERLAY_ENABLED = "overlay_enabled"
        private const val KEY_CLIPBOARD_PARSE_ENABLED = "clipboard_parse_enabled"
        private const val KEY_CURRENCY_SYNC_ENABLED = "currency_sync_enabled"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_LAST_USD_RATE = "last_usd_rate"
        private const val KEY_LAST_EUR_RATE = "last_eur_rate"
        private const val KEY_LAST_RATE_FETCH_TIME = "last_rate_fetch_time"
        private const val KEY_IS_CURRENCY_ENABLED = "is_currency_enabled"
        private const val KEY_IS_UNITS_ENABLED = "is_units_enabled"
        private const val KEY_PREFERRED_CURRENCY_DISPLAY = "preferred_currency_display"
        private const val KEY_ENABLED_UNIT_CATEGORIES = "enabled_unit_categories"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_IGNORED_APPS = "ignored_apps"
        private const val KEY_SHOW_USD_CONVERSION = "show_usd_conversion"
        private const val KEY_SHOW_EUR_CONVERSION = "show_eur_conversion"
    }

    var isOverlayEnabled: Boolean
        get() = prefs.getBoolean(KEY_OVERLAY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_OVERLAY_ENABLED, value).apply()

    var isClipboardParseEnabled: Boolean
        get() = prefs.getBoolean(KEY_CLIPBOARD_PARSE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_CLIPBOARD_PARSE_ENABLED, value).apply()

    var isCurrencySyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_CURRENCY_SYNC_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_CURRENCY_SYNC_ENABLED, value).apply()

    var darkMode: String
        get() = prefs.getString(KEY_DARK_MODE, "system") ?: "system"
        set(value) = prefs.edit().putString(KEY_DARK_MODE, value).apply()

    var lastUsdRate: Float
        get() = prefs.getFloat(KEY_LAST_USD_RATE, 65000f) // 1 USD = 65000 Tomans (650,000 Rials) baseline
        set(value) = prefs.edit().putFloat(KEY_LAST_USD_RATE, value).apply()

    var lastEurRate: Float
        get() = prefs.getFloat(KEY_LAST_EUR_RATE, 71000f) // 1 EUR = 71000 Tomans baseline
        set(value) = prefs.edit().putFloat(KEY_LAST_EUR_RATE, value).apply()

    var lastRateFetchTime: Long
        get() = prefs.getLong(KEY_LAST_RATE_FETCH_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_RATE_FETCH_TIME, value).apply()

    var isCurrencyEnabled: Boolean
        get() = prefs.getBoolean(KEY_IS_CURRENCY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_IS_CURRENCY_ENABLED, value).apply()

    var isUnitsEnabled: Boolean
        get() = prefs.getBoolean(KEY_IS_UNITS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_IS_UNITS_ENABLED, value).apply()

    var preferredCurrencyDisplay: String
        get() = prefs.getString(KEY_PREFERRED_CURRENCY_DISPLAY, "usd_eur") ?: "usd_eur"
        set(value) = prefs.edit().putString(KEY_PREFERRED_CURRENCY_DISPLAY, value).apply()

    var enabledUnitCategories: String
        get() = prefs.getString(KEY_ENABLED_UNIT_CATEGORIES, "length,mass,temperature,speed,area,volume,time,digital storage,pressure,energy,power,angle") ?: "length,mass,temperature,speed,area,volume,time,digital storage,pressure,energy,power,angle"
        set(value) = prefs.edit().putString(KEY_ENABLED_UNIT_CATEGORIES, value).apply()

    var appLanguage: String
        get() {
            val defaultLang = java.util.Locale.getDefault().language
            val fallback = if (defaultLang == "fa" || defaultLang == "ur" || defaultLang == "ar") "fa" else "en"
            return prefs.getString(KEY_APP_LANGUAGE, fallback) ?: fallback
        }
        set(value) = prefs.edit().putString(KEY_APP_LANGUAGE, value).apply()

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    var ignoredApps: Set<String>
        get() = prefs.getStringSet(KEY_IGNORED_APPS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_IGNORED_APPS, value).apply()

    var showUsdConversion: Boolean
        get() = prefs.getBoolean(KEY_SHOW_USD_CONVERSION, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_USD_CONVERSION, value).apply()

    var showEurConversion: Boolean
        get() = prefs.getBoolean(KEY_SHOW_EUR_CONVERSION, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_EUR_CONVERSION, value).apply()
}
