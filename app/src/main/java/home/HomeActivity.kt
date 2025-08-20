package com.example.cyclink.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import com.example.cyclink.helpers.AIHelper
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.cyclink.R
import com.example.cyclink.chat.HelpChatActivity
import com.example.cyclink.helpers.*
import com.example.cyclink.team.TeamDashboardActivity
import com.example.cyclink.team.TeamMapActivity
import com.example.cyclink.team.TeamMember
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.math.sqrt

class HomeActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions: Map<String, Boolean> ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        Log.d("HomeActivity", "🔐 Permission results - Fine: $fineLocationGranted, Coarse: $coarseLocationGranted")

        if (fineLocationGranted || coarseLocationGranted) {
            Log.d("HomeActivity", "✅ Location permissions granted!")
        } else {
            Log.e("HomeActivity", "❌ Location permissions denied!")
        }
    }
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("HomeActivity", "Notification permission granted")
        } else {
            Log.w("HomeActivity", "Notification permission denied")
        }
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    private val userSensorData = mutableMapOf<String, MutableList<SensorRecord>>()
    private var lastStatusWasPerfect = true


    // Single onCreate method
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                HomeScreen(requestPermissions = ::requestLocationPermissions)
            }
        }
    }

    private fun requestLocationPermissions() {
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
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

@SuppressLint("DefaultLocale")
@Composable
fun PersonalStatusSection(
    heartRate: Double,
    breathFrequency: Double,
    intensity: Double,
    calculatedSpeed: Double
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
                VitalMetric("Speed", "${String.format("%.1f", calculatedSpeed)} km/h", Icons.Filled.Timeline)
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
                    val teamData = userDoc.get("currentTeam") as? Map<*, *>
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

@SuppressLint("DefaultLocale")
@Composable
fun SessionSummarySection(
    isRiding: Boolean,
    onRideToggle: (Boolean) -> Unit,
    currentGPS: GPSData?
) {
    val context = LocalContext.current
    var startTime by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var totalDistance by remember { mutableStateOf(0.0) }
    var lastGPS by remember { mutableStateOf<GPSData?>(null) }

    // Timer for duration tracking - keeps running even when stopped
    LaunchedEffect(isRiding) {
        if (isRiding) {
            if (startTime == 0L) {
                // Only reset on first start
                startTime = System.currentTimeMillis()
                totalDistance = 0.0
                lastGPS = null
                Log.d("HomeActivity", "📊 Session started")
            } else {
                // Resume from pause
                val pausedDuration = duration
                startTime = System.currentTimeMillis() - pausedDuration
                Log.d("HomeActivity", "📊 Session resumed")
            }
        } else if (startTime > 0) {
            Log.d("HomeActivity", "📊 Session paused - Duration: ${formatDurationWithHours(duration)}, Distance: ${String.format("%.2f", totalDistance)}km")
        }

        while (isRiding && startTime > 0) {
            delay(1000) // Update every second
            duration = System.currentTimeMillis() - startTime
        }
    }

    // GPS distance
    LaunchedEffect(currentGPS, isRiding) {
        if (isRiding && currentGPS != null) {
            lastGPS?.let { lastLocation ->
                val distance = calculateDistance(
                    lastLocation.latitude, lastLocation.longitude,
                    currentGPS.latitude, currentGPS.longitude
                )

                if (distance > 0.005) { // 5 meters in km
                    totalDistance += distance
                    Log.d("HomeActivity", "📍 Distance: ${String.format("%.3f", distance)}km")
                }
            }
            lastGPS = currentGPS
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
                    if (startTime > 0) formatDurationWithHours(duration) else "00:00:00"
                )
                StatItem(
                    "Distance",
                    "${String.format("%.2f", totalDistance)} km"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Reset button
                Button(
                    onClick = {
                        startTime = 0L
                        duration = 0L
                        totalDistance = 0.0
                        lastGPS = null
                        Log.d("HomeActivity", "📊 Session reset")
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.berkeley_blue).copy(alpha = 0.1f),
                        contentColor = colorResource(id = R.color.berkeley_blue)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Reset",
                        color = colorResource(id = R.color.berkeley_blue),
                        fontSize = 14.sp
                    )
                }

                // Start/Stop button
                Button(
                    onClick = { onRideToggle(!isRiding) },
                    modifier = Modifier.weight(2f),
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

            // Finish & Save button (only show when stopped and has data)
            if (!isRiding && startTime > 0 && (duration > 0 || totalDistance > 0)) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        saveRideRecord(context, duration, totalDistance, startTime)
                        // Reset after saving
                        startTime = 0L
                        duration = 0L
                        totalDistance = 0.0
                        lastGPS = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.cerulean)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Finish & Save Record",
                        color = colorResource(id = R.color.honeydew),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun saveRideRecord(context: android.content.Context, duration: Long, distance: Double, startTime: Long) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    if (currentUser == null) {
        Log.e("HomeActivity", "No authenticated user for saving record")
        return
    }

    val finishTime = System.currentTimeMillis()
    val rideRecord = hashMapOf(
        "userId" to currentUser.uid,
        "duration" to duration,
        "distance" to distance,
        "startTime" to startTime,
        "finishTime" to finishTime,
        "date" to java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date(finishTime)),
        "startHour" to java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(startTime)),
        "finishHour" to java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(finishTime)),
        "participants" to listOf(currentUser.uid) // Will be expanded for team rides
    )

    db.collection("ride_records")
        .add(rideRecord)
        .addOnSuccessListener { documentReference ->
            Log.d("HomeActivity", "✅ Ride record saved with ID: ${documentReference.id}")
            android.widget.Toast.makeText(
                context,
                "Ride record saved successfully!",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
        .addOnFailureListener { e ->
            Log.e("HomeActivity", "❌ Error saving ride record", e)
            android.widget.Toast.makeText(
                context,
                "Failed to save ride record",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
}

// Updated helper function to format duration with hours
private fun formatDurationWithHours(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = (millis / (1000 * 60 * 60))

    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

// Helper function to calculate distance between two GPS points (Haversine formula)
private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371.0 // Earth's radius in kilometers
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
    val c = 2 * kotlin.math.atan2(sqrt(a), sqrt(1 - a))
    return R * c
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
fun TeamMemberItem(name: String, healthStatus: String) {
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
    }
}

@Composable
fun AlertItem(message: String, type: String, isGood: Boolean) {
    Row(
        verticalAlignment = Alignment.Top,
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
fun HomeScreen(requestPermissions: () -> Unit = {}) {
    val context = LocalContext.current

    var hasLocationPermission by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    var userRole by remember { mutableStateOf("member") }
    var isRiding by remember { mutableStateOf(false) }

    // State variables for speed calculation
    var calculatedSpeed by remember { mutableStateOf(0.0) }
    var lastAccelerationTime by remember { mutableStateOf(0L) }
    var velocity by remember { mutableStateOf(0.0) }
    var lastAccelerationMagnitude by remember { mutableStateOf(0.0) }

    // Session ID for grouping records
    var sessionId by remember { mutableStateOf("") }

    // Real-time sensor data states from MQTT
    var heartRate by remember { mutableStateOf(0.0) }
    var breathFrequency by remember { mutableStateOf(0.0) }
    var hrv by remember { mutableStateOf(0.0) }
    var intensity by remember { mutableStateOf(0.0) }
    var currentGPS by remember { mutableStateOf<GPSData?>(null) }

    // Phone acceleration data
    var phoneAccelerationX by remember { mutableStateOf(0.0) }
    var phoneAccelerationY by remember { mutableStateOf(0.0) }
    var phoneAccelerationZ by remember { mutableStateOf(0.0) }

    // Acceleration data for intensity calculation from MQTT
    var accelerationX by remember { mutableStateOf(0.0) }
    var accelerationY by remember { mutableStateOf(0.0) }
    var accelerationZ by remember { mutableStateOf(0.0) }
    var lastDataReceived by remember { mutableStateOf(0L) }

    // R-R intervals for HRV calculation
    var rrIntervals by remember { mutableStateOf<List<Double>>(emptyList()) }

    // Additional sensor data storage
    var ecgData by remember { mutableStateOf<List<Double>?>(null) }
    var respirationData by remember { mutableStateOf<List<Double>?>(null) }

    // Initialize helpers
    val mqttHelper = remember { MQTTHelper(context) }
    val gpsHelper = remember { GPSHelper(context) }
    val firestoreHelper = remember { FirestoreHelper() }

    // Phone sensors
    val sensorManager = remember { context.getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    LaunchedEffect(Unit) {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        hasLocationPermission = fineLocationGranted || coarseLocationGranted

        Log.d("HomeActivity", "🔐 Location permissions - Fine: $fineLocationGranted, Coarse: $coarseLocationGranted")

        if (!hasLocationPermission) {
            Log.w("HomeActivity", "❌ No location permissions granted! Requesting...")
            requestPermissions()
        }
    }

    // Generate session ID when ride starts
    LaunchedEffect(isRiding) {
        if (isRiding && sessionId.isEmpty()) {
            sessionId = "session_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
            Log.d("HomeActivity", "🆔 New session ID: $sessionId")
        } else if (!isRiding) {
            // Keep session ID for potential resume
        }
    }

    fun updateSpeedFromAcceleration() {
        val currentTime = System.currentTimeMillis()
        val accelerationMagnitude = sqrt(accelerationX * accelerationX + accelerationY * accelerationY + accelerationZ * accelerationZ)

        // Remove gravity (approximately 9.8 m/s²)
        val netAcceleration = kotlin.math.abs(accelerationMagnitude - 9.8)

        if (lastAccelerationTime > 0) {
            val deltaTime = (currentTime - lastAccelerationTime) / 1000.0 // Convert to seconds

            if (deltaTime > 0 && deltaTime < 2.0) { // Reasonable time delta
                // Update velocity using acceleration
                velocity += netAcceleration * deltaTime

                // Apply some damping to prevent unrealistic speeds
                velocity *= 0.85

                // Convert to km/h and apply smoothing
                val newSpeed = kotlin.math.abs(velocity * 3.6)
                calculatedSpeed = (calculatedSpeed * 0.7) + (newSpeed * 0.3)

                // Cap maximum speed for cycling (reasonable limit)
                calculatedSpeed = kotlin.math.min(calculatedSpeed, 60.0)

                Log.d("HomeActivity", "🚴 Speed calculated: ${String.format("%.1f", calculatedSpeed)} km/h")
            }
        }

        lastAccelerationTime = currentTime
        lastAccelerationMagnitude = accelerationMagnitude

        // Update intensity
        intensity = accelerationMagnitude
    }

    // Function to save complete sensor record to Firestore
    // Update the saveSensorRecord function to ensure GPS data is included
    fun saveSensorRecord() {
        val currentTime = System.currentTimeMillis()

        // Ensure we have GPS data before saving
        val gpsToSave = currentGPS
        if (gpsToSave == null) {
            Log.w("HomeActivity", "⚠️ No GPS data available, skipping save")
            return
        }

        val sensorRecord = SensorRecord(
            timestamp = currentTime,
            userId = "", // Will be set in FirestoreHelper
            sessionId = sessionId,
            // MQTT sensor data
            heartRate = if (heartRate > 0) heartRate else null,
            breathFrequency = if (breathFrequency > 0) breathFrequency else null,
            hrv = if (hrv > 0) hrv else null,
            intensity = if (intensity > 0) intensity else null,
            accelerationX = if (accelerationX != 0.0) accelerationX else null,
            accelerationY = if (accelerationY != 0.0) accelerationY else null,
            accelerationZ = if (accelerationZ != 0.0) accelerationZ else null,
            ecg = ecgData,
            respiration = respirationData,
            r2rIntervals = if (rrIntervals.isNotEmpty()) rrIntervals else null,
            // Phone sensor data
            phoneAccelerationX = if (phoneAccelerationX != 0.0) phoneAccelerationX else null,
            phoneAccelerationY = if (phoneAccelerationY != 0.0) phoneAccelerationY else null,
            phoneAccelerationZ = if (phoneAccelerationZ != 0.0) phoneAccelerationZ else null,
            // GPS data - ensure it's not null
            latitude = gpsToSave.latitude,
            longitude = gpsToSave.longitude,
            altitude = gpsToSave.altitude,
            gpsAccuracy = gpsToSave.accuracy,
            gpsSpeed = gpsToSave.speed,
            gpsBearing = gpsToSave.bearing
        )

        Log.d("HomeActivity", "📍 Saving GPS data: lat=${gpsToSave.latitude}, lon=${gpsToSave.longitude}")

        firestoreHelper.saveSensorRecord(
            sensorRecord = sensorRecord,
            onSuccess = {
                Log.d("HomeActivity", "💾 Sensor record with GPS saved successfully")
            },
            onError = { error ->
                Log.e("HomeActivity", "❌ Failed to save sensor record: ${error.message}")
            }
        )

        mqttHelper.sendSensorDataToMqtt(sensorRecord = sensorRecord, FirebaseAuth.getInstance().currentUser?.uid ?: "")
    }

    // Phone sensor listener for acceleration and speed
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

                    // Store phone acceleration data
                    phoneAccelerationX = x.toDouble()
                    phoneAccelerationY = y.toDouble()
                    phoneAccelerationZ = z.toDouble()

                    // Calculate speed estimation
                    lastAcceleration = currentAcceleration
                    currentAcceleration = sqrt(x * x + y * y + z * z)
                    val delta = currentAcceleration - lastAcceleration
                    velocityEstimate += delta * 0.1
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

    // MQTT sensor data handler with Firestore integration
    val onMQTTSensorDataReceived: (SensorDataMessage) -> Unit = { sensorData: SensorDataMessage ->
        Log.d("HomeActivity", "🔥 MQTT SENSOR DATA RECEIVED:")
        Log.d("HomeActivity", "📊 Type: '${sensorData.measureType}'")
        Log.d("HomeActivity", "📊 Values: ${sensorData.value.size} samples")
        Log.d("HomeActivity", "📊 User ID: ${sensorData.userId}")
        Log.d("HomeActivity", "📊 Date: ${sensorData.date}")

        lastDataReceived = System.currentTimeMillis()

        // Calculate average value from the array
        val averageValue = if (sensorData.value.isNotEmpty()) {
            sensorData.value.average()
        } else {
            0.0
        }

        // Handle measurement types
        when (sensorData.measureType) {
            "HeartRate" -> {
                heartRate = averageValue
                Log.d("HomeActivity", "❤️ Heart Rate updated: $averageValue bpm")
            }
            "BreathFrequency" -> {
                breathFrequency = averageValue
                Log.d("HomeActivity", "🫁 Breath Frequency updated: $averageValue /min")
            }
            "R2R" -> {
                rrIntervals = sensorData.value
                hrv = calculateHRV(rrIntervals)
                Log.d("HomeActivity", "💓 HRV updated: $hrv ms (from ${rrIntervals.size} R-R intervals)")
            }
            "ECG" -> {
                ecgData = sensorData.value
                Log.d("HomeActivity", "📈 ECG data received: ${sensorData.value.size} samples")
            }
            "Respiration" -> {
                respirationData = sensorData.value
                Log.d("HomeActivity", "🫁 Respiration data received: ${sensorData.value.size} samples")
            }
            "AccelerationX" -> {
                accelerationX = averageValue
                updateSpeedFromAcceleration() // Call when X acceleration updates
            }
            "AccelerationY" -> {
                accelerationY = averageValue
                updateSpeedFromAcceleration() // Call when Y acceleration updates
            }
            "AccelerationZ" -> {
                accelerationZ = averageValue
                updateSpeedFromAcceleration() // Call when Z acceleration updates
            }
            else -> {
                Log.w("HomeActivity", "❓ Unknown measurement: ${sensorData.measureType}")
            }
        }

        // Save to Firestore after processing MQTT data
        if (isRiding && sessionId.isNotEmpty()) {
            saveSensorRecord()
        }
    }

    // Auto-save sensor records periodically when riding (only when GPS is available)
    LaunchedEffect(isRiding, sessionId) {
        if (isRiding && sessionId.isNotEmpty()) {
            Log.d("HomeActivity", "🔄 Starting periodic save routine...")

            // Wait for GPS data with timeout
            var waitCount = 0
            while (isRiding && currentGPS == null && waitCount < 30) { // 30 second timeout
                delay(1000)
                waitCount++
                Log.d("HomeActivity", "⏳ Waiting for GPS data... (${waitCount}s)")
            }

            if (currentGPS == null) {
                Log.w("HomeActivity", "⚠️ GPS timeout after 30s, continuing without GPS")
                return@LaunchedEffect
            }

            Log.d("HomeActivity", "✅ GPS available, starting periodic saves")

            // Now start periodic saves
            while (isRiding && currentGPS != null) {
                delay(10000) // Save every 10 seconds
                Log.d("HomeActivity", "💾 Attempting periodic save...")
                saveSensorRecord()
            }
        }
    }

    // Rest of your existing LaunchedEffect blocks remain the same...
    LaunchedEffect(isRiding) {
        if (isRiding) {
            while (isRiding) {
                delay(10000)
                val currentTime = System.currentTimeMillis()
                if (lastDataReceived > 0 && (currentTime - lastDataReceived) > 15000) {
                    Log.w("HomeActivity", "⏰ No MQTT data received for 15+ seconds")
                } else if (lastDataReceived == 0L) {
                    Log.w("HomeActivity", "⏰ No MQTT data received yet")
                } else {
                    Log.d("HomeActivity", "✅ MQTT data flow normal")
                }
            }
        }
    }

    LaunchedEffect(isRiding) {
        Log.d("HomeActivity", "🚴 RIDE STATE: $isRiding")

        if (isRiding) {
            Log.d("HomeActivity", "=== STARTING RIDE SERVICES ===")

            // Start GPS services first and wait for initial location
            val lastKnownLocation = gpsHelper.getLastKnownLocation()
            if (lastKnownLocation != null) {
                currentGPS = lastKnownLocation
                Log.d("HomeActivity", "📍 Using last known GPS: ${lastKnownLocation.latitude}, ${lastKnownLocation.longitude}")
            }

            // Start continuous GPS updates
            gpsHelper.startLocationUpdates { gpsData ->
                currentGPS = gpsData
                Log.d("HomeActivity", "📍 GPS updated: ${gpsData.latitude}, ${gpsData.longitude}")
            }

            // Request immediate GPS location (non-blocking)
            gpsHelper.requestImmediateLocation { immediateGPS ->
                if (immediateGPS != null) {
                    currentGPS = immediateGPS
                    Log.d("HomeActivity", "📍 Immediate GPS acquired: ${immediateGPS.latitude}, ${immediateGPS.longitude}")
                }
            }

            // Give GPS a moment to initialize before starting MQTT
            delay(2000)

            mqttHelper.connect(
                onConnected = {
                    Log.d("HomeActivity", "✅ MQTT connected and subscribed to sensor data")
                },
                onError = { error ->
                    Log.e("HomeActivity", "❌ MQTT connection failed: $error")
                },
                onSensorDataReceived = onMQTTSensorDataReceived
            )

        } else {
            Log.d("HomeActivity", "=== STOPPING RIDE SERVICES ===")
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
            PersonalStatusSection(heartRate, breathFrequency, intensity, calculatedSpeed)
            SessionSummarySection(
                isRiding = isRiding,
                onRideToggle = { newState -> isRiding = newState },
                currentGPS = currentGPS
            )
            AlertsSection()
            QuickActionsSection()
            TeamDashboardSection()
        }
    }
}

// Helper function to calculate HRV from R-R intervals
private fun calculateHRV(rrIntervals: List<Double>): Double {
    if (rrIntervals.size < 2) return 0.0

    // Calculate RMSSD (Root Mean Square of Successive Differences)
    var sumSquaredDiffs = 0.0
    for (i in 1 until rrIntervals.size) {
        val diff = rrIntervals[i] - rrIntervals[i - 1]
        sumSquaredDiffs += diff * diff
    }

    val rmssd = sqrt(sumSquaredDiffs / (rrIntervals.size - 1))
    return rmssd * 1000 // Convert to milliseconds
}

@Composable
fun AlertsSection() {
    var alerts by remember { mutableStateOf<List<AlertData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    StatusCard(
        title = "Alerts & Notifications",
        icon = Icons.Filled.Warning
    ) {
        Column {
            if (isLoading && alerts.isEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = colorResource(id = R.color.berkeley_blue)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Monitoring team health...",
                        color = colorResource(id = R.color.berkeley_blue)
                    )
                }
            } else if (alerts.isEmpty()) {
                Text(
                    text = "No alerts at this time",
                    color = colorResource(id = R.color.berkeley_blue)
                )
            } else {
                alerts.forEach { alert ->
                    AlertItem(
                        message = alert.message,
                        type = alert.type,
                        isGood = alert.isGood
                    )
                    if (alert != alerts.last()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    // Start team health monitoring within LaunchedEffect
    LaunchedEffect(Unit) {
        startTeamHealthMonitoringWithContext(context) { newAlerts, loading ->
            isLoading = loading
            // Add new alerts to existing ones instead of replacing them
            if (newAlerts.isNotEmpty()) {
                val currentTime = System.currentTimeMillis()
                val alertsWithTimestamp = newAlerts.map { alert ->
                    alert.copy(message = "${alert.message} (${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(currentTime))})")
                }
                alerts = alerts + alertsWithTimestamp
            }
        }
    }
}

// Data class for alerts
data class AlertData(
    val message: String,
    val type: String,
    val isGood: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

private suspend fun getTeamUserIds(): List<String> = withContext(Dispatchers.IO) {
    try {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val user = auth.currentUser ?: return@withContext emptyList()

        val userDoc = db.collection("users").document(user.uid).get().await()
        val teamData = userDoc.get("currentTeam") as? Map<*, *> ?: return@withContext emptyList()
        val teamId = teamData["teamId"] as? String ?: return@withContext emptyList()

        val teamDoc = db.collection("teams").document(teamId).get().await()
        (teamDoc.get("members") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
    } catch (e: Exception) {
        Log.e("HomeActivity", "Error getting team user IDs: ${e.message}")
        emptyList()
    }
}

private suspend fun getUserDisplayNames(userIds: List<String>): Map<String, String> = withContext(Dispatchers.IO) {
    try {
        val db = FirebaseFirestore.getInstance()
        val userNames = mutableMapOf<String, String>()

        userIds.forEach { userId ->
            try {
                val userDoc = db.collection("users").document(userId).get().await()
                val displayName = userDoc.getString("displayName")
                    ?: userDoc.getString("name")
                    ?: userDoc.getString("email")?.substringBefore("@")
                    ?: "Unknown User"
                userNames[userId] = displayName
            } catch (e: Exception) {
                Log.e("HomeActivity", "Error getting name for user $userId: ${e.message}")
                userNames[userId] = "Unknown User"
            }
        }

        userNames
    } catch (e: Exception) {
        Log.e("HomeActivity", "Error getting user display names: ${e.message}")
        emptyMap()
    }
}

private suspend fun startTeamHealthMonitoringWithContext(
    context: Context,
    onAlertsUpdate: (List<AlertData>, Boolean) -> Unit
) = withContext(Dispatchers.IO) {
    val mqttHelper = MQTTHelper(context)
    val aiHelper = AIHelper(context)
    val userSensorData = mutableMapOf<String, MutableList<SensorRecord>>()
    var lastStatusWasPerfect = true
    var lastAlertMessage = "" // Track last alert to avoid duplicates

    try {
        onAlertsUpdate(emptyList(), true) // Show loading

        val teamUserIds = getTeamUserIds()
        if (teamUserIds.isEmpty()) {
            onAlertsUpdate(listOf(AlertData("No team members found", "info", true)), false)
            return@withContext
        }

        val userDisplayNames = getUserDisplayNames(teamUserIds)
        Log.d("HomeActivity", "👥 User display names: $userDisplayNames")

        subscribeToTeamMqttTopics(mqttHelper, teamUserIds) { userId: String, sensorData: SensorRecord ->
            userSensorData.getOrPut(userId) { mutableListOf() }.add(sensorData)
            if (userSensorData[userId]!!.size > 10) {
                userSensorData[userId]!!.removeAt(0)
            }
        }

        // Monitor every minute
        while (true) {
            delay(60000) // 1 minute

            if (userSensorData.isNotEmpty()) {
                val dataForAI = userSensorData.entries.joinToString("\n") { (userId, records) ->
                    val latest = records.lastOrNull()
                    val userName = userDisplayNames[userId] ?: "Unknown User"
                    "User $userName: HR=${latest?.heartRate ?: "N/A"}, " +
                            "Breath=${latest?.breathFrequency ?: "N/A"}, " +
                            "HRV=${latest?.hrv ?: "N/A"}, " +
                            "Intensity=${latest?.intensity ?: "N/A"}"
                }

                val prompt = """
                Analyze this team cycling data and determine if anyone needs help:
                $dataForAI
                
                The data is gathered from multiple users in a cycling team.
                
                You must respond with ONLY one of these formats:
                - "Everything is perfect and everyone is healthy" if all normal
                - A single sentence describing the specific issue like "User [Name] has dangerously high heart rate - may need immediate help"
                
                Use the actual user names provided, not User IDs.
                Focus on safety-critical issues only.
                Do not provide explanations or additional context - just the direct assessment.
                """.trimIndent()

                val aiResponse = aiHelper.sendMessage(prompt)
                aiResponse.onSuccess { response ->
                    Log.d("HomeActivity", "🧠 AI API raw response: $response")

                    val cleanedResponse = parseAlertsFromResponse(response)
                    val responseText = cleanedResponse.firstOrNull() ?: ""

                    // Only add new alert if it's different from the last one
                    if (responseText != lastAlertMessage) {
                        val isPerfect = responseText.contains("Everything is perfect") ||
                                responseText.contains("everyone is healthy")

                        if (!isPerfect) {
                            // There are issues - add new alert
                            val newAlerts = listOf(AlertData(responseText, "danger", false))
                            onAlertsUpdate(newAlerts, false)

                            // Send notification for critical issues
                            if (responseText.contains("dangerously") ||
                                responseText.contains("immediate help")) {
                                sendDangerNotification(context, responseText)
                            }
                        }

                        lastAlertMessage = responseText
                        lastStatusWasPerfect = isPerfect
                    }
                }

                aiResponse.onFailure { error ->
                    Log.e("HomeActivity", "❌ AI API error: ${error.message}")
                }
            } else {
                Log.d("HomeActivity", "⏳ No sensor data available yet")
            }
        }
    } catch (e: Exception) {
        Log.e("HomeActivity", "❌ Error in team health monitoring: ${e.message}")
    }
}

private suspend fun subscribeToTeamMqttTopics(
    mqttHelper: MQTTHelper,
    teamUserIds: List<String>,
    onSensorData: (String, SensorRecord) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        teamUserIds.forEach { userId ->
            mqttHelper.connectAndSubscribeToUser(
                userId = userId,
                onConnected = {
                    Log.d("HomeActivity", "Connected to user $userId MQTT topic")
                },
                onLocationUpdate = { sensorRecord ->
                    onSensorData(userId, sensorRecord)
                },
                onError = { error ->
                    Log.e("HomeActivity", "Error subscribing to user $userId: $error")
                }
            )
        }
    } catch (e: Exception) {
        Log.e("HomeActivity", "Error subscribing to team MQTT topics: ${e.message}")
    }
}

private fun sendDangerNotification(context: Context, message: String) {
    try {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        // Check for notification permission on Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w("HomeActivity", "POST_NOTIFICATIONS permission not granted, cannot send notification")
                return // Exit early if permission not granted
            }
        }

        // Create notification channel for Android 8.0+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "team_alerts",
                "Team Health Alerts",
                android.app.NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = androidx.core.app.NotificationCompat.Builder(context, "team_alerts")
            .setContentTitle("Team Health Alert")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        Log.d("HomeActivity", "Notification sent: $message")

    } catch (e: Exception) {
        Log.e("HomeActivity", "Error sending notification: ${e.message}")
    }
}

private fun parseAlertsFromResponse(response: String): List<String> {
    // Clean and format the response as a single alert
    val cleanResponse = response
        .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1") // Remove **bold**
        .replace(Regex("\\*([^*]+)\\*"), "$1") // Remove *italic*
        .replace(Regex("__([^_]+)__"), "$1") // Remove __bold__
        .replace(Regex("_([^_]+)_"), "$1") // Remove _italic_
        .replace(Regex("`([^`]+)`"), "$1") // Remove `code`
        .replace(Regex("#{1,6}\\s*"), "") // Remove markdown headers
        .replace(Regex("^[-*+]\\s+", RegexOption.MULTILINE), "• ") // Convert list items to bullets
        .trim()

    return listOf(cleanResponse)
}

@Composable
fun QuickActionsSection() {
    val context = LocalContext.current
    val gpsHelper = remember { GPSHelper(context) }

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
                ) {
                    val intent = Intent(context, TeamMapActivity::class.java)
                    context.startActivity(intent)
                }

                QuickActionButton(
                    text = "Help",
                    icon = Icons.AutoMirrored.Filled.Help,
                    modifier = Modifier.weight(1f)
                ) {
                    val intent = Intent(context, HelpChatActivity::class.java)
                    context.startActivity(intent)
                }
            }

            QuickActionButton(
                text = "History & Records",
                icon = Icons.Filled.History,
                modifier = Modifier.fillMaxWidth()
            ) {
                val intent = Intent(context, RideHistoryActivity::class.java)
                context.startActivity(intent)
            }
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