package com.example.cyclink.helpers

data class SensorRecord(
    val timestamp: Long,
    val userId: String,
    val sessionId: String,
    // MQTT sensor data
    val heartRate: Double?,
    val breathFrequency: Double?,
    val hrv: Double?,
    val intensity: Double?,
    val accelerationX: Double?,
    val accelerationY: Double?,
    val accelerationZ: Double?,
    val ecg: List<Double>?,
    val respiration: List<Double>?,
    val r2rIntervals: List<Double>?,
    // Phone sensor data
    val phoneSpeed: Double?,
    val phoneAccelerationX: Double?,
    val phoneAccelerationY: Double?,
    val phoneAccelerationZ: Double?,
    // GPS data
    val latitude: Double?,
    val longitude: Double?,
    val altitude: Double?,
    val gpsAccuracy: Float?,
    val gpsSpeed: Float?,
    val gpsBearing: Float?
)