package com.cab9.driver.network

import com.squareup.moshi.*
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId

//private const val READABLE_DATE_TIME_FORMAT = "E, dd MMM yyyy HH:mm:ss z"

@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
internal annotation class UTCDateTime

@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
internal annotation class UTCOffsetDateTime

@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
internal annotation class TimeZoneId

//
//@Retention(AnnotationRetention.RUNTIME)
//@JsonQualifier
//internal annotation class ZoneOffsetDateTime

class UTCDateTimeAdapter {

    @ToJson
    fun toJson(@UTCDateTime value: Instant): String = value.toString()

    @FromJson
    @UTCDateTime
    fun fromJson(value: String): Instant = Instant.parse(value)

}

class UTCOffsetDateTimeAdapter {

    @ToJson
    fun toJson(@UTCOffsetDateTime value: OffsetDateTime): String = value.toString()

    @FromJson
    @UTCOffsetDateTime
    fun fromJson(value: String): OffsetDateTime = OffsetDateTime.parse(value)
}

//class TimeZoneAdapter {
//
//    @ToJson
//    fun toJson(@TimeZoneId value: ZoneId): String = value.toString()
//
//    @FromJson
//    @TimeZoneId
//    fun fromJson(value: String): ZoneId = ZoneId.of(value)
//}


//
//class ZoneOffsetDateTimeAdapter {
//
//    @ToJson
//    fun toJson(@ZoneOffsetDateTime value: ZonedDateTime): String = value.toString()
//
//    @FromJson
//    @ZoneOffsetDateTime
//    fun fromJson(value: String): ZonedDateTime = ZonedDateTime.parse(value)

//}