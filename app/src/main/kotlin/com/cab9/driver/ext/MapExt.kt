package com.cab9.driver.ext

import com.google.android.gms.maps.model.TileOverlay

inline fun TileOverlay.show() {
    if (!isVisible) isVisible = true
}

inline fun TileOverlay.hide() {
    if (isVisible) isVisible = false
}