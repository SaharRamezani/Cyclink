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
        private const val MIN_TIME_BETWEEN_UPDATES = 5000L // 5 seconds
        private const val MIN_DISTANCE_CHANGE = 10.0f // 10 meters
    }

    fun startLocationUpdates(onLocationReceived: (GPSData) -> Unit) {
        if (!hasLocationPermissions()) {
            Log.e(TAG, "Location permissions not granted")
            return
        }

        onLocationUpdate = onLocationReceived
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val gpsData = GPSData(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = location.altitude,
                    accuracy = location.accuracy,
                    timestamp = location.time
                )
                onLocationReceived(gpsData)
                Log.d(TAG, "Location updated: ${location.latitude}, ${location.longitude}")
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                MIN_TIME_BETWEEN_UPDATES,
                MIN_DISTANCE_CHANGE,
                locationListener!!
            )
            Log.d(TAG, "GPS location updates started")
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission denied", e)
        }
    }

    fun stopLocationUpdates() {
        locationListener?.let {
            locationManager?.removeUpdates(it)
        }
        Log.d(TAG, "GPS location updates stopped")
    }

    private fun hasLocationPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}