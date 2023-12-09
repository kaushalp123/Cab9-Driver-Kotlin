package com.cab9.driver.data.models

import com.cab9.driver.utils.BALANCE_ZERO
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.json.JSONObject

@JsonClass(generateAdapter = true)
data class SumUpTokenResponse(
    @field:Json(name = "access_token") val accessToken: String?,
    @field:Json(name = "token_type") val type: String?,
    @field:Json(name = "expires_in") val expiresIn: String?,
    @field:Json(name = "refresh_token") val refreshToken: String?,
)

@JsonClass(generateAdapter = true)
data class UpdateSumUpPaymentRequest(
    @Transient val status: String = "",
    @field:Json(name = "Amount") val amount: String,
    @field:Json(name = "BookingId") val bookingId: String,
    @field:Json(name = "InCarPaymentDriverIdentifierId") val driverId: String,
    @field:Json(name = "LocalId") val localId: String,
    @field:Json(name = "TransactionInfo") val jsonStrTransactionInfo: String?
)

data class Cab9GoParams(
    val bookingId: String,
    val localId: String,
    val driverId: String,
    val amount: Double
) {

    constructor(jsonObj: JSONObject) : this(
        localId = jsonObj.optString("localId"),
        driverId = jsonObj.optString("driverId"),
        amount = jsonObj.getDouble("amount"),
        bookingId = jsonObj.getString("bookingId"),
    )

    val isValid: Boolean
        get() = bookingId.isNotEmpty() && localId.isNotEmpty() && amount > BALANCE_ZERO
}
