package com.example.cyclink.helpers

data class SensorDataMessage(
    val date: Long,
    val value: List<Double>,
    val userId: Int,
    val measureType: String
)