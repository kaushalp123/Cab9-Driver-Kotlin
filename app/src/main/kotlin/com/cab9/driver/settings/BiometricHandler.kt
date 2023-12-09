package com.cab9.driver.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.cab9.driver.R

interface BiometricInteractor {
    val isBiometricLoginEnabled: Boolean
    fun goHomeWithBiometricCheck(isBtnPressed: Boolean)
}

interface BiometricHandlerCallback {
    fun onBiometricAuthenticationSuccess()

    fun onBiometricAuthenticationError(errorCode: Int, errString: String?)

}

class BiometricHandler constructor(
    private val context: Context,
    private val biometricManager: BiometricManager,
    private val callback: BiometricHandlerCallback
) : BiometricPrompt.AuthenticationCallback() {

    companion object {
        const val BIOMETRIC_GENERIC_ERROR = -1

        /**
         * Return true if the device supports either of FEATURE_FINGERPRINT, FEATURE_FACE or FEATURE_IRIS features.
         */
        fun hasSystemFeature(context: Context): Boolean =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
                        || context.packageManager.hasSystemFeature(PackageManager.FEATURE_FACE)
                        || context.packageManager.hasSystemFeature(PackageManager.FEATURE_IRIS)
            } else context.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
    }

    private var biometricPrompt: BiometricPrompt? = null

    // Any biometric (e.g. fingerprint, iris, or face) on the device that meets or exceeds
    // the requirements for Class 2 (formerly Weak), as defined by the Android CDD.
    private val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK

    /**
     * Checks if the user can authenticate with an authenticator that meets the given requirements.
     * This requires at least one of the specified authenticators to be present, enrolled, and available on the device.
     */
    val isRegistered: Boolean
        get() = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

    /**
     * Checks if the user can authenticate with an authenticator that meets the given requirements.
     * This requires at least one of the specified authenticators to be present, enrolled, and available on the device.
     *
     * @return BIOMETRIC_SUCCESS if the user can authenticate with an allowed authenticator.
     * Otherwise, returns BIOMETRIC_STATUS_UNKNOWN or an error code indicating why the user can't authenticate.
     */
    fun canAuthenticate(): Int = biometricManager.canAuthenticate(authenticators)

    /**
     * Constructs a BiometricPrompt, which can be used to prompt the user to authenticate with a
     * biometric such as fingerprint or face. The prompt can be shown to the user by calling
     * authenticate() and persists across device configuration changes by default.
     *
     * @return BiometricPrompt instance
     */
    private fun createBiometricPrompt(activityRef: FragmentActivity): BiometricPrompt {
        val mainExecutor = ContextCompat.getMainExecutor(context)
        return BiometricPrompt(activityRef, mainExecutor, this)
    }

    /**
     * Constructs a BiometricPrompt, which can be used to prompt the user to authenticate with a
     * biometric such as fingerprint or face. The prompt can be shown to the user by calling
     * authenticate() and persists across device configuration changes by default.
     *
     * @return BiometricPrompt instance
     */
    private fun createBiometricPrompt(fragmentRef: Fragment): BiometricPrompt {
        val mainExecutor = ContextCompat.getMainExecutor(context)
        return BiometricPrompt(fragmentRef, mainExecutor, this)
    }

    private fun createBiometricPromptObject(title: String, negativeText: String) =
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setNegativeButtonText(negativeText)
            .setConfirmationRequired(false)
            .build()

    fun authenticate(forConfirmation: Boolean, activityRef: FragmentActivity) {
        val promptInfo = if (forConfirmation) {
            createBiometricPromptObject(
                context.getString(R.string.biometric_dialog_title_confirm),
                context.getString(R.string.action_cancel)
            )
        } else {
            createBiometricPromptObject(
                context.getString(R.string.biometric_dialog_title_login),
                context.getString(R.string.biometric_dialog_negative_text)
            )
        }
        biometricPrompt = createBiometricPrompt(activityRef)
        biometricPrompt?.authenticate(promptInfo)
    }

    fun authenticate(forConfirmation: Boolean, fragmentRef: Fragment) {
        val promptInfo = if (forConfirmation) {
            createBiometricPromptObject(
                context.getString(R.string.biometric_dialog_title_confirm),
                context.getString(R.string.action_cancel)
            )
        } else {
            createBiometricPromptObject(
                context.getString(R.string.biometric_dialog_title_login),
                context.getString(R.string.biometric_dialog_negative_text)
            )
        }
        biometricPrompt = createBiometricPrompt(fragmentRef)
        biometricPrompt?.authenticate(promptInfo)
    }

    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        super.onAuthenticationError(errorCode, errString)
        callback.onBiometricAuthenticationError(errorCode, errString.toString())
    }

    override fun onAuthenticationFailed() {
        super.onAuthenticationFailed()
        callback.onBiometricAuthenticationError(
            BIOMETRIC_GENERIC_ERROR,
            "Biometric authentication failed!"
        )
    }

    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        super.onAuthenticationSucceeded(result)
        callback.onBiometricAuthenticationSuccess()
    }

    /**
     * Create enroll biometric authentication intent.
     */
    fun createEnrollIntent(): Intent {
        // Prompts the user to create credentials that your app accepts.
        val enrollIntent: Intent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL)
            enrollIntent.putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, authenticators)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            enrollIntent = Intent(Settings.ACTION_FINGERPRINT_ENROLL)
        } else enrollIntent = Intent(Settings.ACTION_SECURITY_SETTINGS)
        return enrollIntent
    }

    fun dismiss() {
        biometricPrompt?.cancelAuthentication()
        biometricPrompt = null
    }
}

