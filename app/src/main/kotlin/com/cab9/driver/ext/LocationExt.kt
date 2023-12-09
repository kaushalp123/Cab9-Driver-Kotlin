package com.cab9.driver.ext

import android.location.Location
import com.google.android.gms.maps.model.LatLng

inline val LatLng.lat: Double
    get() = latitude

inline val LatLng.lng: Double
    get() = longitude

inline val Location.lat: Double
    get() = latitude

inline val Location.lng: Double
    get() = longitude

inline fun Location.toLatLng() = LatLng(latitude, longitude)

inline fun Location.toLogString() = "LOCATION[$provider $latitude $longitude]"