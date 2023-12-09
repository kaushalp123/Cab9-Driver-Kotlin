package com.cab9.driver.data.models

import android.os.Parcelable
import androidx.annotation.DrawableRes
import com.cab9.driver.ext.MMM_dd
import com.cab9.driver.ext.ofPattern
import com.cab9.driver.network.UTCOffsetDateTime
import com.cab9.driver.utils.BALANCE_ZERO
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import org.threeten.bp.OffsetDateTime

@Parcelize
data class PaymentModel(
    val id: String,
    val driverId: String,
    val fromDate: OffsetDateTime,
    val toDate: OffsetDateTime,
    val bonusAmount: Double,
    val bookingsCount: Int,
    val driverInvoice: InvoiceModel?,
    val companyInvoice: InvoiceModel?,
    val driver: DriverModel?,
    val weekName: String,
    val driverCommission: Double,
    val companyCommission: Double,
    val totalPayout: Double
) : Parcelable {

    val driverBookingCommissionAmount: Double
        get() = driverInvoice?.bookingsSubTotal?.plus(driverInvoice.bookingsTaxAmount)
            ?.plus(driverInvoice.waitingSubTotal)?.plus(driverInvoice.waitingTaxAmount)
            ?.plus(driverInvoice.extrasSubTotal)?.plus(driverInvoice.extrasTaxAmount)
            ?: BALANCE_ZERO

    val adjustmentTotalAmount: Double
        get() = driverInvoice?.adjustmentSubTotal
            ?.plus(driverInvoice.adjustmentsTaxAmount)
            ?: BALANCE_ZERO

    val companyBookingCommissionAmount: Double
        get() = companyInvoice?.bookingsSubTotal?.plus(companyInvoice.bookingsTaxAmount)
            ?.plus(companyInvoice.waitingSubTotal)?.plus(companyInvoice.waitingTaxAmount)
            ?.plus(companyInvoice.extrasSubTotal)?.plus(companyInvoice.extrasTaxAmount)
            ?: BALANCE_ZERO

    val companyAdjustmentTotalAmount: Double
        get() = companyInvoice?.adjustmentSubTotal
            ?.plus(companyInvoice.adjustmentsTaxAmount)
            ?: BALANCE_ZERO

    val weekStartEndDate: String
        get() {
            val endDate = toDate.plusDays(6)
            return toDate.ofPattern(MMM_dd).plus(" - ").plus(endDate.ofPattern(MMM_dd))
        }

}

@Parcelize
data class InvoiceModel(
    val id: String,
    val totalAmount: Double,
    val bookingsSubTotal: Double,
    val bookingsTaxAmount: Double,
    val adjustmentSubTotal: Double,
    val adjustmentsTaxAmount: Double,
    val extrasSubTotal: Double,
    val extrasTaxAmount: Double,
    val waitingSubTotal: Double,
    val waitingTaxAmount: Double,
) : Parcelable

@JsonClass(generateAdapter = true)
data class Payment(
    @field:Json(name = "Id") val id: String?,
    @field:Json(name = "DriverId") val driverId: String?,
    @UTCOffsetDateTime @field:Json(name = "PaymentFrom") val fromDate: OffsetDateTime?,
    @UTCOffsetDateTime @field:Json(name = "PaymentTo") val toDate: OffsetDateTime?,
    @field:Json(name = "BonusAmount") val bonusAmount: Double?,
    @field:Json(name = "BookingsCount") val bookingsCount: Int?,
    @field:Json(name = "Bill") val driverInvoice: Invoice?,
    @field:Json(name = "Invoice") val companyInvoice: Invoice?,
    @field:Json(name = "Driver") val driver: Driver?,
)

@JsonClass(generateAdapter = true)
data class Invoice(
    @field:Json(name = "Id") val id: String?,
    @field:Json(name = "TotalAmount") val totalAmount: Double?,
    @field:Json(name = "BookingsSubTotal") val bookingsSubTotal: Double?,
    @field:Json(name = "BookingsTaxAmount") val bookingsTaxAmount: Double?,
    @field:Json(name = "AdjustmentsSubTotal") val adjustmentSubTotal: Double?,
    @field:Json(name = "AdjustmentsTaxAmount") val adjustmentsTaxAmount: Double?,
    @field:Json(name = "ExtrasSubTotal") val extrasSubTotal: Double?,
    @field:Json(name = "ExtrasTaxAmount") val extrasTaxAmount: Double?,
    @field:Json(name = "WaitingSubTotal") val waitingSubTotal: Double?,
    @field:Json(name = "WaitingTaxAmount") val waitingTaxAmount: Double?,
)

@JsonClass(generateAdapter = true)
data class PaymentCard(
    @field:Json(name = "Id") val id: String?,
    @field:Json(name = "CardNumber") val number: String?,
    @field:Json(name = "ExpirationMonth") val expMonth: Int?,
    @field:Json(name = "ExpirationYear") val expYear: Int?,
    @field:Json(name = "BillingName") val billingName: String?,
    @field:Json(name = "DefaultCard") val isDefault: Boolean?,
    @field:Json(name = "PlatformType") val platformType: Boolean?,
    @field:Json(name = "Type") val type: String?
)

data class PaymentCardModel(
    val id: String,
    val number: String,
    val expMonth: Int,
    val expYear: Int,
    val billingName: String,
    val isDefault: Boolean,
    val type: String,
    @DrawableRes val typeIconResId: Int
) {
    val expiresOn: String
        get() = "$expMonth/$expYear"
}

