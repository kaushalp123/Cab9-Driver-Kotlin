package com.cab9.driver.network

import com.cab9.driver.BuildConfig
import com.cab9.driver.settings.SessionManager
import com.cab9.driver.utils.AUTHORIZATION
import com.cab9.driver.utils.BEARER
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber

/**
 * Observes, modifies requests going out.
 */
class AuthTokenInterceptor constructor(private val sessionManager: SessionManager) :
    Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        if (request.header(AUTHORIZATION).isNullOrEmpty()) {
            val authToken = sessionManager.authToken
            if (BuildConfig.DEBUG) Timber.d("Authorization: $authToken")
            if (authToken.isNotEmpty()) {
                val headers = request.headers.newBuilder()
                    .add(AUTHORIZATION, "$BEARER $authToken").build()
                request = request.newBuilder().headers(headers).build()
            }
        }

        return chain.proceed(request)
    }
}