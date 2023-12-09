package com.cab9.driver.ui.booking.offers

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cab9.driver.R
import com.cab9.driver.base.Cab9DriverApp
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.Booking
import com.cab9.driver.data.models.BookingOfferPayload
import com.cab9.driver.data.models.BookingOfferStatus
import com.cab9.driver.data.models.MobileState
import com.cab9.driver.di.user.UserComponentManager
import com.cab9.driver.ext.*
import com.cab9.driver.network.ApiErrorHandler
import com.cab9.driver.network.BookingOfferReadException
import com.cab9.driver.settings.SessionManager
import com.cab9.driver.utils.BALANCE_ZERO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import timber.log.Timber
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BookingOfferViewModel @Inject constructor(
    private val application: Cab9DriverApp,
    private val userComponentManager: UserComponentManager,
    private val sessionManager: SessionManager,
    private val apiErrorHandler: ApiErrorHandler
) : ViewModel() {

    val mobileState: MobileState?
        get() = userComponentManager.cab9Repo.mobileState

    private val _bookingOfferPayloadFlow = MutableStateFlow<BookingOfferPayload?>(null)
    private val offerPayload: BookingOfferPayload?
        get() = _bookingOfferPayloadFlow.value

    val bookingId: String?
        get() = offerPayload?.bookingId

    val isTestOffer: Boolean
        get() = offerPayload?.isTestOffer == true

    val isFollowOnOffer: Boolean
        get() = offerPayload?.isFollowOn == true

    private val _timerCounter = MutableStateFlow("00:00")
    val timerCounter = _timerCounter.asStateFlow()

    private val _timerExpired = MutableSharedFlow<Boolean>(replay = 0)
    val timerExpired = _timerExpired.asSharedFlow()

    private var timeRemainingInMillis: Long = 0
    private var offerExpiryCounterHandler: Handler? = null
    private val mOfferExpiryRunnable = Runnable { updateExpiryTimer() }

    private var offerAlertPlayer: MediaPlayer? = null

    fun setBookingOfferPayload(newPayload: BookingOfferPayload) {
        if (!newPayload.isTestOffer) newPayload.offerId?.let { updateOfferStatus(it) }
        sessionManager.clearSavedBookingOfferData()

        timeRemainingInMillis = Duration.between(Instant.now(), newPayload.expiresIn).toMillis()
        Timber.i("Offer expires in ${timeRemainingInMillis / 1000} seconds")

        startOfferAudioPlayer()
        if (!newPayload.isTestOffer) startOfferExpiryCounter()
        _bookingDetailOutcome.value = Outcome.Empty
        _rejectBookingOfferOutcome.value = Outcome.Empty
        _acceptBookingOfferOutcome.value = Outcome.Empty
        _bookingOfferPayloadFlow.value = newPayload
    }

    private fun startOfferExpiryCounter() {
        stopOfferExpiryCounter()
        offerExpiryCounterHandler = Handler(Looper.getMainLooper())
        offerExpiryCounterHandler?.post(mOfferExpiryRunnable)
    }

    fun stopOfferExpiryCounter() {
        offerExpiryCounterHandler?.removeCallbacksAndMessages(null)
        offerExpiryCounterHandler = null
    }

    private fun updateExpiryTimer() {
        timeRemainingInMillis -= 1000
        // Update ui with correct count
        val durationSeconds = timeRemainingInMillis / 1000
        _timerCounter.value = "%02d:%02d".format(durationSeconds % 3600 / 60, durationSeconds % 60)
        // Stop if timer expires
        if (timeRemainingInMillis < 1000) {
            stopOfferAudioPlayer()
            stopOfferExpiryCounter()
            viewModelScope.launch { _timerExpired.emit(true) }
        } else offerExpiryCounterHandler?.postDelayed(mOfferExpiryRunnable, 1000)
    }

    private fun updateOfferStatus(offerId: String) {
        combine(
            flow {
                emit(
                    userComponentManager.bookingRepo
                        .updateBookingOffer(BookingOfferStatus.READ, offerId, null)
                )
            },
            flow { emit(userComponentManager.cab9Repo.fetchMobileState()) }
        ) { isSuccess, _ ->
            Timber.d("Offer read status updated -> $isSuccess")
        }
            .catch { Timber.e(BookingOfferReadException(it)) }
            .launchIn(viewModelScope)
    }

    private fun startOfferAudioPlayer() {
        stopOfferAudioPlayer()
        application.maximizeVolume()
        offerAlertPlayer = MediaPlayer.create(application, R.raw.tone_booking_offer)
            .apply { isLooping = true }
            .also { it.start() }
    }

    public fun stopOfferAudioPlayer() {
        offerAlertPlayer?.let { alertPlayer ->
            if (alertPlayer.isPlaying) alertPlayer.stop()
            alertPlayer.release()
        }
        offerAlertPlayer = null
    }

    val bookingTime = _bookingOfferPayloadFlow.mapLatest { payload ->
        //payload?.bookedDateTime?.let { DateTimeFormatter.ofPattern(HH_mm).format(it) }
        payload?.bookedDateTime?.let { formatDateTimeWithZone(sessionManager.timeZone, it, HH_mm) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val vehicleType = _bookingOfferPayloadFlow.mapLatest { it?.vehicleName.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val bookingCommission = _bookingOfferPayloadFlow.mapLatest {
        if (sessionManager.isOfferCommissionVisibilityEnabled
            && it?.driverCommission != null
            && it.driverCommission > BALANCE_ZERO
        ) prefixCurrency(application, it.driverCommission)
        else null
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val paymentMethod = _bookingOfferPayloadFlow.mapLatest { it?.paymentMethod }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val offerType = _bookingOfferPayloadFlow.mapLatest { it?.offerType }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val offerTypeVisibility = _bookingOfferPayloadFlow.mapLatest {
        if (!it?.offerType.isNullOrEmpty()) View.VISIBLE else View.GONE
    }.stateIn(viewModelScope, SharingStarted.Lazily, View.GONE)

    val pickupLabel = _bookingOfferPayloadFlow.mapLatest {
        if (it?.pickupTime != null && it.pickupTime > 0) {
            application.quantityText(
                R.plurals.pl_pickup_min_duration_label,
                R.string.zero_min_pickup_duration_label,
                it.pickupTime
            )
        } else application.getString(R.string.label_pickup)
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val pickupDistance = _bookingOfferPayloadFlow.mapLatest {
        if (it?.pickupDistance != null && it.pickupDistance > 0.0) {
            application.getString(R.string.temp_mile, it.pickupDistance)
        } else null
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val pickupAddress = _bookingOfferPayloadFlow.mapLatest { it?.pickupAddress }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val dropLabel = _bookingOfferPayloadFlow.mapLatest {
        if (it?.estimatedDuration != null && it.estimatedDuration > 0) {
            application.quantityText(
                R.plurals.pl_pickup_min_duration_label,
                R.string.zero_min_pickup_duration_label,
                it.estimatedDuration
            )
        } else application.getString(R.string.label_drop)
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val dropDistance = _bookingOfferPayloadFlow.mapLatest {
        if (it?.estimatedDistance != null && it.estimatedDistance > 0.0) {
            application.getString(R.string.temp_mile, it.estimatedDistance)
        } else null
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val dropAddress = _bookingOfferPayloadFlow.mapLatest { it?.dropAddress }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val showDropAddress = _bookingOfferPayloadFlow.mapLatest {
        // Show driver drop address based on client settings and address availability
        sessionManager.isOfferPickupVisibilityEnabled
                && !it?.pickupAddress.isNullOrEmpty()
                && sessionManager.isOfferDropVisibilityEnabled
                && !it?.dropAddress.isNullOrEmpty()
    }.stateIn(viewModelScope, SharingStarted.Lazily, true)

    val showPickupAddress = _bookingOfferPayloadFlow.mapLatest {
        // Show driver pickup address based on client settings and address availability
        sessionManager.isOfferPickupVisibilityEnabled
                && !it?.pickupAddress.isNullOrEmpty()
    }.stateIn(viewModelScope, SharingStarted.Lazily, true)

    val showStopCount = _bookingOfferPayloadFlow.mapLatest {
        // Show stop count if pickup and drop are showing
        sessionManager.isOfferPickupVisibilityEnabled
                && !it?.pickupAddress.isNullOrEmpty()
                && sessionManager.isOfferDropVisibilityEnabled
                && !it?.dropAddress.isNullOrEmpty()
                && it?.totalStops != null && it.totalStops > 2
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    val stopCount = _bookingOfferPayloadFlow.mapLatest {
        if (it?.totalStops != null && it.totalStops > 2) {
            val actualStopCount = it.totalStops - 2
            application.quantityText(
                R.plurals.pl_stop_count,
                R.string.zero_stop_count,
                actualStopCount
            )
        } else null
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _acceptBookingOfferOutcome =
        MutableStateFlow<Outcome<Pair<Boolean, BookingOfferPayload?>>>(Outcome.Empty)
    val acceptBookingOfferOutcome = _acceptBookingOfferOutcome.asStateFlow()

    fun acceptOffer() {
        stopOfferAudioPlayer()
        stopOfferExpiryCounter()
        viewModelScope.launch {
            try {
                _acceptBookingOfferOutcome.loadingOverlay()
                val result = userComponentManager.bookingRepo
                    .updateBookingOffer(
                        BookingOfferStatus.ACCEPT,
                        offerPayload?.offerId.orEmpty(),
                        null
                    )
                _acceptBookingOfferOutcome.success(result to offerPayload)
            } catch (ex: Exception) {
                _acceptBookingOfferOutcome.failureOverlay(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    private val _rejectBookingOfferOutcome = MutableStateFlow<Outcome<Boolean>>(Outcome.Empty)
    val rejectBookingOfferOutcome = _rejectBookingOfferOutcome.asStateFlow()

    fun rejectOffer(reasonId: String?) {
        stopOfferAudioPlayer()
        stopOfferExpiryCounter()
        viewModelScope.launch {
            try {
                _rejectBookingOfferOutcome.loadingOverlay()
                val result = userComponentManager.bookingRepo
                    .updateBookingOffer(
                        BookingOfferStatus.REJECT,
                        offerPayload?.offerId.orEmpty(),
                        reasonId
                    )
                _rejectBookingOfferOutcome.success(result)
            } catch (ex: Exception) {
                _rejectBookingOfferOutcome.failureOverlay(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    private val _bookingDetailOutcome = MutableStateFlow<Outcome<Booking>>(Outcome.Empty)
    val bookingDetailOutcome = _bookingDetailOutcome.asStateFlow()

    fun getBookingDetail() {
        viewModelScope.launch {
            try {
                _bookingDetailOutcome.loadingOverlay()
                val result = userComponentManager.bookingRepo.getBookingDetail(bookingId.orEmpty())
                _bookingDetailOutcome.success(result)
            } catch (ex: Exception) {
                _bookingDetailOutcome.failureOverlay(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    override fun onCleared() {
        stopOfferAudioPlayer()
        stopOfferExpiryCounter()
        super.onCleared()
    }
}