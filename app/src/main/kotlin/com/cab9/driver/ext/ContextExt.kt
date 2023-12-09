package com.cab9.driver.ext

import android.app.Activity
import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.LocationManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.TypedValue
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.annotation.ArrayRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.cab9.driver.R
import com.cab9.driver.base.Cab9DriverApp
import com.cab9.driver.services.Cab9DriverService
import com.cab9.driver.utils.map.hasLocationPermissions
import timber.log.Timber

inline val Context.screenWidth: Int
    get() = resources.displayMetrics.widthPixels

inline val Context.screenHeight: Int
    get() = resources.displayMetrics.heightPixels

inline val Context.localBroadcastManager: LocalBroadcastManager
    get() = LocalBroadcastManager.getInstance(this)

inline val Context.notificationManager: NotificationManagerCompat
    get() = NotificationManagerCompat.from(this)

inline val Activity.cab9App: Cab9DriverApp
    get() = application as Cab9DriverApp

inline val Activity.canEnterPictureInPictureMode: Boolean
    // Minimum android version for PIP mode support is API 26,
    // since we are facing some crash on Samsung device API level
    // less than 26, changing minimum to API 29 for now.
    get() = hasPictureInPictureFeature
            // Some Samsung device reported crash when talkback accessibility is enabled.
            // Adding this check to sustain crash on those devices
            && !(isSamsungDevice() && isTouchExplorationEnabled)

inline val Context.hasPictureInPictureFeature: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

/**
 * Checks to see that this device has accessibility and touch exploration enabled.
 */
inline val Context.isTouchExplorationEnabled: Boolean
    get() {
        val am = getSystemService<AccessibilityManager>()
        return am?.isEnabled == true && am.isTouchExplorationEnabled
    }

//inline fun Context.startForegroundServiceCompat(serviceIntent: Intent) {
//    ContextCompat.startForegroundService(this, serviceIntent)
//}

inline fun startCab9DriverService(context: Context, newAction: String? = null) {
    Timber.w("Starting Cab9DriverService...".uppercase())
    ContextCompat.startForegroundService(
        context,
        Intent(context, Cab9DriverService::class.java).apply {
            if (!newAction.isNullOrEmpty()) action = newAction
        })
}

inline fun Drawable.tint(@ColorInt coloInt: Int) {
    DrawableCompat.setTint(this, coloInt)
}

inline fun Context.bitmapDrawable(@DrawableRes drawableResId: Int) =
    BitmapDrawable(resources, BitmapFactory.decodeResource(resources, drawableResId))

inline val Context.configuration: Configuration
    get() = resources.configuration

inline val Context.isDarkThemeActive: Boolean
    get() = (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

inline fun Context.isPermissionGranted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

inline fun Context.isPermissionGranted(permissions: List<String>) =
    permissions.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

inline fun Context.hasFeature(feature: String): Boolean = packageManager.hasSystemFeature(feature)

inline fun Activity.shouldShowRequestPermissionRationale(permission: String): Boolean =
    ActivityCompat.shouldShowRequestPermissionRationale(this, permission)

inline fun Context.quantityText(
    @PluralsRes pluralResId: Int,
    @StringRes zeroResId: Int,
    quantity: Int
): String {
    return if (quantity == 0) getString(zeroResId)
    else resources.getQuantityString(pluralResId, quantity, quantity)
}

inline fun Context.colorInt(@ColorRes colorResId: Int): Int =
    ContextCompat.getColor(this, colorResId)

inline fun Context.drawable(@DrawableRes drawableResId: Int): Drawable? =
    ContextCompat.getDrawable(this, drawableResId)

inline fun Context.arrayRes(@ArrayRes arrayResId: Int) = resources.getStringArray(arrayResId)

inline fun Context.toast(text: String) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}

inline fun Context.toast(@StringRes textResId: Int) {
    Toast.makeText(this, textResId, Toast.LENGTH_SHORT).show()
}

inline fun Context.longToast(text: String) {
    Toast.makeText(this, text, Toast.LENGTH_LONG).show()
}

inline fun Context.longToast(@StringRes textResId: Int) {
    Toast.makeText(this, textResId, Toast.LENGTH_LONG).show()
}

inline fun Context.dpToPx(dip: Float) {
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, resources.displayMetrics)
}

inline fun Number.toPx() = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    this.toFloat(),
    Resources.getSystem().displayMetrics
)

inline fun Activity.showKeyboard() {
    WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.ime())
}

inline fun Activity.hideKeyboard() {
    WindowInsetsControllerCompat(window, window.decorView).hide(WindowInsetsCompat.Type.ime())
}


//fun Context.materialAlert(init: MaterialAlertBuilder<DialogInterface>.() -> Unit): MaterialAlertBuilder<DialogInterface> =
//    AndroidMaterialAlertBuilder(this).apply { init() }

//fun Context.openPdfFile(file: File) {
//    try {
//        val uri = FileManager.getUriForFile(this, file)
//        val intent = Intent(Intent.ACTION_VIEW)
//        intent.setDataAndType(uri, "application/pdf")
//        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
//        startActivity(intent)
//    } catch (ex: Exception) {
//        toast(getString(R.string.err_msg_generic))
//    }
//}

inline fun Context.isDrawOverlayAllowed() = Settings.canDrawOverlays(this)

inline fun Context.areNotificationsEnabled() =
    NotificationManagerCompat.from(this).areNotificationsEnabled()

inline fun Context.isBatteryOptimised() =
    getSystemService<PowerManager>()?.isIgnoringBatteryOptimizations(packageName) == true

fun Context.maximizeVolume() {
    getSystemService<AudioManager>()?.let { audioManager ->
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
    }
}

/**
 * Returns true if application is foreground.
 */
fun Context.isAppForeground(): Boolean {
    getSystemService<ActivityManager>()?.let { activityManager ->
        val matches = activityManager.runningAppProcesses.firstOrNull { appProcess ->
            appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    && appProcess.processName == packageName
        }
        return matches != null
    }
    return false
}

/**
 * Returns true if application is running.
 */
fun Context.isAppRunning(): Boolean {
    getSystemService<ActivityManager>()?.let { activityManager ->
        val matches = activityManager.runningAppProcesses.firstOrNull {
            it.processName == packageName
        }
        return matches != null
    }
    return false
}

fun Context.isLockscreenEnabled(): Boolean {
    val isInteractive = getSystemService<PowerManager>()?.isInteractive == true
    val isKeyguardLocked = getSystemService<KeyguardManager>()?.isKeyguardLocked == true
    return !isInteractive || isKeyguardLocked
}

inline fun Context.isAutoDateTimeEnabled(): Boolean {
    return Settings.Global.getInt(contentResolver, Settings.Global.AUTO_TIME) == 1
}

inline fun Context.isAutoTimeZoneEnabled(): Boolean {
    return Settings.Global.getInt(contentResolver, Settings.Global.AUTO_TIME_ZONE) == 1
}

inline fun Context.isAutomaticDateTimeSettingsEnabled(): Boolean {
    return isAutoDateTimeEnabled() && isAutoTimeZoneEnabled()
}

/**
 * Returns the current enabled/disabled status of the GPS provider.
 */
inline fun Context.isGPSEnabled(): Boolean {
    return getSystemService<LocationManager>()?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
}

inline fun Context.openExternalUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (ex: Exception) {
        toast(R.string.err_app_not_found)
    }
}

inline fun Context.openDialApp(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(url)))
    } catch (ex: Exception) {
        toast(R.string.err_app_not_found)
    }
}

inline fun Context.openSmsApp(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse(url)))
    } catch (ex: Exception) {
        toast(R.string.err_app_not_found)
    }
}

fun isSamsungDevice() = Build.MANUFACTURER.equals("Samsung", ignoreCase = true)
