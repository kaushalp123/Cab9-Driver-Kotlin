package com.cab9.driver.ui.booking.expense

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.Expense
import com.cab9.driver.di.user.UserComponentManager
import com.cab9.driver.ext.*
import com.cab9.driver.network.ApiErrorHandler
import com.cab9.driver.utils.BUNDLE_KEY_BOOKING_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddBookingExpenseViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val userComponentManager: UserComponentManager,
    private val apiErrorHandler: ApiErrorHandler
) : ViewModel() {

    var selectedExpense: Expense? = null

    private val bookingId: String
        get() = savedStateHandle.get<String>(BUNDLE_KEY_BOOKING_ID)
            ?: throw IllegalArgumentException("Booking id is missing in arguments!")

    private val _expenseTypesOutcome = MutableStateFlow<Outcome<List<Expense>>>(Outcome.Empty)
    val expenseTypesOutcome = _expenseTypesOutcome

    private fun getExpenseTypes() {
        viewModelScope.launch {
            try {
                _expenseTypesOutcome.loading()
                val result = userComponentManager.cab9Repo.getExpenseTypes()
                _expenseTypesOutcome.success(result)
            } catch (ex: Exception) {
                _expenseTypesOutcome.failure(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    private val _addExpenseOutcome = MutableStateFlow<Outcome<Boolean>>(Outcome.Empty)
    val addExpenseOutcome = _addExpenseOutcome

    fun addExpense(expense: Expense) {
        viewModelScope.launch {
            try {
                _addExpenseOutcome.loadingOverlay()
                val result = userComponentManager.bookingRepo.addExpense(bookingId, expense)
                _addExpenseOutcome.success(result)
            } catch (ex: Exception) {
                _addExpenseOutcome.failureOverlay(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    init {
        getExpenseTypes()
    }

}