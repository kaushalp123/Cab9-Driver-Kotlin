package com.cab9.driver.ui.booking.bid

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageView
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cab9.driver.R
import com.cab9.driver.base.BaseFragment
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.BidCategory
import com.cab9.driver.data.models.JobPoolBidModel
import com.cab9.driver.databinding.FragmentJobBidListBinding
import com.cab9.driver.databinding.ItemBookingBinding
import com.cab9.driver.ext.backgroundColor
import com.cab9.driver.ext.drawable
import com.cab9.driver.ext.gone
import com.cab9.driver.ext.layoutInflater
import com.cab9.driver.ext.navigateSafely
import com.cab9.driver.ext.prefixCurrency
import com.cab9.driver.ext.show
import com.cab9.driver.ext.toLatLng
import com.cab9.driver.ext.visibility
import com.cab9.driver.network.InvalidBidItemException
import com.cab9.driver.ui.home.HomeViewModel
import com.cab9.driver.widgets.dialog.cancelButton
import com.cab9.driver.widgets.paging.ListLoadSateAdapter
import com.google.android.gms.maps.model.LatLng
import com.sumup.designlib.soloui.extensions.text
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class JobPoolBidListFragment : BaseFragment(R.layout.fragment_job_bid_list),
    SwipeRefreshLayout.OnRefreshListener {

    companion object {
        private const val BUNDLE_JOB_POOL_BID_TYPE = "job_pool_bid_type"
        fun newInstance(bidCategory: BidCategory) = JobPoolBidListFragment()
            .apply { arguments = bundleOf(BUNDLE_JOB_POOL_BID_TYPE to bidCategory.name) }
    }

    private val binding by viewBinding(FragmentJobBidListBinding::bind)
    private val homeViewModel by activityViewModels<HomeViewModel>()
    private val viewModel by activityViewModels<JobPoolBidViewModel>()

    private val bidCategory: BidCategory
        get() {
            val strType = requireArguments().getString(BUNDLE_JOB_POOL_BID_TYPE)
            return if (!strType.isNullOrEmpty()) BidCategory.valueOf(strType)
            else throw IllegalArgumentException("Job pool type is missing in arguments!")
        }

    private val currentLatLng: LatLng?
        get() = homeViewModel.currentLocation?.toLatLng()

    private val bidPagingAdapter: JobBidPagingAdapter
        get() = binding.jobPoolBidList.adapter as JobBidPagingAdapter

    private val emptyBidTitle: String
        get() = when (bidCategory) {
            BidCategory.RECENT -> getString(R.string.no_recent)
            BidCategory.NEAREST -> getString(R.string.no_nearby_jobs)
            BidCategory.ARCHIVED -> getString(R.string.no_archived)
            BidCategory.ALL -> getString(R.string.no_new_bids)
            BidCategory.SELECTED -> getString(R.string.no_submitted)
        }

    private val emptyBidSubTitle: String
        get() = when (bidCategory) {
            BidCategory.RECENT -> getString(R.string.msg_recent_job_allocated)
            BidCategory.NEAREST -> getString(R.string.msg_nearby_job_allocated)
            BidCategory.ARCHIVED -> getString(R.string.msg_archived_job_allocated)
            BidCategory.ALL -> getString(R.string.msg_bidding_job_allocated)
            BidCategory.SELECTED -> getString(R.string.msg_submitted_job_allocated)
        }

    private val onBookingSelected: (JobPoolBidModel) -> Unit = {
        findNavController().navigateSafely(
            JobPoolBidFragmentDirections
                .actionNavJobPoolBidsToJobBidBookingDetailFragment(it, bidCategory)
        )
    }

    private val jobBidsPagingDataObserver = Observer<PagingData<JobPoolBidModel>> {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.jobBidSwipeRefresh.isRefreshing = false
            bidPagingAdapter.submitData(it)
        }
    }

    private val onGetDriverCommission: (JobPoolBidModel) -> Unit = { bookingModel ->
        viewModel.getBiddingPrice(bookingModel.bookingModel.id, bidCategory)
    }

    private val onJobBidSwipeListener = object : OnJobBidSwipeListener {
        override fun onSwipeToBid(position: Int) {
            submitBidAfterConfirmationDialog(position)
        }

        override fun onSwipeToArchive(position: Int) {
            bidPagingAdapter.peek(position)?.let { bid ->
                // Auctioning is not allowed, send request directly
                viewModel.archiveBooking(bid.bookingModel.id, bidCategory)
            }
        }

        override fun onSwipeToSubmitFromArchive(position: Int) {
            submitBidAfterConfirmationDialog(position)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.jobBidSwipeRefresh.setOnRefreshListener(this)

        binding.rlJobBid.setResources(
            emptyBidTitle,
            emptyBidSubTitle,
            requireContext().drawable(R.drawable.ic_baseline_bookings)
        )
        binding.rlJobBid.onRetryListener { bidPagingAdapter.retry() }

        val adapter = JobBidPagingAdapter(bidCategory, onBookingSelected, onGetDriverCommission)
        adapter.withLoadStateFooter(ListLoadSateAdapter { bidPagingAdapter.retry() })
        binding.jobPoolBidList.adapter = adapter

        // do not allow swipe action from submitted section.
        if (bidCategory != BidCategory.SELECTED) {
            val swipeDir =
                if (bidCategory == BidCategory.ARCHIVED) ItemTouchHelper.RIGHT
                else ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            val itemCallback =
                ItemSwipeCallback(requireContext(), swipeDir, bidCategory, onJobBidSwipeListener)
            val itemTouchHelper = ItemTouchHelper(itemCallback)
            itemTouchHelper.attachToRecyclerView(binding.jobPoolBidList)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    bidPagingAdapter.loadStateFlow.collectLatest { loadState ->
                        if (binding.jobBidSwipeRefresh.isRefreshing) {
                            binding.jobBidSwipeRefresh.isRefreshing =
                                loadState.refresh !is LoadState.Loading
                        }
                        binding.rlJobBid.setState(loadState, bidPagingAdapter)
                    }
                }
                launch {
                    viewModel.archiveBidOutcome.collectLatest {
                        if (it is Outcome.Success) {
                            showBottomNavSnack(getString(R.string.msg_bid_archived_success))
                        } else if (it is Outcome.Failure) showBottomNavSnack(it.msg)
                    }
                }
                launch {
                    viewModel.acceptBidOutcome.collectLatest {
                        if (it is Outcome.Success) {
                            showBottomNavSnack(getString(R.string.msg_bid_submit_success))
                        } else if (it is Outcome.Failure) showBottomNavSnack(it.msg)
                    }
                }
                launch {
                    if (bidCategory == BidCategory.RECENT) {
                        viewModel.recentJobBids.collectLatest {
                            binding.jobBidSwipeRefresh.isRefreshing = false
                            bidPagingAdapter.submitData(it)
                        }
                    } else if (bidCategory == BidCategory.NEAREST) {
                        viewModel.getNearbyBidsStream(currentLatLng)
                            .collectLatest {
                                binding.jobBidSwipeRefresh.isRefreshing = false
                                bidPagingAdapter.submitData(it)
                            }
                    }
                }
                launch {
                    viewModel.bookingCommissionOutcome.collectLatest {
                        if (it is Outcome.Failure) {
                            showBottomNavSnack(it.msg)
                        }
                    }
                }
            }
        }

        when (bidCategory) {
            BidCategory.ALL -> {
                viewModel.allJobBids.distinctUntilChanged()
                    .observe(viewLifecycleOwner, jobBidsPagingDataObserver)
            }

            BidCategory.ARCHIVED -> {
                viewModel.archivedJobBids.distinctUntilChanged()
                    .observe(viewLifecycleOwner, jobBidsPagingDataObserver)
            }

            BidCategory.SELECTED -> {
                viewModel.submittedJobBids.distinctUntilChanged()
                    .observe(viewLifecycleOwner, jobBidsPagingDataObserver)
            }

            else -> {}
        }
    }

    override fun onRefresh() {
        binding.jobBidSwipeRefresh.isRefreshing = true
        bidPagingAdapter.refresh()
    }

    private fun submitBidAfterConfirmationDialog(position: Int) {
        try {
            if (position < bidPagingAdapter.itemCount) {
                val selectedBid = bidPagingAdapter.peek(position)
                if (selectedBid != null) showConfirmationDialog(selectedBid, position)
                else logBidSwipeActionError("Item at position $position is null")
            } else logBidSwipeActionError("Found invalid item position $position")
        } catch (ex: Exception) {
            Timber.e(InvalidBidItemException(ex))
            showBottomNavSnack(getString(R.string.err_invalid_position))
        }
    }

    private fun logBidSwipeActionError(errorMessage: String) {
        Timber.e(InvalidBidItemException(errorMessage))
        showBottomNavSnack(getString(R.string.err_invalid_position))
    }

    private fun showConfirmationDialog(bid: JobPoolBidModel, position: Int) {
        showMaterialAlert {
            messageResource = R.string.dialog_msg_swipe_confirm
            positiveButton(R.string.action_confirm) {
                if (bid.isAuctionBooking) {
                    // Show bid auction dialog
                    findNavController().navigate(
                        JobPoolBidFragmentDirections
                            .actionNavJobPoolBidsToJobBidAuctionBottomDialogFragment2(
                                bidCategory = bidCategory,
                                bookingAmount = bid.bookingModel.amount.toFloat(),
                                bookingId = bid.bookingModel.id,
                            )
                    )
                } else {
                    // Auctioning is not allowed, send request directly
                    viewModel.acceptBid(
                        bid.bookingModel.id,
                        bid.bookingModel.amount,
                        bidCategory
                    )
                }
                bidPagingAdapter.notifyItemChanged(position)
                it.dismiss()
            }
            cancelButton {
                // Reset item swap animation
                bidPagingAdapter.notifyItemChanged(position)
                it.dismiss()
            }
        }
    }

    private class JobBidPagingAdapter(
        private val bidCategory: BidCategory,
        private val onBookingSelected: (JobPoolBidModel) -> Unit,
        private val onGetDriverCommission: (JobPoolBidModel) -> Unit
    ) : PagingDataAdapter<JobPoolBidModel, JobBidPagingAdapter.ViewHolder>(BID_CALLBACK) {

        companion object {
            private val BID_CALLBACK = object : DiffUtil.ItemCallback<JobPoolBidModel>() {
                override fun areContentsTheSame(
                    oldItem: JobPoolBidModel,
                    newItem: JobPoolBidModel
                ): Boolean {
                    return oldItem.bookingModel == newItem.bookingModel
                }

                override fun areItemsTheSame(
                    oldItem: JobPoolBidModel,
                    newItem: JobPoolBidModel
                ): Boolean {
                    return oldItem.bookingModel.id == newItem.bookingModel.id
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder.create(parent, bidCategory, onBookingSelected, onGetDriverCommission)

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            getItem(position)?.let { holder.bind(it) }
        }

        class ViewHolder(
            private val binding: ItemBookingBinding,
            private val bidCategory: BidCategory,
            private val onBookingSelected: (JobPoolBidModel) -> Unit,
            private val onGetDriverCommission: (JobPoolBidModel) -> Unit
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(bid: JobPoolBidModel) {
                binding.booking = bid.bookingModel
                // Hide start button
                binding.btnStartRide.gone()
                // Show distance only for NEARBY tabs
                if (bidCategory == BidCategory.NEAREST
                    && bid.bookingModel.distance != null
                    && bid.bookingModel.distance > 0.0
                ) {
                    binding.lblBookingOperationalZone.gravity = Gravity.START
                    binding.lblBookingDistance.show()
                    binding.lblBookingDistance.text(
                        binding.root.context.getString(
                            R.string.temp_zone_distance,
                            bid.bookingModel.distance
                        )
                    )
                } else {
                    binding.lblBookingOperationalZone.gravity = Gravity.CENTER
                    binding.lblBookingDistance.gone()
                }

                // Show booking timing based on client settings
                binding.lblBookingTime.text = bid.bookingModel.bookedTime

                if (bid.hasAmount) stopAnimation(binding.imgRefreshAmount)
                binding.imgRefreshAmount.visibility(!bid.hasAmount)
                binding.lblBookingAmount.visibility(bid.hasAmount)

                binding.lblBookingAmount.text =
                    prefixCurrency(binding.root.context, bid.bookingModel.amount)
                //android:text="@{@string/temp_amount_in_currency(booking.amount)}"

                binding.root.setOnClickListener { onBookingSelected.invoke(bid) }
                binding.imgRefreshAmount.setOnClickListener {
                    onGetDriverCommission.invoke(bid)
                    animateRefreshIcon(binding.imgRefreshAmount)
                }

                binding.lblPrebooked.visibility(bid.isPreBooked)
                if (bid.isPreBooked) {
                    binding.clBooking.backgroundColor(R.color.bg_color_prebooked_bid)
                } else binding.clBooking.setBackgroundColor(0)

                binding.executePendingBindings()
            }

            companion object {
                fun create(
                    parent: ViewGroup,
                    bidCategory: BidCategory,
                    onBookingSelected: (JobPoolBidModel) -> Unit,
                    onGetDriverCommission: (JobPoolBidModel) -> Unit
                ) = ViewHolder(
                    binding = ItemBookingBinding.inflate(parent.layoutInflater, parent, false),
                    bidCategory = bidCategory,
                    onBookingSelected = onBookingSelected,
                    onGetDriverCommission = onGetDriverCommission
                )

                fun animateRefreshIcon(imageView: ImageView) {
                    imageView.clearAnimation()
                    val anim = RotateAnimation(
                        0f,
                        359f, Animation.RELATIVE_TO_SELF,
                        0.5f, Animation.RELATIVE_TO_SELF,
                        0.5f
                    )
                    anim.fillAfter = true
                    anim.duration = 1000
                    // once the animation is complete, repeat again until the value is fetched.
                    anim.repeatMode = Animation.RESTART
                    imageView.startAnimation(anim)
                }
            }

            fun stopAnimation(imageView: ImageView) {
                imageView.clearAnimation()
            }
        }
    }
}