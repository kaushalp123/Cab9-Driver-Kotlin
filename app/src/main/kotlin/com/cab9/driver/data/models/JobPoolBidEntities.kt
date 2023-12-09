package com.cab9.driver.data.models

import android.os.Parcelable
import com.cab9.driver.ext.ofPattern
import com.cab9.driver.network.UTCDateTime
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate

data class JobBidDateRange(val startDate: LocalDate, val endDate: LocalDate) {
    companion object {
        private const val START_TIME = "T00:00:00Z"
        private const val END_TIME = "T23:59:00Z"

        val NEXT_7_DAY = JobBidDateRange(LocalDate.now(), LocalDate.now().plusDays(7))
    }

    val strStartDate: String
        get() = startDate.ofPattern("yyyy-MM-dd").orEmpty().plus(START_TIME)

    val strEndDate: String
        get() = endDate.ofPattern("yyyy-MM-dd").orEmpty().plus(END_TIME)
}

@Parcelize
data class JobPoolBidModel(
    val bookingModel: BookingListModel,
    val isAuctionBooking: Boolean,
    val hasAmount: Boolean,
    val isPreBooked: Boolean
) : Parcelable

@JsonClass(generateAdapter = true)
data class JobPoolBid(
    @field:Json(name = "Id") val id: String?,
    @field:Json(name = "LocalId") val localId: String?,
    @field:Json(name = "TenantId") val tenantId: String?,
    @field:Json(name = "PaymentMethod") val paymentMode: PaymentMode?,
    @field:Json(name = "AsDirected") val asDirected: Boolean?,
    @field:Json(name = "EstimatedDistance") val estimatedDistance: Double?,
    @field:Json(name = "Distance") val distance: Double?,
    @field:Json(name = "ActualCost") val actualCost: Double?,
    @field:Json(name = "ImageUrl") val imageUrl: String?,
    @field:Json(name = "BookingStatus") val bookingStatus: Booking.Status?,
    @field:Json(name = "NextStop") val nextStop: String?,
//    @UTCOffsetDateTime @field:Json(name = "BookedDateTime") val bookedAt: OffsetDateTime?,
//    @UTCOffsetDateTime @field:Json(name = "CreationTime") val createdAt: OffsetDateTime?,
    @UTCDateTime @field:Json(name = "BookedDateTimeUTC") val bookedAt: Instant?,
    @UTCDateTime @field:Json(name = "CreationTime") val createdAt: Instant?,
    @field:Json(name = "DriverNotes") val driverNote: String?,
    @field:Json(name = "Amount") val amount: Double?,
    @field:Json(name = "IsAuctionBooking") val isAuctionBooking: Boolean?,
    @field:Json(name = "FlightInfoId") val flightInfoId: String?,
    @field:Json(name = "Origin") val origin: String?,
    @field:Json(name = "FlightNumber") val flightNumber: String?,
    @field:Json(name = "OriginCode") val originCode: String?,
    @field:Json(name = "BookingStops") val stops: List<Booking.Stop>?,
    @field:Json(name = "VehicleType") val vehicleType: String?,
    @field:Json(name = "VehicleTypeColour") val vehicleTypeColor: String?,
    @field:Json(name = "OperationalZone") val operationalZone: String?,
    @field:Json(name = "BookingTags") val tags: List<Tag>?,
    @field:Json(name = "calculatedPrice") val calculatedPrice: Double?,
) {

    @JsonClass(generateAdapter = true)
    data class Status(
        @field:Json(name = "OpenToBid") val isOpenToBid: Boolean?,
    )

    @JsonClass(generateAdapter = true)
    data class DriverCommission(
        @field:Json(name = "DriverCommission") val commission: Double?,
    )

}