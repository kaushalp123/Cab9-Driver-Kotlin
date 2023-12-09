package com.cab9.driver.data.mapper

import android.content.Context
import com.cab9.driver.R
import com.cab9.driver.data.models.*
import com.cab9.driver.utils.BALANCE_ZERO
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.temporal.WeekFields
import java.util.*
import kotlin.math.abs

class InvoiceMapper : Mapper<Invoice, InvoiceModel> {
    override fun map(input: Invoice) = InvoiceModel(
        id = input.id.orEmpty(),
        totalAmount = input.totalAmount ?: BALANCE_ZERO,
        bookingsSubTotal = input.bookingsSubTotal ?: BALANCE_ZERO,
        bookingsTaxAmount = input.bookingsTaxAmount ?: BALANCE_ZERO,
        adjustmentSubTotal = input.adjustmentSubTotal ?: BALANCE_ZERO,
        adjustmentsTaxAmount = input.adjustmentsTaxAmount ?: BALANCE_ZERO,
        extrasSubTotal = input.extrasSubTotal ?: BALANCE_ZERO,
        extrasTaxAmount = input.extrasTaxAmount ?: BALANCE_ZERO,
        waitingSubTotal = input.waitingSubTotal ?: BALANCE_ZERO,
        waitingTaxAmount = input.waitingTaxAmount ?: BALANCE_ZERO
    )
}

class PaymentMapper(
    private val context: Context,
    private val invoiceMapper: InvoiceMapper,
    private val driverMapper: DriverMapper
) : Mapper<Payment, PaymentModel> {
    override fun map(input: Payment) =
        PaymentModel(
            id = input.id.orEmpty(),
            driverId = input.driverId.orEmpty(),
            fromDate = input.fromDate!!,
            toDate = input.toDate!!,
            bonusAmount = input.bonusAmount ?: BALANCE_ZERO,
            bookingsCount = input.bookingsCount ?: 0,
            driverInvoice = input.driverInvoice?.let { invoiceMapper.map(it) },
            companyInvoice = input.companyInvoice?.let { invoiceMapper.map(it) },
            driver = input.driver?.let { driverMapper.map(it) },
            weekName = getWeekName(context, input.toDate),
            driverCommission = input.driverInvoice?.totalAmount ?: BALANCE_ZERO,
            companyCommission = input.companyInvoice?.totalAmount ?: BALANCE_ZERO,
            totalPayout = getTotalPayout(input)
        )

    companion object {
        fun getWeekName(context: Context, date: OffsetDateTime): String {
            val weekField = WeekFields.of(Locale.ENGLISH).weekOfWeekBasedYear()
            return context.getString(R.string.temp_week_name, date.get(weekField))
        }

        fun getTotalPayout(payment: Payment): Double {
            val driverCommissionTotal = payment.driverInvoice?.totalAmount ?: BALANCE_ZERO
            val companyCommissionTotal = payment.companyInvoice?.totalAmount ?: BALANCE_ZERO

            return if (abs(companyCommissionTotal) > driverCommissionTotal) BALANCE_ZERO
            else driverCommissionTotal - abs(companyCommissionTotal)
        }
    }
}

class PaymentListMapper(mapper: PaymentMapper) : ListMapperImpl<Payment, PaymentModel>(mapper)

class PaymentCardMapper : Mapper<PaymentCard, PaymentCardModel> {
    override fun map(input: PaymentCard) = PaymentCardModel(
        id = input.id.orEmpty(),
        number = input.number.orEmpty(),
        expMonth = input.expMonth!!,
        expYear = input.expYear!!,
        billingName = input.billingName.orEmpty(),
        isDefault = input.isDefault ?: false,
        type = input.type.orEmpty(),
        typeIconResId = when (input.type) {
            "visa" -> R.drawable.img_visa_card
            "mastercard" -> R.drawable.img_mastercard_card
            "maestro" -> R.drawable.img_maestro_card
            "amex" -> R.drawable.img_amex_card
            else -> R.drawable.img_card_placeholder
        }
    )
}

class PaymentCardListMapper(mapper: PaymentCardMapper) :
    ListMapperImpl<PaymentCard, PaymentCardModel>(mapper)