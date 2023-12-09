package com.cab9.driver.sumup

import android.content.Intent
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentActivity
import com.cab9.driver.R
import com.cab9.driver.data.models.Cab9GoParams
import com.cab9.driver.data.models.SumUpDetail
import com.cab9.driver.data.repos.SumUpRepository
import com.cab9.driver.ext.parcelable
import com.cab9.driver.network.SumUpException
import com.sumup.merchant.reader.api.SumUpAPI
import com.sumup.merchant.reader.api.SumUpLogin
import com.sumup.merchant.reader.api.SumUpPayment
import com.sumup.merchant.reader.models.TransactionInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.math.BigDecimal

abstract class SumUpPaymentHandler(
    private val externalScope: CoroutineScope,
    private val params: Cab9GoParams,
    private val sumUpDetail: SumUpDetail,
    private val sumUpRepo: SumUpRepository,
) {

    abstract fun onTransactionComplete(
        message: String?,
        params: Cab9GoParams,
        transactionInfo: TransactionInfo?
    )

    abstract fun showSumUpMessage(@StringRes msgResId: Int)

    /**
     * Create SumUp payment transaction checkout.
     *
     * @param activity calling activity
     */
    fun checkout(activity: FragmentActivity) {
        when {
            params.amount < 1.0 -> showSumUpMessage(R.string.err_sumup_minimum_amount)
            SumUpAPI.isLoggedIn() -> {
                // Create payment checkout
                val payment = SumUpPayment.builder() // mandatory parameters
                    .total(BigDecimal(params.amount)) // minimum 1.00
                    .currency(SumUpPayment.Currency.GBP)
                    .title("Job ${params.localId}")
                    .addAdditionalInfo("BookingId", params.bookingId)
                    .addAdditionalInfo("LocalId", params.localId)
                    .build()
                SumUpAPI.checkout(activity, payment, REQUEST_CODE_PAYMENT)
            }

            else -> fetchAccessToken(activity)
        }
    }

    /**
     * Checks SumUp detail in mobile state and calls SumUp access token API.
     */
    private fun fetchAccessToken(activity: FragmentActivity) {
        externalScope.launch {
            try {
                Timber.d("Refresh sumUp access token...")
                showSumUpMessage(R.string.msg_sumup_start_login)
                val accessToken = sumUpRepo.fetchAccessToken(sumUpDetail)
                if (!accessToken.isNullOrEmpty()) doLogin(activity, accessToken)
                else {
                    Timber.e(SumUpException("Token refresh failed!"))
                    showSumUpMessage(R.string.err_sumup_token_not_found)
                }
            } catch (ex: Exception) {
                Timber.e(SumUpException("Token refresh failed!", ex))
                showSumUpMessage(R.string.err_sumup_token_not_found)
            }
        }
    }

    private fun doLogin(activity: FragmentActivity, accessToken: String) {
        try {
            Timber.d("Do SumUp login...")
            val sumUpLogin = SumUpLogin.builder(sumUpDetail.apiKey.orEmpty())
                .accessToken(accessToken).build()
            SumUpAPI.openLoginActivity(activity, sumUpLogin, REQUEST_CODE_SUMUP_LOGIN)
        } catch (ex: Exception) {
            Timber.e(SumUpException("Login failed!", ex))
            showSumUpMessage(R.string.err_sumup_login_failed)
        }
    }


    /**
     * Add this method inside [FragmentActivity.onActivityResult]
     */
    fun onActivityResult(requestCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_SUMUP_LOGIN -> {
                val extras = data?.extras
                if (extras != null) {
                    val resultCode = extras.getInt(SumUpAPI.Response.RESULT_CODE)
                    val message = extras.getString(SumUpAPI.Response.MESSAGE)
                    Timber.d("SumUp Result code:$resultCode")
                    Timber.d("SumUp Message:$message")
                    showSumUpMessage(R.string.msg_sumup_login_done)
                } else Timber.e(SumUpException("Login bundle is null"))
            }

            REQUEST_CODE_PAYMENT -> {
                val extras = data?.extras
                if (extras != null) {
                    val resultCode = extras.getInt(SumUpAPI.Response.RESULT_CODE)
                    val message = extras.getString(SumUpAPI.Response.MESSAGE)
                    Timber.i("SumUp Result code: $resultCode")
                    Timber.i("SumUp Message: $message")

                    val transactionInfo =
                        extras.parcelable<TransactionInfo>(SumUpAPI.Response.TX_INFO)
                    onTransactionComplete(message, params, transactionInfo)
                } else {
                    Timber.e(SumUpException("Payment bundle is null for ID[${params.localId}]"))
                    onTransactionComplete(null, params, null)
                }
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_PAYMENT = 1001
        private const val REQUEST_CODE_SUMUP_LOGIN = 2002

        const val SUMUP_APP_PACKAGE_NAME = "com.kaching.merchant"

        const val STATUS_FAILED = "FAILED"
        const val STATUS_SUCCESSFUL = "SUCCESSFUL"
        const val STATUS_CANCELLED = "SUCCESSFUL"
        const val STATUS_PENDING = "PENDING"


    }
}