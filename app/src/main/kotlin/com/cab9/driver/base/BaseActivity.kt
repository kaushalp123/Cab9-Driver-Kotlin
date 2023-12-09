package com.cab9.driver.base

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.Configuration
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import com.cab9.driver.widgets.dialog.AndroidMaterialAlertBuilder
import com.cab9.driver.widgets.dialog.MaterialAlertBuilder
import timber.log.Timber

open class BaseActivity(@LayoutRes contentLayoutId: Int) : AppCompatActivity(contentLayoutId) {

    private var dialogRef: Dialog? = null

    fun showMaterialAlert(init: MaterialAlertBuilder<DialogInterface>.() -> Unit) {
        dialogRef?.dismiss()
        dialogRef = AndroidMaterialAlertBuilder(this).apply { init() }.show()
    }

    override fun onDestroy() {
        hideDialog()
        super.onDestroy()
    }

    protected fun hideDialog() {
        dialogRef?.dismiss()
        dialogRef = null
    }

    override fun attachBaseContext(newBase: Context?) {
        newBase?.resources?.configuration?.run {
            // Ignores the device level settings of the font size and sets
            // the app fonts size to default forcefully.
            if (fontScale != 1.0F) {
                Timber.w("Font size changed from settings, ignore...".uppercase())
                applyOverrideConfiguration(Configuration(this).apply { fontScale = 1.0F })
            }
        }
        super.attachBaseContext(newBase)
    }

}