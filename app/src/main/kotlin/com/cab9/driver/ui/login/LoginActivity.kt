package com.cab9.driver.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.biometric.BiometricPrompt
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cab9.driver.BuildConfig
import com.cab9.driver.R
import com.cab9.driver.base.BaseActivity
import com.cab9.driver.data.models.LoginConfig
import com.cab9.driver.data.repos.UserAuth
import com.cab9.driver.databinding.ActivityLoginBinding
import com.cab9.driver.di.qualifiers.ActivityBiometricHandler
import com.cab9.driver.ext.toast
import com.cab9.driver.settings.AppSettings
import com.cab9.driver.settings.BiometricHandler
import com.cab9.driver.settings.BiometricHandlerCallback
import com.cab9.driver.settings.BiometricInteractor
import com.cab9.driver.settings.SessionManager
import com.cab9.driver.ui.account.Cab9RequiredPermission
import com.cab9.driver.ui.account.settings.PermissionCheckActivity
import com.cab9.driver.ui.home.HomeActivity
import com.cab9.driver.utils.NOTIFICATION_TYPE
import com.cab9.driver.utils.TYPE_BOOKING_OFFER
import com.cab9.driver.utils.createNotificationChannel
import com.cab9.driver.utils.getNotificationToneUri
import com.cab9.driver.widgets.dialog.noButton
import com.cab9.driver.widgets.dialog.yesButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : BaseActivity(R.layout.activity_login), BiometricHandlerCallback,
    BiometricInteractor {

    companion object {
        @JvmStatic
        fun start(context: Context) {
            val starter = Intent(context, LoginActivity::class.java)
            context.startActivity(starter)
        }
    }

    private val binding by viewBinding(ActivityLoginBinding::bind)
    private val viewModel by viewModels<LoginViewModel>()

    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    @Inject
    lateinit var appSettings: AppSettings

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    @ActivityBiometricHandler
    lateinit var biometricHandler: BiometricHandler

    private var skipBiometricCheck = false
    private var isFirstTimeLogin = false
    private var isPasswordLoginSelected = false

    override val isBiometricLoginEnabled: Boolean
        get() = appSettings.isBiometricEnabled
                && BiometricHandler.hasSystemFeature(this)
                && biometricHandler.isRegistered

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.loginToolbar)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_login) as NavHostFragment
        val graphInflater = navHostFragment.navController.navInflater
        val graph = graphInflater.inflate(R.navigation.login_graph)

        // Show signup option only if company code for current client is
        // available in build config
        appBarConfiguration =
            if (sessionManager.isLoginCredentialAvailable || BuildConfig.COMPANY_CODE.isEmpty()) {
                graph.setStartDestination(R.id.loginFragment)
                AppBarConfiguration(setOf(R.id.loginFragment))
            } else {
                graph.setStartDestination(R.id.welcomeFragment)
                AppBarConfiguration(setOf(R.id.welcomeFragment))
            }

        navController = navHostFragment.navController
        navController.graph = graph

        setupActionBarWithNavController(navController, appBarConfiguration)
        createNotificationChannel(
            this,
            getString(R.string.default_notification_channel_id),
            getString(R.string.default_notification_channel_name),
            getString(R.string.default_notification_channel_desc),
            getNotificationToneUri(applicationContext)
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userAuth.collectLatest {
                    if (it is UserAuth.Authenticated) onUserAuthenticationComplete(it.loginConfig)
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        //val navController = findNavController(R.id.nav_host_fragment_login)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        biometricHandler.dismiss()
        super.onDestroy()
    }

    private fun onUserAuthenticationComplete(loginConfig: LoginConfig) {
        when {
            skipBiometricCheck -> goHome("onUserAuthenticationComplete: Skip biometric")
            // If app is opened via booking offer notification or user is already online, take user home
            // without any biometric check or any verification
            intent.extras?.getString(NOTIFICATION_TYPE) == TYPE_BOOKING_OFFER || viewModel.isDriverOnline -> {
                if (intent.extras?.getString(NOTIFICATION_TYPE) == TYPE_BOOKING_OFFER) {
                    sessionManager.clearSavedBookingOfferData()
                }
                goHome("onUserAuthenticationComplete: Notification login")
            }
            // Check whether we have already asked enabling biometric
            !appSettings.isAskedToEnabledBiometric -> {
                appSettings.isAskedToEnabledBiometric = true
                // Check if the device has biometric hardware support and has at least one registered
                if (BiometricHandler.hasSystemFeature(this) && biometricHandler.isRegistered) {
                    isFirstTimeLogin = true
                    showEnableBiometricDialog()
                    return
                }
                goHome("onUserAuthenticationComplete: No biometric available")
            }

            else -> goHomeWithBiometricCheck(false)
        }
    }

    private fun showEnableBiometricDialog() {
        showMaterialAlert {
            isCancelable = false
            titleResource = R.string.dialog_title_unlock_with_biometric
            messageResource = R.string.dialog_msg_unlock_with_biometric
            yesButton {
                biometricHandler.authenticate(true, this@LoginActivity)
                it.dismiss()
            }
            noButton {
                appSettings.isBiometricEnabled = false
                it.dismiss()
                goHome("showEnableBiometricDialog: user cancelled")
            }
        }
    }

    override fun goHomeWithBiometricCheck(isBtnPressed: Boolean) {
        if (isBtnPressed) isPasswordLoginSelected = false
        when {
            isPasswordLoginSelected -> goHome("goHomeWithBiometricCheck: password login")
            isBiometricLoginEnabled -> {
                isFirstTimeLogin = false
                biometricHandler.authenticate(false, this)
            }

            else -> goHome("goHomeWithBiometricCheck: general")
        }
    }

    override fun onBiometricAuthenticationSuccess() {
        toast(if (isFirstTimeLogin) R.string.msg_biometric_enabled_successfully else R.string.msg_login_success)
        appSettings.isBiometricEnabled = true
        if (sessionManager.isUserLoggedIn) goHome("onBiometricAuthenticationSuccess: user logged in")
        else {
            skipBiometricCheck = true
            sessionManager.isUserLoggedIn = true
            viewModel.initializeUserSession()
        }
    }

    override fun onBiometricAuthenticationError(errorCode: Int, errString: String?) {
        if (isFinishing) return
        toast(errString.orEmpty().ifEmpty { getString(R.string.err_msg_generic) })
        Timber.d("Biometric error: $errString($errorCode)")
        if (isFirstTimeLogin) {
            biometricHandler.dismiss()
            goHome("onBiometricAuthenticationError: First login")
        }
        // We show `Use Password` as negative button when user is already created a login session
        isPasswordLoginSelected = errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
    }

    private fun goHome(msg: String) {
        Timber.d(msg.uppercase())
        // Show permission screen if required
        if (!appSettings.isPermissionIntroCompleted
            && !Cab9RequiredPermission.isMandatoryPermissionGranted(this)
        ) {
            PermissionCheckActivity.start(this)
            finish()
            return
        }
        // Permissions are taken, skip this screen for next run onwards
        appSettings.isPermissionIntroCompleted = true
        // Go to home screen with bundle if available
        HomeActivity.start(this, intent.extras)
        finish()
    }

}