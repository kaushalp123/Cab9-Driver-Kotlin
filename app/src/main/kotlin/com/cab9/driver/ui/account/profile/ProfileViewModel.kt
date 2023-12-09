package com.cab9.driver.ui.account.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.ChangePasswordRequest
import com.cab9.driver.data.models.Driver
import com.cab9.driver.data.models.GenericResponse
import com.cab9.driver.data.repos.UserAuth
import com.cab9.driver.data.repos.UserManager
import com.cab9.driver.ext.*
import com.cab9.driver.network.ApiErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userManager: UserManager,
    private val apiErrorHandler: ApiErrorHandler
) : ViewModel() {

    val user: Driver?
        get() = userManager.user

    val currentPassword: String
        get() = userManager.password

    val username = userManager.userAuth.mapLatest {
        if (it is UserAuth.Authenticated) it.loginConfig.username else null
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val password = userManager.userAuth.mapLatest {
        if (it is UserAuth.Authenticated) userManager.password else null
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val displayName = userManager.userAuth.mapLatest {
        if (it is UserAuth.Authenticated) it.driver.displayName else null
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val avatarUrl = userManager.userAuth.mapLatest {
        if (it is UserAuth.Authenticated) it.driver.imageUrl else null
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val firstname = userManager.userAuth.mapLatest {
        if (it is UserAuth.Authenticated) it.driver.firstName else null
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val lastName = userManager.userAuth.mapLatest {
        if (it is UserAuth.Authenticated) it.driver.lastName else null
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val callSign = userManager.userAuth.mapLatest {
        if (it is UserAuth.Authenticated) it.driver.callSign else null
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val address = userManager.userAuth.mapLatest {
        if (it is UserAuth.Authenticated) it.driver.address else null
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _userProfileOutcome = MutableStateFlow<Outcome<Driver>>(Outcome.Empty)
    val userProfileOutcome = _userProfileOutcome.asStateFlow()

    fun getUserProfile(showLoading: Boolean) {
        viewModelScope.launch {
            try {
                if (showLoading) _userProfileOutcome.loading()
                else _updateMobileOutcome.emitLoadingOverlay()
                val result = userManager.getUserProfile()
                _userProfileOutcome.success(result)
            } catch (ex: Exception) {
                _userProfileOutcome.failure(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    val emailAddress = _userProfileOutcome.mapLatest {
        if (it is Outcome.Success) it.data.email else null
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val mobileNumber = _userProfileOutcome.mapLatest {
        if (it is Outcome.Success) it.data.mobile.orEmpty().ifEmpty { "NA" } else null
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val bankName = _userProfileOutcome.mapLatest {
        if (it is Outcome.Success) it.data.bankName else null
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val bankSortCode = _userProfileOutcome.mapLatest {
        if (it is Outcome.Success) it.data.bankSortCode else null
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val bankAccountNo = _userProfileOutcome.mapLatest {
        if (it is Outcome.Success) it.data.bankAccountNo else null
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _updateMobileOutcome = MutableSharedFlow<Outcome<GenericResponse>>(replay = 0)
    val updateMobileOutcome = _updateMobileOutcome.asSharedFlow()

    fun updateMobileNumber(newMobileNumber: String) {
        viewModelScope.launch {
            try {
                if (newMobileNumber == user?.mobile) return@launch
                _updateMobileOutcome.emitLoadingOverlay()
                val result = userManager
                    .updateUserProfile(Driver.UpdateProfile(mobile = newMobileNumber))
                delay(1500)
                getUserProfile(false)
                _updateMobileOutcome.emitSuccess(result)
            } catch (ex: Exception) {
                _updateMobileOutcome.emitFailureOverlay(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    private val _updateEmailOutcome = MutableStateFlow<Outcome<GenericResponse>>(Outcome.Empty)
    val updateEmailOutcome = _updateEmailOutcome.asStateFlow()

    fun updateEmailAddress(newEmail: String) {
        viewModelScope.launch {
            try {
                if (newEmail == user?.email) return@launch
                _updateEmailOutcome.loadingOverlay()
                val result = userManager.updateUserProfile(Driver.UpdateProfile(email = newEmail))
                delay(1500)
                getUserProfile(false)
                _updateEmailOutcome.success(result)
            } catch (ex: Exception) {
                _updateEmailOutcome.failureOverlay(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    private val _updateBankNameOutcome = MutableStateFlow<Outcome<GenericResponse>>(Outcome.Empty)
    val updateBankNameOutcome = _updateBankNameOutcome.asStateFlow()

    fun updateBankName(bankName: String) {
        viewModelScope.launch {
            try {
                if (bankName == user?.bankName) return@launch
                _updateBankNameOutcome.loadingOverlay()
                val result = userManager
                    .updateUserProfile(Driver.UpdateProfile(bankName = bankName))
                delay(1500)
                getUserProfile(false)
                _updateBankNameOutcome.success(result)
            } catch (ex: Exception) {
                _updateBankNameOutcome.failureOverlay(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    private val _updateBankSortCodeOutcome =
        MutableStateFlow<Outcome<GenericResponse>>(Outcome.Empty)
    val updateBankSortCodeOutcome = _updateBankSortCodeOutcome.asStateFlow()

    fun updateBankSortCode(sortCode: String) {
        viewModelScope.launch {
            try {
                if (sortCode == user?.bankSortCode) return@launch
                _updateBankSortCodeOutcome.loadingOverlay()
                val result = userManager
                    .updateUserProfile(Driver.UpdateProfile(bankSortCode = sortCode))
                delay(1500)
                getUserProfile(false)
                _updateBankSortCodeOutcome.success(result)
            } catch (ex: Exception) {
                _updateBankSortCodeOutcome.failureOverlay(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    private val _updateBankAccountNoOutcome =
        MutableStateFlow<Outcome<GenericResponse>>(Outcome.Empty)
    val updateBankAccountNoOutcome = _updateBankAccountNoOutcome.asStateFlow()

    fun updateBankAccountNo(accountNumber: String) {
        viewModelScope.launch {
            try {
                if (accountNumber == user?.bankAccountNo) return@launch
                _updateBankAccountNoOutcome.loadingOverlay()
                val result = userManager
                    .updateUserProfile(Driver.UpdateProfile(bankAccountNo = accountNumber))
                delay(1500)
                getUserProfile(false)
                _updateBankAccountNoOutcome.success(result)
            } catch (ex: Exception) {
                _updateBankAccountNoOutcome.failureOverlay(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    private val _changePasswordOutcome = MutableStateFlow<Outcome<Boolean>>(Outcome.Empty)
    val changePasswordOutcome = _changePasswordOutcome.asStateFlow()

    fun changePassword(request: ChangePasswordRequest) {
        viewModelScope.launch {
            try {
                request.username = userManager.username
                _changePasswordOutcome.loadingOverlay()
                userManager.changePassword(request)
                _changePasswordOutcome.success(true)
            } catch (ex: Exception) {
                _changePasswordOutcome.failureOverlay(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    init {
        getUserProfile(true)
    }
}