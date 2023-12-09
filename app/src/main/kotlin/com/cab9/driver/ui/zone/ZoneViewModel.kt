package com.cab9.driver.ui.zone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.HeatMapInterval
import com.cab9.driver.data.models.HeatMapLatLng
import com.cab9.driver.data.models.ZoneModelResult
import com.cab9.driver.di.user.UserComponentManager
import com.cab9.driver.ext.failureOverlay
import com.cab9.driver.ext.loadingOverlay
import com.cab9.driver.ext.success
import com.cab9.driver.network.ApiErrorHandler
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ZoneViewModel @Inject constructor(
    private val userComponentManager: UserComponentManager,
    private val apiErrorHandler: ApiErrorHandler
) : ViewModel() {

    var selectedHeatmapInterval = HeatMapInterval.MIN_15
        private set

    private var zonalPollingJob: Job? = null
    //private val zonalJobScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _zonalOverviewOutcome = MutableStateFlow<Outcome<ZoneModelResult?>>(Outcome.Empty)
    val zonalOverviewOutcome = _zonalOverviewOutcome.asStateFlow()

    fun startPollingZonalOverview(latLng: LatLng?) {
        zonalPollingJob = viewModelScope.launch {
            while (isActive) {
                try {
                    _zonalOverviewOutcome.loadingOverlay()
                    val result = userComponentManager.cab9Repo.getZonalOverview(latLng)
                    _zonalOverviewOutcome.success(result)
                    delay(1000 * 60)
                } catch (ex: Exception) {
                    if (ex is CancellationException) return@launch
                    _zonalOverviewOutcome.failureOverlay(apiErrorHandler.errorMessage(ex), ex)
                }
            }
        }
    }

    fun stopZonalPolling() {
        Timber.w("Stop polling zone data!".uppercase())
        zonalPollingJob?.cancel()
    }

    private var heatmapJob: Job? = null

    private val _heatMapOutcome = MutableStateFlow<Outcome<List<HeatMapLatLng>>>(Outcome.Empty)
    val heatMapOutcome = _heatMapOutcome.asStateFlow()

    fun fetchHeatMapData(interval: HeatMapInterval) {
        heatmapJob?.cancel()
        heatmapJob = viewModelScope.launch {
            try {
                _heatMapOutcome.loadingOverlay()
                selectedHeatmapInterval = interval
                val result = userComponentManager.cab9Repo.getHeatMapData(selectedHeatmapInterval)
                _heatMapOutcome.success(result)
            } catch (ex: Exception) {
                if (ex is CancellationException) return@launch
                _heatMapOutcome.failureOverlay(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    override fun onCleared() {
        //zonalJobScope.coroutineContext.cancelChildren()
        zonalPollingJob?.cancel()
        heatmapJob?.cancel()
        super.onCleared()
    }

}