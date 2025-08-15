package com.example.cyclink.team

import kotlinx.serialization.Serializable

@Serializable
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val userId: String,
    val timestamp: Long
)