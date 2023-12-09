package com.cab9.driver.data.mapper

import android.content.Context
import com.cab9.driver.R
import com.cab9.driver.data.models.Booking
import com.cab9.driver.data.models.BookingListModel
import com.cab9.driver.data.models.Expense
import com.cab9.driver.data.models.ExpenseModel
import com.cab9.driver.data.models.JobPoolBid
import com.cab9.driver.data.models.JobPoolBidModel
import com.cab9.driver.ext.HH_mm
import com.cab9.driver.ext.dd_MM_yy
import com.cab9.driver.ext.formatDateTimeWithZone
import com.cab9.driver.ext.isToday
import com.cab9.driver.ext.isTomorrow
import com.cab9.driver.ext.ofPattern
import com.cab9.driver.ext.quantityText
import com.cab9.driver.ext.toDateTime
import com.cab9.driver.settings.SessionManager
import com.cab9.driver.utils.BALANCE_ZERO
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime

private fun toReadableDate(
    context: Context,
    sessionManager: SessionManager,
    bookedDateTime: Instant?
): String {
    return if (bookedDateTime == null) ""
    else {
        val bookedAt = bookedDateTime.toDateTime(sessionManager.timeZone)
        when {
            bookedAt.isToday() -> context.getString(R.string.today)
            bookedAt.isTomorrow() -> context.getString(R.string.tomorrow)
            else -> bookedAt.ofPattern(dd_MM_yy).orEmpty()
        }
    }
}

private fun toReadableTime(
    context: Context,
    sessionManager: SessionManager,
    bookedDateTime: Instant?
): String {
    //val bookedAt = LocalDateTime.ofInstant(bookedDateTime, sessionManager.timeZone)
    if (bookedDateTime == null) return context.getString(R.string.not_available)
    return if (sessionManager.isTimeOnPastBookingEnabled)
        formatDateTimeWithZone(sessionManager.timeZone, bookedDateTime, HH_mm)
    else {
        if (bookedDateTime.isBefore(Instant.now()))
            context.getString(R.string.action_asap).uppercase()
        else formatDateTimeWithZone(sessionManager.timeZone, bookedDateTime, HH_mm)
    }
}

private fun resolvePickup(context: Context, stops: List<Booking.Stop>?): String {
    return if (stops.isNullOrEmpty()) context.getString(R.string.not_available)
    else stops.first().summary.orEmpty().ifEmpty { context.getString(R.string.not_available) }
}

private fun resolvePickupPostcode(stops: List<Booking.Stop>?): String? =
    stops?.firstOrNull()?.postcode?.trim()

private fun resolveDrop(
    context: Context,
    asDirected: Boolean,
    stops: List<Booking.Stop>?
): String {
    return if (stops.isNullOrEmpty()) context.getString(R.string.not_available)
    else if (stops.size == 1) if (asDirected) context.getString(R.string.label_as_directed) else context.getString(
        R.string.not_available
    )
    else stops.last().summary.orEmpty().ifEmpty { context.getString(R.string.not_available) }
}

private fun resolveDropPostcode(asDirected: Boolean, stops: List<Booking.Stop>?): String? {
    return if (stops.isNullOrEmpty()) null
    else if (stops.size == 1 || asDirected) null
    else stops.lastOrNull()?.postcode?.trim()
}

fun isPreBooked(
    sessionManager: SessionManager,
    bookedDateTime: Instant?,
    creationDateTime: Instant?
): Boolean =
    when {
        bookedDateTime != null && creationDateTime != null -> {
            val bookedAt = LocalDateTime.ofInstant(bookedDateTime, sessionManager.timeZone)
            val createdAt = LocalDateTime.ofInstant(creationDateTime, sessionManager.timeZone)
            Duration.between(createdAt, bookedAt).toMinutes() > 15
        }

        else -> false
    }

private fun resolveStopCount(context: Context, stops: List<Booking.Stop>?): String? =
    if (!stops.isNullOrEmpty() && stops.size > 2)
        context.quantityText(R.plurals.pl_stops, R.string.zero_stop, stops.size - 2)
    else null

class BookingModelMapper(private val context: Context, val sessionManager: SessionManager) :
    Mapper<Booking, BookingListModel> {

    override fun map(input: Booking) = BookingListModel(
        id = input.id.orEmpty(),
        localId = input.localId.orEmpty(),
        bookedDate = toReadableDate(context, sessionManager, input.bookedDateTime),
        bookedExactTime = formatDateTimeWithZone(sessionManager.timeZone, input.bookedDateTime, HH_mm),
        bookedTime = toReadableTime(context, sessionManager, input.bookedDateTime),
        pickupAddress = resolvePickup(context, input.stops),
        pickupPostCode = resolvePickupPostcode(input.stops).orEmpty(),
        dropAddress = resolveDrop(context, input.asDirected ?: false, input.stops),
        dropPostCode = resolveDropPostcode(input.asDirected ?: false, input.stops).orEmpty(),
        flightNumber = input.flightInfo?.number.orEmpty(),
        vehicleType = input.vehicleType.orEmpty(),
        amount = input.driverCommission ?: BALANCE_ZERO,
        hasNotes = !input.driverNote.isNullOrEmpty(),
        hasTags = input.tags.orEmpty().isNotEmpty(),
        viaStopCountInString = resolveStopCount(context, input.stops),
        operationalZone = input.operationalZone,
        paymentMode = input.paymentMode,
        hasPaymentMode = input.paymentMode != null,
        vehicleTypeColor = input.vehicleTypeColor,
        driverAck = input.isAcknowledged ?: false,
        distance = input.distance
    )
}

class BookingModelListMapper(mapper: BookingModelMapper) :
    ListMapperImpl<Booking, BookingListModel>(mapper)

class JobBidMapper(private val context: Context, private val sessionManager: SessionManager) :
    Mapper<JobPoolBid, JobPoolBidModel> {

    override fun map(input: JobPoolBid) = JobPoolBidModel(
        bookingModel = BookingListModel(
            id = input.id.orEmpty(),
            localId = input.localId.orEmpty(),
            bookedDate = toReadableDate(context, sessionManager, input.bookedAt),
            bookedExactTime = formatDateTimeWithZone(sessionManager.timeZone, input.bookedAt, HH_mm),
            bookedTime = toReadableTime(context, sessionManager, input.bookedAt),
            pickupAddress = resolvePickup(context, input.stops),
            pickupPostCode = resolvePickupPostcode(input.stops).orEmpty(),
            dropAddress = resolveDrop(context, input.asDirected ?: false, input.stops),
            dropPostCode = resolveDropPostcode(input.asDirected ?: false, input.stops).orEmpty(),
            flightNumber = input.flightNumber.orEmpty(),
            vehicleType = input.vehicleType.orEmpty(),
            amount = input.amount ?: (input.calculatedPrice ?: BALANCE_ZERO),
            hasNotes = !input.driverNote.isNullOrEmpty(),
            hasTags = input.tags.orEmpty().isNotEmpty(),
            viaStopCountInString = resolveStopCount(context, input.stops),
            operationalZone = input.operationalZone,
            paymentMode = input.paymentMode,
            hasPaymentMode = input.paymentMode != null,
            vehicleTypeColor = input.vehicleTypeColor,
            driverAck = true,
            distance = input.distance
        ),
        isAuctionBooking = input.isAuctionBooking ?: false,
        hasAmount = input.amount != null || input.calculatedPrice != null,
        isPreBooked = isPreBooked(sessionManager, input.bookedAt, input.createdAt)
    )
}

class JobBidListMapper(mapper: JobBidMapper) : ListMapperImpl<JobPoolBid, JobPoolBidModel>(mapper)

class ExpenseMapper(private val context: Context) : Mapper<Expense, ExpenseModel> {
    override fun map(input: Expense) = ExpenseModel(
        name = input.name.orEmpty(),
        amount = input.amount ?: BALANCE_ZERO,
        status = when (input.isApproved) {
            true -> context.getString(R.string.status_approved)
            false -> context.getString(R.string.status_declined)
            else -> context.getString(R.string.status_pending)
        },
        textColor = when (input.isApproved) {
            true -> R.color.approved_expense_text_color
            false -> R.color.declined_expense_text_color
            else -> R.color.pending_expense_text_color
        }
    )
}

class ExpenseListMapper(mapper: ExpenseMapper) : ListMapperImpl<Expense, ExpenseModel>(mapper)