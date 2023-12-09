package com.cab9.driver.utils

import android.annotation.SuppressLint
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import com.cab9.driver.di.user.LoggedInScope
import com.cab9.driver.ext.toLogString
import com.cab9.driver.network.LocationUpdateSetupException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class LocationUpdateMode {
    OFFLINE, ONLINE, NONE;
}

private fun List<Address>.firstAddress() = buildString {
    if (this@firstAddress.isNotEmpty()) {
        val address = this@firstAddress.first()
        (0..address.maxAddressLineIndex).map { append(address.getAddressLine(it)) }
    } else append("")
}

interface SharedLocationManager {

    val lastKnownLocation: Location?

    val locationUpdatesFlow: Flow<Location?>

    fun startLocationUpdates(mode: LocationUpdateMode)

    fun stopLocationUpdates()

    suspend fun fetchCurrentLocation(token: CancellationTokenSource): Location?

    suspend fun getAddressFrom(latLng: LatLng): String

}

@LoggedInScope
class SharedLocationManagerImpl @Inject constructor(
    private val fusedLocationClient: FusedLocationProviderClient,
    private val geocoder: Geocoder,
    private val externalScope: CoroutineScope
) : SharedLocationManager {

    private var _lastKnownLocation: Location? = null
    override val lastKnownLocation: Location?
        get() = _lastKnownLocation

    private var locationUpdateJob: Job? = null
    private var locationUpdateMode = LocationUpdateMode.NONE

    private val _locationUpdateFlow = MutableStateFlow<Location?>(null)
    override val locationUpdatesFlow: Flow<Location?> = _locationUpdateFlow.asStateFlow()
        .map {
            _lastKnownLocation = it
            return@map it
        }

    private val offlineLocationRequest: LocationRequest
        get() = LocationRequest.create().apply {
            interval = 10_000
            fastestInterval = 10_000
            priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY
            //smallestDisplacement = 10F
        }

    private val onlineLocationRequest: LocationRequest
        get() = LocationRequest.create().apply {
            interval = 0
            fastestInterval = 0
            priority = Priority.PRIORITY_HIGH_ACCURACY
            //smallestDisplacement = 0F
            //isWaitForAccurateLocation = true
        }

    @SuppressLint("MissingPermission")
    private fun registerLocationCallback(request: LocationRequest) = callbackFlow {
        // 1. Create callback and add elements into the flow
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                Timber.i("Received new location")
                // Send the new location to the Flow observers
                result.locations.firstOrNull()?.let { trySend(it) }
            }
        }
        Timber.w("Starting location updates".uppercase())
        // 2. Register the callback to get location updates by calling requestLocationUpdates
        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            ).await()
        } catch (ex: Exception) {
            close(ex)
        }
        // 3. Wait for the consumer to cancel the coroutine and unregister
        // the callback. This suspends the coroutine until the Flow is closed.
        awaitClose {
            Timber.w("Stopping location updates".uppercase())
            // clean up when Flow collection ends
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
        .catch {
            stopLocationUpdates()
            Timber.w(LocationUpdateSetupException(it))
        }
        .shareIn(externalScope, replay = 0, started = SharingStarted.WhileSubscribed())

    private fun registerOnlineLocationUpdates() {
        stopLocationUpdates()
        locationUpdateJob = externalScope.launch {
            registerLocationCallback(onlineLocationRequest).collectLatest {
                Timber.d("ONLINE: ${it.toLogString()}".uppercase())
                _locationUpdateFlow.value = it
            }
        }
    }

    private fun registerOfflineLocationUpdates() {
        stopLocationUpdates()
        locationUpdateJob = externalScope.launch {
            registerLocationCallback(offlineLocationRequest).collectLatest {
                Timber.d("OFFLINE: ${it.toLogString()}".uppercase())
                _locationUpdateFlow.value = it
            }
        }
    }

    override fun startLocationUpdates(mode: LocationUpdateMode) {
        if (mode != locationUpdateMode || locationUpdateJob?.isActive == false) {
            Timber.w("Change location update mode from $locationUpdateMode to $mode")
            if (mode == LocationUpdateMode.ONLINE) registerOnlineLocationUpdates()
            else registerOfflineLocationUpdates()
            locationUpdateMode = mode
        } else Timber.w("$locationUpdateMode location update already running".uppercase())
    }

    override fun stopLocationUpdates() {
        locationUpdateMode = LocationUpdateMode.NONE
        // Cancel running job
        locationUpdateJob?.cancel()
    }

    @SuppressLint("MissingPermission")
    override suspend fun fetchCurrentLocation(token: CancellationTokenSource) = try {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token)
            .await().also { _lastKnownLocation = it }
    } catch (ex: Exception) {
        Timber.e(ex)
        null
    }

    override suspend fun getAddressFrom(latLng: LatLng) = suspendCancellableCoroutine { cont ->
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) { addresses ->
                    cont.resume(addresses.firstAddress())
                }
            } else {
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                cont.resume(addresses?.firstAddress().orEmpty())
            }
        } catch (ex: Exception) {
            cont.resumeWithException(ex)
        }
    }

}