package com.cab9.driver.ui.dialogs

import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cab9.driver.R
import com.cab9.driver.databinding.DialogFragmentNoInternetBinding

class NoInternetDialogFragment : DialogFragment(R.layout.dialog_fragment_no_internet) {

    companion object {
        fun newInstance() = NoInternetDialogFragment()
    }

    private val binding by viewBinding(DialogFragmentNoInternetBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnSettingsInternet.setOnClickListener {

        }
    }

}