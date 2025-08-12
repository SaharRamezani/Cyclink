package com.example.cyclink.helpers

data class MQTTSensorData(
    val userId: String,
    val date: Long,
    val sensorData: SensorData,
    val gpsData: GPSData?,
    val deviceId: String
)