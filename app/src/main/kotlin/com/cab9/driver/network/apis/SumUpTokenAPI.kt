package com.cab9.driver.network.apis

import com.cab9.driver.data.models.SumUpTokenResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface SumUpTokenAPI {

    @FormUrlEncoded
    @POST("token")
    suspend fun token(
        @Field("grant_type") grantType: String,
        @Field("client_id") id: String,
        @Field("client_secret") secret: String,
        @Field("username") username: String? = null,
        @Field("password") password: String? = null,
        @Field("refresh_token") refreshToken: String? = null
    ): SumUpTokenResponse
}