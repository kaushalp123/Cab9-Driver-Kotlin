package com.cab9.driver.ui.flagdown

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.CreateFlagDownBookingRequest
import com.cab9.driver.di.user.UserComponentManager
import com.cab9.driver.ext.failureOverlay
import com.cab9.driver.ext.loadingOverlay
import com.cab9.driver.ext.success
import com.cab9.driver.network.ApiErrorHandler
import com.cab9.driver.network.MissingArgumentException
import com.cab9.driver.ui.flagdown.CreateFlagDownBookingFragment.Companion.BUNDLE_PICKUP_LAT
import com.cab9.driver.ui.flagdown.CreateFlagDownBookingFragment.Companion.BUNDLE_PICKUP_LNG
import com.cab9.driver.ui.search.SearchPlaceResult
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateFlagDownBookingViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val userComponentManager: UserComponentManager,
    private val apiErrorHandler: ApiErrorHandler
) : ViewModel() {

    val pickupLatLng: LatLng
        get() {
            val lat = savedStateHandle.get<Double>(BUNDLE_PICKUP_LAT)
            val lng = savedStateHandle.get<Double>(BUNDLE_PICKUP_LNG)
            return if (lat != null && lng != null) LatLng(lat, lng)
            else throw MissingArgumentException("Invalid LatLng provided for pickup!")
        }

    var dropPlace: SearchPlaceResult? = null

    private val _pickupAddress = MutableStateFlow<Outcome<String>>(Outcome.Empty)
    val pickupAddress = _pickupAddress.asStateFlow()

    private fun getFromLocation() {
        viewModelScope.launch {
            try {
                _pickupAddress.loadingOverlay()
                val result = userComponentManager.sharedLocationManager.getAddressFrom(pickupLatLng)
                _pickupAddress.value = Outcome.success(result)
            } catch (ex: Exception) {
                _pickupAddress.failureOverlay(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    private val _createBookingOutcome = MutableStateFlow<Outcome<String?>>(Outcome.Empty)
    val createBookingOutcome = _createBookingOutcome.asStateFlow()

    fun createBooking(request: CreateFlagDownBookingRequest) {
        viewModelScope.launch {
            try {
                _createBookingOutcome.loadingOverlay()
                val result = userComponentManager.bookingRepo.createFlagDownBooking(request)
                _createBookingOutcome.success(result.bookingId)
            } catch (ex: Exception) {
                _createBookingOutcome.failureOverlay(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    init {
        getFromLocation()
    }

}