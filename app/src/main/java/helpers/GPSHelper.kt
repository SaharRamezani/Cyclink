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
    }

    fun hasLocationPermissions(): Boolean {
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
            Log.w(TAG, "❌ Location permissions not granted")
            return
        }

        Log.d(TAG, "🔍 Starting GPS location updates...")
        this.onLocationUpdate = onLocationUpdate
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Check if location services are enabled
        val isGpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
        val isNetworkEnabled = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ?: false

        Log.d(TAG, "📍 GPS Provider enabled: $isGpsEnabled")
        Log.d(TAG, "📍 Network Provider enabled: $isNetworkEnabled")

        if (!isGpsEnabled && !isNetworkEnabled) {
            Log.e(TAG, "❌ No location providers are enabled!")
            return
        }

        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                Log.d(TAG, "✅ GPS location received: ${location.latitude}, ${location.longitude}")
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

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                Log.d(TAG, "📍 Provider $provider status changed: $status")
            }
            override fun onProviderEnabled(provider: String) {
                Log.d(TAG, "✅ Provider $provider enabled")
            }
            override fun onProviderDisabled(provider: String) {
                Log.w(TAG, "❌ Provider $provider disabled")
            }
        }

        try {
            // Try GPS provider first with more aggressive settings
            if (isGpsEnabled) {
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    500L, // More frequent updates
                    0.5f, // More sensitive
                    locationListener!!
                )
                Log.d(TAG, "🛰️ GPS updates requested")
            }

            // Network provider as backup
            if (isNetworkEnabled) {
                locationManager?.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000L,
                    1.0f,
                    locationListener!!
                )
                Log.d(TAG, "🌐 Network updates requested")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Security exception: ${e.message}")
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