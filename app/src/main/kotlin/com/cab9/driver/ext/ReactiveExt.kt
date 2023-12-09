package com.cab9.driver.ext

import androidx.lifecycle.MutableLiveData
import com.cab9.driver.base.Outcome
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

suspend inline fun <T> MutableSharedFlow<Outcome<T>>.emitLoadingOverlay() {
    emit(Outcome.loading())
}

suspend inline fun <T> MutableSharedFlow<Outcome<T>>.emitSuccess(data: T) {
    emit(Outcome.success(data))
}

suspend inline fun <T> MutableSharedFlow<Outcome<T>>.emitFailureOverlay(
    msg: String,
    ex: Throwable
) {
    emit(Outcome.failure(msg, ex, true))
}

inline fun <T> MutableLiveData<Outcome<T>>.loading() {
    value = Outcome.loading()
}

inline fun <T> MutableLiveData<Outcome<T>>.success(data: T) {
    value = Outcome.success(data)
}

inline fun <T> MutableLiveData<Outcome<T>>.failure(msg: String, ex: Throwable) {
    value = Outcome.failure(msg, ex, false)
}

inline fun <T> MutableStateFlow<Outcome<T>>.loading() {
    value = Outcome.loading()
}

inline fun <T> MutableStateFlow<Outcome<T>>.loadingOverlay() {
    value = Outcome.loading(true)
}

//inline fun <T> MutableStateFlow<Outcome<T>>.success(data: T, isEmpty: Boolean = false) {
//    value = Outcome.success(data, isEmpty)
//}

inline fun <T> MutableStateFlow<Outcome<T>>.success(data: T) {
    value =
        if (data is List<*>) Outcome.success(data, data.isEmpty())
        else Outcome.success(data, false)

}

inline fun <T> MutableStateFlow<Outcome<T>>.failure(
    msg: String,
    ex: Throwable = IllegalStateException()
) {
    value = Outcome.failure(msg, ex)
}

inline fun <T> MutableStateFlow<Outcome<T>>.failureOverlay(
    msg: String,
    ex: Throwable = IllegalStateException()
) {
    value = Outcome.failure(msg, ex, true)
}

//inline fun <T> MutableStateFlow<ListItemOutcome<T>>.loading(itemPosition: Int) {
//    value = ListItemOutcome.Progress(itemPosition)
//}
//
//inline fun <T> MutableStateFlow<ListItemOutcome<T>>.success(itemPosition: Int, data: T) {
//    value = ListItemOutcome.Success(itemPosition, data)
//}
//
//inline fun <T> MutableStateFlow<ListItemOutcome<T>>.failure(
//    itemPosition: Int,
//    msg: String,
//    ex: Throwable
//) {
//    value = ListItemOutcome.Failure(ex, itemPosition, msg)
//}
//
//suspend inline fun <T> MutableSharedFlow<ListItemOutcome<T>>.emitLoading(itemPosition: Int) {
//    emit(ListItemOutcome.Progress(itemPosition))
//}
//
//suspend inline fun <T> MutableSharedFlow<ListItemOutcome<T>>.emitSuccess(
//    itemPosition: Int,
//    data: T
//) {
//    emit(ListItemOutcome.Success(itemPosition, data))
//}
//
//suspend inline fun <T> MutableSharedFlow<ListItemOutcome<T>>.emitFailure(
//    itemPosition: Int,
//    msg: String,
//    ex: Throwable
//) {
//    emit(ListItemOutcome.Failure(ex, itemPosition, msg))
//}