package com.cab9.driver.ui.booking.expense

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cab9.driver.R
import com.cab9.driver.base.BaseFragment
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.Expense
import com.cab9.driver.databinding.FragmentAddBookingExpenseBinding
import com.cab9.driver.ext.anchorSnack
import com.cab9.driver.ext.text
import com.cab9.driver.utils.BUNDLE_KEY_STATUS
import com.cab9.driver.utils.REQUEST_KEY_UPDATE_EXPENSE
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddBookingExpenseFragment : BaseFragment(R.layout.fragment_add_booking_expense),
    View.OnClickListener {

    private val binding by viewBinding(FragmentAddBookingExpenseBinding::bind)
    private val viewModel by viewModels<AddBookingExpenseViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var adapter = ArrayAdapter<Expense>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            emptyList()
        )
        binding.autoTxtExpenseTypes.setAdapter(adapter)

        binding.btnCancelExpenseAdd.setOnClickListener(this)
        binding.btnConfirmExpense.setOnClickListener(this)

        binding.autoTxtExpenseTypes.setOnItemClickListener { _, _, i, _ ->
            viewModel.selectedExpense = adapter.getItem(i)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.expenseTypesOutcome.collectLatest {
                        binding.root.setState(it)
                        if (it is Outcome.Success) {
                            adapter = ArrayAdapter<Expense>(
                                requireContext(),
                                android.R.layout.simple_dropdown_item_1line,
                                it.data
                            )
                            binding.autoTxtExpenseTypes.setAdapter(adapter)
                        }
                    }
                }
                launch {
                    viewModel.addExpenseOutcome.collectLatest {
                        binding.root.setState(it)
                        if (it is Outcome.Success) {
                            setFragmentResult(
                                REQUEST_KEY_UPDATE_EXPENSE,
                                bundleOf(BUNDLE_KEY_STATUS to it.data)
                            )
                            findNavController().popBackStack()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setToolbarTitle(getString(R.string.title_add_expense))
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_cancel_expense_add -> findNavController().popBackStack()
            R.id.btn_confirm_expense -> {
                when {
                    viewModel.selectedExpense == null -> binding.btnConfirmExpense.anchorSnack(R.string.err_select_expense_type)
                    binding.txtBookingExpenseAmount.text().isEmpty() ->
                        binding.btnConfirmExpense.anchorSnack(R.string.err_enter_expense_amount)
                    else -> {
                        val newExpense = Expense(
                            id = viewModel.selectedExpense?.id,
                            name = viewModel.selectedExpense?.name,
                            note = binding.txtBookingExpenseNote.text(),
                            amount = binding.txtBookingExpenseAmount.text().toDoubleOrNull(),
                            isApproved = null
                        )
                        viewModel.addExpense(newExpense)
                    }
                }
            }
        }
    }
}