package com.cab9.driver.ui.account

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cab9.driver.BuildConfig
import com.cab9.driver.R
import com.cab9.driver.base.BaseFragment
import com.cab9.driver.data.models.Driver
import com.cab9.driver.databinding.FragmentAccountMenuBinding
import com.cab9.driver.databinding.ItemAccountMenuBinding
import com.cab9.driver.ext.gone
import com.cab9.driver.ext.layoutInflater
import com.cab9.driver.ui.home.HomeViewModel
import com.cab9.driver.utils.IntentFactory
import com.cab9.driver.widgets.dialog.okButton
import com.sumup.merchant.reader.api.SumUpAPI
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AccountMenuFragment : BaseFragment(R.layout.fragment_account_menu) {

    private val binding by viewBinding(FragmentAccountMenuBinding::bind)
    private val homeViewModel by activityViewModels<HomeViewModel>()

    private val onMenuItemSelected: (AccountMenuItem) -> Unit = {
        when (it) {
            AccountMenuItem.PROFILE -> findNavController().navigate(AccountMenuFragmentDirections.actionAccountMenuFragmentToNavGrpProfile())
            AccountMenuItem.SETTINGS -> findNavController().navigate(AccountMenuFragmentDirections.actionAccountMenuFragmentToSettingsFragment())
            AccountMenuItem.FEEDBACK -> {
                val callSign = homeViewModel.mobileState?.callSign.orEmpty()
                IntentFactory.openMailForFeedback(requireContext(), callSign)
            }

            AccountMenuItem.LOGOUT -> logout()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lblAppVersionName.text = getString(R.string.temp_version, BuildConfig.VERSION_NAME)
        binding.accountMenuList.adapter =
            MenuItemAdapter(AccountMenuItem.values().asList(), onMenuItemSelected)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    homeViewModel.userAuth.collectLatest {
                        binding.pBarAccountMenu.gone()
                    }
                }
            }
        }
    }

    private fun logout() {
        homeViewModel.mobileState?.let { mobileState ->
            when (mobileState.driverStatus) {
                Driver.Status.ON_BREAK ->
                    showMaterialAlert {
                        messageResource = R.string.msg_cannot_logout_on_break
                        okButton { it.dismiss() }
                    }

                Driver.Status.ONLINE ->
                    showMaterialAlert {
                        messageResource = R.string.msg_cannot_logout_on_shift
                        okButton { it.dismiss() }
                    }

                Driver.Status.ON_JOB, Driver.Status.CLEARING ->
                    showMaterialAlert {
                        messageResource = R.string.msg_cannot_logout_on_job
                        okButton { it.dismiss() }
                    }

                else -> {
                    binding.pBarAccountMenu.show()
                    homeViewModel.logout()
                }
            }
        }
    }

    private class MenuItemAdapter(
        private val items: List<AccountMenuItem>,
        private val onMenuItemSelected: (AccountMenuItem) -> Unit
    ) : Adapter<MenuItemAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(
                ItemAccountMenuBinding.inflate(parent.layoutInflater, parent, false),
                onMenuItemSelected
            )

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        private class ViewHolder(
            val binding: ItemAccountMenuBinding,
            private val onMenuItemSelected: (AccountMenuItem) -> Unit
        ) : RecyclerView.ViewHolder(binding.root) {
            fun bind(item: AccountMenuItem) {
                binding.imgMenuItemIcon.setImageResource(item.iconResId)
                binding.lblMenuItemTitle.setText(item.labelResId)
                binding.root.setOnClickListener { onMenuItemSelected.invoke(item) }
            }
        }

    }

}