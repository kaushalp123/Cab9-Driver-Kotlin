package com.cab9.driver.ui.login

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cab9.driver.BuildConfig
import com.cab9.driver.R
import com.cab9.driver.base.BaseFragment
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.repos.UserAuth
import com.cab9.driver.databinding.FragmentLoginBinding
import com.cab9.driver.ext.gone
import com.cab9.driver.ext.hideError
import com.cab9.driver.ext.hideKeyboard
import com.cab9.driver.ext.isCab9GenericApp
import com.cab9.driver.ext.keyboardVisibilityChanges
import com.cab9.driver.ext.showError
import com.cab9.driver.ext.text
import com.cab9.driver.ext.visibility
import com.cab9.driver.settings.AppSettings
import com.cab9.driver.settings.BiometricInteractor
import com.cab9.driver.widgets.dialog.okButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : BaseFragment(R.layout.fragment_login), OnClickListener {

    private val binding by viewBinding(FragmentLoginBinding::bind)
    private val loginViewModel by activityViewModels<LoginViewModel>()

    @Inject
    lateinit var appSettings: AppSettings

    private var biometricInteractor: BiometricInteractor? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is BiometricInteractor) {
            biometricInteractor = context
        }
    }

    override fun onDetach() {
        biometricInteractor = null
        super.onDetach()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    binding.rlLoginMain.keyboardVisibilityChanges()
                        .collectLatest { isKeyboardVisible ->
                            binding.grpLoginFooter.visibility(!isKeyboardVisible)
                            if (isKeyboardVisible) binding.btnBiometricLogin.gone()
                            else binding.btnBiometricLogin.visibility(biometricInteractor?.isBiometricLoginEnabled == true)
                        }
                }
                launch {
                    loginViewModel.userAuthLoader.collectLatest { isProcessing(it) }
                }
                launch {
                    loginViewModel.userAuth.collectLatest {
                        if (it is UserAuth.Unauthenticated && !it.errorMsg.isNullOrEmpty()) {
                            showMaterialAlert {
                                titleResource = R.string.title_error
                                message = it.errorMsg
                                okButton { d -> d.dismiss() }
                            }
                        }
                    }
                }
            }
        }

        binding.txtCompanyCode.setText(loginViewModel.companyCode)
        binding.txtLoginUsername.setText(loginViewModel.username)
        //binding.txtLoginPassword.setText(appSettings.password)

        binding.lblTnc.setOnClickListener(this)
        binding.lblPrivacyPolicy.setOnClickListener(this)
        binding.btnResetPassword.setOnClickListener(this)
        binding.btnLogin.setOnClickListener(this)
        binding.btnBiometricLogin.setOnClickListener(this)

        binding.btnBiometricLogin.visibility(biometricInteractor?.isBiometricLoginEnabled == true)
        binding.inputCompanyCode.visibility(isCab9GenericApp())
    }

    private fun isProcessing(isLoading: Boolean) {
        binding.btnLogin.isEnabled = !isLoading
        binding.btnBiometricLogin.isEnabled = !isLoading
        binding.btnResetPassword.isEnabled = !isLoading
        binding.rlLoginMain.setState(if (isLoading) Outcome.loading(asOverlay = true) else Outcome.Empty)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.lbl_tnc -> {
                findNavController().navigate(
                    LoginFragmentDirections.actionLoginFragmentToWebViewFragment(
                        BuildConfig.TERMS_CONDITIONS_URL,
                        getString(R.string.app_name)
                    )
                )
            }

            R.id.lbl_privacy_policy -> {
                findNavController().navigate(
                    LoginFragmentDirections.actionLoginFragmentToWebViewFragment(
                        BuildConfig.PRIVACY_POLICY_URL,
                        getString(R.string.app_name)
                    )
                )
            }

            R.id.btn_reset_password -> {
                findNavController()
                    .navigate(LoginFragmentDirections.actionLoginFragmentToResetPasswordFragment())
            }

            R.id.btn_login -> validateLogin()
            R.id.btn_biometric_login -> biometricInteractor?.goHomeWithBiometricCheck(true)
        }
    }

    private fun validateLogin() {
        requireActivity().hideKeyboard()
        when {
            binding.txtLoginUsername.text().isEmpty() ->
                binding.inputLoginUsername.showError(R.string.err_invalid_username)

            binding.txtLoginPassword.text().isEmpty() ->
                binding.inputLoginPassword.showError(R.string.err_invalid_password)

            !appSettings.isLocationTermsAgreed -> {
                binding.inputLoginUsername.hideError()
                binding.inputLoginPassword.hideError()
                showMaterialAlert {
                    titleResource = R.string.dialog_title_location_access
                    messageResource = R.string.dialog_msg_location_access
                    positiveButton(R.string.action_i_agree) {
                        appSettings.isLocationTermsAgreed = true
                        doLogin()
                        it.dismiss()
                    }
                }
            }

            else -> doLogin()
        }
    }

    private fun doLogin() {
        binding.inputLoginUsername.hideError()
        binding.inputLoginPassword.hideError()
        val companyCode =
            if (isCab9GenericApp()) binding.txtCompanyCode.text() else BuildConfig.TENANT_ID
        loginViewModel.doLogin(
            companyCode,
            binding.txtLoginUsername.text(),
            binding.txtLoginPassword.text()
        )
    }
}