package com.cab9.driver.ui.account

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.net.toUri
import com.cab9.driver.R
import com.cab9.driver.ext.areNotificationsEnabled
import com.cab9.driver.ext.isAutomaticDateTimeSettingsEnabled
import com.cab9.driver.ext.isBatteryOptimised
import com.cab9.driver.ext.isDrawOverlayAllowed
import com.cab9.driver.ext.isGPSEnabled
import com.cab9.driver.ext.isPermissionGranted
import com.cab9.driver.ext.lat
import com.cab9.driver.ext.lng
import com.cab9.driver.utils.IntentFactory
import com.cab9.driver.utils.hasNotificationPermission
import com.cab9.driver.utils.map.hasLocationPermissions
import com.google.android.gms.maps.model.LatLng

enum class AccountMenuItem(@DrawableRes val iconResId: Int, @StringRes val labelResId: Int) {
    PROFILE(R.drawable.ic_profile, R.string.menu_item_profile),
    SETTINGS(R.drawable.ic_settings, R.string.menu_item_settings),
    FEEDBACK(R.drawable.ic_contact_support, R.string.menu_item_feedback),
    LOGOUT(R.drawable.ic_logout, R.string.menu_item_logout);
}

enum class SettingsOption(@DrawableRes val iconResId: Int, @StringRes val labelResId: Int) {
    DISPLAY(R.drawable.ic_display, R.string.menu_item_display),
    VEHICLES(R.drawable.ic_vehicles, R.string.menu_item_vehicles),
    BIOMETRIC(R.drawable.ic_baseline_fingerprint_24, R.string.menu_item_biometric),
    DIAGNOSTICS(R.drawable.baseline_cellphone_cog_24, R.string.menu_item_diagnostics);

    companion object {
        fun valuesWithoutBiometric(): List<SettingsOption> =
            SettingsOption.values().filter { it != BIOMETRIC }
    }
}

enum class NavigationMode(private val packageName: String) {
    GOOGLE_MAPS("com.google.android.apps.maps"), WAZE("com.waze");

    private fun mapIntent(uriString: String): Intent {
        val mapIntent = Intent(Intent.ACTION_VIEW, uriString.toUri())
        mapIntent.setPackage(packageName)
        return mapIntent
    }

    fun geoLocationIntent(latLng: LatLng): Intent {
        val mapsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:${latLng.lat},${latLng.lng}"))
        mapsIntent.setPackage(packageName)
        return mapsIntent
    }

    fun navigationIntent(latLng: LatLng): Intent =
        when (this) {
            GOOGLE_MAPS -> {
                val googleUrl = "google.navigation:q=${latLng.lat},${latLng.lng}"
                val intentGoogleNav = Intent(Intent.ACTION_VIEW, Uri.parse(googleUrl))
                intentGoogleNav.setPackage(packageName)
                intentGoogleNav
            }

            WAZE -> {
                val wazeUrl = "waze://?ll=${latLng.lat},${latLng.lng}&navigate=yes"
                val intentWaze = Intent(Intent.ACTION_VIEW, Uri.parse(wazeUrl))
                intentWaze.setPackage(packageName)
                intentWaze
            }
        }

    fun openSafely(context: Context, uriString: String) {
        try {
            context.startActivity(mapIntent(uriString))
        } catch (ex: ActivityNotFoundException) {
            context.startActivity(IntentFactory.playStoreIntent(packageName))
        }
    }
}

enum class Cab9RequiredPermission(
    @StringRes val titleResId: Int,
    @DrawableRes val imageResId: Int
) {
    LOCATION_PERMISSION(
        R.string.permission_title_location,
        R.drawable.ic_undraw_location
    ),
    GPS(
        R.string.permission_title_gps,
        R.drawable.ic_undraw_gps_settings
    ),
    AUTO_DATE_TIME(
        R.string.title_date_mismatch,
        R.drawable.ic_undraw_date_time
    ),
    NOTIFICATION(
        R.string.permission_title_notification,
        R.drawable.ic_undraw_alert
    ),
    OVERLAY(
        R.string.permission_title_overlay,
        R.drawable.ic_undraw_overlay
    ),
    BATTERY(
        R.string.permission_title_battery,
        R.drawable.ic_undraw_battery
    ),
    RECORD_AUDIO(
        R.string.permission_title_audio,
        R.drawable.ic_undraw_recording
    );


    fun getSummary(context: Context): String = when (this) {
        LOCATION_PERMISSION -> context.getString(
            R.string.temp_dialog_msg_location_data,
            context.getString(R.string.app_name)
        )

        GPS -> context.getString(
            R.string.dialog_msg_gps_request,
            context.getString(R.string.app_name)
        )

        AUTO_DATE_TIME -> context.getString(
            R.string.dialog_msg_set_auto_date_time,
            context.getString(R.string.app_name)
        )

        NOTIFICATION -> context.getString(R.string.dialog_msg_notification_off)
        OVERLAY -> context.getString(
            R.string.temp_dialog_msg_draw_over,
            context.getString(R.string.app_name)
        )

        BATTERY -> context.getString(
            R.string.temp_dialog_msg_battery_optimize,
            context.getString(R.string.app_name)
        )

        RECORD_AUDIO -> context.getString(
            R.string.dialog_msg_use_speech,
            context.getString(R.string.app_name)
        )
    }

    fun isGranted(context: Context): Boolean =
        when (this) {
            LOCATION_PERMISSION -> hasLocationPermissions(context)
            GPS -> context.isGPSEnabled()
            AUTO_DATE_TIME -> context.isAutomaticDateTimeSettingsEnabled()
            NOTIFICATION -> hasNotificationPermission(context) && context.areNotificationsEnabled()
            OVERLAY -> context.isDrawOverlayAllowed()
            BATTERY -> context.isBatteryOptimised()
            RECORD_AUDIO -> context.isPermissionGranted(Manifest.permission.RECORD_AUDIO)
        }

    companion object {
        fun mandatoryPermissions() =
            listOf(LOCATION_PERMISSION, GPS, NOTIFICATION, AUTO_DATE_TIME)

        fun isAllPermissionGranted(context: Context) =
            Cab9RequiredPermission.values().all { it.isGranted(context) }

        fun isMandatoryPermissionGranted(context: Context) =
            mandatoryPermissions().all { it.isGranted(context) }
    }
}

enum class DiagnosticArea {
    INTERNET, NOTIFICATION, SOUND, BATTERY, OVERLAY;
}