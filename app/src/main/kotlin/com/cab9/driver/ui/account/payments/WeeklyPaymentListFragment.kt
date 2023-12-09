package com.cab9.driver.ui.account.payments

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cab9.driver.R
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.PaymentModel
import com.cab9.driver.databinding.FragmentWeeklyPaymentListBinding
import com.cab9.driver.databinding.ItemPaymentInfoBinding
import com.cab9.driver.databinding.ItemPaymentMonthBinding
import com.cab9.driver.ext.font
import com.cab9.driver.ext.layoutInflater
import com.cab9.driver.ext.textColor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.Month
import org.threeten.bp.YearMonth
import org.threeten.bp.format.TextStyle
import java.util.*
import kotlin.properties.Delegates

private inline fun YearMonth.pastMonths() = arrayListOf<Month>().apply {
    (1..this@pastMonths.monthValue).map { add(Month.of(it)) }
}

private inline fun allMonths() = arrayListOf<Month>().apply {
    (1..12).map { add(Month.of(it)) }
}

@AndroidEntryPoint
class WeeklyPaymentListFragment : Fragment(R.layout.fragment_weekly_payment_list),
    View.OnClickListener {

    private val binding by viewBinding(FragmentWeeklyPaymentListBinding::bind)
    private val viewModel by viewModels<PaymentViewModel>()

    private val monthAdapter: MonthAdapter
        get() = binding.paymentMonthList.adapter as MonthAdapter

    private val weeklyPaymentAdapter: WeeklyPaymentAdapter
        get() = binding.weeklyPaymentList.adapter as WeeklyPaymentAdapter

    private var yearMonth by Delegates.observable(YearMonth.now()) { _, _, newVal ->
        binding.lblPaymentYear.text = newVal.year.toString()
        monthAdapter.months = getMonths(newVal)
        val date = LocalDate.of(newVal.year, MonthAdapter.selectedMonth, LocalDate.now().dayOfMonth)
        viewModel.getPayment(date)
    }

    private val onMonthSelected: (Month) -> Unit = {
        monthAdapter.onMonthSelected(it)
        val date = LocalDate.of(yearMonth.year, it, LocalDate.now().dayOfMonth)
        viewModel.getPayment(date)
    }

    private val onPaymentSelected: (PaymentModel) -> Unit = {
        findNavController().navigate(
            WeeklyPaymentListFragmentDirections
                .actionWeeklyPaymentListFragmentToPaymentSummaryFragment(it)
        )
    }

    init {
        lifecycleScope.launchWhenCreated { getPayments() }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.weeklyPaymentListContainer.onRetryListener { getPayments() }

        binding.imgBtnNextMonth.setOnClickListener(this)
        binding.imgBtnPreviousMonth.setOnClickListener(this)
        binding.lblPaymentYear.text = yearMonth.year.toString()
        binding.paymentMonthList.adapter =
            MonthAdapter(onMonthSelected).apply { months = getMonths(yearMonth) }

        binding.weeklyPaymentList.adapter = WeeklyPaymentAdapter(onPaymentSelected)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.monthlyPaymentOutcome.collectLatest {
                    binding.weeklyPaymentListContainer.setState(it)
                    if (it is Outcome.Success) weeklyPaymentAdapter.submitList(it.data)
                }
            }
        }
    }

    private fun getPayments() {
        val date = LocalDate.of(yearMonth.year, yearMonth.month, LocalDate.now().dayOfMonth)
        viewModel.getPayment(date)
    }

    private fun getMonths(ym: YearMonth): List<Month> {
        return if (ym.year == YearMonth.now().year) {
            ym.pastMonths().reversed()
        } else allMonths().reversed()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.img_btn_next_month -> {
                if (yearMonth.year < YearMonth.now().year) {
                    yearMonth = yearMonth.plusYears(1)
                }
            }

            R.id.img_btn_previous_month -> {
                if (yearMonth.year == YearMonth.now().year) {
                    yearMonth = yearMonth.minusYears(1)
                }
            }
        }
    }

    private class MonthAdapter(
        private val onMonthSelected: (Month) -> Unit
    ) : RecyclerView.Adapter<MonthAdapter.ViewHolder>() {

        companion object {
            var selectedMonth: Month = Month.JANUARY
        }

        var months: List<Month> = emptyList()
            set(value) {
                field = value
                selectedMonth = value.first()
                notifyDataSetChanged()
            }

        fun onMonthSelected(newMonth: Month) {
            val previousSelectedPosition = months.indexOf(selectedMonth)
            val newSelectedPosition = months.indexOf(newMonth)
            selectedMonth = newMonth
            notifyItemChanged(previousSelectedPosition)
            notifyItemChanged(newSelectedPosition)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder.create(parent, onMonthSelected)

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(months[position])
        }

        override fun getItemCount(): Int = months.size

        class ViewHolder(
            private val binding: ItemPaymentMonthBinding,
            private val onMonthSelected: (Month) -> Unit
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(month: Month) {
                binding.root.text = month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)

                if (selectedMonth == month) {
                    binding.root.textColor(R.color.brand_color)
                    binding.root.font(R.font.sf_pro_display_semibold)
                    binding.root.textSize = 18F
                } else {
                    binding.root.textColor(R.color.unselected_month_text_color)
                    binding.root.font(R.font.sf_pro_display_medium)
                    binding.root.textSize = 14F
                }

                binding.root.setOnClickListener { onMonthSelected.invoke(month) }
            }

            companion object {
                fun create(
                    parent: ViewGroup,
                    onMonthSelected: (Month) -> Unit
                ) = ViewHolder(
                    ItemPaymentMonthBinding.inflate(
                        parent.layoutInflater,
                        parent,
                        false
                    ),
                    onMonthSelected
                )
            }
        }
    }

    private class WeeklyPaymentAdapter(private val onPaymentSelected: (PaymentModel) -> Unit) :
        ListAdapter<PaymentModel, WeeklyPaymentAdapter.ViewHolder>(PAYMENT_CALLBACK) {

        companion object {
            private val PAYMENT_CALLBACK = object : DiffUtil.ItemCallback<PaymentModel>() {
                override fun areItemsTheSame(
                    oldItem: PaymentModel,
                    newItem: PaymentModel
                ): Boolean {
                    return oldItem.id == newItem.id
                }

                override fun areContentsTheSame(
                    oldItem: PaymentModel,
                    newItem: PaymentModel
                ): Boolean {
                    return oldItem == newItem
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder.create(parent, onPaymentSelected)

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            getItem(position)?.let { holder.bind(it) }
        }

        class ViewHolder(
            private val binding: ItemPaymentInfoBinding,
            private val onPaymentSelected: (PaymentModel) -> Unit
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(payment: PaymentModel) {
                binding.payment = payment
                binding.root.setOnClickListener { onPaymentSelected.invoke(payment) }
                binding.executePendingBindings()
            }

            companion object {
                fun create(parent: ViewGroup, onPaymentSelected: (PaymentModel) -> Unit) =
                    ViewHolder(
                        ItemPaymentInfoBinding.inflate(parent.layoutInflater, parent, false),
                        onPaymentSelected
                    )
            }
        }

    }
}