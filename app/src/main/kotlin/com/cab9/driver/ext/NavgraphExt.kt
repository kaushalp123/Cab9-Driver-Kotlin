package com.cab9.driver.ext

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavDirections

inline fun NavController.navigateSafely(directions: NavDirections) {
    currentDestination?.getAction(directions.actionId)?.run { navigate(directions) }
}

inline fun NavController.navigateSafely(
    @IdRes currentDestinationId: Int,
    @IdRes resId: Int,
    bundle: Bundle?
) {
    if (currentDestinationId == currentDestination?.id) navigate(resId, bundle)
}