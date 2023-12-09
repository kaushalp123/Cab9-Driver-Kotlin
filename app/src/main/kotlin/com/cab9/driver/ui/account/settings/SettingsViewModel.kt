package com.cab9.driver.ui.account.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cab9.driver.base.Outcome
import com.cab9.driver.di.user.UserComponentManager
import com.cab9.driver.ext.emitFailureOverlay
import com.cab9.driver.ext.emitLoadingOverlay
import com.cab9.driver.ext.emitSuccess
import com.cab9.driver.network.ApiErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userComponentManager: UserComponentManager,
    private val apiErrorHandler: ApiErrorHandler
) : ViewModel() {

    private val _sendTestNotificationResult = MutableSharedFlow<Outcome<Boolean>>(replay = 0)
    val testNotificationResult = _sendTestNotificationResult.asSharedFlow()

    fun sendTestNotification() {
        viewModelScope.launch {
            try {
                _sendTestNotificationResult.emitLoadingOverlay()
                val result = userComponentManager.cab9Repo.sendTestNotification()
                _sendTestNotificationResult.emitSuccess(result)
            } catch (ex: Exception) {
                _sendTestNotificationResult.emitFailureOverlay(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

}