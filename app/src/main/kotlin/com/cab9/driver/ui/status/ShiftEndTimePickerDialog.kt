package com.cab9.driver.ui.status

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cab9.driver.databinding.DialogFragmentShiftEndTimePickerBinding
import com.cab9.driver.ext.toLocalDateTime
import com.cab9.driver.ui.home.HomeViewModel
import com.github.florent37.singledateandtimepicker.DateHelper
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date

@AndroidEntryPoint
class ShiftEndTimePickerDialog : DialogFragment() {

    companion object {
        fun newInstance() = ShiftEndTimePickerDialog()
    }

    private val binding by viewBinding(DialogFragmentShiftEndTimePickerBinding::bind)
    private val homeViewModel by activityViewModels<HomeViewModel>()

    private val dateHelper = DateHelper()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = DialogFragmentShiftEndTimePickerBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setCancelable(false)
        dialog?.setCanceledOnTouchOutside(false)

        //picker = view.findViewById(R.id.dateTimePicker)
        binding.dateTimePicker.run {
            setDateHelper(dateHelper)
            setCurved(true)
            setVisibleItemCount(7)
            setMustBeOnFuture(true)
            setDefaultDate(Date())
            setIsAmPm(true)
            setDisplayDays(true)
            setDisplayMinutes(true)
            setDisplayHours(true)
        }

        binding.btnSubmitShiftEndTime.setOnClickListener {
            val dateTime = binding.dateTimePicker.date.toLocalDateTime()
            homeViewModel.updateShiftEndTime(dateTime)
            dismissAllowingStateLoss()
        }

        binding.btnCancelShiftEndTime.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }

}