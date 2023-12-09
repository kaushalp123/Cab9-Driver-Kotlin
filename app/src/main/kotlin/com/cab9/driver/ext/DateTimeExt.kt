package com.cab9.driver.ext

import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.ChronoUnit
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

const val ZONE_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'"

const val DAY_START_TIME = "T00:00:01Z"
const val DAY_END_TIME = "T23:59:00Z"

const val HH_mm = "HH:mm"
const val EEEE_dd_MMMM = "EEEE, dd MMMM"

const val MMM_dd = "MMM dd"
const val dd_MM_yy = "dd.MM.yy"
const val dd_MMM_yyyy = "dd MMM yyyy"

const val DATE_FORMAT_DAY_ONLY = "dd"
const val DATE_FORMAT_FULL_MONTH_NAME = "MMMM"
const val DATE_FORMAT_FULL_MONTH_NAME_WITH_DAY = "MMMM dd"

const val SERVER_DATE_FORMAT = "yyyy-MM-dd"

const val ROOM_DB_DATE_SAVE_FORMAT = "yyyy/MM/dd HH:mm:ss"

inline val Calendar.year: Int
    get() = get(Calendar.YEAR)

inline val Calendar.month: Int
    get() = get(Calendar.MONTH) + 1

inline val Calendar.dayOfMonth: Int
    get() = get(Calendar.DAY_OF_MONTH)

inline val Calendar.hourOfDay: Int
    get() = get(Calendar.HOUR_OF_DAY)

inline val Calendar.minutes: Int
    get() = get(Calendar.MINUTE)


//inline fun Calendar.of(date: Date): Calendar = Calendar.getInstance().apply { time = date }

inline fun Date.toLocalDate(): LocalDate {
    val cal = Calendar.getInstance().apply { time = this@toLocalDate }
    return LocalDate.of(cal.year, cal.month, cal.dayOfMonth)
}

inline fun Date.toLocalDateTime(): LocalDateTime {
    val cal = Calendar.getInstance().apply { time = this@toLocalDateTime }
    return LocalDateTime.of(cal.year, cal.month, cal.dayOfMonth, cal.hourOfDay, cal.minutes)
}

inline fun Calendar.toLocalDate(): LocalDate =
    LocalDate.of(this.year, this.month, this.dayOfMonth)

inline fun Date.toPattern(outPattern: String): String? = try {
    SimpleDateFormat(outPattern, Locale.ENGLISH).format(this)
} catch (ex: Exception) {
    null
}

inline fun LocalDate.toPattern(outPattern: String): String? = try {
    format(DateTimeFormatter.ofPattern(outPattern))
} catch (ex: Exception) {
    null
}

inline fun Instant.ofPattern(outPattern: String): String? = try {
    DateTimeFormatter.ofPattern(outPattern).withZone(ZoneId.systemDefault()).format(this)
} catch (ex: Exception) {
    null
}

inline fun OffsetDateTime.ofPattern(outPattern: String): String? = try {
    DateTimeFormatter.ofPattern(outPattern).format(this)
} catch (ex: Exception) {
    null
}

inline fun ZonedDateTime.ofPattern(outPattern: String): String? = try {
    DateTimeFormatter.ofPattern(outPattern).format(this)
} catch (ex: Exception) {
    null
}

inline fun LocalDate.ofPattern(outPattern: String): String? = try {
    DateTimeFormatter.ofPattern(outPattern).format(this)
} catch (ex: Exception) {
    null
}

inline fun LocalDateTime.ofPattern(outPattern: String): String? = try {
    DateTimeFormatter.ofPattern(outPattern).format(this)
} catch (ex: Exception) {
    null
}

inline fun LocalDateTime.isToday(): Boolean {
    val today = LocalDateTime.now()
    return today.dayOfMonth == dayOfMonth
            && today.month == month
            && today.year == year
}

inline fun LocalDateTime.isTomorrow(): Boolean {
    val currentDateTime = LocalDateTime.now()
    val tomorrow = currentDateTime.plusDays(1)
    return tomorrow.dayOfMonth == dayOfMonth
            && tomorrow.month == month
            && tomorrow.year == year
}

fun isSameDay(date1: Instant, date2: Instant): Boolean =
    date1.truncatedTo(ChronoUnit.DAYS).equals(date2.truncatedTo(ChronoUnit.DAYS))


//inline fun OffsetDateTime.isToday(): Boolean {
//    val today = ZonedDateTime.now()
//    return today.dayOfMonth == dayOfMonth
//            && today.month == month
//            && today.year == year
//}
//
//inline fun OffsetDateTime.isTomorrow(): Boolean {
//    val currentDateTime = ZonedDateTime.now()
//    val tomorrow = currentDateTime.plusDays(1)
//    return tomorrow.dayOfMonth == dayOfMonth
//            && tomorrow.month == month
//            && tomorrow.year == year
//}

fun Instant.toDateTime(zoneId: ZoneId): LocalDateTime = LocalDateTime.ofInstant(this, zoneId)

fun formatDateTimeWithZone(
    zoneId: ZoneId,
    bookedDateTime: Instant?,
    outPattern: String
): String {
    val bookedAt = LocalDateTime.ofInstant(bookedDateTime, zoneId)
    return bookedAt.ofPattern(outPattern).orEmpty()
}
