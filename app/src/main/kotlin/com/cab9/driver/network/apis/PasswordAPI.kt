package com.cab9.driver.network.apis

import com.cab9.driver.data.models.ChangePasswordRequest
import retrofit2.http.Body
import retrofit2.http.PUT

interface PasswordAPI {

    @PUT("api/Account/ChangePassword")
    suspend fun changePassword(@Body request: ChangePasswordRequest): ChangePasswordRequest.Response
}