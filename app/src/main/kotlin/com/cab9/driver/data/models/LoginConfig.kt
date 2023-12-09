package com.cab9.driver.data.models

import com.cab9.driver.network.UTCOffsetDateTime
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.threeten.bp.OffsetDateTime

@JsonClass(generateAdapter = true)
data class LoginConfig(
    @field:Json(name = "access_token") val accessToken: String?,
    @field:Json(name = "token_type") val tokenType: String,
    @field:Json(name = "expires_in") val expiresIn: Long,
    @field:Json(name = "UserId") val userId: String,
    @field:Json(name = "UserName") val username: String,
    @field:Json(name = "Name") val name: String,
    @field:Json(name = "ImageUrl") val imageUrl: String?,
    @field:Json(name = "Email") val email: String?,
    @field:Json(name = "TenantId") val tenantId: String,
    @field:Json(name = "Claims") val claims: String?,
    @field:Json(name = "GoogleApiKey") val googleApiKey: String?,
    @field:Json(name = "DefaultLocale") val locale: String?,
    @field:Json(name = "ChauffeurModeActive") val isChauffeurModeActive: String?,
    @field:Json(name = "UseMetric") val useMetric: String?,
    //@TimeZoneId @field:Json(name = "DefaultTimezone") val zoneId: ZoneId?,
    @field:Json(name = "DefaultTimezone") val timeZone: String?,
    @UTCOffsetDateTime @field:Json(name = "CurrentServerTime") val serverTime: OffsetDateTime?,
//    @StringDateTime @field:Json(name = ".issued") val issuedAt: ZonedDateTime?,
//    @StringDateTime @field:Json(name = ".expires") val expiresAt: ZonedDateTime?
)