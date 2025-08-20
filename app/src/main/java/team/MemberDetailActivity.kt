package com.example.cyclink.team

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cyclink.R
import com.example.cyclink.helpers.GPSData
import com.example.cyclink.helpers.MQTTHelper
import com.example.cyclink.helpers.SensorDataMessage
import com.example.cyclink.helpers.SensorRecord
import kotlin.math.sqrt

class MemberDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Extract member details from intent
        val memberId = intent.getStringExtra("memberId") ?: ""
        val memberName = intent.getStringExtra("memberName") ?: "Unknown Member"

        Log.d("MemberDetail", "📱 Opening details for member: $memberName (ID: $memberId)")

        setContent {
            MaterialTheme {
                MemberDetailScreen(
                    memberId = memberId,
                    memberName = memberName,
                    onBackPressed = { finish() }
                )
            }
        }
    }
}

@Composable
fun MemberDetailScreen(
    memberId: String,
    memberName: String,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current

    var currentSpeed by remember { mutableStateOf(0.0) }
    var member by remember { mutableStateOf<TeamMember?>(null) }
    var currentSensorData by remember { mutableStateOf<SensorRecord?>(null) }

    var isLoading by remember { mutableStateOf(true) }
    var sensorData by remember { mutableStateOf<SensorRecord?>(null) }
    var locationHistory by remember { mutableStateOf<List<SensorRecord>>(emptyList()) }
    var alertHistory by remember { mutableStateOf<List<String>>(emptyList()) }

    // Real-time sensor data states
    var heartRate by remember { mutableStateOf(0.0) }
    var breathFrequency by remember { mutableStateOf(0.0) }
    var hrv by remember { mutableStateOf(0.0) }
    var intensity by remember { mutableStateOf(0.0) }
    var currentGPS by remember { mutableStateOf<GPSData?>(null) }
    var calculatedSpeed by remember { mutableStateOf(0.0) }

    // Acceleration data for calculations
    var accelerationX by remember { mutableStateOf(0.0) }
    var accelerationY by remember { mutableStateOf(0.0) }
    var accelerationZ by remember { mutableStateOf(0.0) }

    // Connection status
    var isConnected by remember { mutableStateOf(false) }
    var lastDataReceived by remember { mutableStateOf(0L) }

    // Initialize MQTT helper
    val mqttHelper = remember { MQTTHelper(context) }

    // Function to update speed from acceleration
    fun updateSpeedFromAcceleration() {
        val accelerationMagnitude = sqrt(accelerationX * accelerationX + accelerationY * accelerationY + accelerationZ * accelerationZ)
        val netAcceleration = kotlin.math.abs(accelerationMagnitude - 9.8)
        calculatedSpeed = netAcceleration * 3.6 // Convert to km/h
        intensity = accelerationMagnitude
    }

    // MQTT sensor data handler
    val onMQTTSensorDataReceived: (SensorDataMessage) -> Unit = { sensorData ->
        Log.d("MemberDetail", "📊 Received data for ${sensorData.measureType}: ${sensorData.value.average()}")
        lastDataReceived = System.currentTimeMillis()

        val averageValue = if (sensorData.value.isNotEmpty()) {
            sensorData.value.average()
        } else {
            0.0
        }

        when (sensorData.measureType) {
            "HeartRate" -> heartRate = averageValue
            "BreathFrequency" -> breathFrequency = averageValue
            "R2R" -> {
                if (sensorData.value.size >= 2) {
                    var sumSquaredDiffs = 0.0
                    for (i in 1 until sensorData.value.size) {
                        val diff = sensorData.value[i] - sensorData.value[i - 1]
                        sumSquaredDiffs += diff * diff
                    }
                    hrv = sqrt(sumSquaredDiffs / (sensorData.value.size - 1)) * 1000
                }
            }
            "AccelerationX" -> {
                accelerationX = averageValue
                updateSpeedFromAcceleration()
            }
            "AccelerationY" -> {
                accelerationY = averageValue
                updateSpeedFromAcceleration()
            }
            "AccelerationZ" -> {
                accelerationZ = averageValue
                updateSpeedFromAcceleration()
            }
        }
    }

    // Function to handle GPS/location updates from MQTT
    val onLocationUpdate: (SensorRecord) -> Unit = { sensorRecord ->
        Log.d("MemberDetail", "📍 Location update: lat=${sensorRecord.latitude}, lon=${sensorRecord.longitude}")

        // Create GPSData from SensorRecord with all required parameters
        currentGPS = GPSData(
            latitude = sensorRecord.latitude,
            longitude = sensorRecord.longitude,
            altitude = sensorRecord.altitude, // Convert Double to Float
            accuracy = sensorRecord.gpsAccuracy?.toFloat(), // Convert Double to Float
            speed = sensorRecord.gpsSpeed?.toFloat() ?: 0.0f,
            bearing = sensorRecord.gpsBearing?.toFloat() // Convert Double to Float
        )

        // Update other sensor data from the record
        sensorRecord.heartRate?.let { heartRate = it }
        sensorRecord.breathFrequency?.let { breathFrequency = it }
        sensorRecord.intensity?.let { intensity = it }
        sensorRecord.hrv?.let { hrv = it }

        // Update calculated speed if GPS speed is available
        sensorRecord.gpsSpeed?.let { gpsSpeed ->
            calculatedSpeed = gpsSpeed.toDouble()
        }
    }

    // Load basic member info and connect to MQTT
    LaunchedEffect(memberId) {
        if (memberId.isNotEmpty()) {
            // Subscribe to the specific user's MQTT data
            mqttHelper.connectAndSubscribeToUser(
                userId = memberId,
                onConnected = {
                    Log.d("MemberDetail", "✅ Connected to MQTT for user: $memberId")
                },
                onLocationUpdate = { sensorRecord ->
                    Log.d("MemberDetail", "📊 Received sensor data for $memberId: $sensorRecord")

                    // Update all the sensor states
                    heartRate = sensorRecord.heartRate ?: 0.0
                    breathFrequency = sensorRecord.breathFrequency ?: 0.0
                    hrv = sensorRecord.hrv ?: 0.0
                    intensity = sensorRecord.intensity ?: 0.0
                    currentSpeed = sensorRecord.gpsSpeed?.toDouble() ?: 0.0

                    // Update GPS data if available
                    currentGPS = GPSData(
                        latitude = sensorRecord.latitude!!,
                        longitude = sensorRecord.longitude!!,
                        altitude = sensorRecord.altitude ?: 0.0,
                        accuracy = sensorRecord.gpsAccuracy ?: 0.0f,
                        speed = sensorRecord.gpsSpeed?.toFloat() ?: 0.0f,
                        bearing = sensorRecord.gpsBearing ?: 0.0f
                    )

                    lastDataReceived = System.currentTimeMillis()
                    isLoading = false
                },
                onError = { error ->
                    Log.e("MemberDetail", "❌ MQTT error for user $memberId: $error")
                    isLoading = false
                }
            )
        }
    }

    // Also add cleanup in DisposableEffect
    DisposableEffect(Unit) {
        onDispose {
            mqttHelper.disconnect()
            Log.d("MemberDetail", "🔌 MQTT disconnected for user: $memberId")
        }
    }

    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colorResource(id = R.color.berkeley_blue),
                        colorResource(id = R.color.cerulean)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colorResource(id = R.color.honeydew)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = memberName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(id = R.color.honeydew)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (isConnected && System.currentTimeMillis() - lastDataReceived < 30000)
                                        colorResource(id = R.color.non_photo_blue)
                                    else
                                        colorResource(id = R.color.berkeley_blue).copy(alpha = 0.5f),
                                    RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sensor Data Cards
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(getSensorDataList(heartRate, breathFrequency, hrv, intensity, calculatedSpeed, currentGPS)) { sensorData ->
                    SensorDataCard(sensorData = sensorData)
                }
            }
        }
    }

    // Cleanup when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            mqttHelper.disconnect()
        }
    }
}

// Data class for sensor information
data class SensorData(
    val title: String,
    val value: String,
    val unit: String,
    val icon: ImageVector,
    val isHealthy: Boolean = true
)

// Function to create sensor data list
private fun getSensorDataList(
    heartRate: Double,
    breathFrequency: Double,
    hrv: Double,
    intensity: Double,
    calculatedSpeed: Double,
    currentGPS: GPSData?
): List<SensorData> {
    return listOf(
        SensorData(
            title = "Heart Rate",
            value = if (heartRate > 0) "${heartRate.toInt()}" else "No data",
            unit = "bpm",
            icon = Icons.Filled.Favorite,
            isHealthy = heartRate in 60.0..180.0 || heartRate == 0.0
        ),
        SensorData(
            title = "Breath Frequency",
            value = if (breathFrequency > 0) "${breathFrequency.toInt()}" else "No data",
            unit = "/min",
            icon = Icons.Filled.Air,
            isHealthy = breathFrequency in 12.0..30.0 || breathFrequency == 0.0
        ),
        SensorData(
            title = "Speed",
            value = if (calculatedSpeed > 0) String.format("%.1f", calculatedSpeed) else "No data",
            unit = "km/h",
            icon = Icons.Filled.Speed,
            isHealthy = true
        ),
        SensorData(
            title = "Activity Intensity",
            value = if (intensity > 0) String.format("%.1f", intensity) else "No data",
            unit = "m/s²",
            icon = Icons.Filled.FitnessCenter,
            isHealthy = true
        )
    )
}

@Composable
fun SensorDataCard(sensorData: SensorData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(id = R.color.honeydew)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = sensorData.icon,
                contentDescription = sensorData.title,
                tint = if (sensorData.isHealthy)
                    colorResource(id = R.color.cerulean)
                else
                    colorResource(id = R.color.red_pantone),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sensorData.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorResource(id = R.color.berkeley_blue)
                )

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = sensorData.value,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (sensorData.isHealthy)
                            colorResource(id = R.color.berkeley_blue)
                        else
                            colorResource(id = R.color.red_pantone)
                    )

                    if (sensorData.unit.isNotEmpty()) {
                        Text(
                            text = " ${sensorData.unit}",
                            fontSize = 14.sp,
                            color = colorResource(id = R.color.berkeley_blue).copy(alpha = 0.7f),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            if (!sensorData.isHealthy) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Warning",
                    tint = colorResource(id = R.color.red_pantone),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}