package com.cab9.driver.ext

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

fun DialogFragment.removeSafely(fragmentManager: FragmentManager, tag: String) {
    if (dialog?.isShowing == true && !isRemoving) this.dismissAllowingStateLoss()
    // Check fragment manager for safe remove
    val dialogFragment = fragmentManager.findFragmentByTag(tag)
    if (dialogFragment != null) {
        fragmentManager.beginTransaction()
            .remove(dialogFragment)
            .commitNowAllowingStateLoss()
    }
}

//fun isDialogShowing(dialogFragment: DialogFragment?): Boolean {
//    return (dialogFragment != null && dialogFragment.dialog != null && dialogFragment.dialog!!.isShowing
//            && !dialogFragment.isRemoving)
//}