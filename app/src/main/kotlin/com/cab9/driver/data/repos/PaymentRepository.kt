package com.cab9.driver.data.repos

import android.app.Application
import com.cab9.driver.data.mapper.PaymentCardListMapper
import com.cab9.driver.data.mapper.PaymentListMapper
import com.cab9.driver.data.models.PaymentCardModel
import com.cab9.driver.data.models.PaymentModel
import com.cab9.driver.di.user.LoggedInScope
import com.cab9.driver.ext.DAY_START_TIME
import com.cab9.driver.ext.SERVER_DATE_FORMAT
import com.cab9.driver.ext.ofPattern
import com.cab9.driver.network.apis.Cab9API
import com.cab9.driver.network.apis.NodeAPI
import okhttp3.ResponseBody
import org.threeten.bp.LocalDate
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

interface PaymentRepository {
    suspend fun getPayments(date: LocalDate): List<PaymentModel>

    suspend fun downloadPaymentFile(payment: PaymentModel): File?

    suspend fun getPaymentCards(): List<PaymentCardModel>
}

@LoggedInScope
class PaymentRepositoryImpl @Inject constructor(
    private val context: Application,
    private val nodeService: NodeAPI,
    private val cabService: Cab9API,
    private val listMapper: PaymentListMapper,
    private val paymentCardListMapper: PaymentCardListMapper
) : PaymentRepository {

    override suspend fun getPayments(date: LocalDate): List<PaymentModel> {
        val firstDateOfMonth = date.withDayOfMonth(1)
        val endDateOfMonth = date.withDayOfMonth(date.month.length(date.isLeapYear))
        val strStartDate = firstDateOfMonth.ofPattern(SERVER_DATE_FORMAT).plus(DAY_START_TIME)
        val strEndDate = endDateOfMonth.ofPattern(SERVER_DATE_FORMAT).plus(DAY_START_TIME)
        val result = nodeService.getPayments(strStartDate, strEndDate)
        return listMapper.map(result)
    }

    override suspend fun downloadPaymentFile(payment: PaymentModel): File? {
        val pdfFile = createFile(payment)
        if (pdfFile.exists()) return pdfFile
        val result = cabService.downloadPaymentFile(payment.id)
        return result.body()?.let { savePaymentSummaryDocument(pdfFile, it) }
    }

    private fun savePaymentSummaryDocument(pdfFile: File, body: ResponseBody): File {
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null

        try {
            inputStream = body.byteStream()
            outputStream = FileOutputStream(pdfFile)
            var c: Int
            while (inputStream.read().also { c = it } != -1) outputStream.write(c)
        } catch (ex: Exception) {
            Timber.w(ex)
        } finally {
            inputStream?.close()
            outputStream?.close()
        }

        return pdfFile
    }

    private fun createFile(payment: PaymentModel): File {
        val fileName = "payment_summary_"
            .plus(payment.weekName.replace(" ", "_")).plus("_")
            .plus(payment.weekStartEndDate.replace(" ", "").replace("-", "_"))
            .plus(".pdf")
        //val directory = fileManager.docsDirectory
        //if (!directory.exists()) directory.mkdirs()
        return File(context.filesDir, fileName)
    }

    override suspend fun getPaymentCards(): List<PaymentCardModel> {
        val result = nodeService.getPaymentCards()
        return paymentCardListMapper.map(result)
    }
}