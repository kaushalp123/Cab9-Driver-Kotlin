package com.cab9.driver.ui.booking.ongoing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.UpdateSumUpPaymentRequest
import com.cab9.driver.di.user.UserComponentManager
import com.cab9.driver.ext.failure
import com.cab9.driver.ext.loading
import com.cab9.driver.ext.success
import com.cab9.driver.network.ApiErrorHandler
import com.cab9.driver.network.SumUpException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retryWhen
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class Cab9GoViewModel @Inject constructor(
    private val userComponentManager: UserComponentManager,
    private val apiErrorHandler: ApiErrorHandler
) : ViewModel() {

    private val _sumUpPaymentStatusOutcome =
        MutableStateFlow<Outcome<Triple<Boolean, String, String?>>>(Outcome.Empty)
    val sumUpPaymentStatusOutcome = _sumUpPaymentStatusOutcome.asStateFlow()

    fun updatePaymentStatus(isSuccess: Boolean, request: UpdateSumUpPaymentRequest) {
        flow { emit(userComponentManager.sumUpRepo.updatePaymentStatus(isSuccess, request)) }
            .onStart { _sumUpPaymentStatusOutcome.loading() }
            .retryWhen { cause, attempt ->
                delay(1500)
                return@retryWhen onRetry(request.localId, cause, attempt)
            }
            .map { _sumUpPaymentStatusOutcome.success(it) }
            .catch { onError(request.localId, it) }
            .launchIn(viewModelScope)
    }

    private fun onRetry(
        localId: String,
        cause: Throwable,
        attempt: Long
    ): Boolean {
        Timber.w(SumUpException("Retry($attempt) payment update for ID[${localId}]", cause))
        return attempt < 3
    }

    private fun onError(localId: String, error: Throwable) {
        Timber.w(SumUpException("Failed to update payment status for ID[${localId}]", error))
        _sumUpPaymentStatusOutcome.failure(apiErrorHandler.errorMessage(error), error)
    }
}