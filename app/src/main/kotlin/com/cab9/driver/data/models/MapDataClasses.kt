package com.cab9.driver.data.models

import com.cab9.driver.R
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Zone(
    @field:Json(name = "Area") val areaName: String?,
    @field:Json(name = "Latitude") val lat: Double?,
    @field:Json(name = "Longitude") val lng: Double?,
    @field:Json(name = "BookingsNow") val bookingNow: Int?,
    @field:Json(name = "Bookings15") val booking15: Int?,
    @field:Json(name = "Bookings30") val booking30: Int?,
    //@field:Json(name = "Bookings45") val booking45: Int?,
    @field:Json(name = "Bookings60") val booking60: Int?,
    @field:Json(name = "Drivers") val driverCount: Int?,
    @field:Json(name = "Distance") val distance: Double?,
    @field:Json(name = "RankPosition") val rankPosition: Int?
)

@JsonClass(generateAdapter = true)
data class HeatMapLatLng(
    @field:Json(name = "Latitude") val lat: Double?,
    @field:Json(name = "Longitude") val lng: Double?
)

enum class HeatMapInterval(val minutes: Int) {
    MIN_15(15), MIN_30(30), MIN_60(60), MIN_120(120);
}

data class ZoneModel(
    val areaName: String,
    val driverCount: Int,
    val distance: Double?,
    val rank: Int?,
    val data: List<Int>
)

data class Column(val key: String) {

    val nameResId = when {
        key.contains("now", ignoreCase = true) -> R.string.label_now
        key.contains("15", ignoreCase = true) -> R.string.label_15m
        key.contains("30", ignoreCase = true) -> R.string.label_30m
        key.contains("45", ignoreCase = true) -> R.string.label_45m
        key.contains("60", ignoreCase = true) -> R.string.label_60m
        else -> R.string.label_na
    }

    val iconResId = when {
        key.contains("now", ignoreCase = true) -> R.drawable.baseline_circle_outline_24
        key.contains("15", ignoreCase = true) -> R.drawable.baseline_circle_slice_2_24
        key.contains("30", ignoreCase = true) -> R.drawable.baseline_circle_slice_4_24
        key.contains("45", ignoreCase = true) -> R.drawable.baseline_circle_slice_6_24
        key.contains("60", ignoreCase = true) -> R.drawable.baseline_circle_slice_8_24
        else -> 0
    }

}

data class ZoneModelResult(
    val columns: List<Column>,
    val zones: List<ZoneModel>
)