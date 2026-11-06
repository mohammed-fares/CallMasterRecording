package com.example.preferences

import android.content.Context
import android.content.SharedPreferences

class CallMasterPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("callmaster_prefs", Context.MODE_PRIVATE)

    var isAutoRecordEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_RECORD, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_RECORD, value).apply()

    var isQuietModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_QUIET_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_QUIET_MODE, value).apply()

    var appLanguage: String
        get() = prefs.getString(KEY_APP_LANGUAGE, "ar") ?: "ar" // Default Arabic as requested
        set(value) = prefs.edit().putString(KEY_APP_LANGUAGE, value).apply()

    var isDarkTheme: Boolean
        get() = prefs.getBoolean(KEY_DARK_THEME, true)
        set(value) = prefs.edit().putBoolean(KEY_DARK_THEME, value).apply()

    var audioQuality: String
        get() = prefs.getString(KEY_AUDIO_QUALITY, "HIGH") ?: "HIGH"
        set(value) = prefs.edit().putString(KEY_AUDIO_QUALITY, value).apply()

    var audioFormat: String
        get() = prefs.getString(KEY_AUDIO_FORMAT, "M4A") ?: "M4A"
        set(value) = prefs.edit().putString(KEY_AUDIO_FORMAT, value).apply()

    var isPinLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_PIN_LOCK, false)
        set(value) = prefs.edit().putBoolean(KEY_PIN_LOCK, value).apply()

    var pinHash: String
        get() = prefs.getString(KEY_PIN_HASH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PIN_HASH, value).apply()

    var isBiometricsEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRICS, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRICS, value).apply()

    var isStorageEncryptionEnabled: Boolean
        get() = prefs.getBoolean(KEY_STORAGE_ENCRYPTION, false)
        set(value) = prefs.edit().putBoolean(KEY_STORAGE_ENCRYPTION, value).apply()

    var subscriptionTier: String
        get() = prefs.getString(KEY_SUBSCRIPTION_TIER, "FREE") ?: "FREE"
        set(value) = prefs.edit().putString(KEY_SUBSCRIPTION_TIER, value).apply()

    var hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    var retentionDays: Int
        get() = prefs.getInt(KEY_RETENTION_DAYS, 0)
        set(value) = prefs.edit().putInt(KEY_RETENTION_DAYS, value).apply()

    companion object {
        private const val KEY_AUTO_RECORD = "pref_auto_record"
        private const val KEY_QUIET_MODE = "pref_quiet_mode"
        private const val KEY_APP_LANGUAGE = "pref_app_language"
        private const val KEY_DARK_THEME = "pref_dark_theme"
        private const val KEY_AUDIO_QUALITY = "pref_audio_quality"
        private const val KEY_AUDIO_FORMAT = "pref_audio_format"
        private const val KEY_PIN_LOCK = "pref_pin_lock"
        private const val KEY_PIN_HASH = "pref_pin_hash"
        private const val KEY_BIOMETRICS = "pref_biometrics"
        private const val KEY_STORAGE_ENCRYPTION = "pref_storage_encryption"
        private const val KEY_SUBSCRIPTION_TIER = "pref_subscription_tier"
        private const val KEY_ONBOARDING_COMPLETED = "pref_onboarding_completed"
        private const val KEY_RETENTION_DAYS = "pref_retention_days"
    }
}
