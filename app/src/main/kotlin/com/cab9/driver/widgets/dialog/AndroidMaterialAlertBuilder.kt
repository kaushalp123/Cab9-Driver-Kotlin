package com.cab9.driver.widgets.dialog

import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.view.KeyEvent
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AndroidMaterialAlertBuilder(override val ctx: Context) : MaterialAlertBuilder<AlertDialog> {
    private val builder = MaterialAlertDialogBuilder(ctx)

    override var title: CharSequence
        @Deprecated(
            NO_GETTER,
            level = DeprecationLevel.ERROR
        ) get() = throw RuntimeException("Property does not have a getter")
        set(value) {
            builder.setTitle(value)
        }

    override var titleResource: Int
        @Deprecated(
            NO_GETTER,
            level = DeprecationLevel.ERROR
        ) get() = throw RuntimeException("Property does not have a getter")
        set(value) {
            builder.setTitle(value)
        }

    override var message: CharSequence
        @Deprecated(
            NO_GETTER,
            level = DeprecationLevel.ERROR
        ) get() = throw RuntimeException("Property does not have a getter")
        set(value) {
            builder.setMessage(value)
        }

    override var messageResource: Int
        @Deprecated(
            NO_GETTER,
            level = DeprecationLevel.ERROR
        ) get() = throw RuntimeException("Property does not have a getter")
        set(value) {
            builder.setMessage(value)
        }

    override var icon: Drawable
        @Deprecated(
            NO_GETTER,
            level = DeprecationLevel.ERROR
        ) get() = throw RuntimeException("Property does not have a getter")
        set(value) {
            builder.setIcon(value)
        }

    override var iconResource: Int
        @Deprecated(
            NO_GETTER,
            level = DeprecationLevel.ERROR
        ) get() = throw RuntimeException("Property does not have a getter")
        set(value) {
            builder.setIcon(value)
        }

//    override var customTitle: View
//        @Deprecated(
//            NO_GETTER,
//            level = DeprecationLevel.ERROR
//        ) get() = throw RuntimeException("Property does not have a getter")
//        set(value) {
//            builder.setCustomTitle(value)
//        }
//
//    override var customView: View
//        @Deprecated(
//            NO_GETTER,
//            level = DeprecationLevel.ERROR
//        ) get() = throw RuntimeException("Property does not have a getter")
//        set(value) {
//            builder.setView(value)
//        }

    override var isCancelable: Boolean
        @Deprecated(
            NO_GETTER,
            level = DeprecationLevel.ERROR
        ) get() = throw RuntimeException("Property does not have a getter")
        set(value) {
            builder.setCancelable(value)
        }

    override fun onCancelled(handler: (DialogInterface) -> Unit) {
        builder.setOnCancelListener(handler)
    }

    override fun onKeyPressed(handler: (dialog: DialogInterface, keyCode: Int, e: KeyEvent) -> Boolean) {
        builder.setOnKeyListener(handler)
    }

    override fun positiveButton(buttonText: String, onClicked: (dialog: DialogInterface) -> Unit) {
        builder.setPositiveButton(buttonText) { dialog, _ -> onClicked(dialog) }
    }

    override fun positiveButton(
        buttonTextResource: Int,
        onClicked: (dialog: DialogInterface) -> Unit
    ) {
        builder.setPositiveButton(buttonTextResource) { dialog, _ -> onClicked(dialog) }
    }

    override fun negativeButton(buttonText: String, onClicked: (dialog: DialogInterface) -> Unit) {
        builder.setNegativeButton(buttonText) { dialog, _ -> onClicked(dialog) }
    }

    override fun negativeButton(
        buttonTextResource: Int,
        onClicked: (dialog: DialogInterface) -> Unit
    ) {
        builder.setNegativeButton(buttonTextResource) { dialog, _ -> onClicked(dialog) }
    }

    override fun neutralPressed(buttonText: String, onClicked: (dialog: DialogInterface) -> Unit) {
        builder.setNeutralButton(buttonText) { dialog, _ -> onClicked(dialog) }
    }

    override fun neutralPressed(
        buttonTextResource: Int,
        onClicked: (dialog: DialogInterface) -> Unit
    ) {
        builder.setNeutralButton(buttonTextResource) { dialog, _ -> onClicked(dialog) }
    }

    override fun items(
        items: List<CharSequence>,
        onItemSelected: (dialog: DialogInterface, index: Int) -> Unit
    ) {
        builder.setItems(Array(items.size) { i -> items[i].toString() }) { dialog, which ->
            onItemSelected(dialog, which)
        }
    }

    override fun <T> items(
        items: List<T>,
        onItemSelected: (dialog: DialogInterface, item: T, index: Int) -> Unit
    ) {
        builder.setItems(Array(items.size) { i -> items[i].toString() }) { dialog, which ->
            onItemSelected(dialog, items[which], which)
        }
    }

    override fun build() = builder.create()

    override fun show(): AlertDialog = builder.show()

}