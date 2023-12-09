package com.cab9.driver.network

import com.cab9.driver.BuildConfig
import com.cab9.driver.ext.isCab9GenericApp
import com.cab9.driver.network.apis.LoginAPI
import com.cab9.driver.settings.SessionManager
import com.cab9.driver.utils.AUTHORIZATION
import com.cab9.driver.utils.BEARER
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber

//http://sangsoonam.github.io/2019/03/06/okhttp-how-to-refresh-access-token-efficiently.html
//https://www.lordcodes.com/articles/authorization-of-web-requests-for-okhttp-and-retrofit

private val Response.retryCount: Int
    get() {
        var currentResponse = priorResponse
        var result = 0
        while (currentResponse != null) {
            result++
            currentResponse = currentResponse.priorResponse
        }
        return result
    }

/**
 * Returns a request that includes a credential to satisfy an authentication challenge in response.
 * Returns null if the challenge cannot be satisfied.
 */
class AuthTokenAuthenticator(
    private val loginApi: LoginAPI,
    private val sessionManager: SessionManager
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? = when {
        response.retryCount > 2 -> {
            Timber.e(RefreshAuthTokenException("Token retry exceeds limit, fail efficiently!"))
            null
        }

        else -> validateAccessToken(response.request)
    }

    private fun validateAccessToken(request: Request): Request? {
        return runBlocking {
            val accessToken = getRefreshedAuthToken()
            if (!accessToken.isNullOrEmpty()) {
                newRequestWithAccessToken(request, accessToken)
            } else null
        }
    }

//    private fun isRequestWithAccessToken(response: Response): Boolean {
//        val header = response.request.header(AUTHORIZATION)
//        return header != null && header.startsWith(BEARER)
//    }

    private fun newRequestWithAccessToken(request: Request, accessToken: String): Request {
        return request.newBuilder()
            .header(AUTHORIZATION, "$BEARER $accessToken")
            .build()
    }

    private suspend fun getRefreshedAuthToken(): String? = try {
        Timber.w("Refreshing access token...")
        val response = loginApi.doLogin(
            sessionManager.companyCode.ifEmpty { BuildConfig.TENANT_ID },
            sessionManager.username,
            sessionManager.password
        )
        sessionManager.updateLoginConfig(response)
        sessionManager.authToken
    } catch (ex: Exception) {
        Timber.e(RefreshAuthTokenException(ex))
        null
    }

}