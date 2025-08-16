package com.example.cyclink.helpers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat

class GPSHelper(private val context: Context) {
    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null
    private var onLocationUpdate: ((GPSData) -> Unit)? = null

    companion object {
        private const val TAG = "GPSHelper"
        private const val MIN_TIME_BETWEEN_UPDATES = 1000L // 1 second (more frequent)
        private const val MIN_DISTANCE_CHANGE = 1.0f // 1 meter (more sensitive)
    }

    // Add the missing hasLocationPermissions function
    private fun hasLocationPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    fun getLastKnownLocation(): GPSData? {
        if (!hasLocationPermissions()) return null

        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            // Use the most recent location
            val bestLocation = when {
                gpsLocation != null && networkLocation != null -> {
                    if (gpsLocation.time > networkLocation.time) gpsLocation else networkLocation
                }
                gpsLocation != null -> gpsLocation
                networkLocation != null -> networkLocation
                else -> null
            }

            return bestLocation?.let { location ->
                GPSData(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = location.altitude,
                    accuracy = location.accuracy,
                    speed = if (location.hasSpeed()) location.speed else 0f,
                    bearing = if (location.hasBearing()) location.bearing else 0f,
                    timestamp = location.time
                )
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "No permission to access location", e)
            return null
        }
    }

    fun startLocationUpdates(onLocationUpdate: (GPSData) -> Unit) {
        if (!hasLocationPermissions()) {
            Log.w(TAG, "Location permissions not granted")
            return
        }

        this.onLocationUpdate = onLocationUpdate
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val gpsData = GPSData(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = location.altitude,
                    accuracy = location.accuracy,
                    speed = if (location.hasSpeed()) location.speed else 0f,
                    bearing = if (location.hasBearing()) location.bearing else 0f,
                    timestamp = location.time
                )
                onLocationUpdate(gpsData)
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            // Try GPS provider first
            if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_BETWEEN_UPDATES,
                    MIN_DISTANCE_CHANGE,
                    locationListener!!
                )
                Log.d(TAG, "GPS location updates started")
            }

            // Also use network provider as backup
            if (locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
                locationManager?.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    MIN_TIME_BETWEEN_UPDATES,
                    MIN_DISTANCE_CHANGE,
                    locationListener!!
                )
                Log.d(TAG, "Network location updates started")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when requesting location updates", e)
        }
    }

    fun stopLocationUpdates() {
        locationListener?.let { listener ->
            locationManager?.removeUpdates(listener)
            Log.d(TAG, "Location updates stopped")
        }
        locationListener = null
        onLocationUpdate = null
    }

    fun requestImmediateLocation(callback: (GPSData?) -> Unit) {
        if (!hasLocationPermissions()) {
            callback(null)
            return
        }

        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            // Try to get last known location first
            val lastKnown = getLastKnownLocation()
            if (lastKnown != null) {
                callback(lastKnown)
                return
            }

            // Request a single location update
            val singleUpdateListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    val gpsData = GPSData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        altitude = location.altitude,
                        accuracy = location.accuracy,
                        speed = if (location.hasSpeed()) location.speed else 0f,
                        bearing = if (location.hasBearing()) location.bearing else 0f,
                        timestamp = location.time
                    )
                    callback(gpsData)
                    locationManager.removeUpdates(this)
                }

                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            // Request single update from both providers
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, singleUpdateListener, null)
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, singleUpdateListener, null)
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception when requesting immediate location", e)
            callback(null)
        }
    }
}