package com.cab9.driver.utils.map

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.location.Location
import android.view.animation.LinearInterpolator
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker

class MarkerAnimator(
    private val marker: Marker,
    private val latLngInterpolator: LatLngInterpolator,
    private val linearInterpolator: LinearInterpolator,
    private val startPos: LatLng,
    private val endPos: LatLng,
    private val startRotation: Float,
    private val newLocation: Location
) : AnimatorUpdateListener {

    private var valueAnimator = ValueAnimator.ofFloat(0F, 1F)
        .apply {
            duration = 1000
            interpolator = linearInterpolator
            addUpdateListener(this@MarkerAnimator)
        }

    fun start() {
        valueAnimator.start()
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        try {
            val v = animation.animatedFraction
            marker.position = latLngInterpolator.interpolate(v, startPos, endPos)
            marker.rotation = computeRotation(v, startRotation, newLocation.bearing)
        } catch (ex: Exception) {
            // ignore
        }
    }

    private fun computeRotation(fraction: Float, start: Float, end: Float): Float {
        val normalizeEnd = end - start // rotate start to 0
        val normalizedEndAbs = (normalizeEnd + 360) % 360
        // -1 = anticlockwise, 1 = clockwise
        val direction = if (normalizedEndAbs > 180) -1 else 1
        val rotation = if (direction > 0) normalizedEndAbs else normalizedEndAbs - 360
        val result = fraction * rotation + start
        return (result + 360) % 360
    }

}