package com.cab9.driver.data.models

import android.os.Build
import androidx.annotation.StringRes
import com.cab9.driver.R
import com.cab9.driver.network.UTCDateTime
import com.cab9.driver.network.UTCOffsetDateTime
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import kotlin.math.abs

@JsonClass(generateAdapter = true)
data class AddDeviceRequest(
    @field:Json(name = "DeviceToken") val fcmToken: String,
    @field:Json(name = "DeviceType") val type: String = "Android",
    @field:Json(name = "DeviceModel") val model: String = Build.MODEL,
    @field:Json(name = "DeviceName") val name: String = " ${Build.BRAND}, Android v${Build.VERSION.RELEASE}"
) {

    @JsonClass(generateAdapter = true)
    data class Response(
        @field:Json(name = "_id") val id: String?,
        @field:Json(name = "Token") val token: String?,
        @field:Json(name = "Name") val name: String?,
        @field:Json(name = "Model") val model: String?,
        @field:Json(name = "TimeStamp") val timestamp: String?
    )

}

@JsonClass(generateAdapter = true)
data class MobileState(
    @field:Json(name = "Id") val id: String,
    @field:Json(name = "TenantId") val tenantId: String,
    @field:Json(name = "Firstname") val firstName: String,
    @field:Json(name = "Surname") val lastName: String,
    @field:Json(name = "Callsign") val callSign: String?,
    @field:Json(name = "Mobile") val mobile: String?,
    @field:Json(name = "CurrentBookingId") val currentBookingId: String?,
    @field:Json(name = "CurrentBooking") val currentBooking: Booking?,
    @field:Json(name = "CurrentShiftId") val currentShiftId: String?,
    @field:Json(name = "CurrentShift") val currentShift: Shift?,
    @field:Json(name = "CurrentVehicleId") val currentVehicleId: String?,
    @field:Json(name = "CurrentVehicle") val currentVehicle: Vehicle?,
    @field:Json(name = "DriverStatus") val driverStatus: Driver.Status,
    //@field:Json(name = "TenantFlagDownConfig") val isFlagDown: Boolean?,
    @field:Json(name = "CanDriverCOA") val canDriverCancelOnArrival: Boolean?,
    @field:Json(name = "CanDriverHandback") val canDriverHandBack: Boolean?,
    @field:Json(name = "CanDriverStartWaiting") val canDriverStartWaiting: Boolean?,
    @UTCDateTime @field:Json(name = "OnBreakTimeout") val onBreakTimeout: Instant?,
    @field:Json(name = "SumupDetails") val sumUpDetail: SumUpDetail?,
    @field:Json(name = "SumupMode") val sumUpMode: SumUpMode?,
    @field:Json(name = "ShowDestinationOnOffer") val isDestinationOnOfferEnabled: Boolean?,
    @field:Json(name = "ShowEstimatedCommissionOnOffer") val isCommissionOnOfferEnabled: Boolean?,
    @field:Json(name = "ShowPickupOnOffer") val isPickupOnOfferEnabled: Boolean?,
    @field:Json(name = "AutoDispatchMode") val autoDispatchMode: AutoDispatchMode?,
    @field:Json(name = "AutoDispatchMin") val autoDispatchMin: Float?,
    @field:Json(name = "AutoDispatchMax") val autoDispatchMax: Float?,
    @field:Json(name = "ShowBookingTimeOnLateBooking") val isTimeOnLateBookingEnabled: Boolean?,
) {

    val isDriverOffline: Boolean
        get() = driverStatus == Driver.Status.OFFLINE

    val isDriverOnline: Boolean
        get() = driverStatus != Driver.Status.OFFLINE

    val isBreakAllocationActive: Boolean
        get() {
            return if (onBreakTimeout != null) {
                Duration.between(onBreakTimeout, Instant.now()).toMinutes() < 0
            } else false
        }

    val breakAllocationMinutesLeft: Int
        get() {
            return if (onBreakTimeout != null) {
                return abs(Duration.between(onBreakTimeout, Instant.now()).toMinutes())
                    .toInt()
            } else 0
        }

    val isSumUpAvailable: Boolean
        get() = !sumUpDetail?.username.isNullOrEmpty()
                && !sumUpDetail?.password.isNullOrEmpty()
                && !sumUpDetail?.clientId.isNullOrEmpty()
                && !sumUpDetail?.clientSecret.isNullOrEmpty()
                && !sumUpDetail?.apiKey.isNullOrEmpty()

}

@JsonClass(generateAdapter = true)
data class Shift(
    @field:Json(name = "Id") val id: String?,
    @UTCDateTime @field:Json(name = "ShiftStart") val startTime: Instant?,
    @UTCOffsetDateTime @field:Json(name = "EstimatedShiftEnd") val estEndTime: OffsetDateTime?,
    @field:Json(name = "LastKnownLatitude") val lastKnownLat: Double?,
    @field:Json(name = "LastKnownLongitude") val lastKnownLng: Double?,
    @field:Json(name = "Stats") val stats: Stats?,
    @field:Json(name = "MaximumDispatchTime") val maxDispatchTime: Int?,
    @field:Json(name = "MaximumDispatchDistance") val maxDispatchDistance: Float?,
    @field:Json(name = "DriverId") val driverId: String?,
    @field:Json(name = "VehicleId") val vehicleId: String?,
    @field:Json(name = "Status") val status: Driver.Status?
) {

    sealed class Response(val status: Driver.Status) {
        data class Online(val online: Shift) : Response(Driver.Status.ONLINE)
        data class Other(val newStatus: Driver.Status, val response: GenericResponse) :
            Response(newStatus)

        companion object {
            fun online(online: Shift): Response = Online(online)
            fun other(status: Driver.Status, response: GenericResponse): Response =
                Other(status, response)
        }
    }
}

//sealed class DriverStatusChangeResponse(val status: Driver.Status) {
//    data class Online(val online: Shift.Online) : DriverStatusChangeResponse(Driver.Status.ONLINE)
//    data class Other(val newStatus: Driver.Status, val response: GenericResponse) :
//        DriverStatusChangeResponse(newStatus)
//
//    companion object {
//        fun online(online: Shift.Online): DriverStatusChangeResponse = Online(online)
//        fun other(status: Driver.Status, response: GenericResponse): DriverStatusChangeResponse =
//            Other(status, response)
//    }
//}

@JsonClass(generateAdapter = true)
data class Stats(
    @field:Json(name = "Earnings") val earnings: Double?,
    @field:Json(name = "BookingCount") val bookingCount: Int?,
)

@JsonClass(generateAdapter = true)
data class Vehicle(
    @field:Json(name = "Id") val id: String?,
    @field:Json(name = "Make") val make: String?,
    @field:Json(name = "Model") val model: String?,
    @field:Json(name = "Colour") val color: String?,
    @field:Json(name = "Registration") val regNo: String?,
    @field:Json(name = "RegYear") val regYear: String?,
    @field:Json(name = "Active") val isActive: Boolean?,
    @field:Json(name = "Pax") val pax: Int?,
    @field:Json(name = "VehicleType") val type: Type?,
    @field:Json(name = "PlateNumber") val plateNumber: String?,
    @field:Json(name = "VehicleTypeId") val typeId: String?
) {

    val name: String
        get() = listOfNotNull(color, make, model).joinToString(" ")

    @JsonClass(generateAdapter = true)
    data class Type(
        @field:Json(name = "Id") val id: String?,
        @field:Json(name = "Name") val name: String?,
        @field:Json(name = "Description") val desc: String?,
        @field:Json(name = "FlagDownConfiguration_Id") val flagDownConfigId: String?,
        @field:Json(name = "VehicleType_Id") val flagDownTypeId: String?,
    )
}

@JsonClass(generateAdapter = true)
data class GenericResponse(
    @field:Json(name = "success") val isSuccess: Boolean?
)

@JsonClass(generateAdapter = true)
data class SumUpDetail(
    @field:Json(name = "Username") val username: String?,
    @field:Json(name = "Password") val password: String?,
    @field:Json(name = "ClientId") val clientId: String?,
    @field:Json(name = "ClientSecret") val clientSecret: String?,
    @field:Json(name = "APIKey") val apiKey: String?
)

@JsonClass(generateAdapter = false)
enum class SumUpMode(val constant: String) {
    @Json(name = "Driver")
    DRIVER("Driver"),

    @Json(name = "Company")
    COMPANY("Company"),

    @Json(name = "None")
    NONE("None");
}

@JsonClass(generateAdapter = false)
enum class AutoDispatchMode(val stepSize: Float) {

    @Json(name = "Distance")
    DISTANCE(0.5F),

    @Json(name = "Time")
    TIME(1F),

    @Json(name = "None")
    NONE(0F)
}

@JsonClass(generateAdapter = true)
data class AppConfig(
    @field:Json(name = "_id") val id: String?,
    @field:Json(name = "TenantId") val tenantId: String?,
    @field:Json(name = "BiddingTabs") val bidCategories: List<BidCategory>?,
)

@JsonClass(generateAdapter = false)
enum class BidCategory(@StringRes val labelResId: Int) {
    @Json(name = "Nearest")
    NEAREST(R.string.bid_type_nearby),

    @Json(name = "Recent")
    RECENT(R.string.bid_type_recent),

    @Json(name = "All")
    ALL(R.string.bid_type_all),

    @Json(name = "Selected")
    SELECTED(R.string.bid_type_submitted),

    @Json(name = "Archived")
    ARCHIVED(R.string.bid_type_archived),
}