package com.cab9.driver.ui.account.profile

import android.os.Bundle
import android.view.View
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.cab9.driver.R
import com.cab9.driver.base.FragmentX
import com.cab9.driver.base.Outcome
import com.cab9.driver.databinding.FragmentUpdateProfileBinding
import com.cab9.driver.ext.hideKeyboard
import com.cab9.driver.ext.isValidMobileNumber
import com.cab9.driver.ext.showKeyboard
import com.cab9.driver.ext.snack
import com.cab9.driver.ext.text
import com.cab9.driver.ext.visibility
import com.sumup.base.common.extensions.showKeyboard
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UpdateProfileFragment :
    FragmentX<FragmentUpdateProfileBinding>(R.layout.fragment_update_profile) {

    private val viewModel by hiltNavGraphViewModels<ProfileViewModel>(R.id.nav_grp_profile)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewModel = viewModel

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.updateMobileOutcome.collectLatest {
                        binding.btnUpdateMobileNumber.isEnabled = it !is Outcome.Progress
                        binding.btnUpdateMobileNumber.visibility(it !is Outcome.Progress)
                        binding.pBarUpdateMobile.visibility(it is Outcome.Progress)
                        if (it is Outcome.Failure) binding.root.snack(it.msg)
                    }
                }
            }
        }

        binding.btnUpdateMobileNumber.setOnClickListener {
            if (binding.btnUpdateMobileNumber.text == getString(R.string.action_edit)) {
                if (binding.txtMobileNumber.text() == "NA")
                    binding.txtMobileNumber.setText("")
                binding.inputMobileNumber.isEnabled = true
                binding.btnUpdateMobileNumber.setText(R.string.action_save)
                binding.txtMobileNumber.setSelection(binding.txtMobileNumber.length())
                binding.txtMobileNumber.requestFocus()
                binding.txtMobileNumber.showKeyboard()
            } else if (binding.btnUpdateMobileNumber.text == getString(R.string.action_save)) {
                validateAndSaveMobileNumber(binding.txtMobileNumber.text())
            }
        }
    }

    private fun validateAndSaveMobileNumber(newNumber: String) {
        requireActivity().hideKeyboard()
        when {
            newNumber == viewModel.user?.mobile -> {
                binding.root.snack(R.string.err_same_number)
                binding.txtMobileNumber.setText(viewModel.user?.mobile.orEmpty())
                binding.txtMobileNumber.setSelection(binding.txtMobileNumber.length())
            }

            newNumber.isEmpty() || !newNumber.isValidMobileNumber() -> {
                binding.root.snack(R.string.err_invalid_mobile)
                binding.txtMobileNumber.setText(viewModel.user?.mobile.orEmpty())
                binding.txtMobileNumber.setSelection(binding.txtMobileNumber.length())
            }

            else -> {
                binding.btnUpdateMobileNumber.setText(R.string.action_edit)
                binding.inputMobileNumber.isEnabled = false
                viewModel.updateMobileNumber(newNumber)
            }
        }
    }

}