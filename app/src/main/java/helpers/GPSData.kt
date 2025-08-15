package com.example.cyclink.helpers

import kotlinx.serialization.Serializable

@Serializable
data class GPSData(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val accuracy: Float? = null,
    val speed: Float? = null,
    val bearing: Float? = null,
    val timestamp: Long = System.currentTimeMillis()
)