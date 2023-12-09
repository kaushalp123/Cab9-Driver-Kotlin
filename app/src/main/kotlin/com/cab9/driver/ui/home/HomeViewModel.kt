package com.cab9.driver.ui.home

import android.location.Location
import androidx.annotation.IdRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.cab9.driver.R
import com.cab9.driver.base.Cab9DriverApp
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.BidCategory
import com.cab9.driver.data.models.BookingListModel
import com.cab9.driver.data.models.Driver
import com.cab9.driver.data.models.MobileState
import com.cab9.driver.data.models.Shift
import com.cab9.driver.data.remote.RemoteConfig
import com.cab9.driver.data.repos.UserAuth
import com.cab9.driver.data.repos.UserManager
import com.cab9.driver.di.booking.JobInProgress
import com.cab9.driver.di.user.UserComponentManager
import com.cab9.driver.ext.emitFailureOverlay
import com.cab9.driver.ext.emitLoadingOverlay
import com.cab9.driver.ext.emitSuccess
import com.cab9.driver.ext.failure
import com.cab9.driver.ext.failureOverlay
import com.cab9.driver.ext.loading
import com.cab9.driver.ext.loadingOverlay
import com.cab9.driver.ext.success
import com.cab9.driver.network.ApiErrorHandler
import com.cab9.driver.network.AppConfigException
import com.cab9.driver.network.Cab9TokenException
import com.cab9.driver.network.FirebaseTokenException
import com.cab9.driver.network.RemoteConfigException
import com.cab9.driver.services.UpdateFirebaseTokenWorker
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.threeten.bp.LocalDateTime
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val context: Cab9DriverApp,
    private val userManager: UserManager,
    private val apiErrorHandler: ApiErrorHandler,
    private val workManager: WorkManager,
    private val userComponentManager: UserComponentManager,
) : ViewModel() {

    val authToken: String
        get() = userManager.authToken

    val mobileState: MobileState?
        get() = userComponentManager.cab9Repo.mobileState

    val currentLocation: Location?
        get() = userComponentManager.sharedLocationManager.lastKnownLocation

    private val _currentLocationOutcome = MutableStateFlow<Outcome<Location?>>(Outcome.Empty)
    val currentLocationOutcome = _currentLocationOutcome.asStateFlow()

    fun getCurrentLocation() {
        viewModelScope.launch {
            try {
                _currentLocationOutcome.loadingOverlay()
                val result = userComponentManager.sharedLocationManager
                    .fetchCurrentLocation(CancellationTokenSource())
                if (result == null)
                    _currentLocationOutcome.failureOverlay(context.getString(R.string.err_current_location))
                else _currentLocationOutcome.success(result)
            } catch (ex: Exception) {
                val message = ex.localizedMessage ?: ex.message
                _currentLocationOutcome.failureOverlay(
                    if (!message.isNullOrEmpty()) message
                    else context.getString(R.string.err_msg_generic), ex
                )
            }
        }
    }

    /**
     * To listen to changes in [MobileState] for current booking
     */
    val jobInProgressFlow: StateFlow<JobInProgress> =
        userComponentManager.cab9Repo.jobInProgressAsFlow

    /**
     * Use this to listen to any changes in mobile state
     */
    val mobileStateFlow: StateFlow<Outcome<MobileState>> =
        userComponentManager.cab9Repo.mobileStateAsFlow

    /**
     * Fetch latest [MobileState] changes from Cab9 server.
     */
    fun fetchMobileState() {
        userComponentManager.cab9Repo.fetchMobileState()
    }

    /**
     * User this to list to logged in user login status
     */
    val userAuth: StateFlow<UserAuth> = userManager.userAuth

    /**
     * Call this to initiate user logout flow.
     */
    fun logout() {
        userManager.logout()
    }

    private val _openScreenId = MutableStateFlow(0)
    val openScreenId = _openScreenId.asStateFlow()

    fun openScreen(@IdRes id: Int) {
        if (_openScreenId.value != id) {
            _openScreenId.value = id
        }
    }

    private val _acceptedBookingId = MutableStateFlow<String?>(null)
    val acceptedBookingId = _acceptedBookingId.asStateFlow()

    fun openBookingScreen(bookingId: String?) {
        if (_acceptedBookingId.value != bookingId) {
            _acceptedBookingId.value = bookingId
        }
    }

    private val _upcomingBookingsOutcome =
        MutableStateFlow<Outcome<List<BookingListModel>>>(Outcome.Empty)
    val upcomingBookingsOutcome = _upcomingBookingsOutcome.asStateFlow()

    fun getUpcomingBookings() {
        viewModelScope.launch {
            try {
                _upcomingBookingsOutcome.loading()
                val result = userComponentManager.bookingRepo.getUpcomingBookings()
                _upcomingBookingsOutcome.success(result)
            } catch (ex: Exception) {
                _upcomingBookingsOutcome.failure(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    private val _driveStateChangeOutcome =
        MutableSharedFlow<Outcome<Shift.Response>>(replay = 0)
    val driveStateChangeOutcome = _driveStateChangeOutcome.asSharedFlow()

    fun changeStatus(status: Driver.Status) {
        viewModelScope.launch {
            try {
                _driveStateChangeOutcome.emitLoadingOverlay()
                val result = userComponentManager.accountRepo.changeStatus(status)
                if (result is Shift.Response.Online) {
                    Timber.d("Driver status changed to online, update firebase token...")
                    updateFirebaseToken()
                }
                fetchMobileState()
                _driveStateChangeOutcome.emitSuccess(result)
            } catch (ex: Exception) {
                _driveStateChangeOutcome.emitFailureOverlay(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    private val _updateShiftEndTimeOutcome = MutableSharedFlow<Outcome<Shift>>(replay = 0)
    val updateShiftEndTimeOutcome = _updateShiftEndTimeOutcome.asSharedFlow()

    fun updateShiftEndTime(dateTime: LocalDateTime) {
        viewModelScope.launch {
            try {
                _updateShiftEndTimeOutcome.emitLoadingOverlay()
                val result = userComponentManager.accountRepo.updateShiftEndTime(dateTime)
                fetchMobileState()
                _updateShiftEndTimeOutcome.emitSuccess(result)
            } catch (ex: Exception) {
                _updateShiftEndTimeOutcome.emitFailureOverlay(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    private fun updateFirebaseToken() {
        viewModelScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                workManager.enqueueUniqueWork(
                    UpdateFirebaseTokenWorker.NAME,
                    ExistingWorkPolicy.REPLACE,
                    UpdateFirebaseTokenWorker.createRequest(token)
                )
            } catch (ex: Exception) {
                Timber.e(FirebaseTokenException(ex))
            }
        }
    }


    init {
        updateFirebaseToken()
        getUpcomingBookings()
    }
}