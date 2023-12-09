package com.cab9.driver.ui.home

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.cab9.driver.R
import com.cab9.driver.base.BaseFragment
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.Driver
import com.cab9.driver.data.models.HeatMapInterval
import com.cab9.driver.data.models.MobileState
import com.cab9.driver.data.models.Shift
import com.cab9.driver.data.models.ZoneModelResult
import com.cab9.driver.data.repos.UserAuth
import com.cab9.driver.databinding.FragmentHomeBinding
import com.cab9.driver.di.booking.JobInProgress
import com.cab9.driver.ext.anchorSnack
import com.cab9.driver.ext.drawable
import com.cab9.driver.ext.gone
import com.cab9.driver.ext.hide
import com.cab9.driver.ext.iconStart
import com.cab9.driver.ext.iconTint
import com.cab9.driver.ext.navigateSafely
import com.cab9.driver.ext.removeSafely
import com.cab9.driver.ext.show
import com.cab9.driver.ext.slideInBottom
import com.cab9.driver.ext.slideOutBottom
import com.cab9.driver.ext.toLatLng
import com.cab9.driver.ext.visibility
import com.cab9.driver.settings.SessionManager
import com.cab9.driver.ui.account.Cab9RequiredPermission
import com.cab9.driver.ui.flagdown.CreateFlagDownBooking
import com.cab9.driver.ui.status.ChangeStatusBottomDialogFragment
import com.cab9.driver.ui.status.ShiftEndTimePickerDialog
import com.cab9.driver.ui.zone.ZonalAdapter
import com.cab9.driver.ui.zone.ZoneViewModel
import com.cab9.driver.utils.map.hasLocationPermissions
import com.cab9.driver.utils.setCircularAvatarUrl
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.properties.Delegates


@AndroidEntryPoint
class HomeFragment : BaseFragment(), OnClickListener {

    companion object {
        private const val TAG_STATUS_CHANGE_DIALOG = "toggle_driver_status_dialog"
        private const val TAG_SHIFT_END_TIME_PICKER_DIALOG = "tag_shift_end_time_picker_dialog"
    }

    private val homeViewModel by activityViewModels<HomeViewModel>()
    private val appConfigViewModel by activityViewModels<AppConfigViewModel>()
    private val viewModel by viewModels<ZoneViewModel>()

    // Using this approach to avoid ui freeze when opening
    // from notification when app is in killed
    private var _binding: FragmentHomeBinding? = null
    private val binding: FragmentHomeBinding
        get() = _binding!!

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var onlineTicker: OnlineTicker

    private val currentLocation: Location?
        get() = homeViewModel.currentLocation

    private val zonalAdapter: ZonalAdapter
        get() = binding.viewZonalOverview.listZonalOverview.adapter as ZonalAdapter

    private val flagDownBooking = registerForActivityResult(CreateFlagDownBooking()) { bookingId ->
        if (!bookingId.isNullOrEmpty()) {
            homeViewModel.fetchMobileState()
            findNavController().navigateSafely(
                HomeFragmentDirections
                    .actionNavHomeToCab9GoWebFragment(bookingId)
            )
        }
    }

    private var showZoneOverview by Delegates.observable(false) { _, oldVal, newVal ->
        if (oldVal != newVal) {
            if (newVal) {
                binding.root.slideInBottom(R.id.view_zonal_overview)
                binding.imgToggleZonalOverview.setImageResource(R.drawable.ic_baseline_map_outline_24)
                viewModel.startPollingZonalOverview(currentLocation?.toLatLng())
            } else {
                binding.root.slideOutBottom(R.id.view_zonal_overview)
                binding.imgToggleZonalOverview.setImageResource(R.drawable.ic_baseline_zone_rank_24)
                viewModel.stopZonalPolling()
            }
        }
    }

    private var showHeatmap by Delegates.observable(false) { _, oldVal, newVal ->
        if (oldVal != newVal) {
            if (!newVal) {
                binding.viewHeatmapInterval.root.gone()
                homeView?.onRemoveHeatmap()
            } else {
                binding.viewHeatmapInterval.root.show()
                onHeatMapIntervalClick(R.id.lbl_interval_15_mins)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        homeViewModel.fetchMobileState()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    homeViewModel.userAuth.collectLatest {
                        if (it is UserAuth.Authenticated) {
                            setCircularAvatarUrl(
                                binding.imgUserAvatar,
                                it.driver.imageUrl,
                                it.driver.displayName
                            )
                        }
                    }
                }
                launch {
                    homeViewModel.jobInProgressFlow.collectLatest {
                        // Toggle JobInProgress banner visibility
                        binding.jobInProgressBanner.visibility(it is JobInProgress.Assigned)
                    }
                }
                launch {
                    // Update current location icon based on permission provided
                    homeViewModel.currentLocationOutcome.collect {
                        val iconResId =
                            if (hasLocationPermissions(requireContext()))
                                R.drawable.ic_baseline_my_location_24
                            else R.drawable.ic_baseline_question_mark_24
                        binding.imgCurrentLocation.setImageResource(iconResId)
                    }
                }
                launch {
                    homeViewModel.mobileStateFlow.collect {
                        binding.btnStartShift.isEnabled = it !is Outcome.Progress
                        binding.progressBarMobileState.visibility(it is Outcome.Progress)
                        binding.cardOnlineTimer.isEnabled = it !is Outcome.Progress
                        if (it is Outcome.Success) onMobileStateChanged(it.data)
                    }
                }
                launch {
                    homeViewModel.driveStateChangeOutcome.collect {
                        binding.progressBarMobileState.visibility(it is Outcome.Progress)
                        binding.btnStartShift.isEnabled = it !is Outcome.Progress
                        binding.cardOnlineTimer.isEnabled = it !is Outcome.Progress
                        if (it is Outcome.Success) {
                            // Ask for estimated end time if already not available
                            if (it.data is Shift.Response.Online
                                && it.data.online.estEndTime == null
                            ) showShiftEndTimePickerDialog()
                        } else if (it is Outcome.Failure) showBottomNavSnack(it.msg)
                    }
                }
                launch {
                    viewModel.zonalOverviewOutcome.collectLatest {
                        binding.viewZonalOverview.root.setState(it)
                        if (it is Outcome.Success) {
                            updateOperationZoneListUI(it.data)
                        } else if (it is Outcome.Failure) showBottomNavSnack(it.msg)
                    }
                }
                launch {
                    viewModel.heatMapOutcome.collectLatest {
                        binding.viewHeatmapInterval.pBarHeatmap.visibility(it is Outcome.Progress)
                        // Show heatmap update only when interval selection card is showing
                        if (binding.viewHeatmapInterval.root.isVisible) {
                            if (it is Outcome.Success) {
                                if (it.data.isEmpty()) {
                                    binding.viewHeatmapInterval.root.anchorSnack(R.string.err_no_data)
                                } else homeView?.onHeatmapLoaded(it.data)
                            } else if (it is Outcome.Failure) showBottomNavSnack(it.msg)
                        }
                    }
                }
                launch {
                    onlineTicker.timerFlow.collectLatest {
                        binding.lblOnlineTimer.text = it
                    }
                }
                launch {
                    appConfigViewModel.flagDownConfigOutcome.collectLatest { isEnabled ->
                        binding.btnFlagDown.visibility(isEnabled)
                    }
                }
            }
        }

        binding.viewZonalOverview.listZonalOverview.adapter = ZonalAdapter()

        binding.imgCurrentLocation.setOnClickListener(this)
        binding.cardOnlineTimer.setOnClickListener(this)
        binding.btnStartShift.setOnClickListener(this)
        binding.imgUserAvatar.setOnClickListener(this)
        binding.btnFlagDown.setOnClickListener(this)
        binding.jobInProgressBanner.setOnClickListener(this)
        binding.imgToggleZonalOverview.setOnClickListener(this)
        binding.imgToggleHeatMap.setOnClickListener(this)

        binding.viewHeatmapInterval.lblInterval15Mins.setOnClickListener(this)
        binding.viewHeatmapInterval.lblInterval30Mins.setOnClickListener(this)
        binding.viewHeatmapInterval.lblInterval60Mins.setOnClickListener(this)
        binding.viewHeatmapInterval.lblInterval120Mins.setOnClickListener(this)

        updateHeatmapBasedOnPreviousSelected()
    }

    override fun onStop() {
        showZoneOverview = false
        super.onStop()
    }

    override fun onDestroyView() {
        childFragmentManager.removeSafely(TAG_STATUS_CHANGE_DIALOG)
        childFragmentManager.removeSafely(TAG_SHIFT_END_TIME_PICKER_DIALOG)
        _binding = null
        super.onDestroyView()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.img_current_location -> homeView?.getCurrentLocationWithPermissionCheck()
            R.id.job_in_progress_banner -> {
                val bookingId = homeViewModel.mobileState?.currentBooking?.id
                if (!bookingId.isNullOrEmpty()) {
                    findNavController().navigateSafely(
                        HomeFragmentDirections
                            .actionNavHomeToCab9GoWebFragment(bookingId)
                    )
                }
            }

            R.id.card_online_timer -> showStatusChangeDialog()
            R.id.btn_start_shift -> {
                homeView?.run {
                    if (Cab9RequiredPermission.isMandatoryPermissionGranted(requireContext())) {
                        homeViewModel.changeStatus(Driver.Status.ONLINE)
                    } else startMissingPermissionFlow()
                } ?: homeViewModel.changeStatus(Driver.Status.ONLINE)
            }

            R.id.btn_flag_down -> {
                val latLng = currentLocation?.toLatLng()
                if (latLng != null) flagDownBooking.launch(latLng)
                else showBottomNavSnack(getString(R.string.err_current_location))
            }

            R.id.img_user_avatar -> {
                findNavController().navigateSafely(
                    HomeFragmentDirections
                        .actionNavHomeToAccountMenuFragment()
                )
            }

            R.id.img_toggle_zonal_overview -> {
                showZoneOverview = !binding.viewZonalOverview.root.isVisible
            }

            R.id.img_toggle_heat_map -> {
                showHeatmap = !binding.viewHeatmapInterval.root.isVisible
            }

            R.id.lbl_interval_15_mins, R.id.lbl_interval_30_mins,
            R.id.lbl_interval_60_mins, R.id.lbl_interval_120_mins -> onHeatMapIntervalClick(v.id)
        }
    }

    private fun onHeatMapIntervalClick(viewId: Int) {
        binding.viewHeatmapInterval.lblInterval15Mins.isSelected =
            viewId == R.id.lbl_interval_15_mins
        binding.viewHeatmapInterval.lblInterval30Mins.isSelected =
            viewId == R.id.lbl_interval_30_mins
        binding.viewHeatmapInterval.lblInterval60Mins.isSelected =
            viewId == R.id.lbl_interval_60_mins
        binding.viewHeatmapInterval.lblInterval120Mins.isSelected =
            viewId == R.id.lbl_interval_120_mins

        val interval = when (viewId) {
            R.id.lbl_interval_15_mins -> HeatMapInterval.MIN_15
            R.id.lbl_interval_30_mins -> HeatMapInterval.MIN_30
            R.id.lbl_interval_60_mins -> HeatMapInterval.MIN_60
            R.id.lbl_interval_120_mins -> HeatMapInterval.MIN_120
            else -> null
        }
        if (interval != null) viewModel.fetchHeatMapData(interval)
        else showBottomNavSnack(getString(R.string.err_invalid_interval))
    }

    private fun updateHeatmapBasedOnPreviousSelected() {
        if (homeView?.isHeatmapEnabled == true) {
            binding.viewHeatmapInterval.root.show()
            binding.viewHeatmapInterval.lblInterval15Mins.isSelected =
                viewModel.selectedHeatmapInterval == HeatMapInterval.MIN_15
            binding.viewHeatmapInterval.lblInterval30Mins.isSelected =
                viewModel.selectedHeatmapInterval == HeatMapInterval.MIN_30
            binding.viewHeatmapInterval.lblInterval60Mins.isSelected =
                viewModel.selectedHeatmapInterval == HeatMapInterval.MIN_60
            binding.viewHeatmapInterval.lblInterval120Mins.isSelected =
                viewModel.selectedHeatmapInterval == HeatMapInterval.MIN_120
        } else binding.viewHeatmapInterval.root.hide()
    }

    /**
     * Updates UI based on [MobileState] data.
     */
    private fun onMobileStateChanged(state: MobileState) {
        binding.btnStartShift.visibility(state.driverStatus == Driver.Status.OFFLINE)
        binding.cardOnlineTimer.visibility(state.driverStatus != Driver.Status.OFFLINE)

        if (state.isBreakAllocationActive) {
            binding.lblOnlineTimer.iconStart(R.drawable.ic_taxi_alert_24)
            binding.lblOnlineTimer.iconTint(R.color.icon_tint_allocation_break)
        } else {
            binding.lblOnlineTimer.iconStart(R.drawable.ic_baseline_status_18)
            binding.lblOnlineTimer.iconTint(state.driverStatus.colorResId)
        }

        binding.cardOnlineTimer.visibility(state.driverStatus != Driver.Status.OFFLINE)
    }

    private fun showStatusChangeDialog() {
        childFragmentManager.removeSafely(TAG_STATUS_CHANGE_DIALOG)
        ChangeStatusBottomDialogFragment.newInstance().show(
            childFragmentManager,
            TAG_STATUS_CHANGE_DIALOG
        )
    }

    private fun showShiftEndTimePickerDialog() {
        childFragmentManager.removeSafely(TAG_SHIFT_END_TIME_PICKER_DIALOG)
        ShiftEndTimePickerDialog.newInstance().show(
            childFragmentManager,
            TAG_SHIFT_END_TIME_PICKER_DIALOG
        )
    }

    private fun updateOperationZoneListUI(result: ZoneModelResult?) {
        if (result != null) {
            listOf(
                binding.viewZonalOverview.zoneColumn1,
                binding.viewZonalOverview.zoneColumn2,
                binding.viewZonalOverview.zoneColumn3,
                binding.viewZonalOverview.zoneColumn4
            ).mapIndexed { index, zoneLabelView ->
                if (index < result.columns.size) {
                    zoneLabelView.show()
                    zoneLabelView.name = getString(result.columns[index].nameResId)
                    zoneLabelView.icon = requireContext().drawable(result.columns[index].iconResId)
                } else zoneLabelView.gone()
            }
            zonalAdapter.addNewZones(result.zones)
        } else {
            zonalAdapter.addNewZones(emptyList())
            binding.viewZonalOverview.root.setState(Outcome.Empty)
        }
    }
}