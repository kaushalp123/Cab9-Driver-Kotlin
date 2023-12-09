package com.cab9.driver.ui.home

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cab9.driver.data.models.BidCategory
import com.cab9.driver.data.models.Driver
import com.cab9.driver.data.models.FlagDownConfig
import com.cab9.driver.data.models.MobileState
import com.cab9.driver.data.models.OperationalZone
import com.cab9.driver.data.remote.RemoteConfig
import com.cab9.driver.di.user.UserComponentManager
import com.cab9.driver.ext.toLatLng
import com.cab9.driver.network.AppConfigException
import com.cab9.driver.network.RemoteConfigException
import com.cab9.driver.utils.PolylineEncoding
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AppConfigViewModel @Inject constructor(
    private val userComponentManager: UserComponentManager,
    private val remoteConfig: RemoteConfig
) : ViewModel() {

    private val _flagDownConfigOutcome = MutableStateFlow(false)
    val flagDownConfigOutcome = _flagDownConfigOutcome.asStateFlow()

    private val mobileState: MobileState?
        get() = userComponentManager.cab9Repo.mobileState

    private var flagDownConfig: FlagDownConfig? = null
    private val flagDownZones: MutableMap<OperationalZone, List<LatLng>> = mutableMapOf()

    private val _bidCategoryOutcome = MutableStateFlow(BidCategory.values().toList())
    val bidCategoryOutcome = _bidCategoryOutcome.asStateFlow()

    private fun fetchAppConfig() {
        combine(
            flow { emit(userComponentManager.cab9Repo.getAppConfig()) },
            flow { emit(userComponentManager.cab9Repo.fetchRejectionReasons()) },
            flow { emit(userComponentManager.cab9Repo.getFlagDownConfig()) }
        ) { appConfig, _, flagDownConfig ->
            if (appConfig != null && !appConfig.bidCategories.isNullOrEmpty()) {
                _bidCategoryOutcome.value = appConfig.bidCategories.toList()
            } else Timber.w("App configuration not found!".uppercase())
            if (flagDownConfig != null) updateFlagDownConfig(flagDownConfig)
            else Timber.w("Flag down configuration not found!".uppercase())
        }.retryWhen { _, attempt ->
            delay(2000)
            return@retryWhen attempt < 3
        }
            .catch { Timber.e(AppConfigException(it)) }
            .launchIn(viewModelScope)
    }

    private fun updateFlagDownConfig(newConfig: FlagDownConfig) {
        Timber.d("Fetching flag down configuration...")
        // Decode polyline
        flagDownConfig = newConfig
        flagDownZones.clear()
        newConfig.zones
            ?.filterNot { it.overviewPolyline.isNullOrEmpty() }
            ?.map { flagDownZones[it] = PolylineEncoding.decode(it.overviewPolyline) }
    }

    fun onNewLocation(newLocation: Location) {
        _flagDownConfigOutcome.value =
            mobileState?.driverStatus == Driver.Status.ONLINE
                    && mobileState?.currentBooking == null
                    && flagDownConfig?.isEnabled == true
                    && containsVehicleType()
                    && containsLocation(newLocation.toLatLng())
    }

    /**
     * Returns true if vehicle list is empty or vehicle contains current selected vehicle id.
     */
    private fun containsVehicleType(): Boolean =
        if (flagDownConfig?.vehicleTypes.isNullOrEmpty()) true
        else flagDownConfig?.vehicleTypes.orEmpty()
            .any { it.flagDownTypeId == mobileState?.currentVehicle?.typeId }

    /**
     * Returns true if operation zones is empty or current location is inside operation zone polygon.
     */
    private fun containsLocation(latLng: LatLng): Boolean {
        return if (flagDownZones.isEmpty()) true
        else flagDownZones.any { PolyUtil.containsLocation(latLng, it.value, false) }
    }


    private fun fetchAndActivateRemoteConfig() {
        flow { emit(remoteConfig.fetchAndActivate()) }
            .retryWhen { _, attempt ->
                delay(2000)
                return@retryWhen attempt < 3
            }
            .map { Timber.w("Firebase remote config enabled: $it") }
            .catch { Timber.e(RemoteConfigException("Fetch and activate failed!", it)) }
            .launchIn(viewModelScope)
    }

    init {
        fetchAppConfig()
        fetchAndActivateRemoteConfig()
    }

}