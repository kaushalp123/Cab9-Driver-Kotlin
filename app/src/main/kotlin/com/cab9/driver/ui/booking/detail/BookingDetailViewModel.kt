package com.cab9.driver.ui.booking.detail

import android.graphics.Bitmap
import android.view.View
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cab9.driver.R
import com.cab9.driver.base.Cab9DriverApp
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.*
import com.cab9.driver.data.remote.RemoteConfig
import com.cab9.driver.di.user.UserComponentManager
import com.cab9.driver.ext.*
import com.cab9.driver.network.ApiErrorHandler
import com.cab9.driver.network.MissingArgumentException
import com.cab9.driver.settings.SessionManager
import com.cab9.driver.utils.BALANCE_ZERO
import com.cab9.driver.utils.BUNDLE_KEY_BOOKING_ID
import com.cab9.driver.utils.BUNDLE_KEY_TYPE
import com.cab9.driver.utils.saveAsJpg
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BookingDetailViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val context: Cab9DriverApp,
    private val savedStateHandle: SavedStateHandle,
    private val apiErrorHandler: ApiErrorHandler,
    private val userComponentManager: UserComponentManager,
    private val remoteConfig: RemoteConfig
) : ViewModel() {

    private val bookingId: String
        get() = savedStateHandle[BUNDLE_KEY_BOOKING_ID]
            ?: throw MissingArgumentException("Booking id is missing in arguments!")

    private val type: BookingType
        get() = savedStateHandle[BUNDLE_KEY_TYPE]
            ?: throw MissingArgumentException("Booking type is missing in arguments!")

    private val isSumUpAvailable: Boolean
        get() = userComponentManager.cab9Repo.mobileState?.isSumUpAvailable == true

    private val _isBookingAcknowledged = MutableStateFlow(false)

    private val _ackBookingOutcome = MutableSharedFlow<Outcome<GenericResponse>>(replay = 0)
    val ackBookingOutcome = _ackBookingOutcome.asSharedFlow()

    fun acknowledgeBooking() {
        viewModelScope.launch {
            try {
                _ackBookingOutcome.emitLoadingOverlay()
                val result = userComponentManager.bookingRepo.acknowledgeBooking(bookingId)
                _isBookingAcknowledged.value = result.isSuccess == true
                _ackBookingOutcome.emitSuccess(result)
            } catch (ex: Exception) {
                _ackBookingOutcome.emitFailureOverlay(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    private val _canStartRideOutcome = MutableStateFlow<Outcome<Boolean>>(Outcome.Empty)
    val canStartRideOutcome = _canStartRideOutcome.asStateFlow()

    fun canStartRide(latLng: LatLng) {
        viewModelScope.launch {
            try {
                _canStartRideOutcome.loadingOverlay()
                val result = userComponentManager.bookingRepo.canStartRide(bookingId, latLng)
                _canStartRideOutcome.success(result)
            } catch (ex: Exception) {
                _canStartRideOutcome.failureOverlay(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    private val _startBookingOutcome = MutableSharedFlow<Outcome<Boolean>>(replay = 0)
    val startBookingOutcome = _startBookingOutcome.asSharedFlow()

    fun startBooking() {
        viewModelScope.launch {
            try {
                _startBookingOutcome.emitLoadingOverlay()
                val result = userComponentManager.bookingRepo
                    .updateBookingStatus(Booking.Status.ON_ROUTE, bookingId)
                _startBookingOutcome.emitSuccess(result)
            } catch (ex: Exception) {
                _startBookingOutcome.emitFailureOverlay(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    private val _bookingExpensesOutcome =
        MutableStateFlow<Outcome<List<ExpenseModel>>>(Outcome.Empty)
    val bookingExpensesOutcome = _bookingExpensesOutcome.asStateFlow()

    fun getBookingExpenses() {
        viewModelScope.launch {
            try {
                _bookingExpensesOutcome.loadingOverlay()
                val result = userComponentManager.bookingRepo.getBookingExpenses(bookingId)
                _bookingExpensesOutcome.success(result)
            } catch (ex: Exception) {
                _bookingExpensesOutcome.failureOverlay(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    val expenseContainerVisibility = bookingExpensesOutcome.mapLatest {
        if (it is Outcome.Success) {
            if (type == BookingType.HISTORY) {
                if (it.data.isNotEmpty()) View.VISIBLE else View.GONE
            } else View.GONE
        } else View.GONE
    }.stateIn(viewModelScope, SharingStarted.Lazily, View.GONE)

    private val _upcomingBooking = MutableStateFlow(false)
    val upcomingBooking = _upcomingBooking.asStateFlow()

    private val _historyBooking = MutableStateFlow(false)
    val historyBooking = _historyBooking.asStateFlow()

    private val _bookingDetailOutcome = MutableStateFlow<Outcome<Booking>>(Outcome.Empty)
    val bookingDetailOutcome = _bookingDetailOutcome.asStateFlow()

    fun getBookingDetail(asOverlay: Boolean) {
        viewModelScope.launch {
            try {
                if (asOverlay) _bookingDetailOutcome.loadingOverlay() else _bookingDetailOutcome.loading()
                val result = userComponentManager.bookingRepo.getBookingDetail(bookingId)
                _isBookingAcknowledged.value = result.isAcknowledged == true
                _bookingDetailOutcome.success(result)
            } catch (ex: Exception) {
                if (asOverlay)
                    _bookingDetailOutcome.failureOverlay(apiErrorHandler.errorMessage(ex), ex)
                else _bookingDetailOutcome.failure(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    val routeImgUrl = bookingDetailOutcome.mapLatest {
        if (it is Outcome.Success) it.data.imageUrl else null
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    val acknowledgeVisibility = _isBookingAcknowledged.mapLatest { isAcknowledged ->
        if (type == BookingType.UPCOMING && !isAcknowledged) View.VISIBLE else View.GONE
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = View.GONE)

    val bookingDate = bookingDetailOutcome.mapLatest {
        if (it is Outcome.Success)
            formatDateTimeWithZone(sessionManager.timeZone, it.data.bookedDateTime, dd_MMM_yyyy)
        else null
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    val bookingTime = bookingDetailOutcome.mapLatest {
        if (it is Outcome.Success)
            formatDateTimeWithZone(sessionManager.timeZone, it.data.bookedDateTime, HH_mm)
        else null
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    val modeOfPayment = bookingDetailOutcome.mapLatest {
        if (it is Outcome.Success) {
            if (it.data.paymentMode != null)
                context.getString(it.data.paymentMode.labelResId)
            else null
        } else null
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    val strPaymentMode = bookingDetailOutcome.mapLatest {
        if (it is Outcome.Success) it.data.paymentMode?.constant else null
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    val paymentMode = bookingDetailOutcome.mapLatest {
        if (it is Outcome.Success) it.data.paymentMode else null
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    val driverCommission = bookingDetailOutcome.mapLatest {
        if (it is Outcome.Success)
            prefixCurrency(context, it.data.driverCommission ?: BALANCE_ZERO)
        else context.getString(R.string.not_available)
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    val passengerName = bookingDetailOutcome.mapLatest {
        if (it is Outcome.Success) it.data.passenger?.displayName.orEmpty() else null
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    val clientName = bookingDetailOutcome.mapLatest {
        if (it is Outcome.Success) it.data.clientName.orEmpty() else null
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    val clientNameVisibility = bookingDetailOutcome.mapLatest {
        if (it is Outcome.Success)
            if (type == BookingType.UPCOMING
                && !it.data.clientName.isNullOrEmpty()
                && !remoteConfig.isClientNameVisibilityDisabled
            ) View.VISIBLE
            else View.GONE
        else View.GONE
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = View.GONE)

    val bagCount = bookingDetailOutcome.mapLatest {
        if (it is Outcome.Success) it.data.bagCount?.toString() else null
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    val paxCount = bookingDetailOutcome.mapLatest {
        if (it is Outcome.Success) it.data.paxCount?.toString() else null
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    val vehicleType = bookingDetailOutcome.mapLatest {
        if (it is Outcome.Success) it.data.vehicleType else null
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    val pickupAddress = bookingDetailOutcome.mapLatest {
        if (it is Outcome.Success) {
            if (!it.data.stops.isNullOrEmpty()) it.data.stops.first().summary else null
        } else null
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    val flightNumber = bookingDetailOutcome.mapLatest {
        if (it is Outcome.Success) it.data.flightInfo?.number else null
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    val flightNumberVisibility = bookingDetailOutcome.mapLatest {
        if (it is Outcome.Success) {
            val flightNumber = it.data.flightInfo?.number
            if (type == BookingType.UPCOMING && !flightNumber.isNullOrEmpty()) View.VISIBLE
            else View.GONE
        } else View.GONE
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = View.GONE)

    val flightOrigin = bookingDetailOutcome.mapLatest {
        if (it is Outcome.Success) it.data.flightInfo?.origin else null
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    val flightOriginVisibility = bookingDetailOutcome.mapLatest {
        if (it is Outcome.Success) {
            val origin = it.data.flightInfo?.origin
            if (type == BookingType.UPCOMING && !origin.isNullOrEmpty()) View.VISIBLE
            else View.GONE
        } else View.GONE
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = View.GONE)

    val stopVisibility = bookingDetailOutcome.mapLatest {
        if (it is Outcome.Success) {
            if (it.data.hasViaStops) View.VISIBLE else View.GONE
        } else View.GONE
    }.stateIn(viewModelScope, SharingStarted.Lazily, View.GONE)

    val tagsVisibility = bookingDetailOutcome.mapLatest {
        if (it is Outcome.Success) {
            if (it.data.hasTags) View.VISIBLE else View.GONE
        } else View.GONE
    }.stateIn(viewModelScope, SharingStarted.Lazily, View.GONE)

    val dropAddress = bookingDetailOutcome.mapLatest {
        if (it is Outcome.Success) {
            when {
                it.data.asDirected == true -> context.getString(R.string.label_as_directed)
                it.data.stops.isNullOrEmpty() || it.data.stops.size < 2 -> context.getString(R.string.not_available)
                else -> it.data.stops.last().summary.orEmpty()
            }
        } else context.getString(R.string.not_available)
    }.stateIn(
        viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = context.getString(R.string.not_available)
    )

    val bookingNotes = bookingDetailOutcome.mapLatest {
        if (it is Outcome.Success) it.data.driverNote else null
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    val noteVisibility = bookingDetailOutcome.mapLatest {
        if (it is Outcome.Success) {
            if (type == BookingType.UPCOMING && !it.data.driverNote.isNullOrEmpty()) View.VISIBLE
            else View.GONE
        } else View.GONE
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = View.GONE)

    val distanceInMiles = bookingDetailOutcome.mapLatest {
        if (it is Outcome.Success) (it.data.estimatedDistance ?: 0.0).toString().plus("mi")
        else null
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    val bookingCharges = bookingDetailOutcome.mapLatest {
        if (it is Outcome.Success) {
            if (type == BookingType.HISTORY && it.data.totalCostIncludingTaxes > BALANCE_ZERO) {
                val charges = arrayListOf<Charge>()
                // Add actual cost
                charges.add(
                    Charge(
                        context.getString(R.string.label_actual_cost),
                        it.data.actualCost ?: BALANCE_ZERO
                    )
                )
                // Add all adjustments
                it.data.adjustments
                    ?.filter { adjust -> adjust.amt > BALANCE_ZERO }
                    ?.map { adjust -> charges.add(Charge(adjust.name.orEmpty(), adjust.amt)) }
                // Add all extras
                it.data.extras
                    ?.filter { extra -> extra.amt > BALANCE_ZERO }
                    ?.map { extra -> charges.add(Charge(extra.name.orEmpty(), extra.amt)) }
                // Add waiting time
                if (it.data.totalWaitingCost > BALANCE_ZERO) {
                    val waitingLabel = if (it.data.waitingTimeInSeconds != null) {
                        val timer = formatWaitingTime(it.data.waitingTimeInSeconds * 1000L)
                        context.getString(R.string.temp_total_waiting_time_cost_label, timer)
                    } else context.getString(R.string.label_total_waiting_cost)
                    charges.add(Charge(waitingLabel, it.data.totalWaitingCost))
                }
                // Add taxes
                if (it.data.totalTaxes > BALANCE_ZERO) {
                    charges.add(
                        Charge(
                            context.getString(R.string.label_taxes_amp_charges),
                            it.data.totalTaxes
                        )
                    )
                }
                charges
            } else emptyList()
        } else emptyList()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalCostIncludingTaxes = bookingDetailOutcome.mapLatest {
        if (it is Outcome.Success)
            prefixCurrency(context, it.data.totalCostIncludingTaxes)
        else null
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    val costBreakdownVisibility = bookingDetailOutcome.mapLatest {
        if (it is Outcome.Success) {
            // Cost breakdown is visible only when booking is in past and total cost is greater than 0.0
            if (type == BookingType.HISTORY
                && it.data.totalCostIncludingTaxes > BALANCE_ZERO
            ) View.VISIBLE
            else View.GONE
        } else View.GONE
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = View.GONE)

    val customerSigContainerVisibility = bookingDetailOutcome.mapLatest {
        if (it is Outcome.Success) {
            if (type == BookingType.HISTORY
                && it.data.isSignatureRequired == true
                && it.data.passengerSignatureImageUrl.isNullOrEmpty()
            ) {
                val minutesPassed = Duration.between(it.data.jobClearDateTime, Instant.now())
                    .toMinutes()
                if (minutesPassed < 30) View.VISIBLE else View.GONE
            } else View.GONE
        } else View.GONE
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = View.GONE)

    val addCustomerSigVisibility = bookingDetailOutcome.mapLatest {
        if (it is Outcome.Success) {
            if (type == BookingType.HISTORY
                && it.data.isSignatureRequired == true
                && it.data.passengerSignatureImageUrl.isNullOrEmpty()
            ) View.VISIBLE
            else View.GONE
        } else View.GONE
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = View.GONE)

    val customerSigVisibility = bookingDetailOutcome.mapLatest {
        if (it is Outcome.Success) {
            if (type == BookingType.HISTORY
                && !it.data.passengerSignatureImageUrl.isNullOrEmpty()
            ) View.VISIBLE
            else View.GONE
        } else View.GONE
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = View.GONE)

    val customerSigUrl = bookingDetailOutcome.mapLatest {
        if (it is Outcome.Success) it.data.passengerSignatureImageUrl else null
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    val addExpenseBtnVisibility = bookingDetailOutcome.mapLatest {
        if (it is Outcome.Success) {
            if (type == BookingType.HISTORY
                && it.data.isFlagDownBooking == false
                && it.data.jobClearDateTime != null
            ) {
                val minutesPassed = Duration.between(it.data.jobClearDateTime, Instant.now())
                    .toMinutes()
                if (minutesPassed < 30) View.VISIBLE else View.GONE
            } else View.GONE
        } else View.GONE
    }.stateIn(viewModelScope, SharingStarted.Lazily, View.GONE)

    val requestPaymentBtnVisibility = bookingDetailOutcome.mapLatest {
        if (it is Outcome.Success) {
            if (type == BookingType.HISTORY
                && isSumUpAvailable
                && !it.data.isPaymentCompleted
                && it.data.isEligibleForSumUpPayment
                && it.data.jobClearDateTime != null
            ) {
                val minutesPassed = Duration.between(it.data.jobClearDateTime, Instant.now())
                    .toMinutes()
                if (minutesPassed < 30) View.VISIBLE else View.GONE
            } else View.GONE
        } else View.GONE
    }.stateIn(viewModelScope, SharingStarted.Lazily, View.GONE)

    init {
        _upcomingBooking.value = type == BookingType.UPCOMING
        _historyBooking.value = type == BookingType.HISTORY
        getBookingDetail(false)
        getBookingExpenses()
    }

    private fun formatWaitingTime(millis: Long): String {
        val seconds = millis / 1000 % 60
        val mins = millis / (60 * 1000) % 60
        //val hours = millis / (60 * 60 * 1000) % 24

        return buildString {
            if (mins > 0) {
                append(
                    context.quantityText(
                        R.plurals.pl_zero_mins,
                        R.string.zero_mins,
                        mins.toInt()
                    )
                )
            } else if (seconds > 0)
                append(
                    context.quantityText(
                        R.plurals.pl_zero_secs,
                        R.string.zero_secs,
                        seconds.toInt()
                    )
                )
        }
    }

    private val _uploadSignatureOutcome = MutableStateFlow<Outcome<Boolean>>(Outcome.Empty)
    val uploadSignatureOutcome = _uploadSignatureOutcome.asStateFlow()

    fun uploadSignature(signatureBitmap: Bitmap) {
        try {
            // Save bitmap on file
            viewModelScope.launch {
                _uploadSignatureOutcome.loadingOverlay()
                val result = saveAsJpg(context, signatureBitmap)?.let {
                    userComponentManager.bookingRepo.uploadSignature(bookingId, it)
                }
                val isSuccess = result == true
                if (isSuccess) getBookingDetail(true)
                _uploadSignatureOutcome.success(isSuccess)
            }
        } catch (ex: Exception) {
            _uploadSignatureOutcome.failureOverlay(apiErrorHandler.errorMessage(ex), ex)
        }
    }
}