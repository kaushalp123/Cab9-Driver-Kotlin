package com.cab9.driver.data.repos

import com.cab9.driver.BuildConfig
import com.cab9.driver.data.models.AddDeviceRequest
import com.cab9.driver.data.models.ChangePasswordRequest
import com.cab9.driver.data.models.Driver
import com.cab9.driver.data.models.LoginConfig
import com.cab9.driver.network.ApiErrorHandler
import com.cab9.driver.network.InvalidAccessTokenException
import com.cab9.driver.network.InvalidFirebaseTokenException
import com.cab9.driver.network.apis.LoginAPI
import com.cab9.driver.network.apis.NodeAPI
import com.cab9.driver.network.apis.PasswordAPI
import com.cab9.driver.settings.AppSettings
import com.cab9.driver.settings.SessionManager
import com.cab9.driver.utils.BEARER
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.sumup.merchant.reader.api.SumUpAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

sealed class UserAuth {
    data class Authenticated(val loginConfig: LoginConfig, val driver: Driver) : UserAuth() {
        override fun toString(): String {
            return "Authenticated"
        }
    }

    data class Unauthenticated(val errorMsg: String? = null) : UserAuth() {
        override fun toString(): String {
            return "Unauthenticated"
        }
    }
}

@Singleton
class UserManager @Inject constructor(
    private val loginApi: LoginAPI,
    private val nodeApi: NodeAPI,
    private val passwordApi: PasswordAPI,
    private val sessionManager: SessionManager,
    private val appSettings: AppSettings,
    private val apiErrorHandler: ApiErrorHandler
) {

    private val mainScope = MainScope()

    private val _userAuth = MutableStateFlow<UserAuth>(UserAuth.Unauthenticated())
    val userAuth = _userAuth.asStateFlow()

    private val _userAuthLoader = MutableStateFlow(true)
    val userAuthLoader = _userAuthLoader.asStateFlow()

    val companyCode: String
        get() = sessionManager.companyCode

    val username: String
        get() = sessionManager.username

    val password: String
        get() = sessionManager.password

    val authToken: String
        get() = sessionManager.authToken

    val user: Driver?
        get() = sessionManager.loggedInUser

    val isLoggedIn: Boolean
        get() = _userAuth.value is UserAuth.Authenticated

    init {
        initializeUserLoginSession()
    }

    fun initializeUserLoginSession() {
        mainScope.launch {
            withContext(Dispatchers.IO) {
                if (sessionManager.isUserLoggedIn) {
                    Timber.w("Initializing user state...".uppercase())
                    _userAuthLoader.value = true
                    val user = sessionManager.loggedInUser
                    val loginConfig = sessionManager.loginConfig
                    if (user != null && loginConfig != null) {
                        val latestConfig = getLoginConfig() ?: loginConfig
                        updateUserAuthResult(UserAuth.Authenticated(latestConfig, user))
                    } else updateUserAuthResult(UserAuth.Unauthenticated())
                } else updateUserAuthResult(UserAuth.Unauthenticated())
            }
        }
    }

    /**
     * This is used to fetch new changes while user first logs in after app update
     */
    private suspend fun getLoginConfig(): LoginConfig? {
        return try {
            if (appSettings.appVersion != BuildConfig.VERSION_NAME) {
                Timber.w("App version updated, fetch latest login config".uppercase())
                val config = loginApi.doLogin(
                    sessionManager.companyCode,
                    sessionManager.username,
                    sessionManager.password
                )
                appSettings.appVersion = BuildConfig.VERSION_NAME
                sessionManager.updateLoginConfig(config)
                return config
            } else null
        } catch (ex: Exception) {
            null
        }
    }

    fun login(companyCode: String, username: String, password: String) {
        mainScope.launch {
            try {
                Timber.i("Do login with -> [$username,$password]")
                _userAuthLoader.value = true
                val loginConfig = loginApi.doLogin(companyCode, username, password)

                // Check if we have valid access token
                if (loginConfig.accessToken.isNullOrEmpty()) throw InvalidAccessTokenException()
                val authToken = "$BEARER ${loginConfig.accessToken}"

                // Get new firebase token and register
                val fcmToken = FirebaseMessaging.getInstance().token.await()
                if (fcmToken.isNullOrEmpty()) throw InvalidFirebaseTokenException()

                val user = nodeApi.getDriverProfile(authToken)
                val device = nodeApi.registerDevice(AddDeviceRequest(fcmToken), authToken)

                // If different user logged since last login clear settings
                if (sessionManager.username != username) appSettings.clear()

                sessionManager.createLoginSession(
                    companyCode,
                    username,
                    password,
                    loginConfig,
                    user,
                    fcmToken,
                    device.id
                )
                updateUserAuthResult(UserAuth.Authenticated(loginConfig, user))
            } catch (ex: Exception) {
                updateUserAuthResult(UserAuth.Unauthenticated(apiErrorHandler.errorMessage(ex)))
            }
        }
    }

    fun logout() {
        mainScope.launch {
            try {
                _userAuthLoader.value = true
                // get stored firebase token
                val fcmToken = sessionManager.fcmToken
                // set user as logged out
                sessionManager.isUserLoggedIn = false
                // Remove fcm token from server
                if (!fcmToken.isNullOrEmpty()) nodeApi.removeDevice(fcmToken)
                SumUpAPI.logout()
                updateUserAuthResult(UserAuth.Unauthenticated())
            } catch (ex: Exception) {
                updateUserAuthResult(UserAuth.Unauthenticated(apiErrorHandler.errorMessage(ex)))
            }
        }
    }

    suspend fun getUserProfile(): Driver {
        val user = nodeApi.getDriverProfile()
        sessionManager.updateUserDetail(user)
        return user
    }

    suspend fun updateUserProfile(request: Driver.UpdateProfile) =
        nodeApi.updateProfile(request)

    suspend fun changePassword(request: ChangePasswordRequest) {
        passwordApi.changePassword(request)
        sessionManager.updatePassword(request.newPassword)
    }

    suspend fun resetPassword(username: String): Boolean {
        val response = loginApi.resetPassword(username)
        return response.isSuccessful
    }

    private suspend fun updateUserAuthResult(newAuth: UserAuth) {
        if (newAuth is UserAuth.Authenticated) {
            // Set firebase user id here
            newAuth.driver.callSign?.let { Firebase.crashlytics.setUserId(it) }
        }
        _userAuthLoader.value = false
        _userAuth.emit(newAuth)
    }

}