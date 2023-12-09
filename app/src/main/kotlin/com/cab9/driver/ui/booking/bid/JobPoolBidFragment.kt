package com.cab9.driver.ui.booking.bid

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cab9.driver.R
import com.cab9.driver.data.models.BidCategory
import com.cab9.driver.data.models.JobBidDateRange
import com.cab9.driver.databinding.FragmentJobPoolBidBinding
import com.cab9.driver.databinding.ItemDateBinding
import com.cab9.driver.ext.font
import com.cab9.driver.ext.gone
import com.cab9.driver.ext.layoutInflater
import com.cab9.driver.ext.toLocalDate
import com.cab9.driver.ext.toPattern
import com.cab9.driver.ext.visibility
import com.cab9.driver.ui.home.AppConfigViewModel
import com.cab9.driver.utils.BUNDLE_BID_BOOKING_ID
import com.cab9.driver.utils.BUNDLE_BID_TYPE
import com.cab9.driver.utils.BUNDLE_KEY_ENTERED_BID_AMOUNT
import com.cab9.driver.utils.REQUEST_KEY_SUBMIT_AUCTION_BID
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import java.util.Date


@AndroidEntryPoint
class JobPoolBidFragment : Fragment(R.layout.fragment_job_pool_bid) {

    private val binding by viewBinding(FragmentJobPoolBidBinding::bind)
    private val viewModel by activityViewModels<JobPoolBidViewModel>()
    private val appConfigViewModel by activityViewModels<AppConfigViewModel>()

    private val dateAdapter: DateAdapter
        get() = binding.jobPoolDateList.adapter as DateAdapter

    private val bidCategories: List<BidCategory>
        get() = appConfigViewModel.bidCategoryOutcome.value

    private val jobBidDateRange: JobBidDateRange
        get() = if (binding.jobPoolDateList.isVisible) {
            JobBidDateRange(
                dateAdapter.getSelectedDate().toLocalDate(),
                dateAdapter.getSelectedDate().toLocalDate()
            )
        } else JobBidDateRange(LocalDate.now(), LocalDate.now().plusDays(7))

    private val onDateSelected: (Date, Int) -> Unit = { date, position ->
        dateAdapter.onItemSelected(position)
        val selectedBidType = bidCategories[binding.jobPoolBidTypeTab.selectedTabPosition]
        fetchJobBids(selectedBidType, JobBidDateRange(date.toLocalDate(), date.toLocalDate()))
    }

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            onTabSelected(bidCategories[position])
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    appConfigViewModel.bidCategoryOutcome.collectLatest { bidTabs ->
                        binding.jobPoolBidTypePager.adapter =
                            JobPoolTypeFragmentAdapter(
                                childFragmentManager,
                                viewLifecycleOwner.lifecycle,
                                bidTabs
                            )
                        binding.jobPoolBidTypePager.offscreenPageLimit = 1
                        binding.jobPoolBidTypePager.isUserInputEnabled = false
                        TabLayoutMediator(
                            binding.jobPoolBidTypeTab,
                            binding.jobPoolBidTypePager
                        ) { tab, position ->
                            tab.text = getString(bidTabs[position].labelResId)
                        }.attach()

                        onTabSelected(bidTabs[binding.jobPoolBidTypePager.currentItem])
                    }
                }
            }
        }

        binding.jobPoolDateList.adapter = DateAdapter(viewModel.bidFilterDates, onDateSelected)

        binding.imgJobPoolCalendar.setOnClickListener {
            binding.jobPoolDateList.visibility(!binding.jobPoolDateList.isVisible)
            // Fetch bidding list based on tab selected and date calendar visibility
            if (binding.jobPoolDateList.isVisible) {
                val selectedBid = bidCategories[binding.jobPoolBidTypeTab.selectedTabPosition]
                // show bidding list with data from selected date
                fetchJobBids(selectedBid, jobBidDateRange)
            } else {
                // Reset bidding list with data from next 7 days
                fetchJobBids(BidCategory.ALL, jobBidDateRange)
                fetchJobBids(BidCategory.ARCHIVED, jobBidDateRange)
                fetchJobBids(BidCategory.SELECTED, jobBidDateRange)
            }
        }

        binding.jobPoolBidTypePager.registerOnPageChangeCallback(onPageChangeCallback)

        setFragmentResultListener(REQUEST_KEY_SUBMIT_AUCTION_BID) { requestKey, bundle ->
            if (requestKey == REQUEST_KEY_SUBMIT_AUCTION_BID) {
                val enteredAmount = bundle.getDouble(BUNDLE_KEY_ENTERED_BID_AMOUNT)
                val strBidType = bundle.getString(BUNDLE_BID_TYPE)
                val type = BidCategory.valueOf(strBidType.orEmpty())
                val bookingId = bundle.getString(BUNDLE_BID_BOOKING_ID).orEmpty()
                viewModel.acceptBid(bookingId, enteredAmount, type)
            }
        }
    }

    override fun onStop() {
        binding.jobPoolBidTypePager.adapter = null
        super.onStop()
    }

    override fun onDestroyView() {
        binding.jobPoolBidTypePager.unregisterOnPageChangeCallback(onPageChangeCallback)
        super.onDestroyView()
    }

    private fun onTabSelected(bidCategory: BidCategory) {
        binding.imgJobPoolCalendar.visibility(
            bidCategory == BidCategory.ARCHIVED
                    || bidCategory == BidCategory.ALL
                    || bidCategory == BidCategory.SELECTED
        )

        if (bidCategory == BidCategory.RECENT
            || bidCategory == BidCategory.NEAREST
        ) {
            // Reset data adapter
            dateAdapter.onItemSelected(0)
            binding.jobPoolDateList.gone()
        } else fetchJobBids(bidCategory, jobBidDateRange)
    }

    private fun fetchJobBids(bidCategory: BidCategory, jobBidDateRange: JobBidDateRange) {
        when (bidCategory) {
            BidCategory.ALL -> viewModel.fetchAllJobBids(jobBidDateRange)
            BidCategory.ARCHIVED -> viewModel.fetchArchivedJobBids(jobBidDateRange)
            BidCategory.SELECTED -> viewModel.fetchSubmittedJobBids(jobBidDateRange)
            else -> {}
        }
    }

    private class JobPoolTypeFragmentAdapter(
        manager: FragmentManager,
        lifecycle: Lifecycle,
        private val categories: List<BidCategory>
    ) : FragmentStateAdapter(manager, lifecycle) {

        override fun getItemCount(): Int = categories.size

        override fun createFragment(position: Int): Fragment =
            JobPoolBidListFragment.newInstance(categories[position])
    }

    private class DateAdapter(
        private val dates: List<Date>,
        private val onDateSelected: (Date, Int) -> Unit
    ) : RecyclerView.Adapter<DateAdapter.ViewHolder>() {

        companion object {
            var selectedItemPosition = 0
        }

        fun onItemSelected(newPosition: Int) {
            val oldPosition = selectedItemPosition
            selectedItemPosition = newPosition
            notifyItemChanged(oldPosition)
            notifyItemChanged(newPosition)
        }

        fun getSelectedDate() = dates[selectedItemPosition]

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder.create(parent, onDateSelected)

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(dates[position])
        }

        override fun getItemCount(): Int = dates.size

        class ViewHolder(
            private val binding: ItemDateBinding,
            private val onDateSelected: (Date, Int) -> Unit
        ) : RecyclerView.ViewHolder(binding.root) {
            fun bind(date: Date) {
                binding.root.text = date.toPattern("dd EEE")?.replace(" ", "\n")
                binding.root.isSelected = selectedItemPosition == bindingAdapterPosition
                if (selectedItemPosition == bindingAdapterPosition) {
                    binding.root.font(R.font.sf_pro_display_bold)
                } else binding.root.font(R.font.sf_pro_display_regular)

                binding.root.setOnClickListener {
                    onDateSelected.invoke(date, bindingAdapterPosition)
                }
            }

            companion object {
                fun create(parent: ViewGroup, onDateSelected: (Date, Int) -> Unit) =
                    ViewHolder(
                        ItemDateBinding.inflate(parent.layoutInflater, parent, false),
                        onDateSelected
                    )
            }
        }
    }
}