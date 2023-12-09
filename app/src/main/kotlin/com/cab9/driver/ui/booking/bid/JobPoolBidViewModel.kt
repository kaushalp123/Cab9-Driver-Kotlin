package com.cab9.driver.ui.booking.bid

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.map
import com.cab9.driver.R
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.BidCategory
import com.cab9.driver.data.models.JobBidDateRange
import com.cab9.driver.data.source.JobPoolBidPagingSource
import com.cab9.driver.data.source.NearbyBidsPagingSource
import com.cab9.driver.data.source.RecentBidsPagingSource
import com.cab9.driver.di.user.UserComponentManager
import com.cab9.driver.ext.emitFailureOverlay
import com.cab9.driver.ext.emitLoadingOverlay
import com.cab9.driver.ext.emitSuccess
import com.cab9.driver.ext.failureOverlay
import com.cab9.driver.ext.loading
import com.cab9.driver.network.ApiErrorHandler
import com.cab9.driver.network.JobBidExpiredException
import com.cab9.driver.utils.MAX_PAGE_SIZE
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class JobPoolBidViewModel @Inject constructor(
    private val application: Application,
    private val userComponentManager: UserComponentManager,
    private val apiErrorHandler: ApiErrorHandler
) : ViewModel() {

    var bidFilterDates: List<Date> = buildList {
        repeat(14) {
            add(
                Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, it)
                }.time
            )
        }
    }
        private set

    private val _archiveBidOutcome =
        MutableSharedFlow<Outcome<Pair<BidCategory, Boolean>>>(replay = 0)
    val archiveBidOutcome = _archiveBidOutcome.asSharedFlow()

    fun archiveBooking(
        bookingId: String, type: BidCategory
    ) {
        viewModelScope.launch {
            try {
                _archiveBidOutcome.emitLoadingOverlay()
                val isArchived = userComponentManager.jobPoolBidRepo.archiveBid(bookingId)
                _archiveBidOutcome.emitSuccess(Pair(type, isArchived))
                if (isArchived) {
                    _archivedBidsPagingSource?.invalidate()
                    when (type) {
                        BidCategory.RECENT -> _recentBidsPagingSource?.invalidate()
                        BidCategory.NEAREST -> _nearbyBidsPagingSource?.invalidate()
                        BidCategory.ALL -> _allBidsPagingSource?.invalidate()
                        else -> {
                            // do nothing
                        }
                    }
                }
            } catch (ex: Exception) {
                _archiveBidOutcome.emitFailureOverlay(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    private val _acceptBidOutcome =
        MutableSharedFlow<Outcome<Pair<BidCategory, Boolean>>>(replay = 0)
    val acceptBidOutcome = _acceptBidOutcome.asSharedFlow()

    fun acceptBid(
        bookingId: String,
        amount: Double?,
        type: BidCategory
    ) {
        viewModelScope.launch {
            try {
                _acceptBidOutcome.emitLoadingOverlay()
                if (type == BidCategory.ARCHIVED) {
                    // Check if this booking is still available to bid
                    if (userComponentManager.jobPoolBidRepo.openToBid(bookingId))
                        doAcceptBid(bookingId, amount, type)
                    else {
                        _acceptBidOutcome.emitFailureOverlay(
                            application.getString(R.string.err_job_expired),
                            JobBidExpiredException()
                        )
                    }
                } else doAcceptBid(bookingId, amount, type)
            } catch (ex: Exception) {
                _acceptBidOutcome.emitFailureOverlay(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    private suspend fun doAcceptBid(
        bookingId: String,
        amount: Double?,
        type: BidCategory
    ) {
        val isAccepted = userComponentManager.jobPoolBidRepo.acceptBid(bookingId, amount)
        _acceptBidOutcome.emitSuccess(Pair(type, isAccepted))
        if (isAccepted) {
            _submittedBidsPagingSource?.invalidate()
            when (type) {
                BidCategory.RECENT -> _recentBidsPagingSource?.invalidate()
                BidCategory.NEAREST -> _nearbyBidsPagingSource?.invalidate()
                BidCategory.ALL -> _allBidsPagingSource?.invalidate()
                BidCategory.ARCHIVED -> _archivedBidsPagingSource?.invalidate()
                else -> {}
            }
        }
    }

    private var _recentBidsPagingSource: RecentBidsPagingSource? = null
    private var _nearbyBidsPagingSource: NearbyBidsPagingSource? = null
    private var _allBidsPagingSource: JobPoolBidPagingSource? = null
    private var _archivedBidsPagingSource: JobPoolBidPagingSource? = null
    private var _submittedBidsPagingSource: JobPoolBidPagingSource? = null

    val recentJobBids = Pager(config = PagingConfig(
        pageSize = MAX_PAGE_SIZE, enablePlaceholders = false
    ), pagingSourceFactory = {
        RecentBidsPagingSource(userComponentManager.jobPoolBidRepo).also {
            _recentBidsPagingSource = it
        }
    }).flow.map { pagingData ->
        pagingData.map { userComponentManager.jobPoolBidRepo.map(it) }
    }.cachedIn(viewModelScope)

    fun getNearbyBidsStream(latLng: LatLng?) = Pager(config = PagingConfig(
        pageSize = MAX_PAGE_SIZE, enablePlaceholders = false
    ), pagingSourceFactory = {
        NearbyBidsPagingSource(
            userComponentManager.jobPoolBidRepo,
            latLng
        ).also { _nearbyBidsPagingSource = it }
    }).flow.map { pagingData ->
        pagingData.map { userComponentManager.jobPoolBidRepo.map(it) }
    }.cachedIn(viewModelScope)


    private val _archivedBidsDateRange = MutableLiveData(JobBidDateRange.NEXT_7_DAY)

    val archivedJobBids = _archivedBidsDateRange.distinctUntilChanged().switchMap { dateRange ->
        liveData {
            val state = Pager(config = PagingConfig(
                pageSize = MAX_PAGE_SIZE, enablePlaceholders = false
            ), pagingSourceFactory = {
                JobPoolBidPagingSource(
                    userComponentManager.jobPoolBidRepo,
                    BidCategory.ARCHIVED,
                    dateRange
                ).also { _archivedBidsPagingSource = it }
            }).flow.map { pagingData ->
                pagingData.map { userComponentManager.jobPoolBidRepo.map(it) }
            }.cachedIn(viewModelScope).asLiveData(Dispatchers.Main)
            emitSource(state)
        }
    }

    fun fetchArchivedJobBids(range: JobBidDateRange) {
        _archivedBidsDateRange.value = range
    }

    private val _submittedBidsDateRange = MutableLiveData(JobBidDateRange.NEXT_7_DAY)

    val submittedJobBids = _submittedBidsDateRange.distinctUntilChanged().switchMap { dateRange ->
        liveData {
            val state = Pager(config = PagingConfig(
                pageSize = MAX_PAGE_SIZE, enablePlaceholders = false
            ), pagingSourceFactory = {
                JobPoolBidPagingSource(
                    userComponentManager.jobPoolBidRepo,
                    BidCategory.SELECTED,
                    dateRange
                ).also { _submittedBidsPagingSource = it }
            }).flow.map { pagingData ->
                pagingData.map { userComponentManager.jobPoolBidRepo.map(it) }
            }.cachedIn(viewModelScope).asLiveData(Dispatchers.Main)
            emitSource(state)
        }
    }

    fun fetchSubmittedJobBids(range: JobBidDateRange) {
        _submittedBidsDateRange.value = range
    }

    private val _bookingCommissionOutcome =
        MutableStateFlow<Outcome<Pair<String, Double?>>>(Outcome.Empty)
    val bookingCommissionOutcome = _bookingCommissionOutcome.asStateFlow()

    fun getBiddingPrice(bookingId: String, type: BidCategory) {
        viewModelScope.launch {
            try {
                _bookingCommissionOutcome.loading()
                val result = userComponentManager.jobPoolBidRepo.getBookingCommission(bookingId)
                _bookingCommissionOutcome.emitSuccess(Pair(bookingId, result.commission))
                when (type) {
                    BidCategory.RECENT -> _recentBidsPagingSource?.invalidate()
                    BidCategory.NEAREST -> _nearbyBidsPagingSource?.invalidate()
                    BidCategory.ALL -> _allBidsPagingSource?.invalidate()
                    else -> {
                        // do nothing
                    }
                }

            } catch (ex: Exception) {
                _bookingCommissionOutcome.failureOverlay(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    private val _allBidsDateRange = MutableLiveData(JobBidDateRange.NEXT_7_DAY)

    val allJobBids = _allBidsDateRange.distinctUntilChanged().switchMap { dateRange ->
        liveData {
            val state = Pager(config = PagingConfig(
                pageSize = MAX_PAGE_SIZE, enablePlaceholders = false
            ), pagingSourceFactory = {
                JobPoolBidPagingSource(
                    userComponentManager.jobPoolBidRepo,
                    BidCategory.ALL,
                    dateRange
                ).also { _allBidsPagingSource = it }
            }).flow.map { pagingData ->
                pagingData.map { userComponentManager.jobPoolBidRepo.map(it) }
            }.cachedIn(viewModelScope).asLiveData(Dispatchers.Main)
            emitSource(state)
        }
    }

    fun fetchAllJobBids(range: JobBidDateRange) {
        _allBidsDateRange.value = range
    }

    fun refreshIncomingBids() {
        _recentBidsPagingSource?.invalidate()
        _nearbyBidsPagingSource?.invalidate()
        _allBidsPagingSource?.invalidate()
    }

    fun refreshAllBids() {
        refreshIncomingBids()
        _archivedBidsPagingSource?.invalidate()
        _submittedBidsPagingSource?.invalidate()
    }
}