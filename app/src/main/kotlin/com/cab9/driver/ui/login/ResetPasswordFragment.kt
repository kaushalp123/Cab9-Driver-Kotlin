package com.cab9.driver.ui.login

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cab9.driver.R
import com.cab9.driver.base.BaseFragment
import com.cab9.driver.base.Outcome
import com.cab9.driver.databinding.FragmentResetPasswordBinding
import com.cab9.driver.ext.*
import com.cab9.driver.widgets.dialog.okButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ResetPasswordFragment : BaseFragment(R.layout.fragment_reset_password) {

    private val binding by viewBinding(FragmentResetPasswordBinding::bind)
    private val viewModel by viewModels<LoginViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.resetPasswordOutcome.collectLatest {
                    binding.btnResetPassword.isEnabled = it !is Outcome.Progress
                    if (it is Outcome.Success) {
                        showMaterialAlert {
                            titleResource = R.string.dialog_title_reset_password
                            messageResource = R.string.dialog_msg_reset_password
                            okButton { d ->
                                d.dismiss()
                                findNavController().popBackStack()
                            }
                        }
                    } else if (it is Outcome.Failure) binding.root.snack(it.msg)
                }
            }
        }

        binding.btnResetPassword.setOnClickListener {
            if (binding.txtForgotPwdUsername.text().isEmpty()) {
                binding.inputForgotPwdUsername.showError(R.string.err_invalid_username)
            } else {
                requireActivity().hideKeyboard()
                binding.inputForgotPwdUsername.hideError()
                viewModel.resetPassword(binding.txtForgotPwdUsername.text())
            }
        }
    }


}