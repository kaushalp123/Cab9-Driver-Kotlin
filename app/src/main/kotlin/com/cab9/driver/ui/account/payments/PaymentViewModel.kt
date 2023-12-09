package com.cab9.driver.ui.account.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cab9.driver.base.Outcome
import com.cab9.driver.data.models.PaymentCardModel
import com.cab9.driver.data.models.PaymentModel
import com.cab9.driver.di.user.UserComponentManager
import com.cab9.driver.ext.emitFailureOverlay
import com.cab9.driver.ext.emitLoadingOverlay
import com.cab9.driver.ext.emitSuccess
import com.cab9.driver.ext.failure
import com.cab9.driver.ext.loading
import com.cab9.driver.ext.success
import com.cab9.driver.network.ApiErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val userComponentManager: UserComponentManager,
    private val apiErrorHandler: ApiErrorHandler
) : ViewModel() {

    private val _monthlyPaymentOutcome =
        MutableStateFlow<Outcome<List<PaymentModel>>>(Outcome.Empty)
    val monthlyPaymentOutcome = _monthlyPaymentOutcome.asStateFlow()

    fun getPayment(date: LocalDate) {
        viewModelScope.launch {
            try {
                _monthlyPaymentOutcome.loading()
                val result = userComponentManager.paymentRepo.getPayments(date)
                _monthlyPaymentOutcome.success(result)
            } catch (ex: Exception) {
                _monthlyPaymentOutcome.failure(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

//    private val _paymentDocumentOutcome = MutableStateFlow<Outcome<File?>>(Outcome.Empty)
//    val paymentDocumentOutcome = _paymentDocumentOutcome.asStateFlow()
//
//    fun getPaymentDocument(payment: PaymentModel) {
//        viewModelScope.launch {
//            try {
//                _paymentDocumentOutcome.loadingOverlay()
//                val result = paymentRepo.downloadPaymentFile(payment)
//                _paymentDocumentOutcome.emptySuccess(result)
//            } catch (ex: Exception) {
//                _paymentDocumentOutcome.failureOverlay(apiErrorHandler.errorMessage(ex), ex)
//            }
//        }
//    }

    private val _paymentDocumentOutcome = MutableSharedFlow<Outcome<File?>>(replay = 0)
    val paymentDocumentOutcome: SharedFlow<Outcome<File?>> = _paymentDocumentOutcome

    fun getPaymentDocument(payment: PaymentModel) {
        viewModelScope.launch {
            try {
                _paymentDocumentOutcome.emitLoadingOverlay()
                val result = userComponentManager.paymentRepo.downloadPaymentFile(payment)
                _paymentDocumentOutcome.emitSuccess(result)
            } catch (ex: Exception) {
                _paymentDocumentOutcome.emitFailureOverlay(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }

    private val _paymentCardsOutcome =
        MutableStateFlow<Outcome<List<PaymentCardModel>>>(Outcome.Empty)
    val paymentCardsOutcome = _paymentCardsOutcome.asStateFlow()

    fun getPaymentCards() {
        viewModelScope.launch {
            try {
                _paymentCardsOutcome.loading()
                val result = userComponentManager.paymentRepo.getPaymentCards()
                _paymentCardsOutcome.success(result)
            } catch (ex: Exception) {
                _paymentCardsOutcome.failure(apiErrorHandler.errorMessage(ex), ex)
            }
        }
    }
}