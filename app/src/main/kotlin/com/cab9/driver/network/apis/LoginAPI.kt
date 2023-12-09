package com.cab9.driver.network.apis

import com.cab9.driver.data.models.LoginConfig
import com.cab9.driver.utils.GRANT_TYPE_PASSWORD
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface LoginAPI {

    @FormUrlEncoded
    @POST("Token")
    suspend fun doLogin(
        @Header("X-Tenant") tenantId: String,
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("grant_type") grantType: String = GRANT_TYPE_PASSWORD
    ): LoginConfig

    @POST("api/CustomerApp/ForgotPassword")
    suspend fun resetPassword(@Query("username") userName: String): Response<ResponseBody>
}