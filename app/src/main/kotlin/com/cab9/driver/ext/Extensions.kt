package com.cab9.driver.ext

import android.os.Build
import com.google.gson.Gson
import com.cab9.driver.BuildConfig
import com.cab9.driver.data.models.Attachments
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.threeten.bp.Instant
import kotlin.math.floor

/**
 * Execute [f] only if the current Android SDK version is [version] or older.
 * Do nothing otherwise.
 */
inline fun doBeforeSdk(version: Int, f: () -> Unit) {
    if (Build.VERSION.SDK_INT <= version) f()
}

/**
 * Execute [f] only if the current Android SDK version is [version] or newer.
 * Do nothing otherwise.
 */
inline fun doFromSdk(version: Int, f: () -> Unit) {
    if (Build.VERSION.SDK_INT >= version) f()
}

/**
 * Execute [f] only if the current Android SDK version is [version].
 * Do nothing otherwise.
 */
inline fun doIfSdk(version: Int, f: () -> Unit) {
    if (Build.VERSION.SDK_INT == version) f()
}

inline fun Any.toJsonString(): String = Gson().toJson(this)


inline fun isCab9GenericApp(): Boolean {
    return BuildConfig.FLAVOR == "devCab9" || BuildConfig.FLAVOR == "prodCab9"
}

inline fun Float.roundDownToMultipleOf(base: Float): Float = base * floor(this / base)

inline fun <reified T> convertJsonToListObject(json: String?, moshi: Moshi): List<T>? =
    moshi.adapter<List<T>>(Types.newParameterizedType(List::class.java, T::class.java)).fromJson(json).orEmpty()

inline fun <reified T> convertListObjectToJson(objectData: List<T>?, moshi: Moshi): String? =
    moshi.adapter<List<T>>(Types.newParameterizedType(List::class.java, T::class.java)).toJson(objectData)

inline fun convertInstantToString(json: Instant, moshi: Moshi): String =
    moshi.adapter<Instant>(Types.newParameterizedType(Instant::class.java, Instant::class.java)).toJson(json)

inline fun convertStringToInstant(objectData: String, moshi: Moshi): Instant? =
    moshi.adapter<Instant>(Types.newParameterizedType(Instant::class.java, Instant::class.java)).fromJson(objectData)

