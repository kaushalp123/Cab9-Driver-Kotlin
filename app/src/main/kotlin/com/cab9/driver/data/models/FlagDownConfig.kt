package com.cab9.driver.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FlagDownConfig(
    @field:Json(name = "FlagDown") val isEnabled: Boolean?,
    @field:Json(name = "VehicleTypes") val vehicleTypes: List<Vehicle.Type>?,
    @field:Json(name = "OperationalZones") val zones: List<OperationalZone>?,
)

@JsonClass(generateAdapter = true)
data class OperationalZone(
    @field:Json(name = "Id") val id: String?,
    @field:Json(name = "Name") val name: String?,
    @field:Json(name = "OverviewPolyline") val overviewPolyline: String?,
)
