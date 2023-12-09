package com.cab9.driver.data.repos

import com.cab9.driver.data.mapper.JobBidMapper
import com.cab9.driver.data.models.JobBidDateRange
import com.cab9.driver.data.models.JobPoolBid
import com.cab9.driver.data.models.JobPoolBidModel
import com.cab9.driver.di.user.LoggedInScope
import com.cab9.driver.ext.lat
import com.cab9.driver.ext.lng
import com.cab9.driver.network.apis.NodeAPI
import com.google.android.gms.maps.model.LatLng
import javax.inject.Inject

interface JobPoolBidRepository {
    fun map(job: JobPoolBid): JobPoolBidModel

    suspend fun getRecentBids(pageSize: Int, pageNo: Int): List<JobPoolBid>

    suspend fun getNearbyBids(latLng: LatLng?, pageSize: Int, pageNo: Int): List<JobPoolBid>

    suspend fun getArchivedBids(
        dateRange: JobBidDateRange,
        pageSize: Int,
        pageNo: Int
    ): List<JobPoolBid>

    suspend fun getSubmittedBids(
        dateRange: JobBidDateRange,
        pageSize: Int,
        pageNo: Int
    ): List<JobPoolBid>

    suspend fun getAllBids(dateRange: JobBidDateRange, pageSize: Int, pageNo: Int): List<JobPoolBid>

    suspend fun openToBid(bookingId: String): Boolean

    suspend fun acceptBid(bookingId: String, amount: Double?): Boolean

    suspend fun archiveBid(bookingId: String): Boolean

    suspend fun getBookingCommission(bookingId: String): JobPoolBid.DriverCommission
}

@LoggedInScope
class JobPoolBidRepositoryImpl @Inject constructor(
    private val apiService: NodeAPI,
    private val jobBidMapper: JobBidMapper
) : JobPoolBidRepository {

    override fun map(job: JobPoolBid) = jobBidMapper.map(job)

    override suspend fun getRecentBids(pageSize: Int, pageNo: Int) =
        apiService.getRecentBids(pageSize, pageNo)

    override suspend fun getNearbyBids(latLng: LatLng?, pageSize: Int, pageNo: Int) =
        apiService.getNearbyBids(latLng?.lat, latLng?.lng, pageSize, pageNo)

    override suspend fun getArchivedBids(
        dateRange: JobBidDateRange,
        pageSize: Int,
        pageNo: Int
    ): List<JobPoolBid> {
        //val currentDate = LocalDate.now()
        return apiService.getArchivedBids(
            dateRange.strStartDate,
            dateRange.strEndDate,
            pageSize,
            pageNo
        )
    }

    override suspend fun getSubmittedBids(
        dateRange: JobBidDateRange,
        pageSize: Int,
        pageNo: Int
    ): List<JobPoolBid> {
        //val currentDate = LocalDate.now()
        return apiService.getSubmittedBids(
            dateRange.strStartDate,
            dateRange.strEndDate,
            pageSize,
            pageNo
        )
    }

    override suspend fun getAllBids(dateRange: JobBidDateRange, pageSize: Int, pageNo: Int) =
        apiService.getAllBids(
            dateRange.strStartDate,
            dateRange.strEndDate,
            pageSize,
            pageNo
        )

    override suspend fun openToBid(bookingId: String): Boolean {
        val result = apiService.checkStatus(bookingId)
        return result.isOpenToBid == true
    }

    override suspend fun acceptBid(bookingId: String, amount: Double?): Boolean {
        val response = apiService.acceptBid(bookingId, amount)
        return response.isSuccess == true
    }

    override suspend fun archiveBid(bookingId: String): Boolean {
        val result = apiService.archiveBid(bookingId)
        return result.isSuccess == true
    }

    override suspend fun getBookingCommission(bookingId: String): JobPoolBid.DriverCommission =
        apiService.getBiddingPrice(bookingId)

}