package com.cab9.driver.data.repos

import com.cab9.driver.BuildConfig
import com.cab9.driver.data.models.SumUpDetail
import com.cab9.driver.data.models.UpdateSumUpPaymentRequest
import com.cab9.driver.di.user.LoggedInScope
import com.cab9.driver.network.apis.NodeAPI
import com.cab9.driver.network.apis.SumUpTokenAPI
import com.cab9.driver.settings.SessionManager
import timber.log.Timber
import javax.inject.Inject

interface SumUpRepository {
    suspend fun fetchAccessToken(sumUpDetail: SumUpDetail): String?

    suspend fun updatePaymentStatus(
        isSuccess: Boolean,
        request: UpdateSumUpPaymentRequest
    ): Triple<Boolean, String, String?>
}

@LoggedInScope
class SumUpRepositoryImpl @Inject constructor(
    private val nodeService: NodeAPI,
    private val apiService: SumUpTokenAPI,
    private val sessionManager: SessionManager
) : SumUpRepository {

    companion object {
        private const val GRANT_TYPE_PASSWORD = "password"
        private const val GRANT_TYPE_REFRESH_TOKEN = "refresh_token"
    }

    override suspend fun fetchAccessToken(sumUpDetail: SumUpDetail): String? {
        val refreshToken = sessionManager.sumUpRefreshToken
        val response = if (refreshToken.isNullOrEmpty()) {
            apiService.token(
                grantType = GRANT_TYPE_PASSWORD,
                id = sumUpDetail.clientId.orEmpty(),
                secret = sumUpDetail.clientSecret.orEmpty(),
                username = sumUpDetail.username,
                password = sumUpDetail.password
            )
        } else {
            apiService.token(
                grantType = GRANT_TYPE_REFRESH_TOKEN,
                id = sumUpDetail.clientId.orEmpty(),
                secret = sumUpDetail.clientSecret.orEmpty(),
                refreshToken = refreshToken
            )
        }
        sessionManager.sumUpRefreshToken = response.refreshToken.orEmpty()
        Timber.d("SumUp refresh token saved...")
        return response.accessToken
    }

    override suspend fun updatePaymentStatus(
        isSuccess: Boolean,
        request: UpdateSumUpPaymentRequest
    ): Triple<Boolean, String, String?> {
        val url =
            if (isSuccess) BuildConfig.BASE_NODE_URL.plus("v1.4/tp-api/incar-payments/sumup/success")
            else BuildConfig.BASE_NODE_URL.plus("v1.4/tp-api/incar-payments/sumup/failure")
        val response = nodeService.updateSumUpPaymentStatus(url, request)
        val responseStr = response.body()?.string()
        return Triple(response.isSuccessful, request.status, responseStr)
    }

}