package com.cab9.driver.data.models

import android.os.Parcelable
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.cab9.driver.R
import com.cab9.driver.network.UTCDateTime
import com.cab9.driver.utils.BALANCE_ZERO
import com.google.android.gms.maps.model.LatLng
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import org.threeten.bp.Instant

data class Charge(
    val label: String,
    val amount: Double
)

enum class BookingType {
    UPCOMING, HISTORY;
}

@Parcelize
data class BookingTagModel(
    val id: String,
    val name: String,
    val type: String,
) : Parcelable

@Parcelize
data class BookingStopModel(
    val id: String,
    val address: String,
    val shortAddress: String,
    val postcode: String,
    val order: Int?,
    val latLng: LatLng?
) : Parcelable

@Parcelize
data class BookingListModel(
    val id: String,
    val localId: String,
    val bookedDate: String,
    val bookedExactTime: String,
    val bookedTime: String,
    val pickupAddress: String,
    val pickupPostCode: String,
    val dropAddress: String,
    val dropPostCode: String,
    val flightNumber: String,
    val vehicleType: String,
    val amount: Double,
    val paymentMode: PaymentMode?,
    val hasNotes: Boolean,
    val hasTags: Boolean,
    val hasPaymentMode: Boolean,
    val vehicleTypeColor: String?,
    val viaStopCountInString: String?,
    val operationalZone: String?,
    val driverAck: Boolean,
    val distance: Double?
) : Parcelable


@JsonClass(generateAdapter = true)
data class Booking(
    @field:Json(name = "Id") val id: String?,
    @field:Json(name = "LocalId") val localId: String?,
    @field:Json(name = "TenantId") val tenantId: String?,
    @field:Json(name = "BookingStatus") val status: Status?,
    @field:Json(name = "PaymentMethod") val paymentMode: PaymentMode?,
    //@ZoneOffsetDateTime @field:Json(name = "BookedDateTime") val bookedDateTime: ZonedDateTime?,
    @UTCDateTime @field:Json(name = "BookedDateTimeUTC") val bookedDateTime: Instant?,
    @UTCDateTime @field:Json(name = "JobClearDateTime") val jobClearDateTime: Instant?,
    //@UTCOffsetDateTime @field:Json(name = "BookedDateTimeBST") val BookedDateTimeBST: Instant?,
    @field:Json(name = "AsDirected") val asDirected: Boolean?,
    @field:Json(name = "EstimatedDistance") val estimatedDistance: Float?,
    @field:Json(name = "Distance") val distance: Double?,
    @field:Json(name = "DriverNotes") val driverNote: String?,
    @field:Json(name = "ActualCost") val actualCost: Double?,
    @field:Json(name = "TaxAmount") val taxAmount: Double?,
    @field:Json(name = "DriverCommission") val driverCommission: Double?,
    @field:Json(name = "ImageUrl") val imageUrl: String?,
    @field:Json(name = "PassengerSignatureImageUrl") val passengerSignatureImageUrl: String?,
    @field:Json(name = "SignatureRequired") val isSignatureRequired: Boolean?,
    @field:Json(name = "BookingStops") val stops: List<Stop>?,
    @field:Json(name = "FlightInfo") val flightInfo: FlightInfo?,
    @field:Json(name = "VehicleType") val vehicleType: String?,
    @field:Json(name = "ClientName") val clientName: String?,
    @field:Json(name = "Passenger") val passenger: Passenger?,
    @field:Json(name = "Bax") val bagCount: Int?,
    @field:Json(name = "Pax") val paxCount: Int?,
    @field:Json(name = "Adjustments") val adjustments: List<Adjustment>?,
    @field:Json(name = "WaitingTime") val waitingTime: Int?,
    @field:Json(name = "WaitingTimeInSeconds") val waitingTimeInSeconds: Int?,
    @field:Json(name = "WaitingCost") val waitingCost: Double?,
    @field:Json(name = "WaitingTotal") val waitingTotalCost: Double?,
    @field:Json(name = "WaitingTax") val waitTaxAmount: Double?,
    @field:Json(name = "Extras") val extras: List<Extra>?,
    @field:Json(name = "NextStop") val nextStopIndex: Int?,
    @field:Json(name = "DriverAck") val isAcknowledged: Boolean?,
    @field:Json(name = "DoesDriverHardAcknowledge") val isHardAcknowledged: Boolean?,
    @field:Json(name = "BookingTags") val tags: List<Tag>?,
    @field:Json(name = "CardPaymentStatus") val paymentStatus: String?,
    @field:Json(name = "FlagDown") val isFlagDownBooking: Boolean?,
    @field:Json(name = "EstimatedDuration") val estDuration: Int?,
    @field:Json(name = "IsBookingEnRouteAfterAccept") val isBookingEnRouteAfterAccept: Boolean?,
    @field:Json(name = "OperationalZone") val operationalZone: String?,
    @field:Json(name = "VehicleTypeColour") val vehicleTypeColor: String?,
    @field:Json(name = "PricingMode") val pricingMode: String?,
    @field:Json(name = "CompletingDateTime") val strCompletingDateTime: String?,
) {

    val journeyCost: Double
        get() = actualCost ?: BALANCE_ZERO

    val journeyTaxAmt: Double
        get() = taxAmount ?: BALANCE_ZERO

    val totalWaitingCost: Double
        get() = waitingTotalCost ?: BALANCE_ZERO

    val waitTaxAmt: Double
        get() = waitTaxAmount ?: BALANCE_ZERO

    val totalCostWithoutTaxes: Double
        get() {
            val totalAdjustmentCost = adjustments?.sumOf { it.amt } ?: BALANCE_ZERO
            val totalExtraCost = extras?.sumOf { it.amt } ?: BALANCE_ZERO
            return journeyCost + totalWaitingCost + totalAdjustmentCost + totalExtraCost
        }

    val totalTaxes: Double
        get() {
            val totalAdjustmentTaxes = adjustments?.sumOf { it.taxAmt } ?: BALANCE_ZERO
            val totalExtraCost = extras?.sumOf { it.taxAmt } ?: BALANCE_ZERO
            return journeyTaxAmt + waitTaxAmt + totalAdjustmentTaxes + totalExtraCost
        }

    val totalCostIncludingTaxes: Double
        get() = totalCostWithoutTaxes + totalTaxes

    val totalAdjustmentWithTaxes: Double
        get() = adjustments?.sumOf { it.amountWithTax } ?: BALANCE_ZERO

    val totalExtrasWithTaxes: Double
        get() = extras?.sumOf { it.amountWithTax } ?: BALANCE_ZERO

    val isPaymentCompleted: Boolean
        get() = paymentStatus.equals("success", ignoreCase = true)

    val isEligibleForSumUpPayment: Boolean
        get() = paymentMode == PaymentMode.CARD || paymentMode == PaymentMode.CASH

    val hasViaStops: Boolean
        get() = !stops.isNullOrEmpty() && stops.size > 2

    val viaStops: List<Stop>?
        get() = if (hasViaStops) {
            stops?.subList(1, stops.lastIndex)
        } else null

    val hasTags: Boolean
        get() = !tags.isNullOrEmpty()

    @JsonClass(generateAdapter = true)
    data class Stop(
        @field:Json(name = "Id") val id: String?,
        @field:Json(name = "Address1") val address1: String?,
        @field:Json(name = "Area") val area: String?,
        @field:Json(name = "TownCity") val townCity: String?,
        @field:Json(name = "Postcode") val postcode: String?,
        @field:Json(name = "County") val county: String?,
        @field:Json(name = "StopSummary") val summary: String?,
        @field:Json(name = "StopOrder") val order: Int?,
        @field:Json(name = "Latitude") val lat: Double?,
        @field:Json(name = "Longitude") val lng: Double?,
    ) {
        val latLng: LatLng?
            get() = if (lat != null && lng != null) LatLng(lat, lng) else null

        val address: String
            get() = listOfNotNull(address1, area, townCity, postcode, county).joinToString()
    }

    @JsonClass(generateAdapter = true)
    data class Start(
        @field:Json(name = "CanStart") val canStart: Boolean?,
    )

    @JsonClass(generateAdapter = false)
    enum class Status(val constant: String) {
        @Json(name = "OnRoute")
        ON_ROUTE("OnRoute"),

        @Json(name = "Arrived")
        ARRIVED("Arrived"),

        @Json(name = "InProgress")
        IN_PROGRESS("InProgress"),

        @Json(name = "Next Stop")
        NEXT_STOP("Next Stop"),

        @Json(name = "Completed")
        COMPLETED("Completed"),

        @Json(name = "Allocated")
        ALLOCATED("Allocated"),

        @Json(name = "Clearing")
        CLEARING("Clearing"),

        @Json(name = "Incoming")
        INCOMING("Incoming"),

        @Json(name = "OpenToBid")
        OPEN_TO_BID("OpenToBid"),

        @Json(name = "COA")
        COA("COA"),

        @Json(name = "Cancelled")
        CANCELLED("Cancelled"),

        @Json(name = "PreAllocated")
        PRE_ALLOCATED("PreAllocated");
    }
}

@JsonClass(generateAdapter = false)
enum class PaymentMode(
    @StringRes val labelResId: Int,
    @DrawableRes val iconResId: Int,
    @ColorRes val colorResId: Int,
    val constant: String
) {
    @Json(name = "Cash")
    CASH(
        R.string.payment_mode_cash_booking,
        R.drawable.ic_baseline_payments_cash_24,
        R.color.color_cash_payment,
        "Cash"
    ),

    @Json(name = "OnAccount")
    ONACCOUNT(
        R.string.payment_mode_on_account,
        R.drawable.ic_baseline_account_balance_24,
        R.color.color_on_account_payment,
        "OnAccount"
    ),

    @Json(name = "Card")
    CARD(
        R.string.payment_mode_card_booking, R.drawable.ic_baseline_credit_card_24,
        R.color.color_card_payment,
        "Card"
    ),

    @Json(name = "InCarPayment")
    IN_CAR_PAYMENT(
        R.string.payment_mode_in_car,
        R.drawable.ic_baseline_credit_card_24,
        R.color.color_other_payment,
        "InCarPayment"
    ),

    @Json(name = "Other")
    OTHER(
        R.string.payment_mode_other,
        R.drawable.ic_baseline_default_payment,
        R.color.color_other_payment,
        "Other"
    );
}

@JsonClass(generateAdapter = true)
data class FlightInfo(
    @field:Json(name = "Id") val id: String?,
    @field:Json(name = "FlightNumber") val number: String?,
    @field:Json(name = "OriginCode") val originCode: String?,
    @field:Json(name = "Origin") val origin: String?,
)

@JsonClass(generateAdapter = true)
data class Adjustment(
    @field:Json(name = "BookingId") val bookingId: String?,
    @field:Json(name = "Name") val name: String?,
    @field:Json(name = "Amount") val amount: Double?,
    @field:Json(name = "TaxAmount") val taxAmount: Double?,
    @field:Json(name = "Type") val type: String?,
    @field:Json(name = "ActivateByCode") val activateByCode: String?
) {
    val amt: Double
        get() = amount ?: BALANCE_ZERO

    val taxAmt: Double
        get() = taxAmount ?: BALANCE_ZERO

    val amountWithTax: Double
        get() = amt + taxAmt
}

@JsonClass(generateAdapter = true)
data class Extra(
    @field:Json(name = "Name") val name: String?,
    @field:Json(name = "Description") val desc: String?,
    @field:Json(name = "Amount") val amount: Double?,
    @field:Json(name = "TaxAmount") val taxAmount: Double?,
    @field:Json(name = "Note") val note: String?
) {
    val amt: Double
        get() = amount ?: BALANCE_ZERO

    val taxAmt: Double
        get() = taxAmount ?: BALANCE_ZERO

    val amountWithTax: Double
        get() = amt + taxAmt
}

@JsonClass(generateAdapter = true)
data class Expense(
    @field:Json(name = "ExpenseId") val id: String?,
    @field:Json(name = "ExpenseName") val name: String?,
    @field:Json(name = "Note") val note: String?,
    @field:Json(name = "DriverAmount") val amount: Double?,
    @field:Json(name = "Approved") val isApproved: Boolean?
) {
    override fun toString(): String = name.orEmpty()
}

@JsonClass(generateAdapter = true)
data class Tag(
    @field:Json(name = "Id") val id: String?,
    @field:Json(name = "Name") val name: String?,
    @field:Json(name = "Type") val type: String?,
)

//{"ExpenseName":"parking","ExpenseId":"8040cbe3-597e-ea11-a94b-0050f24f187e","Note":"test","DriverAmount":"5"}
//POST https://nodejs-api.cab9.app/v1.1/driver-api/booking/add-expense?bookingId=81A34F2E-0434-ED11-B49A-0022481AE2A1 http/1.1

data class ExpenseModel(
    val name: String,
    val amount: Double,
    val status: String,
    @ColorRes val textColor: Int
)

@JsonClass(generateAdapter = true)
data class CreateFlagDownBookingRequest(
    @field:Json(name = "PickupLatitude") val pickupLat: Double,
    @field:Json(name = "PickupLongitude") val pickupLng: Double,
    @field:Json(name = "PickupStopSummary") val pickupStopSummary: String,
    @field:Json(name = "AsDirected") val asDirected: Boolean,
    @field:Json(name = "DropLatitude") val dropLat: Double? = null,
    @field:Json(name = "DropLongitude") val dropLng: Double? = null,
    @field:Json(name = "DropStopSummary") val dropStopSummary: String? = null,
) {
    @JsonClass(generateAdapter = true)
    data class Response(
        @field:Json(name = "Id") val bookingId: String?
    )
}

enum class BookingOfferStatus {
    READ, ACCEPT, REJECT;
}

@Parcelize
@JsonClass(generateAdapter = true)
class BookingOfferPayload(
    @field:Json(name = "LocalId") val localId: Int?,
    @field:Json(name = "BookingId") val bookingId: String?,
    @field:Json(name = "OfferId") val offerId: String?,
    @field:Json(name = "PickupTime") val pickupTime: Int?,
    @field:Json(name = "PickupDistance") val pickupDistance: Float?,
    @UTCDateTime @field:Json(name = "Expires") val expiresIn: Instant?,
    //@UTCOffsetDateTime @field:Json(name = "BookedDateTime") val bookedDateTime: OffsetDateTime?,
    @UTCDateTime @field:Json(name = "BookedDateTimeUTC") val bookedDateTime: Instant?,
    @field:Json(name = "PickupStopSummary") val pickupAddress: String?,
    @field:Json(name = "DropoffStopSummary") val dropAddress: String?,
    @field:Json(name = "EstimatedDistance") val estimatedDistance: Float?,
    @field:Json(name = "EstimatedDuration") val estimatedDuration: Int?,
    @field:Json(name = "Stops") val totalStops: Int?,
    @field:Json(name = "VehicleTypeName") val vehicleName: String?,
    @field:Json(name = "IsBookingEnRouteAfterAccept") val isBookingEnRouteAfterAccept: Boolean?,
    @field:Json(name = "DriverCommission") val driverCommission: Double?,
    @field:Json(name = "OfferType") val offerType: String?,
    @field:Json(name = "PaymentMethod") val paymentMethod: String?,
) : Parcelable {

    val isFollowOn: Boolean
        get() = !offerType.isNullOrEmpty() && "FollowOn".equals(offerType, ignoreCase = true)

    val isReminder: Boolean
        get() = !offerType.isNullOrEmpty() && "Reminder".equals(offerType, ignoreCase = true)

    val isTestOffer: Boolean
        get() = !offerType.isNullOrEmpty() && "Test offer".equals(offerType, ignoreCase = true)

}

@JsonClass(generateAdapter = true)
data class RejectionReason(
    @field:Json(name = "Id") val id: String?,
    @field:Json(name = "TenantId") val tenantId: String?,
    @field:Json(name = "RejectReason") val rejectReason: String?
)

@JsonClass(generateAdapter = true)
data class BookingPriceUpdate(
    @field:Json(name = "TenantId") val tenantId: String?,
    @field:Json(name = "BookingId") val bookingId: String?,
    @field:Json(name = "DriverId") val driverId: String?,
    @field:Json(name = "WaitingTime") val waitTime: Int?,
    @field:Json(name = "WaitingCost") val waitCost: Double?,
    @field:Json(name = "ActualDistance") val actualDistance: Double?,
    @field:Json(name = "ActualCost") val actualCost: Double?,
    @field:Json(name = "MeterCost") val meterCost: Double?,
    @field:Json(name = "StationaryTime") val stationaryTime: String?,
    @field:Json(name = "MeterPaused") val meterPaused: Any?,
)

@Parcelize
data class BookingMeterModel(
    val bookingId: String,
    val waitTime: Int,
    val waitCost: Double?,
    val actualDistance: Double,
    val actualCost: Double,
    val meterCost: Double
) : Parcelable {
    constructor(bookingPrice: BookingPriceUpdate) : this(
        bookingId = bookingPrice.bookingId.orEmpty(),
        waitTime = bookingPrice.waitTime ?: 0,
        waitCost = bookingPrice.waitCost,
        actualDistance = bookingPrice.actualDistance ?: 0.0,
        actualCost = bookingPrice.actualCost ?: BALANCE_ZERO,
        meterCost = bookingPrice.meterCost ?: BALANCE_ZERO
    )
}

@Parcelize
data class BookingDetailModel(
    val id: String,
    val status: Booking.Status,
    val actualCost: Double,
    val actualTaxAmount: Double,
    val waitingCost: Double?,
    val waitingTaxAmount: Double,
    val extrasWithTaxes: Double,
    val adjustmentWithTaxes: Double,
    val pricingMode: String,
    val paymentMode: PaymentMode,
    val isAsDirected: Boolean,
    val actualDistance: Float,
    val completingDateTime: String?
) : Parcelable {
    constructor(booking: Booking) : this(
        id = booking.id.orEmpty(),
        status = booking.status ?: Booking.Status.ALLOCATED,
        actualCost = booking.actualCost ?: BALANCE_ZERO,
        actualTaxAmount = booking.taxAmount ?: BALANCE_ZERO,
        waitingTaxAmount = booking.waitTaxAmount ?: BALANCE_ZERO,
        waitingCost = booking.waitingCost,
        extrasWithTaxes = booking.totalExtrasWithTaxes,
        adjustmentWithTaxes = booking.totalAdjustmentWithTaxes,
        pricingMode = booking.pricingMode.orEmpty(),
        paymentMode = booking.paymentMode ?: PaymentMode.OTHER,
        isAsDirected = booking.asDirected ?: false,
        actualDistance = booking.estimatedDistance ?: 0F,
        completingDateTime = booking.strCompletingDateTime
    )

    val totalCostWithoutActualAmount: Double
        get() = actualTaxAmount + waitingTaxAmount + extrasWithTaxes + adjustmentWithTaxes
}
