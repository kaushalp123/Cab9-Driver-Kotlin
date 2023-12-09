package com.cab9.driver.services

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
import android.location.Location
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.cab9.driver.BuildConfig
import com.cab9.driver.R
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.*
import com.cab9.driver.data.repos.Cab9Repository
import com.cab9.driver.di.user.UserComponentManager
import com.cab9.driver.ext.*
import com.cab9.driver.network.ServiceNotStartedException
import com.cab9.driver.network.SocketConnectionException
import com.cab9.driver.settings.SessionManager
import com.cab9.driver.ui.booking.offers.BookingOfferManager
import com.cab9.driver.ui.booking.offers.OfferSource
import com.cab9.driver.ui.home.HomeActivity
import com.cab9.driver.utils.ACTION_BOOKING_DETAIL_UPDATE
import com.cab9.driver.utils.ACTION_BOOKING_PRICE_UPDATE
import com.cab9.driver.utils.EXTRA_BOOKING_DETAIL
import com.cab9.driver.utils.EXTRA_BOOKING_PRICE
import com.cab9.driver.utils.LocationUpdateMode
import com.cab9.driver.utils.SharedLocationManager
import com.cab9.driver.utils.TYPE_SHIFT_TIMEOUT_ALERT
import com.cab9.driver.utils.createNotificationChannel
import com.cab9.driver.utils.hasNotificationPermission
import com.cab9.driver.utils.map.hasLocationPermissions
import com.google.android.gms.location.*
import com.squareup.moshi.Moshi
import dagger.hilt.android.AndroidEntryPoint
import io.socket.client.IO
import io.socket.client.Manager
import io.socket.client.Socket
import io.socket.emitter.Emitter
import io.socket.engineio.client.transports.WebSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import java.net.URI
import javax.inject.Inject

/**
 * A class to simulate 1min delay timer.
 */
private class TickHandler(externalScope: CoroutineScope, val delayInSecs: Int) {
    // Backing property to avoid flow emissions from other classes
    private val _tickFlow = MutableSharedFlow<Unit>(replay = 0)
    val tickFlow: SharedFlow<Unit> = _tickFlow

    init {
        externalScope.launch {
            while (true) {
                _tickFlow.emit(Unit)
                delay(delayInSecs * 1000L)
            }
        }
    }
}

@AndroidEntryPoint
class Cab9DriverService : LifecycleService() {

    companion object {
        private const val NOTIFICATION_ID = 1001

        private const val EVENT_SEND_LOCATION = "SEND_LOCATION"
        private const val EVENT_DRIVER_SHIT_UPDATE = "DRIVER_SHIFT_UPDATE"
        private const val EVENT_BOOKING_PRICE_UPDATE = "BOOKING_PRICE_UPDATE"
        private const val EVENT_BOOKING_UPDATE = "BOOKING_UPDATE"
        private const val EVENT_NEW_BOOKING_OFFER = "NEW_BOOKING_OFFER"

        private const val SERVER_DISCONNECT = "io server disconnect"

        const val ACTION_RESTART =
            "com.cab9.driverapp.services.ACTION_RESTART"
        const val ACTION_STOP =
            "com.cab9.driverapp.services.ACTION_STOP"

        const val ACTION_LOCATION_PERMISSION_CHANGED =
            "com.cab9.driverapp.services.ACTION_LOCATION_PERMISSION_CHANGED"
        const val ACTION_SHIFT_STARTED =
            "com.cab9.driverapp.services.ACTION_SHIFT_STARTED"
    }

    @Inject
    lateinit var moshi: Moshi

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var userComponentManager: UserComponentManager

    private val sharedLocationManager: SharedLocationManager
        get() = userComponentManager.sharedLocationManager

    private val bookingOfferManager: BookingOfferManager
        get() = userComponentManager.bookingOfferManager

    private val cab9Repo: Cab9Repository
        get() = userComponentManager.cab9Repo

    private val mobileState: MobileState?
        get() = cab9Repo.mobileState

    private val driverId: String
        get() = sessionManager.userId.orEmpty()

    private var lastLocation: Location? = null
    private var lastLocationSentTimeInMillis: Long = 0L
    private var wakeLock: PowerManager.WakeLock? = null

    private lateinit var socket: Socket

    private val connectListener = Emitter.Listener {
        Timber.w("Web socket connected...".uppercase())
    }

    private val connectErrorListener = Emitter.Listener {
        val reason = it.firstOrNull().toString()
        Timber.w("Socket error -> $reason")
        if (reason == SERVER_DISCONNECT) socket.connect()
    }

    private val disconnectListener = Emitter.Listener {
        Timber.w("Web socket disconnected -> ${it.firstOrNull().toString()}")
    }

    private val socketErrorListener = Emitter.Listener {
        Timber.w("Web socket error -> ${it.firstOrNull().toString()}")
    }

    private val meterChangeListener = Emitter.Listener {
        //Timber.w("Booking price update -> ${it.firstOrNull().toString()}")
        Timber.w("Socket received of type: $EVENT_BOOKING_PRICE_UPDATE".uppercase())
        val jsonStr = it.firstOrNull()?.toString()
        if (!jsonStr.isNullOrEmpty()) {
            moshi.adapter(BookingPriceUpdate::class.java).fromJson(jsonStr)?.let { newPrice ->
                localBroadcastManager.sendBroadcast(Intent(ACTION_BOOKING_PRICE_UPDATE).apply {
                    putExtra(EXTRA_BOOKING_PRICE, BookingMeterModel(newPrice))
                })
            }
        }
    }

    private val bookingChangedListener = Emitter.Listener {
        //Timber.w("Booking detail update -> ${it.firstOrNull().toString()}")
        Timber.w("Socket received of type: $EVENT_BOOKING_UPDATE".uppercase())
        val jsonStr = it.firstOrNull()?.toString()
        if (!jsonStr.isNullOrEmpty()) {
            moshi.adapter(Booking::class.java).fromJson(jsonStr)?.let { booking ->
                localBroadcastManager.sendBroadcast(Intent(ACTION_BOOKING_DETAIL_UPDATE).apply {
                    putExtra(EXTRA_BOOKING_DETAIL, BookingDetailModel(booking))
                })
            }
        }
    }

    private val bookingOfferListener = Emitter.Listener {
        //Timber.w("Booking offer received -> ${it.firstOrNull().toString()}")
        Timber.w("Socket received of type: $EVENT_NEW_BOOKING_OFFER".uppercase())
        val jsonStr = it.firstOrNull()?.toString()
        if (!jsonStr.isNullOrEmpty())
            bookingOfferManager.showSafely(
                OfferSource.SOCKET,
                getString(R.string.notification_title_booking_offer),
                "",
                jsonStr
            )
    }

    private val noLocationAlertReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, data: Intent?) {
            if (data?.action == TYPE_SHIFT_TIMEOUT_ALERT) {
                sharedLocationManager.stopLocationUpdates()
                sharedLocationManager.startLocationUpdates(
                    when (mobileState?.driverStatus) {
                        null, Driver.Status.OFFLINE -> LocationUpdateMode.OFFLINE
                        else -> LocationUpdateMode.ONLINE
                    }
                )
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Timber.w("Cab9DriverService created...".uppercase())

        // Create service notification channel
        createNotificationChannel(
            this,
            getString(R.string.tracking_location_channel_id),
            getString(R.string.app_name),
            getString(R.string.tracking_location_channel_desc),
            null
        )

        // Acquire wakelock to keep service running
        acquireWakeLock()
        // Initialize socket connection
        initializeSocket()

        lifecycleScope.launch {
            launch {
                sharedLocationManager.locationUpdatesFlow
                    .collectLatest {
                        it?.let {
                            lastLocation = it
                            lastLocationSentTimeInMillis = System.currentTimeMillis()
                            trySendingLocationToServer(it)
                        }
                    }
            }
            launch {
                cab9Repo.mobileStateAsFlow.collectLatest {
                    if (it is Outcome.Success) onMobileStateChanged(it.data)
                }
            }
            launch {
                TickHandler(lifecycleScope, 60).tickFlow.collectLatest {
                    trySendingDriverToServer()
                }
            }
        }

        // Refresh mobile state whenever this service is started
        if (mobileState == null) cab9Repo.fetchMobileState()

        localBroadcastManager.registerReceiver(
            noLocationAlertReceiver, IntentFilter(TYPE_SHIFT_TIMEOUT_ALERT)
        )
    }

    private fun onMobileStateChanged(newState: MobileState) {
        updateNotification(newState)
        if (newState.driverStatus == Driver.Status.OFFLINE) disconnectSocket()
        else connectSocket()
        sharedLocationManager.startLocationUpdates(
            when (mobileState?.driverStatus) {
                null, Driver.Status.OFFLINE -> LocationUpdateMode.OFFLINE
                else -> LocationUpdateMode.ONLINE
            }
        )
    }

    private fun createNotification(): Notification {
        Timber.d("Creating Cab9DriverService notification...")
        val pendingIntent = Intent(this, HomeActivity::class.java)
            .let {
                PendingIntent.getActivity(
                    this,
                    0,
                    it,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
        val title = getString(R.string.app_name).plus(" ").plus(getString(R.string.tracking))

        val messageResId =
            if (mobileState == null) R.string.msg_driver_offline
            else mobileState?.driverStatus?.msgResId ?: R.string.msg_driver_offline

        val builder =
            NotificationCompat.Builder(this, getString(R.string.tracking_location_channel_id))
                .setContentTitle(title)
                .setContentText(getString(messageResId))
                .setContentIntent(pendingIntent)
                .setTicker(title)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_notifications)

        return builder.build()
    }

    @SuppressLint("MissingPermission")
    private fun updateNotification(newState: MobileState) {
        val title = getString(R.string.app_name).plus(" ").plus(getString(R.string.tracking))
        val message =
            if (newState.driverStatus == Driver.Status.OFFLINE) getString(R.string.msg_driver_offline)
            else getString(
                R.string.msg_driver_online
            )

        val builder =
            NotificationCompat.Builder(this, getString(R.string.tracking_location_channel_id))
                .setContentTitle(title)
                .setContentText(message)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_notifications)


        if (hasNotificationPermission(this))
            notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Timber.d("onStartCommand executed with startId: $startId")
        Timber.d("Web socket connected -> ${socket.connected()}")
        Timber.d("Driver Status -> ${mobileState?.driverStatus}")

        startForeground()

        if (intent != null) {
            Timber.w("Using intent with action ${intent.action}")
            when (intent.action) {
                ACTION_RESTART -> Timber.w("Cab9Service restarted".uppercase())
                ACTION_SHIFT_STARTED -> Timber.w("Cab9Service started due to shift change".uppercase())
                ACTION_LOCATION_PERMISSION_CHANGED -> {
                    sharedLocationManager.startLocationUpdates(
                        when (mobileState?.driverStatus) {
                            null, Driver.Status.OFFLINE -> LocationUpdateMode.OFFLINE
                            else -> LocationUpdateMode.ONLINE
                        }
                    )
                }

                ACTION_STOP -> stopService()
            }
        } else Timber.d("with a null intent. It has been probably restarted by the system.")
        return START_STICKY
    }

    private fun startForeground() {
        // Before starting the service as foreground check that the app has the
        // appropriate runtime permissions. In this case, verify that the user has
        // granted the LOCATION permissions.
        if (!hasLocationPermissions(this)) {
            // Without camera permissions the service cannot run in the foreground
            // Consider informing user or updating your app UI if visible.
            Timber.e(ServiceNotStartedException("Location permissions missing"))
            stopService()
            return
        }

        try {
            Timber.w("Cab9DriverService started foreground...".uppercase())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    createNotification(),
                    FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else startForeground(NOTIFICATION_ID, createNotification())
        } catch (ex: Exception) {
            Timber.e(ServiceNotStartedException(ex))
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        //releaseResources()
        Timber.w("Cab9DriverService task removed...".uppercase())
        if (mobileState?.driverStatus == Driver.Status.ONLINE) {
            Timber.d("Driver is online, try restarting Cab9DriverService...".uppercase())

            val restartServicePendingIntent =
                Intent(this, Cab9DriverService::class.java)
                    .apply { action = ACTION_RESTART }
                    .also { it.setPackage(packageName) }
                    .let {
                        PendingIntent.getService(
                            this,
                            1,
                            it,
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    }

            getSystemService<AlarmManager>()?.set(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 1000,
                restartServicePendingIntent
            )
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Timber.w("Cab9DriverService trim memory...".uppercase())
    }

    override fun onDestroy() {
        releaseResources()
        localBroadcastManager.unregisterReceiver(noLocationAlertReceiver)
        Timber.w("Cab9DriverService destroyed...".uppercase())
        super.onDestroy()
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        wakeLock = getSystemService<PowerManager>()?.run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Cab9DriverService::lock")
                .apply {
                    Timber.w("Power lock acquired".uppercase())
                    acquire()
                }
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Timber.w("Power lock removed".uppercase())
                }
            }
            //stopForeground(true)
        } catch (e: Exception) {
            Timber.d("Service stopped without being started: ${e.message}")
        }
    }

    private fun initializeSocket() {
        Timber.w("Initialize Cab9Driver Socket...".uppercase())
        val accessToken = sessionManager.authToken
        val ioOptions = IO.Options.builder()
            .setQuery("authorization=$accessToken")
            .setPath("/socket.io")
            .setTransports(arrayOf(WebSocket.NAME))
            .setForceNew(true)
            .setUpgrade(false)
            .build()
        socket = IO.socket(URI.create(BuildConfig.BASE_NODE_URL), ioOptions)
    }

    private fun connectSocket() {
        if (!socket.connected()) {
            Timber.w("Connecting Cab9Driver Socket...".uppercase())
            try {
                socket.on(Socket.EVENT_CONNECT, connectListener)
                socket.on(Socket.EVENT_DISCONNECT, disconnectListener)
                socket.on(Socket.EVENT_CONNECT_ERROR, connectErrorListener)
                socket.on(Manager.EVENT_ERROR, socketErrorListener)
                socket.on(EVENT_BOOKING_PRICE_UPDATE, meterChangeListener)
                socket.on(EVENT_BOOKING_UPDATE, bookingChangedListener)
                socket.on(EVENT_NEW_BOOKING_OFFER, bookingOfferListener)
                socket.connect()
            } catch (ex: Exception) {
                Timber.e(SocketConnectionException(ex))
            }
        }
    }

    private fun disconnectSocket() {
        if (socket.connected()) {
            Timber.w("Disconnecting Cab9Driver Socket...".uppercase())
            try {
                socket.disconnect()
                socket.off(Socket.EVENT_CONNECT, connectListener)
                socket.off(Socket.EVENT_DISCONNECT, disconnectListener)
                socket.off(Socket.EVENT_CONNECT_ERROR, connectErrorListener)
                socket.off(Manager.EVENT_ERROR, socketErrorListener)
                socket.off(EVENT_BOOKING_PRICE_UPDATE, meterChangeListener)
                socket.off(EVENT_BOOKING_UPDATE, bookingChangedListener)
                socket.off(EVENT_NEW_BOOKING_OFFER, bookingOfferListener)
            } catch (ex: Exception) {
                Timber.e(SocketConnectionException(ex))
            }
        }
    }

    private fun trySendingLocationToServer(location: Location) {
        if (socket.connected()) {
            val obj = JSONObject(
                mapOf(
                    "DriverId" to driverId,
                    "Latitude" to location.lat,
                    "Longitude" to location.lng,
                    "Speed" to location.speed,
                    "Bearing" to location.bearing,
                    "DeviceId" to sessionManager.deviceId
                )
            )
            Timber.i("SOCKET [Connected:${socket.connected()}, Emit:$EVENT_SEND_LOCATION]")
            socket.emit(EVENT_SEND_LOCATION, obj)
        }
    }

    private fun trySendingDriverToServer() {
        if (socket.connected()) {
            mobileState?.let {
                val statusJson = JSONObject(
                    mapOf(
                        "DriverId" to driverId,
                        "Status" to it.driverStatus.constant
                    )
                )
                Timber.i("SOCKET [Connected:${socket.connected()}, Emit:$EVENT_DRIVER_SHIT_UPDATE]")
                socket.emit(EVENT_DRIVER_SHIT_UPDATE, statusJson)
            }
        }
    }

    private fun releaseResources() {
        sharedLocationManager.stopLocationUpdates()
        disconnectSocket()
        releaseWakeLock()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    private fun stopService() {
        releaseResources()
        stopSelf()
    }
}