package com.example.cyclink.helpers

import kotlinx.serialization.Serializable

@Serializable
data class SensorDataMessage(
    val date: Long,
    val value: List<Double>,
    val userId: Int,
    val measureType: String
)