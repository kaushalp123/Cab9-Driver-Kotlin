package com.cab9.driver.network

import android.content.Context
import com.cab9.driver.R
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import retrofit2.HttpException
import timber.log.Timber
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

inline val Throwable.isNetworkError: Boolean
    get() = this is ConnectException
            || this is SocketTimeoutException
            || this is UnknownHostException

@Singleton
class ApiErrorHandler @Inject constructor(@ApplicationContext private val context: Context) {

    fun errorMessage(error: Throwable): String {
        Timber.w(error)
        return when {
            error is HttpException -> getMessage(error)
            error is NoLocationFoundException -> context.getString(R.string.err_current_location)
            error.isNetworkError -> context.getString(R.string.err_msg_check_internet)
            else -> when {
                !error.localizedMessage.isNullOrEmpty() -> error.localizedMessage.orEmpty()
                !error.message.isNullOrEmpty() -> error.message.orEmpty()
                else -> context.getString(R.string.err_msg_generic)
            }
        }
    }

    fun resolveError(error: Throwable): Exception {
        val message = errorMessage(error)
        return Exception(message)
    }

    private fun getMessage(ex: HttpException): String =
        try {
            val errorJsonString = ex.response()?.errorBody()?.string()
            if (!errorJsonString.isNullOrEmpty()) {
                val errorJson = JSONObject(errorJsonString)
                if (errorJson.has("error_description"))
                    errorJson.getString("error_description")
                else if (errorJson.has("MessageDetail"))
                    errorJson.getString("MessageDetail")
                else if (errorJson.has("message"))
                    errorJson.getString("message")
                else if (errorJson.has("ExceptionMessage"))
                    errorJson.getString("ExceptionMessage")
                else if (errorJson.has("Message"))
                    errorJson.getString("Message")
                else context.getString(R.string.err_msg_generic)
            } else context.getString(R.string.err_msg_generic)
        } catch (ex: Exception) {
            context.getString(R.string.err_msg_generic)
        }
}