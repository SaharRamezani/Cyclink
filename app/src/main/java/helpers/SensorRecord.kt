package com.example.cyclink.helpers

data class SensorRecord(
    val timestamp: Long = 0L,
    val userId: String= "",
    val sessionId: String= "",
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
    // GPS data
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val gpsAccuracy: Float? = null,
    val gpsSpeed: Float? = null,
    val gpsBearing: Float? = null
)