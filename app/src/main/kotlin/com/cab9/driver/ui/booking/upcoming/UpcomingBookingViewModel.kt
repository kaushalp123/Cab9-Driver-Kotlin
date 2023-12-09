package com.cab9.driver.ui.booking.upcoming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.Booking
import com.cab9.driver.di.user.UserComponentManager
import com.cab9.driver.ext.emitFailureOverlay
import com.cab9.driver.ext.emitLoadingOverlay
import com.cab9.driver.ext.emitSuccess
import com.cab9.driver.network.ApiErrorHandler
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpcomingBookingViewModel @Inject constructor(
    private val userComponentManager: UserComponentManager,
    private val apiErrorHandler: ApiErrorHandler
) : ViewModel() {

    private val _canStartRideOutcome =
        MutableSharedFlow<Outcome<Pair<String, Boolean>>>(replay = 0)
    val canStartRideOutcome = _canStartRideOutcome.asSharedFlow()

    fun canStartRide(bookingId: String, latLng: LatLng) {
        viewModelScope.launch {
            try {
                _canStartRideOutcome.emitLoadingOverlay()
                val canStart = userComponentManager.bookingRepo.canStartRide(bookingId, latLng)
                _canStartRideOutcome.emitSuccess(Pair(bookingId, canStart))
            } catch (ex: Exception) {
                _canStartRideOutcome.emitFailureOverlay(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    private val _startBookingOutcome =
        MutableSharedFlow<Outcome<Pair<String, Boolean>>>(replay = 0)
    val startBookingOutcome = _startBookingOutcome.asSharedFlow()

    fun startBooking(bookingId: String) {
        viewModelScope.launch {
            try {
                _startBookingOutcome.emitLoadingOverlay()
                val result = userComponentManager.bookingRepo.updateBookingStatus(
                    Booking.Status.ON_ROUTE,
                    bookingId
                )
                _startBookingOutcome.emitSuccess(Pair(bookingId, result))
            } catch (ex: Exception) {
                _startBookingOutcome.emitFailureOverlay(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }
}