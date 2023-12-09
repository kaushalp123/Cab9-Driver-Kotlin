package com.cab9.driver.ui.booking.upcoming

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cab9.driver.R
import com.cab9.driver.base.BaseFragment
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.BookingListModel
import com.cab9.driver.data.models.BookingType
import com.cab9.driver.data.models.Driver
import com.cab9.driver.databinding.FragmentUpcomingBookingsBinding
import com.cab9.driver.databinding.ItemBookingBinding
import com.cab9.driver.ext.layoutInflater
import com.cab9.driver.ext.navigateSafely
import com.cab9.driver.ext.prefixCurrency
import com.cab9.driver.ext.show
import com.cab9.driver.ext.toLatLng
import com.cab9.driver.ui.booking.adapter.BookingListItemCallback
import com.cab9.driver.ui.home.HomeViewModel
import com.cab9.driver.utils.BUNDLE_KEY_STATUS
import com.cab9.driver.utils.REQUEST_KEY_REFRESH_UPCOMING
import com.cab9.driver.widgets.dialog.cancelButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UpcomingBookingsFragment : BaseFragment(R.layout.fragment_upcoming_bookings),
    SwipeRefreshLayout.OnRefreshListener {

    private val binding by viewBinding(FragmentUpcomingBookingsBinding::bind)
    private val homeViewModel by activityViewModels<HomeViewModel>()
    private val viewModel by viewModels<UpcomingBookingViewModel>()

    private var isProcessing: Boolean = false

    private val bookingAdapter: UpcomingBookingAdapter
        get() = binding.upcomingBookingList.adapter as UpcomingBookingAdapter

    private val onBookingSelected: (BookingListModel) -> Unit = {
        findNavController().navigateSafely(
            UpcomingBookingsFragmentDirections
                .actionNavUpcomingBookingsToBookingDetailFragment(it.id, BookingType.UPCOMING)
        )
    }

    private val onStartRide: (BookingListModel, Int) -> Unit = { booking, _ ->
        if (!isProcessing) canStartRideNow(booking.id)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    homeViewModel.upcomingBookingsOutcome.collect {
                        isProcessing = it is Outcome.Progress
                        binding.upcomingBookingListContainer.setState(it)
                        if (it is Outcome.Success) {
                            binding.upcomingBookingSwipeRefresh.isRefreshing = false
                            bookingAdapter.submitList(it.data)
                        } else if (it is Outcome.Failure) {
                            binding.upcomingBookingSwipeRefresh.isRefreshing = false
                        }
                    }
                }
                launch {
                    viewModel.canStartRideOutcome.collectLatest {
                        isProcessing = it is Outcome.Progress
                        binding.upcomingBookingListContainer.isProcessing(isProcessing)
                        if (it is Outcome.Success)
                            onStartRideResponse(it.data.first, it.data.second)
                        else if (it is Outcome.Failure) showBottomNavSnack(it.msg)
                    }
                }
                launch {
                    viewModel.startBookingOutcome.collectLatest {
                        isProcessing = it is Outcome.Progress
                        binding.upcomingBookingListContainer.isProcessing(isProcessing)
                        if (it is Outcome.Success) {
                            homeViewModel.fetchMobileState()
                            homeViewModel.getUpcomingBookings()
                            findNavController().navigateSafely(
                                UpcomingBookingsFragmentDirections
                                    .actionNavUpcomingBookingsToCab9GoWebFragment(it.data.first)
                            )
                        } else if (it is Outcome.Failure) showBottomNavSnack(it.msg)
                    }
                }
            }
        }

        binding.upcomingBookingSwipeRefresh.setOnRefreshListener(this)
        binding.upcomingBookingList.adapter = UpcomingBookingAdapter(onBookingSelected, onStartRide)
        binding.upcomingBookingListContainer.onRetryListener {
            homeViewModel.getUpcomingBookings()
        }

        setFragmentResultListener(REQUEST_KEY_REFRESH_UPCOMING) { requestKey, bundle ->
            if (requestKey == REQUEST_KEY_REFRESH_UPCOMING && bundle.containsKey(BUNDLE_KEY_STATUS)) {
                val isSuccess = bundle.getBoolean(BUNDLE_KEY_STATUS)
                if (isSuccess) homeViewModel.getUpcomingBookings()
            }
        }
    }

    override fun onRefresh() {
        binding.upcomingBookingSwipeRefresh.isRefreshing = true
        homeViewModel.getUpcomingBookings()
    }

    private fun canStartRideNow(bookingId: String) {
        val mobileState = homeViewModel.mobileState
        if (mobileState != null) {
            if (mobileState.driverStatus != Driver.Status.OFFLINE
                && mobileState.driverStatus != Driver.Status.ON_BREAK
            ) {
                val currentLocation = homeViewModel.currentLocation
                if (currentLocation != null) {
                    viewModel.canStartRide(bookingId, currentLocation.toLatLng())
                } else showBottomNavSnack(getString(R.string.err_start_ride_no_location))
            } else showBottomNavSnack(getString(R.string.err_start_ride_offline))
        } else showBottomNavSnack(getString(R.string.err_msg_generic))
    }

    private fun onStartRideResponse(bookingId: String, canStart: Boolean) {
        if (canStart) {
            showMaterialAlert {
                titleResource = R.string.dialog_title_are_you_sure
                messageResource = R.string.dialog_msg_start_ride
                cancelButton { it.dismiss() }
                positiveButton(R.string.action_confirm) {
                    it.dismiss()
                    viewModel.startBooking(bookingId)
                }
            }
        } else showBottomNavSnack(getString(R.string.msg_start_ride_early))
    }

    private class UpcomingBookingAdapter(
        private val onBookingSelected: (BookingListModel) -> Unit,
        private val onStartRide: (BookingListModel, Int) -> Unit
    ) : ListAdapter<BookingListModel, UpcomingBookingAdapter.ViewHolder>(BookingListItemCallback()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder.create(parent, onBookingSelected, onStartRide)

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            getItem(position)?.let { holder.bind(it) }
        }

        class ViewHolder(
            private val binding: ItemBookingBinding,
            private val onBookingSelected: (BookingListModel) -> Unit,
            private val onStartRide: (BookingListModel, Int) -> Unit
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(booking: BookingListModel) {
                binding.booking = booking
                binding.btnStartRide.show()

                binding.lblBookingTime.text = booking.bookedTime
                binding.lblBookingAmount.text = prefixCurrency(binding.root.context, booking.amount)

                binding.btnStartRide.setOnClickListener {
                    onStartRide.invoke(booking, bindingAdapterPosition)
                }

                binding.root.setOnClickListener { onBookingSelected.invoke(booking) }
                binding.executePendingBindings()
            }

            companion object {
                fun create(
                    parent: ViewGroup,
                    onBookingSelected: (BookingListModel) -> Unit,
                    onStartRide: (BookingListModel, Int) -> Unit
                ) =
                    ViewHolder(
                        ItemBookingBinding.inflate(
                            parent.layoutInflater,
                            parent,
                            false
                        ),
                        onBookingSelected, onStartRide
                    )
            }
        }
    }
}