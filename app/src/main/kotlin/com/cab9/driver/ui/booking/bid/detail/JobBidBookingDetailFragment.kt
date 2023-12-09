package com.cab9.driver.ui.booking.bid.detail

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.cab9.driver.R
import com.cab9.driver.base.FragmentX
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.BidCategory
import com.cab9.driver.databinding.FragmentJobBidBookingDetailBinding
import com.cab9.driver.ext.localBroadcastManager
import com.cab9.driver.ext.removeSafely
import com.cab9.driver.ext.snack
import com.cab9.driver.ui.booking.adapter.BookingStopAdapter
import com.cab9.driver.ui.booking.adapter.BookingTagAdapter
import com.cab9.driver.ui.booking.bid.JobPoolBidViewModel
import com.cab9.driver.ui.booking.bid.auction.OnSubmitBidAmountListener
import com.cab9.driver.utils.BUNDLE_KEY_ENTERED_BID_AMOUNT
import com.cab9.driver.utils.KEY_BOOKING_ID
import com.cab9.driver.utils.NOTIFICATION_PAYLOAD
import com.cab9.driver.utils.REQUEST_KEY_SUBMIT_AUCTION_BID
import com.cab9.driver.utils.TYPE_CANCEL_BIDDING
import com.cab9.driver.widgets.dialog.okButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber

@AndroidEntryPoint
class JobBidBookingDetailFragment :
    FragmentX<FragmentJobBidBookingDetailBinding>(R.layout.fragment_job_bid_booking_detail),
    JobBidDetailScreenClickListener, OnSubmitBidAmountListener {

    companion object {
        private const val TAG_JOB_BID_AUCTION_DIALOG = "auction_dialog"
    }

    private val args by navArgs<JobBidBookingDetailFragmentArgs>()
    private val viewModel by viewModels<JobBidBookingDetailViewModel>()
    private val jobBidViewModel by activityViewModels<JobPoolBidViewModel>()

    private val jobCancelledBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, data: Intent?) {
            if (data?.action == TYPE_CANCEL_BIDDING) onBidCancelledAlert(data)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewModel = viewModel
        binding.listener = this

        setToolbarTitle("#${args.jobPoolBid.bookingModel.localId}")
        binding.rlJobBidDetail.onRetryListener { viewModel.refreshBidDetail() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.jobBidBookingOutcomeOutcome.collectLatest {
                        if (it is Outcome.Success) {
                            if (it.data.hasViaStops) {
                                binding.listBookingStops.adapter =
                                    BookingStopAdapter(it.data.viaStops.orEmpty())
                            }

                            if (it.data.hasTags) {
                                binding.listBookingTags.adapter =
                                    BookingTagAdapter(it.data.tags.orEmpty())
                            }
                        }
                    }
                }
                launch {
                    viewModel.acceptBidOutcome.collectLatest {
                        if (it is Outcome.Success) {
                            jobBidViewModel.refreshAllBids()
                            findNavController().popBackStack()
                        } else if (it is Outcome.Failure) binding.root.snack(it.msg)
                    }
                }
                launch {
                    viewModel.bookingCommissionOutcome.collectLatest {
                        if (it is Outcome.Success && it.data.second) jobBidViewModel.refreshAllBids()
                    }
                }
            }
        }

        setFragmentResultListener(REQUEST_KEY_SUBMIT_AUCTION_BID) { requestKey, bundle ->
            if (requestKey == REQUEST_KEY_SUBMIT_AUCTION_BID) {
                val enteredAmount = bundle.getDouble(BUNDLE_KEY_ENTERED_BID_AMOUNT)
                viewModel.acceptBid(enteredAmount)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        requireContext().localBroadcastManager.registerReceiver(
            jobCancelledBroadcastReceiver, IntentFilter().apply {
                addAction(TYPE_CANCEL_BIDDING)
            })
    }

    override fun onStop() {
        requireContext().localBroadcastManager.unregisterReceiver(jobCancelledBroadcastReceiver)
        super.onStop()
    }

    override fun onDestroyView() {
        childFragmentManager.removeSafely(TAG_JOB_BID_AUCTION_DIALOG)
        setToolbarTitle("")
        super.onDestroyView()
    }

    override fun onAcceptBid() {
        if (args.bidCategory != BidCategory.SELECTED) {
            if (args.jobPoolBid.isAuctionBooking) {
                findNavController().navigate(
                    JobBidBookingDetailFragmentDirections
                        .actionJobBidBookingDetailFragmentToJobBidAuctionBottomDialogFragment(
                            bookingAmount = args.jobPoolBid.bookingModel.amount.toFloat(),
                            bookingId = args.jobPoolBid.bookingModel.id,
                            bidCategory = args.bidCategory
                        )
                )
            } else viewModel.acceptBid(args.jobPoolBid.bookingModel.amount)
        }
    }

    private fun onBidCancelledAlert(data: Intent) {
        data.getStringExtra(NOTIFICATION_PAYLOAD)?.let { jsonPayload ->
            try {
                val jsonObj = JSONObject(jsonPayload)
                if (jsonObj.has(KEY_BOOKING_ID)) {
                    val bookingId = jsonObj.getString(KEY_BOOKING_ID)
                    if (args.jobPoolBid.bookingModel.id.equals(bookingId, true)) {
                        // This job is now cancelled, invalid the screen
                        showMaterialAlert {
                            isCancelable = false
                            titleResource = R.string.dialog_title_bid_cancelled
                            messageResource = R.string.dialog_msg_job_cancelled
                            okButton {
                                binding.btnAcceptBidDetail.isEnabled = false
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

    override fun onSubmitBidAmount(
        enteredAmount: Double,
        bookingId: String?,
        category: BidCategory?,
        itemPosition: Int?
    ) {
        viewModel.acceptBid(enteredAmount)
    }
}