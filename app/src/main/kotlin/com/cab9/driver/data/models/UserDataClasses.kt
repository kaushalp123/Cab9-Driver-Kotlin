package com.cab9.driver.data.models

import android.os.Parcelable
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.cab9.driver.R
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Parcelize
data class DriverModel(
    val id: String,
    val callSign: String,
    val firstName: String,
    val lastName: String,
    val mobile: String,
    val imageUrl: String,
    val status: Driver.Status,
    val email: String,
    val address1: String,
    val address2: String,
    val townCity: String,
    val postcode: String,
    val country: String,
) : Parcelable

@JsonClass(generateAdapter = true)
data class Driver(
    @field:Json(name = "Id") val id: String?,
    @field:Json(name = "Callsign") val callSign: String?,
    @field:Json(name = "Firstname") val firstName: String?,
    @field:Json(name = "Surname") val lastName: String?,
    @field:Json(name = "Mobile") val mobile: String?,
    @field:Json(name = "ImageUrl") val imageUrl: String?,
    @field:Json(name = "DriverStatus") val status: Status?,
    @field:Json(name = "Email") val email: String?,
    @field:Json(name = "Address1") val address1: String?,
    @field:Json(name = "Address2") val address2: String?,
    @field:Json(name = "TownCity") val townCity: String?,
    @field:Json(name = "Postcode") val postcode: String?,
    @field:Json(name = "Country") val country: String?,
    @field:Json(name = "BankAC") val bankAccountNo: String?,
    @field:Json(name = "BankName") val bankName: String?,
    @field:Json(name = "BankSortCode") val bankSortCode: String?,
    @field:Json(name = "Tags") val tags: List<Tag>?
) {
    val displayName: String
        get() = listOfNotNull(firstName, lastName).joinToString(" ")

    val address: String
        get() = listOfNotNull(address1, address2, townCity, postcode).joinToString()

    @JsonClass(generateAdapter = false)
    enum class Status(
        val constant: String,
        @ColorRes val colorResId: Int,
        @StringRes val msgResId: Int
    ) {
        @Json(name = "Offline")
        OFFLINE("Offline", R.color.color_offline_status, R.string.msg_driver_offline),

        @Json(name = "Available")
        ONLINE("Available", R.color.color_online_status, R.string.msg_driver_online),

        @Json(name = "OnBreak")
        ON_BREAK("OnBreak", R.color.color_on_break_status, R.string.msg_driver_on_break),

        @Json(name = "OnJob")
        ON_JOB("OnJob", R.color.color_online_status, R.string.msg_driver_on_job),

        @Json(name = "Clearing")
        CLEARING("Clearing", R.color.color_online_status, R.string.msg_driver_on_job);
    }

    @JsonClass(generateAdapter = true)
    data class UpdateProfile(
        @field:Json(name = "Email") val email: String? = null,
        @field:Json(name = "Mobile") val mobile: String? = null,
        @field:Json(name = "BankAC") val bankAccountNo: String? = null,
        @field:Json(name = "BankName") val bankName: String? = null,
        @field:Json(name = "BankSortCode") val bankSortCode: String? = null
    )
}

@JsonClass(generateAdapter = true)
data class ChangePasswordRequest(
    @field:Json(name = "UserName") var username: String? = null,
    @field:Json(name = "CurrentPassword") val currentPassword: String,
    @field:Json(name = "NewPassword") val newPassword: String,
    @field:Json(name = "NewPasswordRepeat") val newPasswordConfirm: String
) {
    @JsonClass(generateAdapter = true)
    data class Response(
        @field:Json(name = "PasswordHash") val passwordHash: String,
        @field:Json(name = "SecurityStamp") val securityStamp: String
    )
}

@Parcelize
@JsonClass(generateAdapter = true)
data class Passenger(
    @field:Json(name = "Id") val id: String?,
    @field:Json(name = "TenantId") val tenantId: String?,
    @field:Json(name = "Firstname") val firstName: String?,
    @field:Json(name = "Surname") val lastName: String?,
    @field:Json(name = "ClientId") val clientId: String?,
    @field:Json(name = "Phone") val phone: String?,
    @field:Json(name = "Mobile") val mobile: String?,
    @field:Json(name = "Fax") val fax: String?,
    @field:Json(name = "Email") val email: String?,
    @field:Json(name = "Active") val isActive: Boolean?,
    @field:Json(name = "ImageUrl") val imageUrl: String?,
    @field:Json(name = "Invited") val isInvited: Boolean?
) : Parcelable {
    val displayName: String
        get() = listOfNotNull(firstName, lastName).joinToString(" ")

}

