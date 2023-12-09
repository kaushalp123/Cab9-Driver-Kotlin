package com.cab9.driver.ui.account.settings

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cab9.driver.R
import com.cab9.driver.base.BaseFragment
import com.cab9.driver.databinding.FragmentSettingsBinding
import com.cab9.driver.databinding.ItemAccountMenuBinding
import com.cab9.driver.ext.layoutInflater
import com.cab9.driver.ext.navigateSafely
import com.cab9.driver.settings.BiometricHandler
import com.cab9.driver.ui.account.SettingsOption

class SettingsFragment : BaseFragment(R.layout.fragment_settings) {

    private val binding by viewBinding(FragmentSettingsBinding::bind)

    private val onMenuItemSelected: (SettingsOption) -> Unit = {
        when (it) {
            SettingsOption.DISPLAY ->
                findNavController().navigateSafely(
                    SettingsFragmentDirections
                        .actionSettingsFragmentToDisplaySettingsFragment()
                )

            SettingsOption.VEHICLES ->
                findNavController().navigateSafely(
                    SettingsFragmentDirections
                        .actionSettingsFragmentToChooseVehicleFragment()
                )

            SettingsOption.BIOMETRIC ->
                findNavController().navigateSafely(
                    SettingsFragmentDirections
                        .actionSettingsFragmentToBiometricSettingsFragment()
                )

            SettingsOption.DIAGNOSTICS ->
                findNavController().navigateSafely(
                    SettingsFragmentDirections
                        .actionSettingsFragmentToAppDiagnosticsFragment()
                )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.settingsMenuList.adapter =
            if (BiometricHandler.hasSystemFeature(requireContext())) {
                SettingsItemAdapter(SettingsOption.values().asList(), onMenuItemSelected)
            } else SettingsItemAdapter(SettingsOption.valuesWithoutBiometric(), onMenuItemSelected)
    }

    private class SettingsItemAdapter(
        private val items: List<SettingsOption>,
        private val onMenuItemSelected: (SettingsOption) -> Unit
    ) : RecyclerView.Adapter<SettingsItemAdapter.ViewHolder>() {

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
            private val onSettingsItemSelected: (SettingsOption) -> Unit
        ) : RecyclerView.ViewHolder(binding.root) {
            fun bind(item: SettingsOption) {
                binding.imgMenuItemIcon.setImageResource(item.iconResId)
                binding.lblMenuItemTitle.setText(item.labelResId)
                binding.root.setOnClickListener { onSettingsItemSelected.invoke(item) }
            }
        }

    }
}