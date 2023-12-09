package com.cab9.driver.data.repos.db

import androidx.room.TypeConverter
import com.cab9.driver.data.models.Attachments
import com.cab9.driver.ext.convertInstantToString
import com.cab9.driver.ext.convertJsonToListObject
import com.cab9.driver.ext.convertListObjectToJson
import com.cab9.driver.ext.convertStringToInstant
import com.squareup.moshi.Moshi
import org.threeten.bp.Instant
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date


class RoomTypeConverter {

    private val moshi = Moshi.Builder().build()

    companion object {
        val format = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
    }

    @TypeConverter
    fun fromJson(json: String?): List<Attachments>? {
        if(json == "{}" || json.isNullOrEmpty()) return emptyList()
        return convertJsonToListObject(json, moshi)
    }

    @TypeConverter
    fun toJson(objectData: List<Attachments>?): String? {
        if (objectData.isNullOrEmpty()) return "[]"
        return convertListObjectToJson(objectData, moshi)
    }

    @TypeConverter
    fun fromInstant(instant: Instant?): String? {
        if(instant == null) return null
        return convertInstantToString(instant, moshi)
    }

    @TypeConverter
    fun toInstant(string: String?): Instant? {
        if (string.isNullOrEmpty()) return null
        return convertStringToInstant(string, moshi)
    }

    @TypeConverter
    fun fromDateToString(value: Date?): String? {
        if(value == null) return null
        return format.format(value)
    }
    @TypeConverter
    fun toDateFromString(value: String?) : Date? {
        if(value.isNullOrEmpty()) return null
        return format.parse(value)
    }

}