package com.cab9.driver.ui.status

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import androidx.core.text.superscript
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cab9.driver.R
import com.cab9.driver.base.Outcome
import com.cab9.driver.base.RoundedCornerBottomSheetDialogFragment
import com.cab9.driver.data.models.AutoDispatchMode
import com.cab9.driver.data.models.Driver
import com.cab9.driver.data.models.MobileState
import com.cab9.driver.databinding.BottomDialogToggleStatusBinding
import com.cab9.driver.ext.DATE_FORMAT_DAY_ONLY
import com.cab9.driver.ext.DATE_FORMAT_FULL_MONTH_NAME
import com.cab9.driver.ext.DATE_FORMAT_FULL_MONTH_NAME_WITH_DAY
import com.cab9.driver.ext.HH_mm
import com.cab9.driver.ext.backgroundColor
import com.cab9.driver.ext.drawable
import com.cab9.driver.ext.font
import com.cab9.driver.ext.formatToSingleDigitDecimal
import com.cab9.driver.ext.formatToTwoDigit
import com.cab9.driver.ext.gone
import com.cab9.driver.ext.ofPattern
import com.cab9.driver.ext.prefixCurrency
import com.cab9.driver.ext.roundDownToMultipleOf
import com.cab9.driver.ext.setSpannedText
import com.cab9.driver.ext.show
import com.cab9.driver.ext.toPattern
import com.cab9.driver.ext.visibility
import com.cab9.driver.ui.home.HomeViewModel
import com.cab9.driver.utils.BALANCE_ZERO
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import timber.log.Timber
import java.util.Date


@AndroidEntryPoint
class ChangeStatusBottomDialogFragment : RoundedCornerBottomSheetDialogFragment(),
    SeekBar.OnSeekBarChangeListener, View.OnClickListener {

    companion object {
        private val DAY_STANDS = arrayOf("th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th")

        private const val TAG_SHIFT_END_TIME_PICKER_DIALOG = "shift_end_time_picker_dialog"
        fun newInstance() = ChangeStatusBottomDialogFragment()
    }

    private val binding by viewBinding(BottomDialogToggleStatusBinding::bind)
    private val homeViewModel by activityViewModels<HomeViewModel>()
    private val viewModel by viewModels<ChangeStatusViewModel>()

    private var seekBarValue = 0

    val determineDayStand: (Int) -> String = { day ->
        val m: Int = day % 100
        DAY_STANDS[if (m in 4..20) 0 else m % 10]
    }

    override val isDraggable: Boolean
        get() = true
    override val isCancelableOnTouch: Boolean
        get() = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = BottomDialogToggleStatusBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.statusChangeSeekbar.setOnSeekBarChangeListener(this)
        binding.lblEndShiftTime.setOnClickListener(this)
        binding.imgAddEndShiftTime.setOnClickListener(this)
        binding.imgSlidePlusIndicator.setOnClickListener(this)
        binding.imgSlideMinusIndicator.setOnClickListener(this);

        binding.sliderDispatchSettings.addOnSliderTouchListener(object :
            Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                // Responds to when slider's touch event is being started
            }

            override fun onStopTrackingTouch(slider: Slider) {
                // Responds to when slider's touch event is being stopped
                if (homeViewModel.mobileState?.autoDispatchMode == AutoDispatchMode.TIME) {
                    val newDispatchTime = slider.value.toInt()
                    if (homeViewModel.mobileState?.currentShift?.maxDispatchTime != newDispatchTime) {
                        Timber.d("Updating new dispatch time -> $newDispatchTime")
                        viewModel.updateDispatchTime(newDispatchTime)
                    }
                } else if (homeViewModel.mobileState?.autoDispatchMode == AutoDispatchMode.DISTANCE) {
                    val newDispatchDistance = slider.value
                    if (homeViewModel.mobileState?.currentShift?.maxDispatchDistance != newDispatchDistance) {
                        Timber.d("Updating new dispatch distance -> $newDispatchDistance")
                        viewModel.updateDispatchDistance(newDispatchDistance)
                    }
                }
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    homeViewModel.mobileStateFlow.collect {
                        if (it is Outcome.Success) updateUIState(it.data)
                    }
                }
                launch {
                    homeViewModel.driveStateChangeOutcome.collect {
                        binding.statusChangeSeekbar.isEnabled = it !is Outcome.Progress
                        binding.sliderDispatchSettings.isEnabled = it !is Outcome.Progress
                    }
                }
                launch {
                    homeViewModel.updateShiftEndTimeOutcome.collectLatest {
                        binding.pBarShiftEndTime.visibility(it is Outcome.Progress)
                        binding.lblEndShiftTime.visibility(it !is Outcome.Progress)
                        if (it is Outcome.Success) {
                            binding.imgAddEndShiftTime.visibility(false)
                            binding.lblEndShiftTime.text =
                                it.data.estEndTime?.ofPattern(HH_mm).orEmpty()
                            homeViewModel.fetchMobileState()
                        }
                    }
                }
                launch {
                    viewModel.autoDispatchSettingsOutcome.collectLatest {
                        binding.pBarUpdateDispatchTime.visibility(it is Outcome.Progress)
                        binding.sliderDispatchSettings.isEnabled = it !is Outcome.Progress
                        if (it is Outcome.Success) homeViewModel.fetchMobileState()
                    }
                }
            }
        }
    }

    private fun updateUIState(state: MobileState) {
        binding.cardBreakAllocation.visibility(state.isBreakAllocationActive)
        if (state.isBreakAllocationActive) {
            binding.lblAllocationBreak.text =
                getString(R.string.temp_msg_allocation_break, state.breakAllocationMinutesLeft)
        }

        val today = Date()
        binding.lblOnlineDate.setSpannedText(buildSpannedString {
            val day = today.toPattern(DATE_FORMAT_DAY_ONLY)
            if (!day.isNullOrEmpty()) {
                append(today.toPattern(DATE_FORMAT_FULL_MONTH_NAME))
                append(" $day")
                scale(0.6F) { superscript { append(determineDayStand(day.toInt())) } }
            } else append(today.toPattern(DATE_FORMAT_FULL_MONTH_NAME_WITH_DAY))
        })

        val totalEarning = state.currentShift?.stats?.earnings ?: BALANCE_ZERO
        binding.lblShiftEarnings.text = prefixCurrency(requireContext(), totalEarning)

        binding.lblCompletedBookingCount.text =
            (state.currentShift?.stats?.bookingCount ?: 0).toString()

        //val startTime = state.currentShift?.startTime
        binding.lblOnlineShiftTime.text = if (state.currentShift?.startTime != null) {
            var duration = Duration.between(state.currentShift.startTime, Instant.now())
            if (duration.isNegative) duration = duration.negated()
            "%02d:%02d".format(duration.toHours(), duration.toMinutes() % 60)
        } else "-"

        val endShitTime = state.currentShift?.estEndTime?.ofPattern(HH_mm).orEmpty()
        binding.lblEndShiftTime.text = endShitTime
        binding.imgAddEndShiftTime.visibility(endShitTime.isEmpty())

        when (state.autoDispatchMode) {
            AutoDispatchMode.TIME -> updateTimeAutoDispatchSettingUI(state)
            AutoDispatchMode.DISTANCE -> updateDistanceAutoDispatchSettingUI(state)
            else -> binding.grpAutoDispatch.gone()
        }

        updateSeekBarBasedOnStatus(state.driverStatus, state)
    }

    private fun updateTimeAutoDispatchSettingUI(state: MobileState) {
        val minDispatchTime = state.autoDispatchMin
        val maxDispatchTime = state.autoDispatchMax

        if (minDispatchTime != null && maxDispatchTime != null) {
            binding.lblDispatchSettingLabel.setSpannedText(buildSpannedString {
                appendLine(getString(R.string.label_auto_dispatch_pickup_time))
                scale(0.8F) { append(getString(R.string.label_in_minutes)) }
            })
            binding.sliderDispatchSettings.valueFrom = minDispatchTime
            binding.sliderDispatchSettings.valueTo = maxDispatchTime
            binding.sliderDispatchSettings.stepSize = AutoDispatchMode.TIME.stepSize

            var userSelectedTime = state.currentShift?.maxDispatchTime?.toFloat()
            if (userSelectedTime != null) {
                if (userSelectedTime > maxDispatchTime) userSelectedTime = maxDispatchTime
                else if (userSelectedTime < minDispatchTime) userSelectedTime = minDispatchTime
                // Update the values for slider
                binding.lblDispatchSettingsValue.text = formatToTwoDigit(userSelectedTime.toInt())
                binding.sliderDispatchSettings.value = userSelectedTime
            } else binding.sliderDispatchSettings.value = state.autoDispatchMin
            binding.grpAutoDispatch.show()
        } else binding.grpAutoDispatch.gone()
    }

    private fun updateDistanceAutoDispatchSettingUI(state: MobileState) {
        val minDispatchDistance = state.autoDispatchMin
        val maxDispatchDistance = state.autoDispatchMax

        if (minDispatchDistance != null && maxDispatchDistance != null) {
            binding.lblDispatchSettingLabel.setSpannedText(buildSpannedString {
                appendLine(getString(R.string.label_auto_dispatch_pickup_distance))
                scale(0.8F) { append(getString(R.string.label_in_miles)) }
            })
            binding.sliderDispatchSettings.valueFrom = minDispatchDistance
            binding.sliderDispatchSettings.valueTo = maxDispatchDistance
            binding.sliderDispatchSettings.stepSize = AutoDispatchMode.DISTANCE.stepSize

            var userSelectedDistance = state.currentShift?.maxDispatchDistance
                ?.roundDownToMultipleOf(AutoDispatchMode.DISTANCE.stepSize)
            if (userSelectedDistance != null) {
                if (userSelectedDistance > maxDispatchDistance)
                    userSelectedDistance = maxDispatchDistance
                else if (userSelectedDistance < minDispatchDistance)
                    userSelectedDistance = minDispatchDistance
                // Update the values for slider
                binding.lblDispatchSettingsValue.text =
                    formatToSingleDigitDecimal(userSelectedDistance)
                binding.sliderDispatchSettings.value = userSelectedDistance
            } else binding.sliderDispatchSettings.value = state.autoDispatchMin
            binding.grpAutoDispatch.show()
        } else binding.grpAutoDispatch.gone()
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (fromUser) seekBarValue = progress
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {}

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        val status = when (seekBarValue) {
            in 0..25 -> Driver.Status.ON_BREAK
            in 26..75 -> Driver.Status.ONLINE
            else -> Driver.Status.OFFLINE
        }

        homeViewModel.mobileState?.let {
            if (it.driverStatus != status) {
                updateSeekBarBasedOnStatus(status, it)
                homeViewModel.changeStatus(status)
                dismiss()
            }
        }
    }

    private fun updateSeekBarBasedOnStatus(status: Driver.Status, state: MobileState) {
        when (status) {
            Driver.Status.ONLINE, Driver.Status.ON_JOB, Driver.Status.CLEARING -> {
                if (state.isBreakAllocationActive) {
                    binding.lblStatusOnlineLabel.text = getString(R.string.label_allocation_break)
                    binding.clStatusChangeSeekBarContainer.backgroundColor(R.color.color_allocation_break)
                } else {
                    binding.lblStatusOnlineLabel.text = getString(R.string.label_you_are_online)
                    binding.clStatusChangeSeekBarContainer.backgroundColor(R.color.color_online_status)
                }

                val drawable = requireContext().drawable(R.drawable.ic_seekbar_online_thumb)
                updateSeekBarThumb(drawable, 50)

                binding.lblStatusBreakLabel.font(R.font.sf_pro_display_regular)
                binding.lblStatusOfflineLabel.font(R.font.sf_pro_display_regular)
                binding.lblStatusOnlineLabel.font(R.font.sf_pro_display_semibold)
                binding.clStatusChangeSeekBarContainer.backgroundColor(R.color.color_online_status)
            }

            Driver.Status.ON_BREAK -> {

                val drawable = requireContext().drawable(R.drawable.ic_seekbar_onbreak_thumb)
                updateSeekBarThumb(drawable, 0)

                binding.lblStatusBreakLabel.font(R.font.sf_pro_display_semibold)
                binding.lblStatusOfflineLabel.font(R.font.sf_pro_display_regular)
                binding.lblStatusOnlineLabel.font(R.font.sf_pro_display_regular)
                binding.clStatusChangeSeekBarContainer.backgroundColor(R.color.color_on_break_status)
            }

            else -> {
                binding.statusChangeSeekbar.progress = 100
                binding.lblStatusBreakLabel.font(R.font.sf_pro_display_regular)
                binding.lblStatusOfflineLabel.font(R.font.sf_pro_display_semibold)
                binding.lblStatusOnlineLabel.font(R.font.sf_pro_display_regular)
                binding.clStatusChangeSeekBarContainer.backgroundColor(R.color.color_offline_status)
            }
        }
    }

    private fun updateSeekBarThumb(drawable: Drawable?, progress: Int) {
        binding.statusChangeSeekbar.apply {
            this.progress = progress
            binding.statusChangeSeekbar.thumb = drawable
            binding.statusChangeSeekbar.thumbOffset = 60
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.imgAddEndShiftTime, R.id.lblEndShiftTime -> openShiftEndTimePicker()
            R.id.img_slide_plus_indicator -> updateAutoDispatchSettings(binding.sliderDispatchSettings.value + binding.sliderDispatchSettings.stepSize)
            R.id.img_slide_minus_indicator -> updateAutoDispatchSettings(binding.sliderDispatchSettings.value - binding.sliderDispatchSettings.stepSize)
        }
    }

    private fun openShiftEndTimePicker() {
        ShiftEndTimePickerDialog.newInstance()
            .show(childFragmentManager, TAG_SHIFT_END_TIME_PICKER_DIALOG)
    }

    private fun updateAutoDispatchSettings(newValue: Float) {
        if (newValue in binding.sliderDispatchSettings.valueFrom..binding.sliderDispatchSettings.valueTo) {
            when (homeViewModel.mobileState?.autoDispatchMode) {
                AutoDispatchMode.TIME -> {
                    if (homeViewModel.mobileState?.currentShift?.maxDispatchTime != newValue.toInt()) {
                        binding.sliderDispatchSettings.value = newValue
                        Timber.d("Updating new dispatch time -> $newValue")
                        viewModel.updateDispatchTime(newValue.toInt())
                    }
                }

                AutoDispatchMode.DISTANCE -> {
                    if (homeViewModel.mobileState?.currentShift?.maxDispatchDistance != newValue) {
                        binding.sliderDispatchSettings.value = newValue
                        Timber.d("Updating new dispatch distance -> $newValue")
                        viewModel.updateDispatchDistance(newValue)
                    }
                }

                else -> {}
            }
        }
    }

}