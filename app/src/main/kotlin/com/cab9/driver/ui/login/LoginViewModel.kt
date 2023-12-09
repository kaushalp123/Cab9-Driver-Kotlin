package com.cab9.driver.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.repos.UserManager
import com.cab9.driver.di.user.UserComponentManager
import com.cab9.driver.ext.failureOverlay
import com.cab9.driver.ext.loadingOverlay
import com.cab9.driver.ext.success
import com.cab9.driver.network.ApiErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userManager: UserManager,
    private val userComponentManager: UserComponentManager,
    private val apiErrorHandler: ApiErrorHandler
) : ViewModel() {

    val username: String
        get() = userManager.username

    val companyCode: String
        get() = userManager.companyCode

    val userAuth = userManager.userAuth
    val userAuthLoader = userManager.userAuthLoader

    val isDriverOnline: Boolean
        get() = userComponentManager.cab9Repo.mobileState?.isDriverOnline == true

    fun initializeUserSession() {
        userManager.initializeUserLoginSession()
    }

    fun doLogin(companyCode: String, username: String, password: String) {
        userManager.login(companyCode, username, password)
    }

    private val _resetPasswordOutcome = MutableStateFlow<Outcome<Boolean>>(Outcome.Empty)
    val resetPasswordOutcome = _resetPasswordOutcome.asStateFlow()

    fun resetPassword(username: String) {
        viewModelScope.launch {
            try {
                _resetPasswordOutcome.loadingOverlay()
                val result = userManager.resetPassword(username)
                _resetPasswordOutcome.success(result)
            } catch (ex: Exception) {
                _resetPasswordOutcome.failureOverlay(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }
}