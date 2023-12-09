package com.cab9.driver.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.net.toUri
import com.cab9.driver.ui.account.settings.PermissionCheckActivity
import timber.log.Timber


class BatteryOptimization : ActivityResultContract<Void?, Void?>() {

    @SuppressLint("BatteryLife")
    override fun createIntent(context: Context, input: Void?): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = "package:${context.packageName}".toUri()
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Void? {
        Timber.d("Battery optimization intent result -> $resultCode")
        return null
    }
}

class NotificationSettings : ActivityResultContract<Void?, Void?>() {
    override fun createIntent(context: Context, input: Void?): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else Intent(ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            data = "package:${context.packageName}".toUri()
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Void? {
        Timber.d("Notification intent result -> $resultCode")
        return null
    }
}

class SystemOverlay : ActivityResultContract<Void?, Void?>() {

    override fun createIntent(context: Context, input: Void?): Intent {
        return Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = "package:${context.packageName}".toUri()
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Void? {
        Timber.d("System overlay intent result -> $resultCode")
        return null
    }

}

class CheckRequiredPermission : ActivityResultContract<Void?, Void?>() {

    override fun createIntent(context: Context, input: Void?): Intent {
        return Intent(context, PermissionCheckActivity::class.java)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Void? {
        Timber.d("Permission check  intent result -> $resultCode")
        return null
    }
}

class AutoDateTimeSettings : ActivityResultContract<Void?, Void?>() {
    override fun createIntent(context: Context, input: Void?): Intent {
        return Intent(Settings.ACTION_DATE_SETTINGS)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Void? {
        return null
    }

}

class DeviceGPSSettings : ActivityResultContract<Void?, Void?>() {
    override fun createIntent(context: Context, input: Void?): Intent {
        return Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Void? {
        return null
    }
}