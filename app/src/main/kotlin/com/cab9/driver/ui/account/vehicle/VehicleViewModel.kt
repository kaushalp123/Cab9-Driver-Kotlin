package com.cab9.driver.ui.account.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.Vehicle
import com.cab9.driver.di.user.UserComponentManager
import com.cab9.driver.ext.failure
import com.cab9.driver.ext.failureOverlay
import com.cab9.driver.ext.loading
import com.cab9.driver.ext.loadingOverlay
import com.cab9.driver.ext.success
import com.cab9.driver.network.ApiErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VehicleViewModel @Inject constructor(
    private val userComponentManager: UserComponentManager,
    private val apiErrorHandler: ApiErrorHandler
) : ViewModel() {

    private val _vehiclesOutcome = MutableStateFlow<Outcome<List<Vehicle>>>(Outcome.Empty)
    val vehiclesOutcome = _vehiclesOutcome.asStateFlow()

    fun getVehicles(forceUpdate: Boolean) {
        viewModelScope.launch {
            try {
                if (forceUpdate) _vehiclesOutcome.loading() else _vehiclesOutcome.loadingOverlay()
                val result = userComponentManager.accountRepo.getVehicles()
                _vehiclesOutcome.success(result)
            } catch (ex: Exception) {
                if (forceUpdate) _vehiclesOutcome.failure(apiErrorHandler.errorMessage(ex), ex)
                else _vehiclesOutcome.failureOverlay(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    private val _updateCurrentVehicleOutcome =
        MutableStateFlow<Outcome<Pair<String, Boolean>>>(Outcome.Empty)
    val updateCurrentVehicleOutcome = _updateCurrentVehicleOutcome.asStateFlow()

    fun updateCurrentVehicle(vehicleId: String) {
        viewModelScope.launch {
            try {
                _updateCurrentVehicleOutcome.loadingOverlay()
                val isSuccess = userComponentManager.accountRepo.updateCurrentVehicle(vehicleId)
                _updateCurrentVehicleOutcome.success(vehicleId to isSuccess)
            } catch (ex: Exception) {
                _updateCurrentVehicleOutcome.failureOverlay(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    init {
        getVehicles(true)
    }

}