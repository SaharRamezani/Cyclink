package com.example.cyclink.helpers

import kotlinx.serialization.Serializable

@Serializable
data class SensorData(
    val date: Long,
    val value: List<Double>,
    val userId: Int,
    val measureType: String
) {
    fun getHeartRate(): Double? = if (measureType == "heart_rate" && value.isNotEmpty()) value[0] else null
    fun getBreathingRate(): Double? = if (measureType == "breathing_rate" && value.isNotEmpty()) value[0] else null
    fun getSpeed(): Double? = if (measureType == "speed" && value.isNotEmpty()) value[0] else null
    fun getCadence(): Double? = if (measureType == "cadence" && value.isNotEmpty()) value[0] else null
}