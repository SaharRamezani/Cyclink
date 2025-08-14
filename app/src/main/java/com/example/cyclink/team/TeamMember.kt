package com.example.cyclink.com.example.cyclink.team

data class TeamMember(
    val id: String,
    val name: String,
    val status: String = "online",
    val heartRate: Int = 0,
    val speed: Double = 0.0,
    val lastSeen: Long = System.currentTimeMillis(),
    val alerts: List<String> = emptyList()
)