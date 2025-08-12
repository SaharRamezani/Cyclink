package com.example.cyclink.helpers

data class GPSData(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val accuracy: Float = 0.0f,
    val timestamp: Long = System.currentTimeMillis()
)