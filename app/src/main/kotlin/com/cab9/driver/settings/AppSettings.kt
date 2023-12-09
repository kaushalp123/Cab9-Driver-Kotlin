package com.cab9.driver.settings

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import com.cab9.driver.di.qualifiers.SettingsPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSettings @Inject constructor(@SettingsPreferences private val preferences: SharedPreferences) {

    companion object {
        const val PREFERENCE_NAME = "prefs::settings"

        private const val PREF_KEY_IS_BIOMETRIC_ENABLED = "prefKeyIsBiometricEnabled"
        private const val PREF_KEY_IS_BIOMETRIC_TUTORIAL_DONE = "prefKeyIsBiometricTutorialDone"
        private const val PREF_KEY_IS_LOCATION_TERMS_AGREED = "prefKeyIsLocationTermsAgreed"
        private const val PREF_KEY_IS_PERMISSION_TUTORIAL_DONE = "prefKeyIsPermissionTutorialDone"
        private const val PREF_KEY_DISPLAY_MODE = "prefKeyDisplayMode"

        private const val PREF_KEY_APP_VERSION = "prefKeyAppVersion"
    }

    var isLocationTermsAgreed: Boolean
        get() = preferences.getBoolean(PREF_KEY_IS_LOCATION_TERMS_AGREED, false)
        set(value) = preferences.edit().putBoolean(PREF_KEY_IS_LOCATION_TERMS_AGREED, value).apply()

    var isBiometricEnabled: Boolean
        get() = preferences.getBoolean(PREF_KEY_IS_BIOMETRIC_ENABLED, false)
        set(value) = preferences.edit().putBoolean(PREF_KEY_IS_BIOMETRIC_ENABLED, value).apply()

    var isAskedToEnabledBiometric: Boolean
        get() = preferences.getBoolean(PREF_KEY_IS_BIOMETRIC_TUTORIAL_DONE, false)
        set(value) = preferences.edit().putBoolean(PREF_KEY_IS_BIOMETRIC_TUTORIAL_DONE, value)
            .apply()

    var isPermissionIntroCompleted: Boolean
        get() = preferences.getBoolean(PREF_KEY_IS_PERMISSION_TUTORIAL_DONE, false)
        set(value) = preferences.edit().putBoolean(PREF_KEY_IS_PERMISSION_TUTORIAL_DONE, value)
            .apply()

    var displayMode: Int
        get() = preferences.getInt(PREF_KEY_DISPLAY_MODE, AppCompatDelegate.getDefaultNightMode())
        set(value) = preferences.edit().putInt(PREF_KEY_DISPLAY_MODE, value).apply()

    var appVersion: String?
        get() = preferences.getString(PREF_KEY_APP_VERSION, null)
        set(value) = preferences.edit().putString(PREF_KEY_APP_VERSION, value).apply()

    fun clear() {
        preferences.edit().clear().apply()
    }
}