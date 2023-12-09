package com.cab9.driver.ui.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.Shift
import com.cab9.driver.di.user.UserComponentManager
import com.cab9.driver.ext.failure
import com.cab9.driver.ext.loading
import com.cab9.driver.ext.success
import com.cab9.driver.network.ApiErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChangeStatusViewModel @Inject constructor(
    private val userComponentManager: UserComponentManager,
    private val apiErrorHandler: ApiErrorHandler
) : ViewModel() {

    private val _autoDispatchSettingsOutcome = MutableStateFlow<Outcome<Shift>>(Outcome.Empty)
    val autoDispatchSettingsOutcome = _autoDispatchSettingsOutcome.asStateFlow()

    fun updateDispatchTime(newDispatchTime: Int) {
        viewModelScope.launch {
            try {
                _autoDispatchSettingsOutcome.loading()
                val result = userComponentManager.accountRepo.updateDispatchTime(newDispatchTime)
                _autoDispatchSettingsOutcome.success(result)
            } catch (ex: Exception) {
                _autoDispatchSettingsOutcome.failure(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    fun updateDispatchDistance(newDispatchDistance: Float) {
        viewModelScope.launch {
            try {
                _autoDispatchSettingsOutcome.loading()
                val result = userComponentManager.accountRepo
                    .updateDispatchDistance(newDispatchDistance)
                _autoDispatchSettingsOutcome.success(result)
            } catch (ex: Exception) {
                _autoDispatchSettingsOutcome.failure(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }
}