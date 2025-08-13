package com.example.cyclink.home

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import com.example.cyclink.team.TeamMember
import com.example.cyclink.team.TeamDashboardActivity
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cyclink.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.cyclink.helpers.GPSData
import com.example.cyclink.helpers.BluetoothConnection
import com.example.cyclink.helpers.MQTTHelper
import com.example.cyclink.helpers.GPSHelper
import com.example.cyclink.helpers.SensorData
import com.example.cyclink.helpers.MQTTSensorData
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.delay
import kotlin.math.sqrt
import org.json.JSONObject

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                HomeScreen()
            }
        }
    }
}

@Composable
fun HeaderSection() {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Cyclink",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.honeydew)
            )
            Text(
                text = "Ready to ride toghether?",
                fontSize = 16.sp,
                color = colorResource(id = R.color.non_photo_blue)
            )
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colorResource(id = R.color.cerulean))
                .clickable {
                    try {
                        // Try AccountActivity first
                        val accountIntent = Intent()
                        accountIntent.setClassName(context.packageName, "com.example.cyclink.account.AccountActivity")
                        context.startActivity(accountIntent)
                        Log.d("HomeActivity", "AccountActivity navigation successful")
                    } catch (e: Exception) {
                        try {
                            // Fallback to ProfileActivity
                            val profileIntent = Intent()
                            profileIntent.setClassName(context.packageName, "com.example.cyclink.profile.ProfileActivity")
                            context.startActivity(profileIntent)
                            Log.d("HomeActivity", "ProfileActivity navigation successful")
                        } catch (e2: Exception) {
                            Log.e("HomeActivity", "Profile/Account navigation failed", e2)
                            // Show a toast or handle the error appropriately
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Person,
                contentDescription = "Profile",
                tint = colorResource(id = R.color.honeydew),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun PersonalStatusSection(
    heartRate: Double,
    breathFrequency: Double,
    speed: Double,
    intensity: Double
) {
    StatusCard(
        title = "Personal Status",
        icon = Icons.Filled.Favorite
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                VitalMetric("Heart Rate", "${heartRate.toInt()} bpm", Icons.Filled.Favorite)
                VitalMetric("Breath Freq", "${breathFrequency.toInt()}/min", Icons.Filled.Air)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                VitalMetric("Speed", "${String.format("%.1f", speed)} m/s", Icons.Filled.Speed)
                VitalMetric("Intensity", "${String.format("%.1f", intensity)}", Icons.Filled.FitnessCenter)
            }
        }
    }
}

@Composable
fun TeamDashboardSection() {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var teamMembers by remember { mutableStateOf<List<TeamMember>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val user = auth.currentUser
        if (user != null) {
            db.collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener { userDoc ->
                    val teamData = userDoc.get("currentTeam") as? Map<String, Any>
                    if (teamData != null) {
                        val teamId = teamData["teamId"] as? String ?: ""

                        if (teamId.isNotEmpty()) {
                            db.collection("teams")
                                .document(teamId)
                                .get()
                                .addOnSuccessListener { teamDoc ->
                                    val members = teamDoc.get("members") as? List<*> ?: emptyList<String>()
                                    val memberNames = teamDoc.get("memberNames") as? List<*> ?: emptyList<String>()

                                    val memberList = mutableListOf<TeamMember>()
                                    members.take(3).forEachIndexed { index, memberId ->
                                        if (memberId is String && index < memberNames.size) {
                                            val memberName = memberNames[index] as? String ?: "Unknown"
                                            memberList.add(
                                                TeamMember(
                                                    id = memberId,
                                                    name = memberName,
                                                    status = if (memberId == user.uid) "online" else listOf("online", "riding", "offline").random()
                                                )
                                            )
                                        }
                                    }
                                    teamMembers = memberList
                                    isLoading = false
                                }
                        } else {
                            isLoading = false
                        }
                    } else {
                        isLoading = false
                    }
                }
        }
    }

    StatusCard(
        title = "Team Dashboard",
        icon = Icons.Filled.Group
    ) {
        Column {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = colorResource(id = R.color.berkeley_blue),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else if (teamMembers.isEmpty()) {
                Text(
                    text = "No team members found",
                    color = colorResource(id = R.color.berkeley_blue).copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                teamMembers.forEachIndexed { index, member ->
                    TeamMemberItem(
                        name = member.name,
                        distance = "${(index + 1) * 50}m away",
                        healthStatus = member.status
                    )
                    if (index < teamMembers.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val intent = Intent(context, TeamDashboardActivity::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.berkeley_blue).copy(alpha = 0.1f),
                    contentColor = colorResource(id = R.color.berkeley_blue)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "View Full Dashboard",
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SessionSummarySection(
    isRiding: Boolean,
    onRideToggle: (Boolean) -> Unit,
    currentGPS: GPSData?,
    speed: Double
) {
    var startTime by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var totalDistance by remember { mutableStateOf(0.0) }
    var lastGPS by remember { mutableStateOf<GPSData?>(null) }
    var avgSpeed by remember { mutableStateOf(0.0) }

    // Timer for duration tracking
    LaunchedEffect(isRiding) {
        if (isRiding) {
            startTime = System.currentTimeMillis()
            totalDistance = 0.0
            lastGPS = null
            Log.d("HomeActivity", "📊 Session started")
        } else if (startTime > 0) {
            Log.d("HomeActivity", "📊 Session ended - Duration: ${formatDuration(duration)}, Distance: ${String.format("%.2f", totalDistance)}km")
        }

        while (isRiding) {
            delay(1000) // Update every second
            duration = System.currentTimeMillis() - startTime
        }
    }

    // GPS distance calculation
    LaunchedEffect(currentGPS, isRiding) {
        if (isRiding && currentGPS != null) {
            lastGPS?.let { lastLocation ->
                val distance = calculateDistance(
                    lastLocation.latitude, lastLocation.longitude,
                    currentGPS.latitude, currentGPS.longitude
                )

                // Only add distance if movement is significant (> 5 meters) to filter GPS noise
                if (distance > 0.005) { // 5 meters in km
                    totalDistance += distance
                    Log.d("HomeActivity", "📍 Distance added: ${String.format("%.3f", distance)}km, Total: ${String.format("%.2f", totalDistance)}km")
                }
            }
            lastGPS = currentGPS
        }
    }

    // Calculate average speed
    LaunchedEffect(duration, totalDistance) {
        if (duration > 0 && isRiding) {
            val durationHours = duration / (1000.0 * 60.0 * 60.0)
            avgSpeed = if (durationHours > 0) totalDistance / durationHours else 0.0
        }
    }

    StatusCard(
        title = "Session Summary",
        icon = Icons.Filled.Dashboard
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    "Duration",
                    if (isRiding) formatDuration(duration) else "0:00"
                )
                StatItem(
                    "Distance",
                    "${String.format("%.2f", totalDistance)} km"
                )
                StatItem(
                    "Avg Speed",
                    "${String.format("%.1f", avgSpeed)} km/h"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onRideToggle(!isRiding) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRiding) colorResource(id = R.color.red_pantone) else colorResource(id = R.color.cerulean)
                )
            ) {
                Icon(
                    imageVector = if (isRiding) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isRiding) "Stop Ride" else "Start Ride",
                    color = colorResource(id = R.color.honeydew)
                )
            }
        }
    }
}

// Helper function to calculate distance between two GPS points (Haversine formula)
private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371.0 // Earth's radius in kilometers
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
    val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    return R * c
}

// Helper function to format duration
private fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = (millis / (1000 * 60 * 60))

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

@Composable
fun StatusCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(id = R.color.honeydew)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = colorResource(id = R.color.cerulean),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.berkeley_blue)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun VitalMetric(label: String, value: String, icon: ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = colorResource(id = R.color.cerulean),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(id = R.color.berkeley_blue)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = colorResource(id = R.color.cerulean)
        )
    }
}

@Composable
fun TeamMemberItem(name: String, distance: String, healthStatus: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            fontWeight = FontWeight.Medium,
            color = colorResource(id = R.color.berkeley_blue)
        )
        Icon(
            if (healthStatus == "Good") Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = healthStatus,
            tint = if (healthStatus == "Good") colorResource(id = R.color.cerulean) else colorResource(id = R.color.red_pantone),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun AlertItem(message: String, type: String, isGood: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            if (isGood) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = type,
            tint = if (isGood) colorResource(id = R.color.cerulean) else colorResource(id = R.color.red_pantone),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = message,
            color = colorResource(id = R.color.berkeley_blue)
        )
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(id = R.color.cerulean)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = colorResource(id = R.color.cerulean)
        )
    }
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var userRole by remember { mutableStateOf("member") }
    var isRiding by remember { mutableStateOf(false) }

    // Sensor data states - only for Personal Status
    var heartRate by remember { mutableStateOf(0.0) }
    var breathFrequency by remember { mutableStateOf(0.0) }
    var speed by remember { mutableStateOf(0.0) }
    var intensity by remember { mutableStateOf(0.0) }
    var currentGPS by remember { mutableStateOf<GPSData?>(null) }

    // Acceleration data for intensity calculation
    var accelerationX by remember { mutableStateOf(0.0) }
    var accelerationY by remember { mutableStateOf(0.0) }
    var accelerationZ by remember { mutableStateOf(0.0) }
    var lastDataReceived by remember { mutableStateOf(0L) }

    // Initialize helpers
    val bluetoothConnection = remember { BluetoothConnection(context) }
    val mqttHelper = remember { MQTTHelper(context) }
    val gpsHelper = remember { GPSHelper(context) }

    // Phone sensors for speed calculation
    val sensorManager = remember { context.getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // Phone sensor listener for speed calculation
    DisposableEffect(isRiding) {
        val sensorListener = object : SensorEventListener {
            var lastAcceleration = 9.8f
            var currentAcceleration = 9.8f
            var velocityEstimate = 0.0

            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER && isRiding) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    lastAcceleration = currentAcceleration
                    currentAcceleration = sqrt(x * x + y * y + z * z)
                    val delta = currentAcceleration - lastAcceleration
                    velocityEstimate += delta * 0.1
                    speed = kotlin.math.abs(velocityEstimate * 0.5)

                    Log.d("HomeActivity", "📱 Phone speed: $speed m/s")
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (isRiding && accelerometer != null) {
            sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d("HomeActivity", "📱 Phone accelerometer registered")
        }

        onDispose {
            sensorManager.unregisterListener(sensorListener)
            Log.d("HomeActivity", "📱 Phone accelerometer unregistered")
        }
    }

    // Enhanced Bluetooth data handler matching your simulator format
    val onBluetoothDataReceived: (SensorData) -> Unit = { sensorData ->
        Log.d("HomeActivity", "🔥 BLUETOOTH DATA RECEIVED:")
        Log.d("HomeActivity", "📊 Type: '${sensorData.measureType}'")
        Log.d("HomeActivity", "📊 Value: ${sensorData.value}")
        Log.d("HomeActivity", "📊 User ID: ${sensorData.userId}")
        Log.d("HomeActivity", "📊 Date: ${sensorData.date}")

        lastDataReceived = System.currentTimeMillis()

        // Convert value list to double (take first value from array)
        val doubleValue = if (sensorData.value.isNotEmpty()) {
            sensorData.value[0]
        } else {
            0.0
        }

        // Handle measurement types exactly matching your simulator
        when (sensorData.measureType.lowercase()) {
            "heartrate" -> {
                heartRate = doubleValue
                Log.d("HomeActivity", "❤️ Heart Rate updated: $doubleValue bpm")
            }
            "breathfrequency" -> {
                breathFrequency = doubleValue
                Log.d("HomeActivity", "🫁 Breath Frequency updated: $doubleValue /min")
            }
            "accelerationx" -> {
                accelerationX = doubleValue
                intensity = sqrt(accelerationX * accelerationX + accelerationY * accelerationY + accelerationZ * accelerationZ)
                Log.d("HomeActivity", "📐 AccelerationX: $doubleValue → Intensity: $intensity")
            }
            "accelerationy" -> {
                accelerationY = doubleValue
                intensity = sqrt(accelerationX * accelerationX + accelerationY * accelerationY + accelerationZ * accelerationZ)
                Log.d("HomeActivity", "📐 AccelerationY: $doubleValue → Intensity: $intensity")
            }
            "accelerationz" -> {
                accelerationZ = doubleValue
                intensity = sqrt(accelerationX * accelerationX + accelerationY * accelerationY + accelerationZ * accelerationZ)
                Log.d("HomeActivity", "📐 AccelerationZ: $doubleValue → Intensity: $intensity")
            }
            "respiration" -> {
                Log.d("HomeActivity", "🌬️ Respiration: $doubleValue")
            }
            "position" -> {
                Log.d("HomeActivity", "📍 Position: $doubleValue")
            }
            else -> {
                Log.w("HomeActivity", "❓ Unknown measurement: ${sensorData.measureType}")
            }
        }

        // Send to MQTT
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val mqttData = MQTTSensorData(
                userId = user.uid,
                date = System.currentTimeMillis(),
                sensorData = sensorData,
                gpsData = currentGPS,
                deviceId = android.provider.Settings.Secure.getString(
                    context.contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                )
            )
            mqttHelper.publishSensorData(mqttData)
            Log.d("HomeActivity", "📡 Data sent to MQTT")
        }
    }

    // Bluetooth permissions
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d("HomeActivity", "=== BLUETOOTH PERMISSIONS ===")
        permissions.forEach { (permission, granted) ->
            Log.d("HomeActivity", "$permission: $granted")
        }

        val bluetoothGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            permissions[android.Manifest.permission.BLUETOOTH_CONNECT] == true
        } else true

        val locationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true

        if (bluetoothGranted && locationGranted && isRiding) {
            bluetoothConnection.startServer(onBluetoothDataReceived)
            Log.d("HomeActivity", "🔵 Bluetooth server started")
        }
    }

    // Monitor data reception timeout
    LaunchedEffect(isRiding) {
        if (isRiding) {
            while (isRiding) {
                delay(10000) // Check every 10 seconds
                val currentTime = System.currentTimeMillis()
                if (lastDataReceived > 0 && (currentTime - lastDataReceived) > 15000) {
                    Log.w("HomeActivity", "⏰ No Bluetooth data received for 15+ seconds")
                } else if (lastDataReceived == 0L) {
                    Log.w("HomeActivity", "⏰ No Bluetooth data received yet")
                } else {
                    Log.d("HomeActivity", "✅ Bluetooth data flow normal")
                }
            }
        }
    }

    // Start/stop services based on ride state
    LaunchedEffect(isRiding) {
        Log.d("HomeActivity", "🚴 RIDE STATE: $isRiding")

        if (isRiding) {
            Log.d("HomeActivity", "=== STARTING RIDE SERVICES ===")

            // Request Bluetooth permissions
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                bluetoothPermissionLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.BLUETOOTH_CONNECT,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    )
                )
            } else {
                bluetoothPermissionLauncher.launch(
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
                )
            }

            // Start GPS
            gpsHelper.startLocationUpdates { gpsData ->
                currentGPS = gpsData
                Log.d("HomeActivity", "📍 GPS: ${gpsData.latitude}, ${gpsData.longitude}")
            }

            // Start MQTT
            mqttHelper.connect()

        } else {
            Log.d("HomeActivity", "=== STOPPING RIDE SERVICES ===")
            bluetoothConnection.stopServer()
            gpsHelper.stopLocationUpdates()
            mqttHelper.disconnect()
        }
    }

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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeaderSection()
            PersonalStatusSection(heartRate, breathFrequency, speed, intensity)
            SessionSummarySection(
                isRiding = isRiding,
                onRideToggle = { newState -> isRiding = newState },
                currentGPS = currentGPS,
                speed = speed
            )
            AlertsSection()
            QuickActionsSection()
            TeamDashboardSection()
        }
    }
}

@Composable
fun AlertsSection() {
    StatusCard(
        title = "Alerts & Notifications",
        icon = Icons.Filled.Warning
    ) {
        Column {
            AlertItem("All vitals normal", "info", true)
            Spacer(modifier = Modifier.height(8.dp))
            AlertItem("GPS connection stable", "success", true)
            Spacer(modifier = Modifier.height(8.dp))
            AlertItem("Team member needs assistance", "warning", false)
        }
    }
}

@Composable
fun QuickActionsSection() {
    StatusCard(
        title = "Quick Actions",
        icon = Icons.Filled.Dashboard
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    text = "Team Map",
                    icon = Icons.Filled.Place,
                    modifier = Modifier.weight(1f)
                ) { }

                QuickActionButton(
                    text = "Sync",
                    icon = Icons.Filled.Refresh,
                    modifier = Modifier.weight(1f)
                ) { }
            }

            QuickActionButton(
                text = "Settings",
                icon = Icons.Filled.Settings,
                modifier = Modifier.fillMaxWidth()
            ) { }
        }
    }
}

@Composable
fun QuickActionButton(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colorResource(id = R.color.berkeley_blue).copy(alpha = 0.1f),
            contentColor = colorResource(id = R.color.berkeley_blue)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}