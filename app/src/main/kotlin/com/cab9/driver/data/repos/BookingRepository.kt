package com.cab9.driver.data.repos

import com.cab9.driver.BuildConfig
import com.cab9.driver.data.mapper.BookingModelListMapper
import com.cab9.driver.data.mapper.ExpenseListMapper
import com.cab9.driver.data.models.*
import com.cab9.driver.di.user.LoggedInScope
import com.cab9.driver.ext.*
import com.cab9.driver.network.apis.Cab9API
import com.cab9.driver.network.apis.NodeAPI
import com.google.android.gms.maps.model.LatLng
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.threeten.bp.LocalDate
import java.io.File
import javax.inject.Inject

interface BookingRepository {
    suspend fun getBookingDetail(bookingId: String): Booking

    suspend fun getBookingExpenses(bookingId: String): List<ExpenseModel>

    suspend fun getUpcomingBookings(): List<BookingListModel>

    suspend fun getBookings(date: LocalDate): List<BookingListModel>

    suspend fun acknowledgeBooking(bookingId: String): GenericResponse

    suspend fun createFlagDownBooking(request: CreateFlagDownBookingRequest): CreateFlagDownBookingRequest.Response

    suspend fun canStartRide(bookingId: String, latLng: LatLng): Boolean

    suspend fun updateBookingStatus(status: Booking.Status, bookingId: String): Boolean

    suspend fun updateBookingOffer(
        status: BookingOfferStatus,
        offerId: String,
        reasonId: String?
    ): Boolean

    suspend fun addExpense(bookingId: String, expense: Expense): Boolean

    suspend fun uploadSignature(bookingId: String, signatureImage: File): Boolean
}

@LoggedInScope
class BookingRepositoryImpl @Inject constructor(
    private val apiService: NodeAPI,
    private val cab9Service: Cab9API,
    private val listMapper: BookingModelListMapper,
    private val expenseListMapper: ExpenseListMapper
) : BookingRepository {

    override suspend fun getBookingDetail(bookingId: String): Booking {
        return apiService.getBookingDetail(bookingId)
    }

    override suspend fun getBookingExpenses(bookingId: String): List<ExpenseModel> {
        val result = apiService.getBookingExpenses(bookingId)
        return expenseListMapper.map(result)
    }

    override suspend fun getUpcomingBookings(): List<BookingListModel> {
        val result = apiService.getUpcomingBookings()
        return listMapper.map(result)
    }

    override suspend fun getBookings(date: LocalDate): List<BookingListModel> {
        val strDate = date.toPattern(SERVER_DATE_FORMAT)
        val startDate = strDate.plus(DAY_START_TIME)
        val endDate = strDate.plus(DAY_END_TIME)
        val bookings = apiService.getBookingHistory(mapOf("from" to startDate, "to" to endDate))
        return listMapper.map(bookings)
    }

    override suspend fun acknowledgeBooking(bookingId: String) =
        apiService.acknowledgeBooking(bookingId)

    override suspend fun createFlagDownBooking(request: CreateFlagDownBookingRequest) =
        apiService.createFlagDownBooking(request)

    override suspend fun canStartRide(bookingId: String, latLng: LatLng): Boolean {
        val result = cab9Service.canStartRide(bookingId, latLng.lat, latLng.lng)
        return result.canStart == true
    }

    override suspend fun updateBookingStatus(status: Booking.Status, bookingId: String): Boolean {
        val url = when (status) {
            Booking.Status.ON_ROUTE -> BuildConfig.BASE_NODE_URL.plus("v1.1/driver-api/booking/status/onroute")
            Booking.Status.ARRIVED -> BuildConfig.BASE_NODE_URL.plus("v1.1/driver-api/booking/status/arrived")
            Booking.Status.IN_PROGRESS -> BuildConfig.BASE_NODE_URL.plus("v1.1/driver-api/booking/status/inprogress")
            Booking.Status.NEXT_STOP -> BuildConfig.BASE_NODE_URL.plus("v1.1/driver-api/booking/next-stop")
            Booking.Status.COMPLETED -> BuildConfig.BASE_NODE_URL.plus("v1.1/driver-api/booking/status/complete")
            else -> throw IllegalArgumentException("Unknown booking status implementation!")
        }
        return apiService.updateBookingStatus(url, bookingId).isSuccess ?: false
    }

    override suspend fun updateBookingOffer(
        status: BookingOfferStatus,
        offerId: String,
        reasonId: String?
    ): Boolean {
        val url = when (status) {
            BookingOfferStatus.READ -> BuildConfig.BASE_NODE_URL.plus("v1.1/driver-api/booking-offer/read")
            BookingOfferStatus.ACCEPT -> BuildConfig.BASE_NODE_URL.plus("v1.1/driver-api/booking-offer/accept")
            BookingOfferStatus.REJECT -> BuildConfig.BASE_NODE_URL.plus("v1.1/driver-api/booking-offer/reject")
        }
        return apiService.updateBookingOffer(url, offerId, reasonId).isSuccess ?: false
    }

    override suspend fun addExpense(bookingId: String, expense: Expense): Boolean {
        val response = apiService.addBookingExpense(bookingId, expense)
        return response.isSuccess == true
    }

    override suspend fun uploadSignature(bookingId: String, signatureImage: File): Boolean {
        val requestBody = signatureImage.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val imagePart = MultipartBody.Part.createFormData("file", signatureImage.name, requestBody)
        val response = cab9Service.uploadSignature(bookingId, imagePart)
        return response.isSuccessful
    }
}