package com.cab9.driver.network.apis

import com.cab9.driver.BuildConfig
import com.cab9.driver.data.models.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface NodeAPI {

    @POST("v1.1/driver-api/add-device")
    suspend fun registerDevice(
        @Body request: AddDeviceRequest,
        @Header("Authorization") auth: String? = null,
    ): AddDeviceRequest.Response

    @DELETE("v1.1/driver-api/device")
    suspend fun removeDevice(@Query("DeviceToken") token: String): Response<ResponseBody>

    @GET("v1.1/driver-api/state")
    suspend fun getMobileState(
        @Header("Authorization") auth: String? = null,
        @Query("versionNumber") versionNumber: String = BuildConfig.VERSION_NAME
    ): MobileState

    @GET("v1.1/driver-api/profile")
    suspend fun getDriverProfile(@Header("Authorization") auth: String? = null): Driver

    @PUT("v1.1/driver-api/profile")
    suspend fun updateProfile(@Body request: Driver.UpdateProfile): GenericResponse

    @GET("v1.1/driver-api/booking/details")
    suspend fun getBookingDetail(@Query("bookingId") bookingId: String): Booking

    @PUT
    suspend fun updateBookingOffer(
        @Url url: String,
        @Query("offerId") offerId: String,
        @Query("rejectReasonId") reasonId: String? = null
    ): GenericResponse

    @GET("v1.1/driver-api/booking/expenses")
    suspend fun getBookingExpenses(@Query("bookingId") bookingId: String): List<Expense>

    @GET("v1.1/driver-api/booking/upcoming-bookings")
    suspend fun getUpcomingBookings(): List<Booking>

    @GET("v1.1/driver-api/booking/historic-bookings")
    suspend fun getBookingHistory(@QueryMap query: Map<String, String>): List<Booking>

    @POST("v1.1/driver-api/flagdown/booking")
    suspend fun createFlagDownBooking(@Body request: CreateFlagDownBookingRequest): CreateFlagDownBookingRequest.Response

    @PUT
    suspend fun updateBookingStatus(
        @Url url: String,
        @Query("bookingId") bookingId: String
    ): GenericResponse

    @POST("v1.1/driver-api/booking/add-expense")
    suspend fun addBookingExpense(
        @Query("bookingId") bookingId: String,
        @Body expense: Expense
    ): GenericResponse

    @GET("v1.1/driver-api/vehicles")
    suspend fun getVehicles(): List<Vehicle>

    @PUT("v1.1/driver-api/vehicles/set-current")
    suspend fun updateCurrentVehicle(@Query("vehicleId") vehicleId: String): GenericResponse

//    @POST("v1.1/driver-api/shift/online")
//    suspend fun goOnline(): Shift

//    @POST("v1.1/driver-api/shift/online")
//    suspend fun updateShiftEndTime(@Query("shiftEndTime") endTime: String): Shift

    @POST("v1.1/driver-api/shift/online")
    suspend fun goOnline(
        @Query("shiftEndTime") endTime: String? = null,
        @Query("maximumDispatchTime") maxDispatchTime: Int? = null,
        @Query("maximumDispatchDistance") maxDispatchDistance: Float? = null
    ): Shift

    @POST
    suspend fun updateDriverStatus(@Url url: String): GenericResponse
//
//    @GET
//    suspend fun getGoogleMapDirection(@Url url: String): ResponseBody

    @POST("v1.1/driver-api/booking/acknowledge")
    suspend fun acknowledgeBooking(@Query("bookingId") bookingId: String): GenericResponse

    @GET("v1.1/driver-api/job-pool/all")
    suspend fun getAllBids(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
        @Query("records") pageSize: Int,
        @Query("pageNumber") pageNo: Int
    ): List<JobPoolBid>

    @GET("v1.1/driver-api/job-pool/archived-bookings")
    suspend fun getArchivedBids(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
        @Query("records") pageSize: Int,
        @Query("pageNumber") pageNo: Int
    ): List<JobPoolBid>

    @GET("v1.1/driver-api/job-pool/bid-offered")
    suspend fun getSubmittedBids(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
        @Query("records") pageSize: Int,
        @Query("pageNumber") pageNo: Int
    ): List<JobPoolBid>

    @GET("v1.1/driver-api/job-pool/Recent")
    suspend fun getRecentBids(
        @Query("records") pageSize: Int,
        @Query("pageNumber") pageNo: Int
    ): List<JobPoolBid>

    @GET("v1.1/driver-api/job-pool/nearby")
    suspend fun getNearbyBids(
        @Query("latitude") lat: Double?,
        @Query("longitude") lng: Double?,
        @Query("records") pageSize: Int,
        @Query("pageNumber") pageNo: Int
    ): List<JobPoolBid>

    @GET("v1.1/driver-api/job-pool/check-booking-available")
    suspend fun checkStatus(@Query("bookingId") bookingId: String): JobPoolBid.Status

    @POST("v1.1/driver-api/job-pool/archive")
    suspend fun archiveBid(@Query("bookingId") bookingId: String): GenericResponse

    @POST("v1.1/driver-api/job-pool/offer")
    suspend fun acceptBid(
        @Query("bookingId") bookingId: String,
        @Query("amount") bidAmount: Double?
    ): GenericResponse

    @GET("v1.1/driver-api/payments")
    suspend fun getPayments(
        @Query("from") startDate: String,
        @Query("to") endDate: String
    ): List<Payment>

    @GET("v1.1/driver-api/payment-cards")
    suspend fun getPaymentCards(): List<PaymentCard>

    @GET("v1.4/driver-api/plot")
    suspend fun getZonalOverview(
        @Query("Latitude") lat: Double?,
        @Query("Longitude") lng: Double?
    ): List<Zone>

    @GET("v1.4/driver-api/plot")
    suspend fun getZonalOverviewRaw(
        @Query("Latitude") lat: Double?,
        @Query("Longitude") lng: Double?
    ): Response<ResponseBody>

    @GET("/v1.4/driver-api/heatmap/booking-stop-details")
    suspend fun getHeatMapData(@Query("minsFilter") intervalInMinutes: Int): List<HeatMapLatLng>

    @POST
    suspend fun updateSumUpPaymentStatus(
        @Url url: String,
        @Body request: UpdateSumUpPaymentRequest
    ): Response<ResponseBody>

    @POST("v1.1/driver-api/price-bidding-booking")
    suspend fun getBiddingPrice(@Query("bookingId") bookingId: String): JobPoolBid.DriverCommission

    @GET("v2.0/driver-api/offer/reject-reasons")
    suspend fun getRejectionReasons(): Response<ResponseBody>

    @POST("v2.0/driver-api/offer/notification")
    suspend fun sendTestNotification(): GenericResponse

    @GET("v2.0/driver-api/flagdown-config")
    suspend fun getFlagDownConfig(): FlagDownConfig

    @GET("v2.0/driver-api/app-settings")
    suspend fun getAppConfig(): AppConfig
}