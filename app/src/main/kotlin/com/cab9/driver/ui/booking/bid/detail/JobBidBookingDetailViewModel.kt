package com.cab9.driver.ui.booking.bid.detail

import android.app.Application
import android.view.View
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cab9.driver.R
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.BidCategory
import com.cab9.driver.data.models.Booking
import com.cab9.driver.data.models.JobPoolBidModel
import com.cab9.driver.data.models.PaymentMode
import com.cab9.driver.di.user.UserComponentManager
import com.cab9.driver.ext.*
import com.cab9.driver.network.ApiErrorHandler
import com.cab9.driver.network.JobBidExpiredException
import com.cab9.driver.network.MissingArgumentException
import com.cab9.driver.settings.SessionManager
import com.cab9.driver.utils.BALANCE_ZERO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class JobBidBookingDetailViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val userComponentManager: UserComponentManager,
    private val apiErrorHandler: ApiErrorHandler
) : ViewModel() {

    private val jobBidBooking: JobPoolBidModel
        get() = savedStateHandle.get<JobPoolBidModel>("jobPoolBid")
            ?: throw MissingArgumentException("JobPoolBidModel missing in arguments")

    private val bidCategory: BidCategory
        get() = savedStateHandle.get<BidCategory>("bidCategory")
            ?: throw MissingArgumentException("BidCategory missing in arguments")

    private val bookingId: String
        get() = jobBidBooking.bookingModel.id

    private val _acceptBidOutcome = MutableStateFlow<Outcome<Boolean>>(Outcome.Empty)
    val acceptBidOutcome = _acceptBidOutcome.asStateFlow()

    fun acceptBid(amount: Double?) {
        viewModelScope.launch {
            try {
                _acceptBidOutcome.loadingOverlay()
                if (bidCategory == BidCategory.ARCHIVED) {
                    // CHeck if booking is still available to bid
                    if (userComponentManager.jobPoolBidRepo.openToBid(bookingId)) doAcceptBid(amount)
                    else {
                        _acceptBidOutcome.failureOverlay(
                            application.getString(R.string.err_job_expired),
                            JobBidExpiredException()
                        )
                    }
                } else doAcceptBid(amount)
            } catch (ex: Exception) {
                _acceptBidOutcome.failureOverlay(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    private suspend fun doAcceptBid(amount: Double?) {
        val isSuccess = userComponentManager.jobPoolBidRepo.acceptBid(bookingId, amount)
        _acceptBidOutcome.success(isSuccess)
        getBookingDetail()
    }

    private val _jobBidBookingDetailOutcome = MutableStateFlow<Outcome<Booking>>(Outcome.Empty)
    val jobBidBookingOutcomeOutcome = _jobBidBookingDetailOutcome.asStateFlow()

    private fun getBookingDetail() {
        viewModelScope.launch {
            try {
                _jobBidBookingDetailOutcome.loading()
                val bookingDetail = userComponentManager.bookingRepo.getBookingDetail(bookingId)
                _jobBidBookingDetailOutcome.success(bookingDetail)
            } catch (ex: Exception) {
                _jobBidBookingDetailOutcome.failure(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    private val _bookingCommissionOutcome =
        MutableStateFlow<Outcome<Pair<Double, Boolean>>>(Outcome.Empty)
    val bookingCommissionOutcome = _bookingCommissionOutcome.asStateFlow()

    private fun getBiddingPrice() {
        if (jobBidBooking.hasAmount) {
            _bookingCommissionOutcome.success(Pair(jobBidBooking.bookingModel.amount, false))
        } else {
            viewModelScope.launch {
                try {
                    val result = userComponentManager.jobPoolBidRepo
                        .getBookingCommission(jobBidBooking.bookingModel.id)
                    _bookingCommissionOutcome.success(
                        Pair(
                            (result.commission ?: BALANCE_ZERO),
                            true
                        )
                    )
                } catch (ex: Exception) {
                    _bookingCommissionOutcome.failureOverlay(apiErrorHandler.errorMessage(ex), ex)
                }
            }
        }
    }

    val routeImgUrl = jobBidBookingOutcomeOutcome.mapLatest {
        if (it is Outcome.Success) it.data.imageUrl else null
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    val bookingDate = jobBidBookingOutcomeOutcome.mapLatest {
        if (it is Outcome.Success)
            formatDateTimeWithZone(sessionManager.timeZone, it.data.bookedDateTime, dd_MMM_yyyy)
        else null
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    val bookingTime = jobBidBookingOutcomeOutcome.mapLatest {
        if (it is Outcome.Success)
            formatDateTimeWithZone(sessionManager.timeZone, it.data.bookedDateTime, HH_mm)
        else null
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    val modeOfPayment = jobBidBookingOutcomeOutcome.mapLatest {
        if (it is Outcome.Success) {
            if (it.data.paymentMode != null)
                application.getString(it.data.paymentMode.labelResId)
            else null
        } else null
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    val strPaymentMode = jobBidBookingOutcomeOutcome.mapLatest {
        if (it is Outcome.Success) it.data.paymentMode?.constant else null
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    val paymentMode = jobBidBookingOutcomeOutcome.mapLatest {
        if (it is Outcome.Success) it.data.paymentMode else null
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    val commissionVisibility = jobBidBookingOutcomeOutcome.mapLatest {
        if (it is Outcome.Success) {
            if (it.data.paymentMode == PaymentMode.ONACCOUNT) View.GONE else View.VISIBLE
        } else View.GONE
    }.stateIn(viewModelScope, SharingStarted.Lazily, View.GONE)

    val bagCount = jobBidBookingOutcomeOutcome.mapLatest {
        if (it is Outcome.Success) it.data.bagCount?.toString() else null
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    val paxCount = jobBidBookingOutcomeOutcome.mapLatest {
        if (it is Outcome.Success) it.data.paxCount?.toString() else null
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    val vehicleType = jobBidBookingOutcomeOutcome.mapLatest {
        if (it is Outcome.Success) it.data.vehicleType else null
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    val pickupAddress = jobBidBookingOutcomeOutcome.mapLatest {
        if (it is Outcome.Success) {
            if (!it.data.stops.isNullOrEmpty()) it.data.stops.first().summary else null
        } else null
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    val dropAddress = jobBidBookingOutcomeOutcome.mapLatest {
        if (it is Outcome.Success) {
            when {
                it.data.asDirected == true -> application.getString(R.string.label_as_directed)
                it.data.stops.isNullOrEmpty() || it.data.stops.size < 2 -> application.getString(R.string.not_available)
                else -> it.data.stops.last().summary.orEmpty()
            }
        } else application.getString(R.string.not_available)
    }.stateIn(viewModelScope, SharingStarted.Lazily, application.getString(R.string.not_available))

    val bookingNotes = jobBidBookingOutcomeOutcome.mapLatest {
        if (it is Outcome.Success) it.data.driverNote else null
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    val noteVisibility = jobBidBookingOutcomeOutcome.mapLatest {
        if (it is Outcome.Success) {
            if (!it.data.driverNote.isNullOrEmpty()) View.VISIBLE else View.GONE
        } else View.GONE
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = View.GONE)

    val distanceInMiles = jobBidBookingOutcomeOutcome.mapLatest {
        if (it is Outcome.Success) (it.data.estimatedDistance ?: 0.0).toString().plus("mi")
        else null
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    val jobBidAmount = bookingCommissionOutcome.mapLatest {
        if (it is Outcome.Success) {
            prefixCurrency(application, it.data.first)
        } else null
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    val acceptBookingBtnVisibility = jobBidBookingOutcomeOutcome.mapLatest {
        if (it is Outcome.Success)
            if (bidCategory != BidCategory.SELECTED) View.VISIBLE else View.GONE
        else View.GONE
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = View.GONE)

    val tagsVisibility = jobBidBookingOutcomeOutcome.mapLatest {
        if (it is Outcome.Success) {
            if (it.data.hasTags) View.VISIBLE else View.GONE
        } else View.GONE
    }.stateIn(viewModelScope, SharingStarted.Lazily, View.GONE)

    val acceptBookingBtnText = jobBidBookingOutcomeOutcome.mapLatest {
        if (it is Outcome.Success)
            if (bidCategory != BidCategory.SELECTED) {
                if (jobBidBooking.isAuctionBooking) {
                    val amount = jobBidBooking.bookingModel.amount
                    if (amount > BALANCE_ZERO) {
                        val amountInStr =
                            prefixCurrency(application, jobBidBooking.bookingModel.amount)
                        application.getString(R.string.temp_action_bid_for_booking, amountInStr)
                    } else application.getString(R.string.action_bid_for_booking)
                } else application.getString(R.string.action_accept_booking)
            } else null
        else null
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    val flightNumber = jobBidBookingOutcomeOutcome.mapLatest {
        if (it is Outcome.Success) it.data.flightInfo?.number else null
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    val flightNumberVisibility = jobBidBookingOutcomeOutcome.mapLatest {
        if (it is Outcome.Success) {
            val flightNumber = it.data.flightInfo?.number
            if (!flightNumber.isNullOrEmpty()) View.VISIBLE else View.GONE
        } else View.GONE
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = View.GONE)

    val flightOrigin = jobBidBookingOutcomeOutcome.mapLatest {
        if (it is Outcome.Success) it.data.flightInfo?.origin else null
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = null)

    val flightOriginVisibility = jobBidBookingOutcomeOutcome.mapLatest {
        if (it is Outcome.Success) {
            val origin = it.data.flightInfo?.origin
            if (!origin.isNullOrEmpty()) View.VISIBLE else View.GONE
        } else View.GONE
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = View.GONE)

    val stopVisibility = jobBidBookingOutcomeOutcome.mapLatest {
        if (it is Outcome.Success) {
            if (it.data.hasViaStops) View.VISIBLE else View.GONE
        } else View.GONE
    }.stateIn(viewModelScope, SharingStarted.Lazily, View.GONE)

    fun refreshBidDetail() {
        getBookingDetail()
        getBiddingPrice()
    }

    init {
        refreshBidDetail()
    }

}