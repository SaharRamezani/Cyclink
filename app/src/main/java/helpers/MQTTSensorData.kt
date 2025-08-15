package com.example.cyclink.helpers

import kotlinx.serialization.Serializable

@Serializable
data class MQTTSensorData(
    val userId: String,
    val date: Long,
    val sensorData: SensorData,
    val gpsData: GPSData?,
    val deviceId: String
)