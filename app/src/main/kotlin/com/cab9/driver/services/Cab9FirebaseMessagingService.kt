package com.cab9.driver.services

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.cab9.driver.base.Cab9DriverApp
import com.cab9.driver.data.repos.UserManager
import com.cab9.driver.di.user.UserComponentManager
import com.cab9.driver.settings.SessionManager
import com.cab9.driver.ui.booking.offers.OfferSource
import com.cab9.driver.utils.KEY_BOOKING_ID
import com.cab9.driver.utils.NOTIFICATION_BODY
import com.cab9.driver.utils.NOTIFICATION_PAYLOAD
import com.cab9.driver.utils.NOTIFICATION_TITLE
import com.cab9.driver.utils.NOTIFICATION_TYPE
import com.cab9.driver.utils.NotificationUtils
import com.cab9.driver.utils.TEST_BOOKING_OFFER
import com.cab9.driver.utils.TYPE_BOOKING_ALLOCATED
import com.cab9.driver.utils.TYPE_BOOKING_CANCELLED
import com.cab9.driver.utils.TYPE_BOOKING_CHANGED
import com.cab9.driver.utils.TYPE_BOOKING_OFFER
import com.cab9.driver.utils.TYPE_BOOKING_UNALLOCATED
import com.cab9.driver.utils.TYPE_CANCEL_BIDDING
import com.cab9.driver.utils.TYPE_DRIVER_DOCUMENT_EXPIRATION
import com.cab9.driver.utils.TYPE_DRIVER_FORCED_BREAK
import com.cab9.driver.utils.TYPE_NEW_BIDDING
import com.cab9.driver.utils.TYPE_NEW_CHAT_MESSAGE
import com.cab9.driver.utils.TYPE_SHIFT_CLOSED_ALERT
import com.cab9.driver.utils.TYPE_SHIFT_TIMEOUT_ALERT
import com.cab9.driver.utils.TYPE_UNKNOWN_NOTIFICATION
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.squareup.moshi.Moshi
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class Cab9FirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var moshi: Moshi

    @Inject
    lateinit var cab9App: Cab9DriverApp

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var workManager: WorkManager

    @Inject
    lateinit var userManager: UserManager

    @Inject
    lateinit var userComponentManager: UserComponentManager

    private val knownTypes = listOf(
        TYPE_SHIFT_TIMEOUT_ALERT,
        TYPE_SHIFT_CLOSED_ALERT,
        TYPE_DRIVER_FORCED_BREAK,
        TYPE_BOOKING_ALLOCATED,
        TYPE_BOOKING_UNALLOCATED,
        TYPE_BOOKING_CANCELLED,
        TYPE_NEW_BIDDING,
        TYPE_BOOKING_CHANGED,
        TYPE_CANCEL_BIDDING,
        TYPE_NEW_CHAT_MESSAGE,
        TYPE_DRIVER_DOCUMENT_EXPIRATION,
        TYPE_UNKNOWN_NOTIFICATION
    )

    override fun onNewToken(newToken: String) {
        Timber.d("onNewToken")
        // Update new token only if user is logged in
        if (userManager.isLoggedIn) {
            Timber.d("Register new generated to token...")
            workManager.enqueueUniqueWork(
                UpdateFirebaseTokenWorker.NAME,
                ExistingWorkPolicy.REPLACE,
                UpdateFirebaseTokenWorker.createRequest(newToken)
            )
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        var title = remoteMessage.notification?.title
        val body = remoteMessage.notification?.body
        val payload = remoteMessage.data[NOTIFICATION_PAYLOAD]
        val type = remoteMessage.data[NOTIFICATION_TYPE]

        // For some cases, title does not show in notification body
        // example booking bid cancelled notification
        if (remoteMessage.data.containsKey(NOTIFICATION_TITLE)) {
            title = remoteMessage.data[NOTIFICATION_TITLE]
        }

        Timber.w("Notification received of type: $type".uppercase())

        when (type) {
            TYPE_BOOKING_OFFER, TEST_BOOKING_OFFER -> {
                userComponentManager.bookingOfferManager
                    .showSafely(OfferSource.NOTIFICATION, title, body, payload)
            }

            else -> {
                Timber.d("Notification payload -> $payload")
                Timber.d("Notification body -> $body")
                Timber.d("Notification title -> $title")
                sendNotificationBroadcast(type, title, body, payload)
            }
        }
    }

    override fun handleIntent(intent: Intent) {
        super.handleIntent(intent)
        // Handle offer payload when the app is in background
        if (!cab9App.isForeground && intent.extras?.containsKey(NOTIFICATION_TYPE) == true) {
            when (val notificationType = intent.extras?.getString(NOTIFICATION_TYPE)) {
                TYPE_NEW_CHAT_MESSAGE -> {
                    val jsonPayload = intent.extras?.getString(NOTIFICATION_PAYLOAD)
                    sessionManager.incrementMessageCount()
                    if (NotificationUtils.isPersistentMessage(jsonPayload)) {
                        intent.extras?.getString("gcm.notification.body")?.let { message ->
                            sessionManager.saveNewPersistentMessage(message)
                        }
                    }
                }

                TYPE_BOOKING_UNALLOCATED, TYPE_BOOKING_CANCELLED -> {
                    intent.extras?.getString(NOTIFICATION_PAYLOAD)?.let { jsonPayload ->
                        try {
                            Timber.d("App in killed, save $notificationType booking id!")
                            val jsonObj = JSONObject(jsonPayload)
                            val bookingId = jsonObj.getString(KEY_BOOKING_ID)
                            sessionManager.saveCancelledBookingId(bookingId)
                        } catch (ex: Exception) {
                            // ignore
                        }
                    }
                }

                TYPE_NEW_BIDDING, TYPE_CANCEL_BIDDING -> sessionManager.isJobPoolUpdated = true
            }
        }
    }

    private fun sendNotificationBroadcast(
        type: String?,
        title: String?,
        body: String?,
        payload: String?
    ) {
        val newType = if (type in knownTypes) type else TYPE_UNKNOWN_NOTIFICATION
        LocalBroadcastManager.getInstance(applicationContext)
            .sendBroadcast(Intent(newType).apply {
                putExtra(NOTIFICATION_TITLE, title)
                putExtra(NOTIFICATION_BODY, body)
                putExtra(NOTIFICATION_PAYLOAD, payload)
            })
    }
}