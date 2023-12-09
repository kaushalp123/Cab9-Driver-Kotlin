package com.cab9.driver.ui.booking.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.BookingListModel
import com.cab9.driver.di.user.UserComponentManager
import com.cab9.driver.ext.failure
import com.cab9.driver.ext.loading
import com.cab9.driver.ext.success
import com.cab9.driver.network.ApiErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import javax.inject.Inject

@HiltViewModel
class BookingHistoryViewModel @Inject constructor(
    private val userComponentManager: UserComponentManager,
    private val apiErrorHandler: ApiErrorHandler
) : ViewModel() {

    var selectedDate: LocalDate = LocalDate.now()
        private set

    private val _bookingHistoryOutcome =
        MutableStateFlow<Outcome<List<BookingListModel>>>(Outcome.Empty)
    val bookingHistoryOutcome = _bookingHistoryOutcome

    fun fetchBookingHistory(date: LocalDate) {
        viewModelScope.launch {
            try {
                _bookingHistoryOutcome.loading()
                selectedDate = date
                val result = userComponentManager.bookingRepo.getBookings(date)
                _bookingHistoryOutcome.success(result)
            } catch (ex: Exception) {
                _bookingHistoryOutcome.failure(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    fun retry() {
        fetchBookingHistory(selectedDate)
    }

    init {
        fetchBookingHistory(selectedDate)
    }
}