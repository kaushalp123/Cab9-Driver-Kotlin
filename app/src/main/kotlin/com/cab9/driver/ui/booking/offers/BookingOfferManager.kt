package com.cab9.driver.ui.booking.offers

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cab9.driver.R
import com.cab9.driver.base.Cab9DriverApp
import com.cab9.driver.data.models.BookingOfferPayload
import com.cab9.driver.di.user.LoggedInScope
import com.cab9.driver.ext.isDrawOverlayAllowed
import com.cab9.driver.ext.isLockscreenEnabled
import com.cab9.driver.ext.localBroadcastManager
import com.cab9.driver.network.BookingOfferException
import com.cab9.driver.utils.ACTION_TEST_BOOKING_OFFER_RECEIVED
import com.cab9.driver.utils.BOOKING_OFFER_NOTIFICATION_ID
import com.cab9.driver.utils.getBookingOfferToneUri
import com.squareup.moshi.Moshi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import timber.log.Timber
import javax.inject.Inject

enum class OfferSource {
    NOTIFICATION, SOCKET;
}

@LoggedInScope
class BookingOfferManager @Inject constructor(
    private val cab9App: Cab9DriverApp,
    private val moshi: Moshi
) {

    /**
     * To keep track when booking offer is starting to show. This
     * will be used to avoid entering PictureInPicture while on FOLLOW_ON jobs.
     */
    var isStarting = false
        set(value) {
            field = value
            if (value) {
                // Reset value after 1 secs
                MainScope().launch {
                    delay(1_500)
                    field = false
                }
            }
        }

    fun showSafely(source: OfferSource, title: String?, body: String?, jsonPayload: String?) {
        try {
            if (jsonPayload.isNullOrEmpty()) return
            val payload =
                moshi.adapter(BookingOfferPayload::class.java).fromJson(jsonPayload) ?: return

            // Step 1: Check if offer is test and show it anyway
            if (!cab9App.isSumUpAppOpen && cab9App.isForeground && payload.isTestOffer) {
                openTestBookingOfferScreen(source, jsonPayload)
                return
            }

            // Step 2: Check time remaining before we can show this offer
            val difference = Duration.between(Instant.now(), payload.expiresIn).toMillis()
            if (difference <= 1000) {
                Timber.e(BookingOfferException("Offer received via $source after ${difference / 1000} sec(s)"))
                return
            }

            when {
                // Step 3a: Check if the app is granted necessary permission to show full screen
                !cab9App.isSumUpAppOpen && (cab9App.isForeground || (!cab9App.isLockscreenEnabled() && cab9App.isDrawOverlayAllowed())) -> {
                    openBookingOfferScreen(source, jsonPayload)
                }

                else -> {
                    // Step 3b: Show using notification instead
                    showBookingOfferNotification(
                        source = source,
                        title = title.orEmpty()
                            .ifEmpty { cab9App.getString(R.string.notification_title_booking_offer) },
                        body = body.orEmpty().ifEmpty {
                            payload.pickupAddress.orEmpty()
                                .ifEmpty { cab9App.getString(R.string.notification_msg_booking_offer) }
                        },
                        timeout = difference,
                        jsonPayload = jsonPayload
                    )
                }
            }
        } catch (ex: Exception) {
            Timber.e(BookingOfferException(ex))
        }
    }


    @SuppressLint("MissingPermission")
    private fun showBookingOfferNotification(
        source: OfferSource,
        title: String,
        body: String,
        timeout: Long,
        jsonPayload: String
    ) {
        Timber.d("Create and show booking offer notification...")
        // Create pending intent action for notification
        val fullScreenPendingIntent = PendingIntent
            .getActivity(
                cab9App,
                0,
                BookingOfferActivity.newIntent(cab9App, source, jsonPayload),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

        // create notification builder
        val builder = NotificationCompat
            .Builder(cab9App, cab9App.getString(R.string.booking_offer_channel_id))
            .setSmallIcon(R.drawable.ic_notifications)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentTitle(title)
            .setContentText(body)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .setTimeoutAfter(timeout)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSound(getBookingOfferToneUri(cab9App))
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)

        NotificationManagerCompat.from(cab9App)
            .notify(BOOKING_OFFER_NOTIFICATION_ID, builder.build())
    }


    private fun sendBroadcast(data: Intent) {
        cab9App.localBroadcastManager.sendBroadcast(data)
    }

    private fun openTestBookingOfferScreen(source: OfferSource, jsonPayload: String) {
        // Step 2: Handle test booking offer only when app is foreground
        sendBroadcast(Intent(ACTION_TEST_BOOKING_OFFER_RECEIVED))
        cab9App.startActivity(BookingOfferActivity.newIntent(cab9App, source, jsonPayload))
    }

    private fun openBookingOfferScreen(source: OfferSource, jsonPayload: String) {
        isStarting = true
        cab9App.startActivity(BookingOfferActivity.newIntent(cab9App, source, jsonPayload))
    }
}