package com.cab9.driver.utils

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cab9.driver.R
import com.cab9.driver.ext.doFromSdk
import org.json.JSONObject

const val NOTIFICATION_TYPE = "type"
const val NOTIFICATION_PAYLOAD = "payload"
const val NOTIFICATION_TITLE = "title"
const val NOTIFICATION_BODY = "body"

const val KEY_BOOKING_ID = "BookingId"

const val TYPE_UNKNOWN_NOTIFICATION = "UNKNOWN_NOTIFICATION"
const val TYPE_DRIVER_DOCUMENT_EXPIRATION = "DRIVER_DOCUMENT_EXPIRATION"

const val TYPE_SHIFT_TIMEOUT_ALERT = "SHIFT_TIMEOUT_ALERT"
const val TYPE_SHIFT_CLOSED_ALERT = "SHIFT_CLOSED_ALERT"

const val TYPE_BOOKING_ALLOCATED = "NEW_BOOKING"
const val TYPE_BOOKING_UNALLOCATED = "BOOKING_UNALLOCATED"
const val TYPE_BOOKING_CHANGED = "BOOKING_CHANGED"
const val TYPE_BOOKING_CANCELLED = "BOOKING_CANCELLED"

const val TYPE_NEW_BIDDING = "NEW_BIDDING"
const val TYPE_CANCEL_BIDDING = "CANCEL_BIDDING"

const val TYPE_BOOKING_OFFER = "NEW_BOOKING_OFFER"
const val TEST_BOOKING_OFFER = "TEST_BOOKING_OFFER"
const val TYPE_DRIVER_FORCED_BREAK = "DRIVER_FORCED_BREAK"
const val TYPE_NEW_CHAT_MESSAGE = "NEW_CHAT_MESSAGE"

const val BOOKING_OFFER_NOTIFICATION_ID = 9999

fun createNotificationChannel(
    context: Context,
    id: String,
    name: String,
    desc: String,
    toneUri: Uri?
) {
    doFromSdk(Build.VERSION_CODES.O) {
        // Create cancelled booking notification channel with different sound
        NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH)
            .apply {
                lightColor = Color.RED
                description = desc
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)

                if (toneUri != null) {
                    setSound(
                        toneUri,
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                            .build()
                    )
                }
            }.also { NotificationManagerCompat.from(context).createNotificationChannel(it) }
    }
}

@SuppressLint("MissingPermission")
fun showNotification(context: Context, title: String, message: String) {
    val channelId = context.getString(R.string.default_notification_channel_id)
    val builder = NotificationCompat.Builder(context, channelId)
        .setContentTitle(title)
        .setContentText(message)
        .setSmallIcon(R.drawable.ic_notifications)
        .setAutoCancel(true)
        .setWhen(System.currentTimeMillis())

    NotificationManagerCompat.from(context)
        .notify(System.currentTimeMillis().toInt(), builder.build())
}

object NotificationUtils {

    fun isPersistentMessage(jsonPayload: String?): Boolean = try {
        if (jsonPayload.isNullOrEmpty()) false
        else {
            val jsonObj = JSONObject(jsonPayload)
            jsonObj.getString("Type") == "Persistent"
        }
    } catch (ex: Exception) {
        false
    }

}