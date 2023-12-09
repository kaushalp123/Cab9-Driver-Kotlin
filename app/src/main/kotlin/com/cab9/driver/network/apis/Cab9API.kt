package com.cab9.driver.network.apis

import com.cab9.driver.data.models.Booking
import com.cab9.driver.data.models.Expense
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface Cab9API {

    @Streaming
    @GET("api/DriverPayments/pdf")
    suspend fun downloadPaymentFile(@Query("paymentId") paymentId: String): Response<ResponseBody>

    @GET("api/bookings/CheckDriverStart")
    suspend fun canStartRide(
        @Query("BookingId") bookingId: String,
        @Query("Latitude") lat: Double,
        @Query("Longitude") lng: Double,
    ): Booking.Start

    @GET("api/Expense/types")
    suspend fun getExpenseTypes(): List<Expense>

    @Multipart
    @POST("api/booking/passenger-signature")
    suspend fun uploadSignature(
        @Query("bookingId") bookingId: String,
        @Part image: MultipartBody.Part
    ): Response<ResponseBody>
}