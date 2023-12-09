package com.cab9.driver.services

import android.location.Location
import com.cab9.driver.BuildConfig
import com.cab9.driver.data.models.Booking
import com.cab9.driver.data.models.BookingDetailModel
import com.cab9.driver.data.models.BookingMeterModel
import com.cab9.driver.data.models.BookingPriceUpdate
import com.cab9.driver.data.models.Driver
import com.cab9.driver.data.models.UserTyping
import com.cab9.driver.di.user.LoggedInScope
import com.cab9.driver.ext.lat
import com.cab9.driver.ext.lng
import com.cab9.driver.settings.SessionManager
import com.squareup.moshi.Moshi
import io.socket.client.IO
import io.socket.client.Manager
import io.socket.client.Socket
import io.socket.emitter.Emitter
import io.socket.engineio.client.transports.WebSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.threeten.bp.Duration
import timber.log.Timber
import java.net.URI
import javax.inject.Inject

sealed class SocketEvent {
    object Connected : SocketEvent()
    object Disconnected : SocketEvent()

    data class Error(val msg: String) : SocketEvent()
    data class MeterChanged(val meter: BookingMeterModel) : SocketEvent()
    data class BookingChanged(val booking: BookingDetailModel) : SocketEvent()
    data class ChatMessage(val message: JSONObject) : SocketEvent()
    data class BookingOffer(val jsonPayload: String) : SocketEvent()
    data class UserTypingEvent(val userTyping: UserTyping) : SocketEvent()
}

@LoggedInScope
class SocketManager @Inject constructor(
    private val externalScope: CoroutineScope,
    private val sessionManager: SessionManager,
    private val moshi: Moshi
) {

    private lateinit var socket: Socket

    private val userId: String = sessionManager.userId.orEmpty()
    private val deviceId: String = sessionManager.deviceId.orEmpty()

    private val _failedMessageFlow = MutableStateFlow(JSONObject())
    val failedMessageFlow = _failedMessageFlow.asStateFlow()

    val socketCallback = callbackFlow {
        val connectListener = Emitter.Listener { trySend(SocketEvent.Connected) }
        val disconnectListener = Emitter.Listener { trySend(SocketEvent.Disconnected) }
        val connectionErrorListener = Emitter.Listener {
            val reason = it.firstOrNull().toString()
            trySend(SocketEvent.Error(reason))

            Timber.w("SOCKET CONNECTION ERROR -> $reason")
            if (reason == SERVER_DISCONNECT) {
                Timber.w("Retrying socket connection...".uppercase())
                socket.connect()
            }
        }
        val errorListener = Emitter.Listener {
            trySend(SocketEvent.Error(it.firstOrNull()?.toString().orEmpty()))
        }

        val bookingDetailChangeListener = Emitter.Listener {
            val jsonStr = it.firstOrNull()?.toString()
            if (!jsonStr.isNullOrEmpty()) {
                moshi.adapter(Booking::class.java).fromJson(jsonStr)?.let { booking ->
                    trySend(SocketEvent.BookingChanged(BookingDetailModel(booking)))
                }
            }
        }

        val meterPriceChangeListener = Emitter.Listener {
            val jsonStr = it.firstOrNull()?.toString()
            if (!jsonStr.isNullOrEmpty()) {
                moshi.adapter(BookingPriceUpdate::class.java).fromJson(jsonStr)?.let { newPrice ->
                    trySend(SocketEvent.MeterChanged(BookingMeterModel(newPrice)))
                }
            }
        }

        val bookingOfferListener = Emitter.Listener {
            val jsonStr = it.firstOrNull()?.toString()
            if (!jsonStr.isNullOrEmpty()) trySend(SocketEvent.BookingOffer(jsonStr))
        }

        val chatMessageListener = Emitter.Listener {
            val jsonStr = it.firstOrNull()?.toString()
            Timber.i("NEW CHAT MESSAGE RECEIVED:$jsonStr")
            if (!jsonStr.isNullOrEmpty()) {
                val json = JSONObject(jsonStr)
                val message = JSONObject(json.getString("message"))
                trySend(SocketEvent.ChatMessage(message))

                if(!message.isNull("RefId")) {
                    val refId = message.get("RefId").toString().toLongOrNull()
                    if(refId != null) {
                        if (arrayListOfPendingMsgs.containsKey(refId.toLong())) {
                            arrayListOfPendingMsgs.remove(refId.toLong())
                            resetCurrentMessageObject()
                        }
                    }
                }
            }
        }

        val userTypingMessageListener = Emitter.Listener {
            val jsonStr = it.firstOrNull()?.toString()
            Timber.i("NEW USER TYPING EVENT RECEIVED:$jsonStr")
            if (!jsonStr.isNullOrEmpty()) {
                val json = JSONObject(jsonStr)
                if (!json.getString("UserId").equals(sessionManager.userId))
                    moshi.adapter(UserTyping::class.java).fromJson(jsonStr)?.let { userTyping ->
                        userTyping.timeStamp?.let { timeStamp ->
                            val minutesPassed =
                                Duration.between(timeStamp, org.threeten.bp.Instant.now())
                                    .toSecondsPart()
                            if (minutesPassed > 30) {
                                Timber.i("old event")
                            } else {
                                Timber.i("current event")
                                trySend(SocketEvent.UserTypingEvent(userTyping))
                            }
                        }
                    }
            }
        }

        val ioOptions = IO.Options.builder()
            .setQuery("authorization=${sessionManager.authToken}")
            .setPath("/socket.io")
            .setTransports(arrayOf(WebSocket.NAME))
            .setForceNew(true)
            .setUpgrade(false)
            .build()

        socket = IO.socket(URI.create(BuildConfig.BASE_NODE_URL), ioOptions)

        socket.on(Socket.EVENT_CONNECT, connectListener)
        socket.on(Socket.EVENT_DISCONNECT, disconnectListener)
        socket.on(Socket.EVENT_CONNECT_ERROR, connectionErrorListener)
        socket.on(Manager.EVENT_ERROR, errorListener)
        socket.on(EVENT_BOOKING_PRICE_UPDATE, meterPriceChangeListener)
        socket.on(EVENT_BOOKING_DETAIL_UPDATE, bookingDetailChangeListener)
        socket.on(EVENT_NEW_BOOKING_OFFER, bookingOfferListener)
        socket.on(EVENT_NEW_MESSAGE_ARRIVED, chatMessageListener)
        socket.on(EVENT_USER_TYPING, userTypingMessageListener)

        try {
            Timber.w("Connecting Socket...".uppercase())
            socket.connect()
        } catch (ex: Exception) {
            close(ex)
        }

        awaitClose {
            Timber.w("Disconnecting Socket".uppercase())
            socket.disconnect()
            socket.off(Socket.EVENT_CONNECT, connectListener)
            socket.off(Socket.EVENT_DISCONNECT, disconnectListener)
            socket.off(Socket.EVENT_CONNECT_ERROR, connectionErrorListener)
            socket.off(Manager.EVENT_ERROR, errorListener)
            socket.off(EVENT_BOOKING_PRICE_UPDATE, meterPriceChangeListener)
            socket.off(EVENT_BOOKING_DETAIL_UPDATE, bookingDetailChangeListener)
            socket.off(EVENT_NEW_BOOKING_OFFER, bookingOfferListener)
            socket.off(EVENT_NEW_MESSAGE_ARRIVED, chatMessageListener)
            socket.off(EVENT_USER_TYPING, userTypingMessageListener)
        }
    }.shareIn(externalScope, replay = 0, started = SharingStarted.WhileSubscribed())

    init {
        startTimerForMessageConfirmation()
    }

    fun trySendingLocationUpdates(location: Location) {
        if (this::socket.isInitialized && socket.connected()) {
            Timber.i("SOCKET [Connected:${socket.connected()}, Emit:${EVENT_SEND_LOCATION}]")
            socket.emit(
                EVENT_SEND_LOCATION, JSONObject(
                    mapOf(
                        "DriverId" to userId,
                        "Latitude" to location.lat,
                        "Longitude" to location.lng,
                        "Speed" to location.speed,
                        "Bearing" to location.bearing,
                        "DeviceId" to deviceId
                    )
                )
            )
        }
    }

    fun trySendingDriverStatusUpdates(status: Driver.Status) {
        if (this::socket.isInitialized && socket.connected()) {
            Timber.i("SOCKET [Connected:${socket.connected()}, Emit:${EVENT_DRIVER_SHIT_UPDATE}]")
            socket.emit(
                EVENT_DRIVER_SHIT_UPDATE, JSONObject(
                    mapOf(
                        "DriverId" to deviceId,
                        "Status" to status.constant
                    )
                )
            )
        }
    }

    private val arrayListOfPendingMsgs = mutableMapOf<Long, Long>()
    private var currentMessageObject: JSONObject? = null

    fun sendNewChatMessage(message: JSONObject, currentTimeInMillis: Long) {
        currentMessageObject = message
        val refId = message.get("RefId").toString().toLongOrNull()
        if (this::socket.isInitialized && socket.connected()) {
            Timber.i("SOCKET [Connected:${socket.connected()}, Emit:${EVENT_NEW_CHAT_MESSAGE}], Event data: $message")
            socket.emit(EVENT_NEW_CHAT_MESSAGE, message)
            if(refId != null) {
                arrayListOfPendingMsgs[refId] = currentTimeInMillis
                Timber.i("new refId added " + arrayListOfPendingMsgs[refId])
            }
        } else {
            Timber.i("SOCKET CONNECTION ERROR, Emit:${EVENT_NEW_CHAT_MESSAGE}], Event data: $message")
            refId?.let {
                arrayListOfPendingMsgs[refId] = currentTimeInMillis
                _failedMessageFlow.value = currentMessageObject as JSONObject
            }
        }
    }

    fun resetCurrentMessageObject() {
        currentMessageObject = null
    }

    fun removeSentMessagesRefId(refId: String) {
        arrayListOfPendingMsgs.remove(refId.toLongOrNull())
    }

    fun sendUserTypingEvent(message: JSONObject) {
        if (this::socket.isInitialized && socket.connected()) {
            Timber.i("SOCKET [Connected:${socket.connected()}, Emit:${EVENT_USER_TYPING}], event object: $message")
            socket.emit(EVENT_USER_TYPING, message)
        } else Timber.i("socket connection error")
    }

    fun sendMessageReadStatusEvent(message: JSONObject) {
        if (this::socket.isInitialized && socket.connected()) {
            Timber.i("SOCKET [Connected:${socket.connected()}, Emit:${EVENT_MARK_MESSAGES_SEEN}], event object: $message")
            socket.emit(EVENT_MARK_MESSAGES_SEEN, message)
        } else Timber.i("socket connection error")
    }

    private fun startTimerForMessageConfirmation() {
        externalScope.launch {
            while (isActive) {
                arrayListOfPendingMsgs?.let {
                    if(it.isNotEmpty()) {
                        it.forEach { item ->
                            Timber.i("item.value :" + item.value)
                            if (System.currentTimeMillis() - item.value > 5000) {
                                // update UI as message failed
                                Timber.i("found a message without reply")
                                currentMessageObject?.let { message ->
                                    _failedMessageFlow.value = message
                                }
                            } else Timber.i("no failed messages found")
                        }
                        delay(5000)
                    }
                }
            }
        }
    }

    companion object {
        private const val EVENT_SEND_LOCATION = "SEND_LOCATION"
        private const val EVENT_DRIVER_SHIT_UPDATE = "DRIVER_SHIFT_UPDATE"
        private const val EVENT_BOOKING_PRICE_UPDATE = "BOOKING_PRICE_UPDATE"
        private const val EVENT_BOOKING_DETAIL_UPDATE = "BOOKING_UPDATE"
        private const val SERVER_DISCONNECT = "io server disconnect"
        private const val EVENT_NEW_BOOKING_OFFER = "NEW_BOOKING_OFFER"
        private const val EVENT_NEW_CHAT_MESSAGE = "SEND_MESSAGE"
        private const val EVENT_USER_TYPING = "USER_TYPING"
        private const val EVENT_MARK_MESSAGES_SEEN = "MARK_MESSAGES_SEEN"

        private const val EVENT_NEW_BROADCAST_MESSAGE_ARRIVED = "NEW_BROADCAST_MESSAGE_ARRIVED"
        private const val EVENT_NEW_MESSAGE_ARRIVED = "NEW_MESSAGE_ARRIVED"
    }
}