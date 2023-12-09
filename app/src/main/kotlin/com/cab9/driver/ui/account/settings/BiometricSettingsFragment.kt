package com.cab9.driver.ui.account.settings

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cab9.driver.R
import com.cab9.driver.base.BaseFragment
import com.cab9.driver.databinding.FragmentBiometricSettingsBinding
import com.cab9.driver.di.qualifiers.FragmentBiometricHandler
import com.cab9.driver.ext.toast
import com.cab9.driver.settings.AppSettings
import com.cab9.driver.settings.BiometricHandler
import com.cab9.driver.settings.BiometricHandlerCallback
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class BiometricSettingsFragment : BaseFragment(R.layout.fragment_biometric_settings),
    BiometricHandlerCallback {

    private val binding by viewBinding(FragmentBiometricSettingsBinding::bind)

    @Inject
    lateinit var appSettings: AppSettings

    @Inject
    @FragmentBiometricHandler
    lateinit var biometricHandler: BiometricHandler

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                if (data != null) Timber.d(result.toString())
                if (biometricHandler.isRegistered) biometricHandler.authenticate(true, this)
                else updateDisabledUI()
            } else updateDisabledUI()
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.switchEnableBiometric.isChecked = appSettings.isBiometricEnabled
        binding.switchEnableBiometric.setOnCheckedChangeListener { compoundButton, isChecked ->
            if (compoundButton.isPressed) {
                appSettings.isBiometricEnabled = isChecked
                if (isChecked) {
                    val canAuthenticate = biometricHandler.canAuthenticate()
                    // Check if biometric is enrolled
                    if (canAuthenticate == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
                        startForResult.launch(biometricHandler.createEnrollIntent())
                    } else biometricHandler.authenticate(true, this)
                }
            }
        }
    }

    private fun updateDisabledUI() {
        appSettings.isBiometricEnabled = false
        binding.switchEnableBiometric.isChecked = false
    }

    override fun onBiometricAuthenticationSuccess() {
        appSettings.isBiometricEnabled = true
        requireContext().toast(R.string.msg_biometric_enabled_successfully)
    }

    override fun onBiometricAuthenticationError(errorCode: Int, errString: String?) {
        errString?.let { requireContext().toast(it) }
        if (errorCode == BiometricPrompt.ERROR_USER_CANCELED
            || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
            || errorCode == BiometricPrompt.ERROR_CANCELED
        ) updateDisabledUI()
    }

}