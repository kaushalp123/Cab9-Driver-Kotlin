package com.cab9.driver.ui.booking.history

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cab9.driver.R
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.BookingListModel
import com.cab9.driver.data.models.BookingType
import com.cab9.driver.databinding.FragmentBookingHistoryBinding
import com.cab9.driver.databinding.ItemBookingBinding
import com.cab9.driver.ext.*
import com.cab9.driver.ui.booking.adapter.BookingListItemCallback
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class BookingHistoryFragment : Fragment(R.layout.fragment_booking_history), MenuProvider {

    companion object {
        private const val SUBTITLE_DATE_PATTERN = "dd MMM yyyy"
        private const val TAG_DATE_PICKER = "date_picker_dialog"
    }

    private val binding by viewBinding(FragmentBookingHistoryBinding::bind)
    private val viewModel by viewModels<BookingHistoryViewModel>()

    private var datePicker: MaterialDatePicker<Long>? = null

    private val bookingAdapter: BookingHistoryAdapter
        get() = binding.historyBookingList.adapter as BookingHistoryAdapter

    private val onBookingSelected: (BookingListModel) -> Unit = { booking ->
        findNavController().navigate(
            BookingHistoryFragmentDirections
                .actionBookingHistoryFragmentToBookingDetailFragment(
                    booking.id,
                    BookingType.HISTORY
                )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setSubTitle(viewModel.selectedDate.ofPattern(SUBTITLE_DATE_PATTERN).orEmpty())
        requireActivity().addMenuProvider(this, viewLifecycleOwner)

        binding.historyBookingList.adapter = BookingHistoryAdapter(onBookingSelected)
        binding.root.onRetryListener { viewModel.retry() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.bookingHistoryOutcome.collect {
                    binding.upcomingBookingListContainer.setState(it)
                    if (it is Outcome.Success) bookingAdapter.submitList(it.data)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setTitle(getString(R.string.title_history))
    }

    override fun onPause() {
        setTitle("")
        setSubTitle("")
        super.onPause()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_history, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.action_select_date -> {
                showDatePicker()
                return true
            }
        }
        return false
    }

    private fun setSubTitle(subtitle: String) {
        (activity as? AppCompatActivity)?.supportActionBar?.subtitle = subtitle
    }

    private fun setTitle(title: String) {
        (activity as? AppCompatActivity)?.supportActionBar?.title = title
    }

    private fun showDatePicker() {
        if (datePicker == null) {
            datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.dialog_title_select_date)
                .build()
            datePicker?.addOnPositiveButtonClickListener {
                val selectedDate = Calendar.getInstance().apply { timeInMillis = it }.toLocalDate()
                viewModel.fetchBookingHistory(selectedDate)
                setSubTitle(selectedDate.toPattern(SUBTITLE_DATE_PATTERN).orEmpty())
            }
        }

        datePicker?.show(childFragmentManager, TAG_DATE_PICKER)
    }

    class BookingHistoryAdapter(private val onBookingSelected: (BookingListModel) -> Unit) :
        ListAdapter<BookingListModel, BookingHistoryAdapter.ViewHolder>(BookingListItemCallback()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder.create(parent, onBookingSelected)

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            getItem(position)?.let { holder.bind(it) }
        }

        class ViewHolder(
            private val binding: ItemBookingBinding,
            private val onBookingSelected: (BookingListModel) -> Unit,
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(booking: BookingListModel) {
                binding.booking = booking
                binding.btnStartRide.gone()

                binding.lblBookingTime.text = booking.bookedExactTime
                binding.lblBookingAmount.text = prefixCurrency(binding.root.context,booking.amount)

                binding.root.setOnClickListener { onBookingSelected.invoke(booking) }

                binding.executePendingBindings()
            }

            companion object {
                fun create(
                    parent: ViewGroup,
                    onBookingSelected: (BookingListModel) -> Unit
                ) =
                    ViewHolder(

                        ItemBookingBinding.inflate(
                            parent.layoutInflater,
                            parent,
                            false
                        ),
                        onBookingSelected
                    )
            }
        }
    }
}