package com.cab9.driver.di.booking

import android.app.Activity
import android.location.Location
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.cab9.driver.data.models.Booking
import com.cab9.driver.data.models.BookingDetailModel
import com.cab9.driver.data.models.BookingMeterModel
import com.cab9.driver.data.models.MobileState
import com.cab9.driver.data.models.PaymentMode
import com.cab9.driver.data.remote.RemoteConfig
import com.cab9.driver.di.user.UserComponentManager
import com.cab9.driver.ext.lat
import com.cab9.driver.ext.lng
import com.cab9.driver.ext.postWebMessageCompat
import com.cab9.driver.ext.prefixCurrency
import com.cab9.driver.services.SocketEvent
import com.cab9.driver.services.SocketManager
import com.cab9.driver.settings.SessionManager
import com.cab9.driver.utils.BALANCE_ZERO
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

private const val KEY_TYPE = "type"
private const val KEY_DATA = "data"

private const val TYPE_GEO_LOCATION = "geoLocation"

private const val KEY_DRIVER_ID = "driverId"
private const val KEY_LAT = "lat"
private const val KEY_LNG = "lon"
private const val KEY_BEARING = "bearing"
private const val KEY_SPEED = "speed"

data class MeterChange(
    val meterText: String,
    val distance: String,
    val waitingCost: String
)

sealed class JobInProgress {
    data class Assigned(val booking: Booking) : JobInProgress()
    object Unassigned : JobInProgress()
}

@ActivityScoped
class JobInProgressManager @Inject constructor(
    private val activityRef: Activity,
    private val remoteConfig: RemoteConfig,
    private val sessionManager: SessionManager,
    private val userComponentManager: UserComponentManager
) : DefaultLifecycleObserver {

    private val activityScope = (activityRef as AppCompatActivity).lifecycleScope

    private val socketManger: SocketManager
        get() = userComponentManager.socketManager

    private val _meterChangeFlow = MutableStateFlow<MeterChange?>(null)
    val meterChangeFlow: StateFlow<MeterChange?> = _meterChangeFlow

    private var latestJobInProgress = userComponentManager.cab9Repo.jobInProgressAsFlow.value

    private var bookingJob: Job? = null
    private var bookingMeter: BookingMeterModel? = null
    private var bookingDetail: BookingDetailModel? = null

    /**
     * Returns true if [MobileState] has current booking assigned, same
     * booking socket is connected and booking is in PROGRESS status.
     */
    val isBookingDetailValid: Boolean
        get() = latestJobInProgress is JobInProgress.Assigned
                && !bookingMeter?.bookingId.isNullOrEmpty()
                && bookingDetail?.status == Booking.Status.IN_PROGRESS
                && bookingDetail?.completingDateTime.isNullOrEmpty()

    private val bookingId: String?
        get() {
            return if (latestJobInProgress is JobInProgress.Assigned) {
                (latestJobInProgress as JobInProgress.Assigned).booking.id
            } else if (!bookingMeter?.bookingId.isNullOrEmpty()) bookingMeter?.bookingId
            else null
        }

    private val bookingTotalCost: Double
        get() {
            return bookingMeter?.let { meter ->
                bookingDetail?.let { booking ->
                    var totalCost = if (booking.isAsDirected) meter.meterCost
                    else if (booking.pricingMode.equals("AsQuoted", true)) booking.actualCost
                    else {
                        if (booking.status == Booking.Status.IN_PROGRESS
                            || booking.status == Booking.Status.ARRIVED
                            || booking.status == Booking.Status.CLEARING
                        ) meter.meterCost
                        else booking.actualCost
                    }

                    // Get waiting cost from meter or detail, whichever is available
                    if (meter.waitCost != null) totalCost += meter.waitCost
                    else if (booking.waitingCost != null) totalCost += booking.waitingCost

                    totalCost += booking.waitingTaxAmount
                    totalCost += booking.actualTaxAmount
                    totalCost += booking.extrasWithTaxes
                    totalCost += booking.adjustmentWithTaxes
                    return totalCost
                } ?: BALANCE_ZERO
            } ?: BALANCE_ZERO
        }

    init {
        (activityRef as AppCompatActivity).lifecycle.addObserver(this)
        activityScope.launch {
            launch {
                socketManger.socketCallback.collectLatest { event ->
                    when (event) {
                        is SocketEvent.MeterChanged -> onBookingMeterUpdate(event.meter)
                        is SocketEvent.BookingChanged -> onBookingDetailUpdate(event.booking)
                        else -> {}
                    }
                }
            }
            launch {
                userComponentManager.cab9Repo.jobInProgressAsFlow.collectLatest {
                    if (latestJobInProgress == it) return@collectLatest
                    latestJobInProgress = it
                    if (it is JobInProgress.Assigned) initialize(it.booking) else release()
                }
            }
        }
    }

    private fun initialize(booking: Booking) {
        Timber.d("Initializing JIP...")
        _meterChangeFlow.value = null
        bookingDetail = BookingDetailModel(booking)
        fetchBookingDetail()
    }

    private fun release() {
        Timber.d("Releasing JIP...")
        _meterChangeFlow.value = null
        bookingMeter = null
        bookingDetail = null
        bookingJob?.cancel()
        bookingJob = null
    }

    override fun onDestroy(owner: LifecycleOwner) {
        release()
        super.onDestroy(owner)
    }

    /**
     * Updates location on Cab9Go.
     */
    fun postLocationUpdate(webView: WebView, newLocation: Location) {
        //Timber.d("JIP web location updated...".uppercase())
        val locationJson = JSONObject(
            mapOf(
                KEY_DRIVER_ID to sessionManager.userId,
                KEY_LAT to newLocation.lat,
                KEY_LNG to newLocation.lng,
                KEY_SPEED to newLocation.speed,
                KEY_BEARING to newLocation.bearing
            )
        )
        // Set request json object
        val mainJson = JSONObject(
            mapOf(
                KEY_TYPE to TYPE_GEO_LOCATION,
                KEY_DATA to locationJson
            )
        )
        // Post message to webpage to handle
        webView.postWebMessageCompat(mainJson)
    }

    private fun onBookingDetailUpdate(newBookingDetail: BookingDetailModel) {
        Timber.d("JIP booking detail received for id: ${newBookingDetail.id}".uppercase())
        // Checking booking before setting value (For some reason cancelled booking events are received through socket)
        if (newBookingDetail.id.equals(bookingId, true)) {
            bookingDetail = newBookingDetail
            if (bookingMeter != null) onMeterChanged()
        } else Timber.w("JIP booking detail update received for wrong id".uppercase())
    }

    private fun onBookingMeterUpdate(newBookingMeter: BookingMeterModel) {
        Timber.d("JIP booking meter update received for id: ${newBookingMeter.bookingId}".uppercase())
        if (newBookingMeter.bookingId.equals(bookingId, true)) {
            bookingMeter = newBookingMeter
            // Since this event is called every frequently by socket, we will fetch booking
            // detail only when necessary
            if (bookingDetail == null) fetchBookingDetail()
            else onMeterChanged()
        } else Timber.w("JIP booking meter update received for wrong id".uppercase())
    }

    private fun onMeterChanged() {
        // Calculate total cost including taxes
        val totalCost = bookingTotalCost
        val meterText = when {
            bookingDetail?.paymentMode == PaymentMode.ONACCOUNT || totalCost < BALANCE_ZERO ->
                PaymentMode.ONACCOUNT.constant

            remoteConfig.isMeterEnabled -> prefixCurrency(activityRef, totalCost)
            else -> bookingDetail?.paymentMode?.constant.orEmpty()
        }

        // Determine wait cost based on actual is showing
        val waitCost = bookingMeter?.waitCost ?: (bookingDetail?.waitingCost ?: BALANCE_ZERO)
        val waitCostText = when {
            bookingDetail?.paymentMode == PaymentMode.ONACCOUNT || totalCost < BALANCE_ZERO -> ""
            remoteConfig.isMeterEnabled -> prefixCurrency(activityRef, waitCost)
            else -> ""
        }

        // Find actual distance covered
        val actualDistance = bookingMeter?.actualDistance?.toString()
            ?: (bookingDetail?.actualDistance?.toString() ?: "")

        _meterChangeFlow.value = MeterChange(meterText, actualDistance, waitCostText)
    }

    /**
     * Updates ongoing booking detail.
     */
    fun fetchBookingDetail() {
        if (!bookingId.isNullOrEmpty()) {
            bookingJob?.cancel()
            bookingJob = activityScope.launch {
                try {
                    Timber.d("Fetching JIP booking detail...".uppercase())
                    val booking = userComponentManager.bookingRepo
                        .getBookingDetail(bookingId.orEmpty())
                    onBookingDetailUpdate(BookingDetailModel(booking))
                } catch (ex: Exception) {
                    Timber.w(ex)
                }
            }
        } else Timber.d("JIP booking id is missing!".uppercase())
    }
}