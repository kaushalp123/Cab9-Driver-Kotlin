package com.cab9.driver.data.repos

import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.AppConfig
import com.cab9.driver.data.models.Booking
import com.cab9.driver.data.models.Column
import com.cab9.driver.data.models.Expense
import com.cab9.driver.data.models.FlagDownConfig
import com.cab9.driver.data.models.HeatMapInterval
import com.cab9.driver.data.models.HeatMapLatLng
import com.cab9.driver.data.models.MobileState
import com.cab9.driver.data.models.ZoneModel
import com.cab9.driver.data.models.ZoneModelResult
import com.cab9.driver.data.remote.RemoteConfig
import com.cab9.driver.di.booking.JobInProgress
import com.cab9.driver.di.user.LoggedInScope
import com.cab9.driver.ext.failure
import com.cab9.driver.ext.lat
import com.cab9.driver.ext.lng
import com.cab9.driver.ext.loading
import com.cab9.driver.ext.success
import com.cab9.driver.network.ApiErrorHandler
import com.cab9.driver.network.AppConfigException
import com.cab9.driver.network.apis.Cab9API
import com.cab9.driver.network.apis.NodeAPI
import com.cab9.driver.settings.SessionManager
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import timber.log.Timber
import javax.inject.Inject

interface Cab9Repository {

    val mobileState: MobileState?

    val mobileStateAsFlow: StateFlow<Outcome<MobileState>>

    val jobInProgressAsFlow: StateFlow<JobInProgress>

    fun fetchMobileState()

    suspend fun getExpenseTypes(): List<Expense>

    suspend fun getZonalOverview(latLng: LatLng?): ZoneModelResult?

    suspend fun getHeatMapData(interval: HeatMapInterval): List<HeatMapLatLng>

    suspend fun fetchRejectionReasons(): Boolean

    suspend fun sendTestNotification(): Boolean

    suspend fun getFlagDownConfig(): FlagDownConfig?

    suspend fun getAppConfig(): AppConfig?

}

@LoggedInScope
class Cab9RepositoryImpl @Inject constructor(
    private val nodeApi: NodeAPI,
    private val cab9Api: Cab9API,
    private val sessionManager: SessionManager,
    private val externalScope: CoroutineScope,
    private val apiErrorHandler: ApiErrorHandler,
    private val remoteConfig: RemoteConfig
) : Cab9Repository {

    private var _mobileState: MobileState? = null
    override val mobileState: MobileState?
        get() = _mobileState

    private val _mobileStateFlow = MutableStateFlow<Outcome<MobileState>>(Outcome.Empty)
    override val mobileStateAsFlow: StateFlow<Outcome<MobileState>> = _mobileStateFlow

    private val _jobInProgressFlow = MutableStateFlow<JobInProgress>(JobInProgress.Unassigned)
    override val jobInProgressAsFlow = _jobInProgressFlow.asStateFlow()

    override fun fetchMobileState() {
        externalScope.launch {
            try {
                _mobileStateFlow.loading()
                val newState = nodeApi.getMobileState()
                updateMobileStateFlow(newState)
                updateJobInProgressFlow(newState)
            } catch (ex: Exception) {
                _mobileStateFlow.failure(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    private fun updateMobileStateFlow(newState: MobileState) {
        sessionManager.updateClientSettings(newState)
        _mobileState = newState
        _mobileStateFlow.success(newState)
    }

    private suspend fun updateJobInProgressFlow(newState: MobileState) {
        // Emit any current booking assigned to driver
        val jipBooking = newState.currentBooking
        if (jipBooking != null
            && !jipBooking.id.isNullOrEmpty()
            && jipBooking.status != Booking.Status.CANCELLED
        ) _jobInProgressFlow.emit(JobInProgress.Assigned(jipBooking))
        else _jobInProgressFlow.emit(JobInProgress.Unassigned)
    }

    override suspend fun getExpenseTypes() = cab9Api.getExpenseTypes()

    override suspend fun getZonalOverview(latLng: LatLng?): ZoneModelResult? {
        val response = nodeApi.getZonalOverviewRaw(latLng?.lat, latLng?.lng)
        if (response.isSuccessful) {
            val strJsonResponse = response.body()?.string()
            if (!strJsonResponse.isNullOrEmpty()) {
                val jsonArr = JSONArray(strJsonResponse)
                if (jsonArr.length() > 0) {
                    var jsonObj = jsonArr.getJSONObject(0)
                    val keys = jsonObj.keys()
                    val intervals = arrayListOf<String>()
                    keys.forEach { key ->
                        // Take out column names form JSON response, ignore 60 min
                        if ((key.contains("now", ignoreCase = true)
                                    && remoteConfig.isZoneNowTabEnabled)
                            || key.contains("15", ignoreCase = true)
                            || key.contains("30", ignoreCase = true)
                            || key.contains("45", ignoreCase = true)
                        ) intervals.add(key)
                    }
                    val columns = intervals.map { Column(it) }
                    val zones = arrayListOf<ZoneModel>()
                    (0 until jsonArr.length()).map { index ->
                        jsonObj = jsonArr.getJSONObject(index)
                        zones.add(ZoneModel(
                            areaName = jsonObj.optString("Area"),
                            driverCount = jsonObj.optInt("Drivers"),
                            distance = jsonObj.optDouble("Distance"),
                            rank = jsonObj.optInt("RankPosition"),
                            data = buildList { columns.forEach { add(jsonObj.optInt(it.key)) } }
                        ))
                    }
                    return if (columns.isEmpty() || zones.isEmpty()) return null
                    else ZoneModelResult(columns, zones)
                }
            }
        }
        return null
    }

    override suspend fun getHeatMapData(interval: HeatMapInterval) =
        nodeApi.getHeatMapData(interval.minutes)

    override suspend fun sendTestNotification(): Boolean {
        val response = nodeApi.sendTestNotification()
        return response.isSuccess == true
    }

    override suspend fun fetchRejectionReasons(): Boolean = try {
        val response = nodeApi.getRejectionReasons()
        val jsonResponse = response.body()?.string()
        sessionManager.updateRejectionReasons(jsonResponse)
        response.isSuccessful
    } catch (ex: Exception) {
        Timber.e(AppConfigException(ex))
        false
    }

    override suspend fun getFlagDownConfig(): FlagDownConfig? = try {
        nodeApi.getFlagDownConfig()
    } catch (ex: Exception) {
        Timber.e(AppConfigException(ex))
        null
    }

    override suspend fun getAppConfig(): AppConfig? = try {
        nodeApi.getAppConfig()
    } catch (ex: Exception) {
        Timber.e(AppConfigException(ex))
        null
    }

}