package com.cab9.driver.ui.login

import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import androidx.navigation.fragment.findNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cab9.driver.BuildConfig
import com.cab9.driver.R
import com.cab9.driver.base.BaseFragment
import com.cab9.driver.databinding.FragmentWelcomeBinding

class WelcomeFragment : BaseFragment(R.layout.fragment_welcome), OnClickListener {

    private val binding by viewBinding(FragmentWelcomeBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnLogin.setOnClickListener(this)
        binding.btnSignup.setOnClickListener(this)
        binding.lblPrivacyPolicy.setOnClickListener(this)
        binding.lblTnc.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_login -> findNavController().navigate(WelcomeFragmentDirections.actionWelcomeFragmentToLoginFragment())
            R.id.btn_signup -> findNavController().navigate(WelcomeFragmentDirections.actionWelcomeFragmentToSignupWebFragment())
            R.id.lbl_privacy_policy -> findNavController().navigate(
                WelcomeFragmentDirections.actionWelcomeFragmentToWebViewFragment(
                    BuildConfig.PRIVACY_POLICY_URL, getString(R.string.app_name)
                )
            )

            R.id.lbl_tnc -> findNavController().navigate(
                WelcomeFragmentDirections.actionWelcomeFragmentToWebViewFragment(
                    BuildConfig.TERMS_CONDITIONS_URL, getString(R.string.app_name)
                )
            )
        }
    }

}