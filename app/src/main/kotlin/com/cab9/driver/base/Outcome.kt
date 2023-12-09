package com.cab9.driver.base

sealed class Outcome<out T> {

    data class Success<out T>(val data: T, val isEmptyList: Boolean) : Outcome<T>()
    data class Failure(val e: Throwable, val msg: String, val showAsOverlay: Boolean) :
        Outcome<Nothing>()

    data class Progress(val asOverlay: Boolean = false) : Outcome<Nothing>()

    object Empty : Outcome<Nothing>()

    companion object {
        fun loading(asOverlay: Boolean = false): Outcome<Nothing> = Progress(asOverlay)
        fun <T> success(data: T, isEmptyList: Boolean = false): Outcome<T> =
            Success(data, isEmptyList)

        fun failure(
            msg: String,
            e: Throwable,
            asOverlay: Boolean = false
        ): Outcome<Nothing> =
            Failure(e, msg, asOverlay)
    }
}

//sealed class ListItemOutcome<out T>(val itemPosition: Int) {
//
//    data class Progress(val position: Int) : ListItemOutcome<Nothing>(position)
//    data class Success<out T>(val position: Int, val data: T) : ListItemOutcome<T>(position)
//    data class Failure(val e: Throwable, val position: Int, val msg: String) :
//        ListItemOutcome<Nothing>(position)
//
//    object Empty : ListItemOutcome<Nothing>(-1)
//
//}