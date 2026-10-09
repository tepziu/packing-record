package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GpsCoordinates(
    val latitude: Double = 21.0285, // Default Hanoi coordinates fallback if indoor
    val longitude: Double = 105.8542,
    val accuracyMeters: Float = 0f,
    val isRealFix: Boolean = false
) {
    fun toFormattedString(): String {
        return String.format("%.5f, %.5f", latitude, longitude)
    }
}

class LocationTracker(private val context: Context) {
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val _locationFlow = MutableStateFlow(GpsCoordinates())
    val locationFlow: StateFlow<GpsCoordinates> = _locationFlow.asStateFlow()

    private var locationCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        try {
            // Get last known location immediately
            fusedClient.lastLocation.addOnSuccessListener { loc: Location? ->
                if (loc != null) {
                    _locationFlow.value = GpsCoordinates(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        accuracyMeters = loc.accuracy,
                        isRealFix = true
                    )
                }
            }

            // Periodic updates
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
                .setMinUpdateIntervalMillis(2000L)
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { loc ->
                        _locationFlow.value = GpsCoordinates(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            accuracyMeters = loc.accuracy,
                            isRealFix = true
                        )
                    }
                }
            }
            locationCallback = callback
            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: Exception) {
            // Fallback to LocationManager if Google Play Services fails
            fallbackToLocationManager()
        }
    }

    @SuppressLint("MissingPermission")
    private fun fallbackToLocationManager() {
        try {
            val provider = LocationManager.GPS_PROVIDER
            if (locationManager?.isProviderEnabled(provider) == true) {
                locationManager.requestLocationUpdates(
                    provider,
                    5000L,
                    5f,
                    object : LocationListener {
                        override fun onLocationChanged(loc: Location) {
                            _locationFlow.value = GpsCoordinates(
                                latitude = loc.latitude,
                                longitude = loc.longitude,
                                accuracyMeters = loc.accuracy,
                                isRealFix = true
                            )
                        }
                        override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}
                    },
                    Looper.getMainLooper()
                )
            }
        } catch (_: Exception) {}
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedClient.removeLocationUpdates(it)
        }
    }

    fun getCurrentCoordinates(): GpsCoordinates = _locationFlow.value
}
