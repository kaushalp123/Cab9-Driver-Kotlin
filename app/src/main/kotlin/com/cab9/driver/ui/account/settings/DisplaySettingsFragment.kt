package com.cab9.driver.ui.account.settings

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cab9.driver.R
import com.cab9.driver.databinding.FragmentDisplaySettingsBinding
import com.cab9.driver.settings.AppSettings
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DisplaySettingsFragment : Fragment(R.layout.fragment_display_settings), View.OnClickListener {

    @Inject
    lateinit var appSettings: AppSettings

    private val binding by viewBinding(FragmentDisplaySettingsBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.cardDisplayModeAuto.setOnClickListener(this)
        binding.cardDisplayModeLight.setOnClickListener(this)
        binding.cardDisplayModeDark.setOnClickListener(this)
        updateUIBasedOnSelected(appSettings.displayMode)
    }

    override fun onClick(v: View?) {
        val selectedMode = when (v?.id) {
            R.id.card_display_mode_auto -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            R.id.card_display_mode_light -> AppCompatDelegate.MODE_NIGHT_NO
            R.id.card_display_mode_dark -> AppCompatDelegate.MODE_NIGHT_YES
            else -> throw IllegalStateException("Unknown view click listener")
        }
        AppCompatDelegate.setDefaultNightMode(selectedMode)
        updateUIBasedOnSelected(selectedMode)
    }

    private fun updateUIBasedOnSelected(selectedMode: Int) {
        appSettings.displayMode = selectedMode
        binding.cardDisplayModeAuto.isChecked =
            selectedMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    || selectedMode == AppCompatDelegate.MODE_NIGHT_UNSPECIFIED
        binding.cardDisplayModeLight.isChecked = selectedMode == AppCompatDelegate.MODE_NIGHT_NO
        binding.cardDisplayModeDark.isChecked = selectedMode == AppCompatDelegate.MODE_NIGHT_YES
    }

}