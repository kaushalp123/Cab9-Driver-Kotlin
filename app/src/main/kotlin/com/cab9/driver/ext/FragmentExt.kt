package com.cab9.driver.ext

import androidx.annotation.IdRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commitNow
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.cab9.driver.base.Cab9DriverApp

inline val Fragment.cab9DriverApp: Cab9DriverApp
    get() = requireActivity().application as Cab9DriverApp

/**
 * Removes fragment related to tag from stack allowing state loss.
 *
 * @param tag fragment tag
 */
inline fun FragmentManager.removeSafely(tag: String) {
    findFragmentByTag(tag)?.let { commitNow(allowStateLoss = true) { remove(it) } }
}

inline fun FragmentManager.showDialogSafely(dialog: DialogFragment, tag: String) {
    removeSafely(tag)
    dialog.show(this, tag)
}

fun FragmentManager.removeAllAllowingStateLoss() {
    val transaction = beginTransaction()
    fragments.map { transaction.remove(it) }
    transaction.commitAllowingStateLoss()
}

/**
 * Checks topmost [NavBackStackEntry] for a destination id.
 *
 * @param destinationId ID of a destination that exists on the back stack
 *
 * @return true if exists
 */
inline fun NavController.isOnBackStack(@IdRes destinationId: Int): Boolean {
    return try {
        getBackStackEntry(destinationId)
        true
    } catch (ex: IllegalArgumentException) {
        false
    }
}

fun DialogFragment.showDialog(ctx: FragmentActivity, tag: String) {
    show(ctx.supportFragmentManager, tag)
}