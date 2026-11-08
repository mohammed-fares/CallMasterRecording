package com.example.preferences

import android.content.Context
import android.content.SharedPreferences

// Activated by default

class CallMasterPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("callmaster_prefs", Context.MODE_PRIVATE)

    var isAutoRecordEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_RECORD, false) // Default FALSE for strict privacy compliance
        set(value) = prefs.edit().putBoolean(KEY_AUTO_RECORD, value).apply()

    var hasApprovedCloudAiProcessing: Boolean
        get() = prefs.getBoolean(KEY_CLOUD_AI_CONSENT, false)
        set(value) = prefs.edit().putBoolean(KEY_CLOUD_AI_CONSENT, value).apply()

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

    var geminiCustomApiKey: String
        get() = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GEMINI_API_KEY, value).apply()

    // Scope of recording: "ALL", "CELLULAR_ONLY", "VOIP_ONLY"
    var recordScope: String
        get() = prefs.getString(KEY_RECORD_SCOPE, "ALL") ?: "ALL"
        set(value) = prefs.edit().putString(KEY_RECORD_SCOPE, value).apply()

    // Call Direction: "BOTH", "INCOMING_ONLY", "OUTGOING_ONLY"
    var recordDirection: String
        get() = prefs.getString(KEY_RECORD_DIRECTION, "BOTH") ?: "BOTH"
        set(value) = prefs.edit().putString(KEY_RECORD_DIRECTION, value).apply()

    // Two-sided acoustic voice clarity booster
    var twoSidedAcousticBoost: Boolean
        get() = prefs.getBoolean(KEY_TWO_SIDED_BOOST, true)
        set(value) = prefs.edit().putBoolean(KEY_TWO_SIDED_BOOST, value).apply()

    // Dedicated local folder path
    var customStorageFolder: String
        get() = prefs.getString(KEY_STORAGE_FOLDER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_STORAGE_FOLDER, value).apply()

    // Audio bitrate (bps): 128000 for standard/high clarity, 64000 for compact
    var audioBitrate: Int
        get() = prefs.getInt(KEY_AUDIO_BITRATE, 128000)
        set(value) = prefs.edit().putInt(KEY_AUDIO_BITRATE, value).apply()

    // Google Drive auto-sync settings
    var isGoogleDriveAutoSyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_GDRIVE_AUTO_SYNC, false)
        set(value) = prefs.edit().putBoolean(KEY_GDRIVE_AUTO_SYNC, value).apply()

    var googleDriveAccountEmail: String
        get() = prefs.getString(KEY_GDRIVE_EMAIL, "mf.z.hussein.pal@gmail.com") ?: "mf.z.hussein.pal@gmail.com"
        set(value) = prefs.edit().putString(KEY_GDRIVE_EMAIL, value).apply()

    var googleDriveFolderTitle: String
        get() = prefs.getString(KEY_GDRIVE_FOLDER, "CallMaster Recordings") ?: "CallMaster Recordings"
        set(value) = prefs.edit().putString(KEY_GDRIVE_FOLDER, value).apply()

    var lastGoogleDriveSyncTimestamp: Long
        get() = prefs.getLong(KEY_GDRIVE_LAST_SYNC, 0L)
        set(value) = prefs.edit().putLong(KEY_GDRIVE_LAST_SYNC, value).apply()

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
        private const val KEY_GEMINI_API_KEY = "pref_gemini_api_key"
        private const val KEY_RECORD_SCOPE = "pref_record_scope"
        private const val KEY_RECORD_DIRECTION = "pref_record_direction"
        private const val KEY_TWO_SIDED_BOOST = "pref_two_sided_boost"
        private const val KEY_STORAGE_FOLDER = "pref_storage_folder"
        private const val KEY_AUDIO_BITRATE = "pref_audio_bitrate"
        private const val KEY_GDRIVE_AUTO_SYNC = "pref_gdrive_auto_sync"
        private const val KEY_GDRIVE_EMAIL = "pref_gdrive_email"
        private const val KEY_GDRIVE_FOLDER = "pref_gdrive_folder"
        private const val KEY_GDRIVE_LAST_SYNC = "pref_gdrive_last_sync"
        private const val KEY_CLOUD_AI_CONSENT = "pref_cloud_ai_consent"
    }
}
