package com.cab9.driver.widgets.dialog

import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.view.KeyEvent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.cab9.driver.R

const val NO_GETTER: String = "Property does not have a getter"

interface MaterialAlertBuilder<out D : DialogInterface> {

    val ctx: Context

    var title: CharSequence
        @Deprecated(NO_GETTER, level = DeprecationLevel.ERROR) get

    var titleResource: Int
        @Deprecated(NO_GETTER, level = DeprecationLevel.ERROR) get

    var message: CharSequence
        @Deprecated(NO_GETTER, level = DeprecationLevel.ERROR) get

    var messageResource: Int
        @Deprecated(NO_GETTER, level = DeprecationLevel.ERROR) get

    var icon: Drawable
        @Deprecated(NO_GETTER, level = DeprecationLevel.ERROR) get

    @setparam:DrawableRes
    var iconResource: Int
        @Deprecated(NO_GETTER, level = DeprecationLevel.ERROR) get

//    var customTitle: View
//        @Deprecated(NO_GETTER, level = DeprecationLevel.ERROR) get
//
//    var customView: View
//        @Deprecated(NO_GETTER, level = DeprecationLevel.ERROR) get

    var isCancelable: Boolean
        @Deprecated(NO_GETTER, level = DeprecationLevel.ERROR) get

    fun onCancelled(handler: (dialog: DialogInterface) -> Unit)

    fun onKeyPressed(handler: (dialog: DialogInterface, keyCode: Int, e: KeyEvent) -> Boolean)

    fun positiveButton(buttonText: String, onClicked: (dialog: DialogInterface) -> Unit)
    fun positiveButton(
        @StringRes buttonTextResource: Int,
        onClicked: (dialog: DialogInterface) -> Unit
    )

    fun negativeButton(buttonText: String, onClicked: (dialog: DialogInterface) -> Unit)
    fun negativeButton(
        @StringRes buttonTextResource: Int,
        onClicked: (dialog: DialogInterface) -> Unit
    )

    fun neutralPressed(buttonText: String, onClicked: (dialog: DialogInterface) -> Unit)
    fun neutralPressed(
        @StringRes buttonTextResource: Int,
        onClicked: (dialog: DialogInterface) -> Unit
    )

    fun items(
        items: List<CharSequence>,
        onItemSelected: (dialog: DialogInterface, index: Int) -> Unit
    )

    fun <T> items(
        items: List<T>,
        onItemSelected: (dialog: DialogInterface, item: T, index: Int) -> Unit
    )

    fun build(): D
    fun show(): D
}

//fun MaterialAlertBuilder<*>.customTitle(dsl: ViewManager.() -> Unit) {
//    customTitle = ctx.UI(dsl).view
//}
//
//fun MaterialAlertBuilder<*>.customView(dsl: ViewManager.() -> Unit) {
//    customView = ctx.UI(dsl).view
//}

inline fun MaterialAlertBuilder<*>.okButton(noinline handler: (dialog: DialogInterface) -> Unit) =
    positiveButton(R.string.action_ok, handler)

inline fun MaterialAlertBuilder<*>.cancelButton(noinline handler: (dialog: DialogInterface) -> Unit) =
    negativeButton(R.string.action_cancel, handler)

inline fun MaterialAlertBuilder<*>.yesButton(noinline handler: (dialog: DialogInterface) -> Unit) =
    positiveButton(R.string.action_yes, handler)

inline fun MaterialAlertBuilder<*>.noButton(noinline handler: (dialog: DialogInterface) -> Unit) =
    negativeButton(R.string.action_no, handler)