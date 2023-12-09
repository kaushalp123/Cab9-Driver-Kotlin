package com.cab9.driver.ui.account.profile

import android.os.Bundle
import android.view.View
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.cab9.driver.R
import com.cab9.driver.base.FragmentX
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.ChangePasswordRequest
import com.cab9.driver.databinding.FragmentChangePasswordBinding
import com.cab9.driver.ext.anchorSnack
import com.cab9.driver.ext.hideError
import com.cab9.driver.ext.hideKeyboard
import com.cab9.driver.ext.keyboardVisibilityChanges
import com.cab9.driver.ext.show
import com.cab9.driver.ext.showError
import com.cab9.driver.ext.text
import com.cab9.driver.ext.visibility
import com.cab9.driver.utils.MIN_PASSWORD_LENGTH
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChangePasswordFragment :
    FragmentX<FragmentChangePasswordBinding>(R.layout.fragment_change_password),
    ChangePasswordScreenClickListener {

    private val viewModel by hiltNavGraphViewModels<ProfileViewModel>(R.id.nav_grp_profile)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewModel = viewModel
        binding.listener = this

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    binding.rlMainChangePassword.keyboardVisibilityChanges()
                        .collectLatest { isKeyboardVisible ->
                            binding.btnForgotPassword.visibility(!isKeyboardVisible)
                        }
                }
                launch {
                    viewModel.changePasswordOutcome.collectLatest {
                        if (it is Outcome.Success) {
                            showBottomNavSnack(getString(R.string.msg_password_updated))
                            findNavController().popBackStack()
                        } else if (it is Outcome.Failure) {
                            binding.lblPasswordError.show()
                            binding.lblPasswordError.text = it.msg
                        }
                    }
                }
            }
        }
    }

    override fun onChangePassword() {
        requireActivity().hideKeyboard()
        when {
            binding.txtCurrentPassword.text().isEmpty() ->
                binding.inputCurrentPassword.showError(R.string.err_empty_password)

            binding.txtCurrentPassword.text() != viewModel.currentPassword -> {
                binding.inputCurrentPassword.showError(R.string.err_wrong_current_password)
            }

            binding.txtNewPassword.text().isEmpty() -> {
                binding.inputCurrentPassword.hideError()
                binding.inputNewPassword.showError(R.string.err_empty_password)
            }

            binding.txtNewPassword.text().length < MIN_PASSWORD_LENGTH -> {
                binding.inputCurrentPassword.hideError()
                binding.inputNewPassword.showError(R.string.msg_min_password_length)
            }

            binding.txtConfirmPassword.text().isEmpty() -> {
                binding.inputCurrentPassword.hideError()
                binding.inputNewPassword.hideError()
                binding.inputConfirmPassword.showError(R.string.err_empty_password)
            }

            binding.txtNewPassword.text() != binding.txtConfirmPassword.text() -> {
                binding.inputCurrentPassword.hideError()
                binding.inputNewPassword.hideError()
                binding.inputConfirmPassword.hideError()
                binding.btnSavePassword.anchorSnack(R.string.err_password_mismatch)
            }

            else -> {
                binding.inputCurrentPassword.hideError()
                binding.inputNewPassword.hideError()
                binding.inputConfirmPassword.hideError()

                val request = ChangePasswordRequest(
                    currentPassword = binding.txtCurrentPassword.text(),
                    newPassword = binding.txtNewPassword.text(),
                    newPasswordConfirm = binding.txtConfirmPassword.text()
                )
                viewModel.changePassword(request)
            }
        }
    }

    override fun onForgotPassword() {
        findNavController().navigate(
            ChangePasswordFragmentDirections
                .actionChangePasswordFragmentToResetPasswordFragment2()
        )
    }
}