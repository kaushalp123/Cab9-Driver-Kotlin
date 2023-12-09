package com.cab9.driver.base

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.cab9.driver.ui.home.HomeView
import com.cab9.driver.widgets.dialog.AndroidMaterialAlertBuilder
import com.cab9.driver.widgets.dialog.MaterialAlertBuilder

open class BaseFragment : Fragment {

    constructor() : super()
    constructor(@LayoutRes layoutResId: Int) : super(layoutResId)

    protected var homeView: HomeView? = null
    private var dialogRef: Dialog? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        homeView = context as? HomeView
    }

    override fun onDetach() {
        homeView = null
        super.onDetach()
    }

    override fun onDestroyView() {
        dialogRef?.dismiss()
        dialogRef = null
        super.onDestroyView()
    }

    protected fun showMaterialAlert(init: MaterialAlertBuilder<DialogInterface>.() -> Unit) {
        dialogRef?.dismiss()
        dialogRef = AndroidMaterialAlertBuilder(requireContext()).apply { init() }.show()
    }

    protected fun setToolbarTitle(title: String) {
        (activity as? AppCompatActivity)?.supportActionBar?.title = title
    }

    protected fun showBottomNavSnack(message: String) {
        homeView?.showBottomNavSnack(message)
    }
}