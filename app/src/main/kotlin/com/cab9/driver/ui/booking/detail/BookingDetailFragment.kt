package com.cab9.driver.ui.booking.detail

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import com.cab9.driver.R
import com.cab9.driver.base.FragmentX
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.Charge
import com.cab9.driver.data.models.Driver
import com.cab9.driver.data.models.ExpenseModel
import com.cab9.driver.databinding.FragmentBookingDetailBinding
import com.cab9.driver.databinding.ItemCostBreakdownBinding
import com.cab9.driver.databinding.ItemExpenseBinding
import com.cab9.driver.ext.anchorSnack
import com.cab9.driver.ext.layoutInflater
import com.cab9.driver.ext.localBroadcastManager
import com.cab9.driver.ext.removeSafely
import com.cab9.driver.ext.snack
import com.cab9.driver.ext.toLatLng
import com.cab9.driver.ui.booking.adapter.BookingStopAdapter
import com.cab9.driver.ui.booking.adapter.BookingTagAdapter
import com.cab9.driver.ui.booking.sig.SignatureBottomDialogFragment
import com.cab9.driver.ui.home.HomeViewModel
import com.cab9.driver.utils.BUNDLE_KEY_STATUS
import com.cab9.driver.utils.KEY_BOOKING_ID
import com.cab9.driver.utils.NOTIFICATION_BODY
import com.cab9.driver.utils.NOTIFICATION_PAYLOAD
import com.cab9.driver.utils.NOTIFICATION_TITLE
import com.cab9.driver.utils.REQUEST_KEY_REFRESH_UPCOMING
import com.cab9.driver.utils.REQUEST_KEY_UPDATE_EXPENSE
import com.cab9.driver.utils.TYPE_BOOKING_CANCELLED
import com.cab9.driver.utils.TYPE_BOOKING_UNALLOCATED
import com.cab9.driver.widgets.dialog.cancelButton
import com.cab9.driver.widgets.dialog.okButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber

@AndroidEntryPoint
class BookingDetailFragment :
    FragmentX<FragmentBookingDetailBinding>(R.layout.fragment_booking_detail),
    BookingDetailScreenClickListener {

    companion object {
        private const val TAG_SIGNATURE_DIALOG = "tag_signature_dialog"
    }

    private val args by navArgs<BookingDetailFragmentArgs>()
    private val homeViewModel by activityViewModels<HomeViewModel>()
    private val viewModel by viewModels<BookingDetailViewModel>()

    private val expenseAdapter: ExpenseAdapter
        get() = binding.cardBookingExpenses.bookingExpenseList.adapter as ExpenseAdapter

    private val chargesAdapter: ChargesAdapter
        get() = binding.cardCostBreakdown.costBreakdownList.adapter as ChargesAdapter

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, data: Intent?) {
            if (data?.action == TYPE_BOOKING_UNALLOCATED
                || data?.action == TYPE_BOOKING_CANCELLED
            ) onBookingCancelledAlert(data)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewModel = viewModel
        binding.listener = this

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.bookingDetailOutcome.collectLatest {
                        if (it is Outcome.Success) {
                            if (it.data.hasViaStops) {
                                binding.cardBookingDetail.listBookingStops.adapter =
                                    BookingStopAdapter(it.data.viaStops.orEmpty())
                            }
                            if (it.data.hasTags) {
                                binding.cardBookingDetail.listBookingTags.adapter =
                                    BookingTagAdapter(it.data.tags.orEmpty())
                            }
                        }
                    }
                }
                launch {
                    viewModel.bookingDetailOutcome.collectLatest {
                        if (it is Outcome.Success) setToolbarTitle("#${it.data.localId}")
                    }
                }
                launch {
                    viewModel.bookingExpensesOutcome.collectLatest {
                        if (it is Outcome.Success) expenseAdapter.expenses = it.data
                    }
                }
                launch {
                    viewModel.bookingCharges.collectLatest { chargesAdapter.charges = it }
                }
                launch {
                    viewModel.ackBookingOutcome.collect {
                        binding.btnAckBooking.isEnabled = it !is Outcome.Progress
                        if (it is Outcome.Success) {
                            if (it.data.isSuccess == true) {
                                setRefreshUpcomingResult()
                                binding.btnStartRide.anchorSnack(R.string.msg_job_acknowledged)
                            } else binding.btnStartRide.anchorSnack(R.string.err_job_acknowledge)
                        } else if (it is Outcome.Failure) binding.root.snack(it.msg)
                    }
                }
                launch {
                    viewModel.canStartRideOutcome.collectLatest {
                        if (it is Outcome.Success) onStartRideResponse(it.data)
                        else if (it is Outcome.Failure) binding.root.snack(it.msg)
                    }
                }
                launch {
                    viewModel.startBookingOutcome.collect {
                        binding.btnAckBooking.isEnabled = it !is Outcome.Progress
                        binding.btnStartRide.isEnabled = it !is Outcome.Progress
                        if (it is Outcome.Success) {
                            homeViewModel.fetchMobileState()
                            homeViewModel.getUpcomingBookings()
                            findNavController().navigate(
                                BookingDetailFragmentDirections
                                    .actionBookingDetailFragmentToCab9GoWebFragment(args.bookingId)
                            )
                        } else if (it is Outcome.Failure) binding.root.snack(it.msg)
                    }
                }
            }
        }

        binding.rlBookingDetailMain.onRetryListener { viewModel.getBookingDetail(false) }

        binding.cardBookingExpenses.bookingExpenseList.adapter = ExpenseAdapter()
        binding.cardCostBreakdown.costBreakdownList.adapter = ChargesAdapter()

        setFragmentResultListener(REQUEST_KEY_UPDATE_EXPENSE) { requestKey, bundle ->
            if (requestKey == REQUEST_KEY_UPDATE_EXPENSE) {
                if (bundle.containsKey(BUNDLE_KEY_STATUS)) {
                    val isSuccess = bundle.getBoolean(BUNDLE_KEY_STATUS)
                    if (isSuccess) refreshBookingDetailWithDelay()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        requireContext().localBroadcastManager.registerReceiver(
            notificationReceiver,
            IntentFilter().apply {
                addAction(TYPE_BOOKING_UNALLOCATED)
                addAction(TYPE_BOOKING_CANCELLED)
            }
        )
    }

    override fun onStop() {
        requireContext().localBroadcastManager.unregisterReceiver(notificationReceiver)
        super.onStop()
    }

    override fun onDestroyView() {
        setToolbarTitle("")
        // Hide signature view if showing
        childFragmentManager.removeSafely(TAG_SIGNATURE_DIALOG)
        super.onDestroyView()
    }

    override fun addExpense() {
        findNavController().navigate(
            BookingDetailFragmentDirections
                .actionBookingDetailFragmentToAddBookingExpenseFragment(args.bookingId)
        )
    }

    override fun takeSignature() {
        SignatureBottomDialogFragment.newInstance().show(childFragmentManager, TAG_SIGNATURE_DIALOG)
    }

    override fun requestPayment() {
        // TODO: SumUp implementation
    }

    override fun onAcknowledgeBooking() {
        showMaterialAlert {
            titleResource = R.string.dialog_title_ack_booking
            messageResource = R.string.dialog_msg_ack_booking
            cancelButton { it.dismiss() }
            positiveButton(R.string.action_confirm) { viewModel.acknowledgeBooking() }
        }
    }

    override fun canStartRide() {
        val mobileState = homeViewModel.mobileState
        if (mobileState != null) {
            if (mobileState.driverStatus != Driver.Status.OFFLINE
                && mobileState.driverStatus != Driver.Status.ON_BREAK
            ) {
                val currentLocation = homeViewModel.currentLocation
                if (currentLocation != null) {
                    viewModel.canStartRide(currentLocation.toLatLng())
                } else binding.btnStartRide.anchorSnack(R.string.err_start_ride_no_location)
            } else binding.btnStartRide.anchorSnack(R.string.err_start_ride_offline)
        } else binding.btnStartRide.anchorSnack(R.string.err_msg_generic)
    }

    private fun onStartRideResponse(canStart: Boolean) {
        if (canStart) {
            showMaterialAlert {
                titleResource = R.string.dialog_title_are_you_sure
                messageResource = R.string.dialog_msg_start_ride
                cancelButton { it.dismiss() }
                positiveButton(R.string.action_confirm) {
                    it.dismiss()
                    viewModel.startBooking()
                }
            }
        } else binding.btnStartRide.anchorSnack(R.string.msg_start_ride_early)
    }

    private fun onBookingCancelledAlert(data: Intent) {
        data.getStringExtra(NOTIFICATION_PAYLOAD)?.let { jsonPayload ->
            try {
                val jsonObj = JSONObject(jsonPayload)
                if (jsonObj.has(KEY_BOOKING_ID)) {
                    val bookingId = jsonObj.getString(KEY_BOOKING_ID)
                    if (args.bookingId.equals(bookingId, true)) {
                        val dialogTitle =
                            data.getStringExtra(NOTIFICATION_TITLE).orEmpty().ifEmpty {
                                if (data.action == TYPE_BOOKING_CANCELLED) getString(R.string.dialog_title_booking_cancelled)
                                else getString(R.string.dialog_title_booking_unallocated)
                            }
                        val dialogMsg = data.getStringExtra(NOTIFICATION_BODY).orEmpty().ifEmpty {
                            getString(R.string.dialog_msg_booking_cancelled)
                        }
                        binding.btnAckBooking.isEnabled = false
                        binding.btnStartRide.isEnabled = false
                        showMaterialAlert {
                            isCancelable = false
                            title = dialogTitle
                            message = dialogMsg
                            okButton {
                                it.dismiss()
                                findNavController().popBackStack()
                            }
                        }
                    }
                }
            } catch (ex: Exception) {
                Timber.w(ex)
            }
        }
    }

    private fun setRefreshUpcomingResult() {
        setFragmentResult(REQUEST_KEY_REFRESH_UPCOMING, bundleOf(BUNDLE_KEY_STATUS to true))
    }

    private fun refreshBookingDetailWithDelay() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Adding delay since API taking time to update booking detail
            delay(1500)
            viewModel.getBookingDetail(true)
            viewModel.getBookingExpenses()
        }
    }

    private class ExpenseAdapter : RecyclerView.Adapter<ExpenseAdapter.ViewHolder>() {

        var expenses: List<ExpenseModel> = emptyList()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder.create(parent)

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(expenses[position])
        }

        override fun getItemCount(): Int = expenses.size

        class ViewHolder(private val binding: ItemExpenseBinding) :
            RecyclerView.ViewHolder(binding.root) {
            fun bind(expense: ExpenseModel) {
                binding.expense = expense
                binding.executePendingBindings()
            }

            companion object {
                fun create(parent: ViewGroup) =
                    ViewHolder(ItemExpenseBinding.inflate(parent.layoutInflater, parent, false))
            }
        }
    }

    private class ChargesAdapter : RecyclerView.Adapter<ChargesAdapter.ViewHolder>() {

        var charges: List<Charge> = emptyList()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder.create(parent)

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(charges[position])
        }

        override fun getItemCount(): Int = charges.size

        class ViewHolder(private val binding: ItemCostBreakdownBinding) :
            RecyclerView.ViewHolder(binding.root) {
            fun bind(charge: Charge) {
                binding.charge = charge
                binding.executePendingBindings()
            }

            companion object {
                fun create(parent: ViewGroup) =
                    ViewHolder(
                        ItemCostBreakdownBinding.inflate(
                            parent.layoutInflater,
                            parent,
                            false
                        )
                    )
            }
        }
    }
}