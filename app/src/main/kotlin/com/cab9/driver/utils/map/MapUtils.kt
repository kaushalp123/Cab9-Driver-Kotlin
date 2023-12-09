package com.cab9.driver.utils.map

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.cab9.driver.R
import com.cab9.driver.ext.isPermissionGranted
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

const val HEATMAP_ZOOM_LEVEL = 13.5F
const val NAVIGATION_ZOOM = 17F

val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.ACCESS_FINE_LOCATION
)

fun hasLocationPermissions(context: Context) =
    context.isPermissionGranted(LOCATION_PERMISSIONS.toList())

fun getMarkerIcon(context: Context): BitmapDescriptor {
    val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.img_car_marker)
    return BitmapDescriptorFactory.fromBitmap(
        Bitmap.createScaledBitmap(
            bitmap,
            context.resources.getDimensionPixelSize(R.dimen.marker_icon_width), //200
            context.resources.getDimensionPixelSize(R.dimen.marker_icon_height), //300
            false
        )
    )
}


//object MapUtils {

//    fun checkLocationSettings() {
//        val request = LocationSettingsRequest.Builder()
//            .addLocationRequest(LocationUtils.getHighAccuraryLocationRequest()).build()
//        LocationServices.getSettingsClient(this).checkLocationSettings(request)
//            .addOnCompleteListener { updateLocationUI() }.addOnFailureListener { exception ->
//                if (exception is ResolvableApiException) {
//                    // Location settings are not satisfied, but this can be fixed
//                    // by showing the user a dialog.
//                    try {
//                        // Show the dialog by calling startResolutionForResult(),
//                        // and check the result in onActivityResult().
//                        exception.startResolutionForResult(
//                            this, REQUEST_CHECK_SETTINGS
//                        )
//                    } catch (sendEx: IntentSender.SendIntentException) {
//                        // Ignore the error.
//                    }
//                }
//            }
//    }
//}