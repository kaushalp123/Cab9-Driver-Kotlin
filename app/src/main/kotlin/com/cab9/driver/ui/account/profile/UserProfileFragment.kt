package com.cab9.driver.ui.account.profile

import android.os.Bundle
import android.view.View
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import com.cab9.driver.R
import com.cab9.driver.base.FragmentX
import com.cab9.driver.databinding.FragmentProfileBinding
import com.cab9.driver.ext.navigateSafely
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UserProfileFragment : FragmentX<FragmentProfileBinding>(R.layout.fragment_profile),
    ProfileScreenClickListener {

    private val viewModel by hiltNavGraphViewModels<ProfileViewModel>(R.id.nav_grp_profile)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewModel = viewModel
        binding.listener = this

        binding.rlProfile.onRetryListener { viewModel.getUserProfile(true) }
    }

    override fun showPersonalInfoScreen() {
        findNavController().navigateSafely(
            UserProfileFragmentDirections
                .actionUserProfileFragmentToUpdateProfileFragment()
        )
    }

    override fun showAccountInfoScreen() {
        findNavController().navigateSafely(
            UserProfileFragmentDirections
                .actionUserProfileFragmentToAccountInfoFragment()
        )
    }

    override fun showPaymentCardsScreen() {
//        findNavController().navigate(
//            ProfileFragmentDirections.actionProfileFragmentToPaymentCardListFragment()
//        )
    }

    override fun showAboutScreen() {
        findNavController().navigateSafely(
            UserProfileFragmentDirections
                .actionUserProfileFragmentToAboutFragment()
        )
    }
}