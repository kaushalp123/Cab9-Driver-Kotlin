package com.cab9.driver.network

import com.cab9.driver.base.Outcome
import org.json.JSONObject
import retrofit2.HttpException
import timber.log.Timber

//inline val Throwable.isNetworkError: Boolean
//    get() = this is ConnectException
//            || this is SocketTimeoutException
//            || this is UnknownHostException

private fun HttpException.getApiMessage(): String = try {
    val errorJsonString = response()?.errorBody()?.string()
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
        else ""
    } else ""
} catch (ex: Exception) {
    ""
}

suspend fun <R> safeApiCall(apiCall: suspend () -> R): Outcome<R> = try {
    val response = apiCall.invoke()
    Outcome.success(response)
} catch (ex: Exception) {
    Timber.e(ex)
    when {
        ex is HttpException -> {
            val msg = ex.getApiMessage()
            Outcome.failure(msg, ApiException(msg))
        }

        ex.isNetworkError -> Outcome.failure("No Internet", NoInternetException())
        else -> {
            val msg = ex.localizedMessage.orEmpty().ifEmpty { ex.message.orEmpty() }
            Outcome.failure(msg, ex)
        }
    }
}


