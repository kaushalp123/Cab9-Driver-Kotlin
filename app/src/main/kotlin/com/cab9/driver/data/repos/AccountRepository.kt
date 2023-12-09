package com.cab9.driver.data.repos


import com.cab9.driver.BuildConfig
import com.cab9.driver.data.models.Driver
import com.cab9.driver.data.models.Shift
import com.cab9.driver.data.models.Vehicle
import com.cab9.driver.di.user.LoggedInScope
import com.cab9.driver.network.apis.NodeAPI
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import javax.inject.Inject

interface AccountRepository {
    suspend fun changeStatus(status: Driver.Status): Shift.Response

    suspend fun getVehicles(): List<Vehicle>

    suspend fun updateCurrentVehicle(vehicleId: String): Boolean

    suspend fun updateShiftEndTime(dateTime: LocalDateTime): Shift

    suspend fun updateDispatchTime(newDispatchTimeInMinutes: Int): Shift

    suspend fun updateDispatchDistance(newDispatchDistanceInMiles: Float): Shift
}

@LoggedInScope
class AccountRepositoryImpl @Inject constructor(private val nodeApi: NodeAPI) : AccountRepository {

    override suspend fun changeStatus(status: Driver.Status) = when (status) {
        Driver.Status.ONLINE -> {
            val response = nodeApi.goOnline()
            Shift.Response.online(response)
        }
        Driver.Status.OFFLINE -> {
            val url = BuildConfig.BASE_NODE_URL.plus("v1.1/driver-api/shift/offline")
            val response = nodeApi.updateDriverStatus(url)
            Shift.Response.other(status, response)
        }
        Driver.Status.ON_BREAK -> {
            val url = BuildConfig.BASE_NODE_URL.plus("v1.1/driver-api/shift/onbreak")
            val response = nodeApi.updateDriverStatus(url)
            Shift.Response.other(status, response)
        }
        else -> error("Implementation not found for $status")
    }

    override suspend fun getVehicles() = nodeApi.getVehicles()

    override suspend fun updateCurrentVehicle(vehicleId: String): Boolean {
        val result = nodeApi.updateCurrentVehicle(vehicleId)
        return result.isSuccess == true
    }

    override suspend fun updateShiftEndTime(dateTime: LocalDateTime): Shift {
        val strDateTime = dateTime.format(DateTimeFormatter.ISO_DATE_TIME)
        return nodeApi.goOnline(endTime = strDateTime)
    }

    override suspend fun updateDispatchTime(newDispatchTimeInMinutes: Int): Shift {
        return nodeApi.goOnline(maxDispatchTime = newDispatchTimeInMinutes)
    }

    override suspend fun updateDispatchDistance(newDispatchDistanceInMiles: Float): Shift {
        return nodeApi.goOnline(maxDispatchDistance = newDispatchDistanceInMiles)
    }
}