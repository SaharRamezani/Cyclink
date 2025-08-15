package com.example.cyclink.helpers

import kotlinx.serialization.Serializable

@Serializable
data class SensorRecord(
    val timestamp: Long,
    val userId: String = "",
    val sessionId: String,
    // MQTT sensor data
    val heartRate: Double? = null,
    val breathFrequency: Double? = null,
    val hrv: Double? = null,
    val intensity: Double? = null,
    val accelerationX: Double? = null,
    val accelerationY: Double? = null,
    val accelerationZ: Double? = null,
    val ecg: List<Double>? = null,
    val respiration: List<Double>? = null,
    val r2rIntervals: List<Double>? = null,
    // Phone sensor data
    val phoneSpeed: Double? = null,
    val phoneAccelerationX: Double? = null,
    val phoneAccelerationY: Double? = null,
    val phoneAccelerationZ: Double? = null,
    // GPS data - must be non-null since they're required
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val gpsAccuracy: Float? = null,
    val gpsSpeed: Float? = null,
    val gpsBearing: Float? = null
)