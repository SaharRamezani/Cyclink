package com.example.cyclink.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import androidx.core.app.NotificationCompat
import android.hardware.SensorEventListener
import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import com.example.cyclink.chat.AIHelper
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

                                    // Get all members
                                    members.forEachIndexed { index, memberId ->
                                        if (memberId is String && index < memberNames.size) {
                                            val memberName = memberNames[index] as? String ?: "Unknown"

                                            // Get real-time status from sensor data
                                            db.collection("sensor_records")
                                                .whereEqualTo("userId", memberId)
                                                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                                                .limit(1)
                                                .get()
                                                .addOnSuccessListener { sensorDocs ->
                                                    val latestRecord = sensorDocs.documents.firstOrNull()
                                                    val currentTime = System.currentTimeMillis()

                                                    val status = when {
                                                        latestRecord == null -> "offline"
                                                        currentTime - (latestRecord.getLong("timestamp") ?: 0) < 60000 -> "online" // Active in last minute
                                                        currentTime - (latestRecord.getLong("timestamp") ?: 0) < 300000 -> "riding" // Active in last 5 minutes
                                                        else -> "offline"
                                                    }

                                                    val heartRate = latestRecord?.getDouble("heartRate")?.toInt() ?: 0
                                                    val speed = latestRecord?.getDouble("gpsSpeed") ?: 0.0
                                                    val lastSeen = latestRecord?.getLong("timestamp") ?: 0L

                                                    val member = TeamMember(
                                                        id = memberId,
                                                        name = memberName,
                                                        status = status,
                                                        heartRate = heartRate,
                                                        speed = speed,
                                                        lastSeen = lastSeen
                                                    )

                                                    // Update the member in the list
                                                    val existingIndex = memberList.indexOfFirst { it.id == memberId }
                                                    if (existingIndex >= 0) {
                                                        memberList[existingIndex] = member
                                                    } else {
                                                        memberList.add(member)
                                                    }

                                                    teamMembers = memberList.toList()
                                                }
                                                .addOnFailureListener {
                                                    // Fallback if sensor data query fails
                                                    memberList.add(
                                                        TeamMember(
                                                            id = memberId,
                                                            name = memberName,
                                                            status = "unknown"
                                                        )
                                                    )
                                                    teamMembers = memberList.toList()
                                                }
                                        }
                                    }
                                    isLoading = false
                                }
                        } else {
                            isLoading = false
                        }
                    } else {
                        isLoading = false
                    }
                }
                .addOnFailureListener {
                    isLoading = false
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
        Log.d("HomeActivity", "📊 User ID from MQTT: '${sensorData.userId}'")
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

            // Force immediate GPS check
            val hasGps = gpsHelper.hasLocationPermissions()
            Log.d("HomeActivity", "📍 Has GPS permissions: $hasGps")

            // Start GPS immediately and wait longer for first fix
            gpsHelper.startLocationUpdates { gpsData ->
                currentGPS = gpsData
                Log.d("HomeActivity", "📍 GPS RECEIVED: ${gpsData.latitude}, ${gpsData.longitude}, accuracy=${gpsData.accuracy}")
            }

            // Get last known location immediately
            val lastKnown = gpsHelper.getLastKnownLocation()
            if (lastKnown != null) {
                currentGPS = lastKnown
                Log.d("HomeActivity", "📍 Using last known GPS: ${lastKnown.latitude}, ${lastKnown.longitude}")
            } else {
                Log.w("HomeActivity", "⚠️ No last known location available")
            }

            // Request immediate location with longer timeout
            gpsHelper.requestImmediateLocation { immediateGPS ->
                if (immediateGPS != null) {
                    currentGPS = immediateGPS
                    Log.d("HomeActivity", "📍 Immediate GPS acquired: ${immediateGPS.latitude}, ${immediateGPS.longitude}")
                } else {
                    Log.w("HomeActivity", "⚠️ Immediate GPS request failed")
                }
            }

            // Wait longer for GPS before starting MQTT
            var gpsWaitCount = 0
            while (currentGPS == null && gpsWaitCount < 15) { // 15 seconds
                delay(1000)
                gpsWaitCount++
                Log.d("HomeActivity", "⏳ Waiting for GPS... ${gpsWaitCount}s")
            }

            if (currentGPS == null) {
                Log.e("HomeActivity", "❌ GPS failed to initialize after 15 seconds!")
            } else {
                Log.d("HomeActivity", "✅ GPS ready, starting MQTT...")
            }

            // Start MQTT regardless (but saves will be skipped without GPS)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    mqttHelper.connect(
                        onConnected = {
                            Log.d("HomeActivity", "✅ MQTT connected")
                        },
                        onError = { error ->
                            Log.e("HomeActivity", "❌ MQTT error: $error")
                        },
                        onSensorDataReceived = onMQTTSensorDataReceived
                    )
                } catch (e: Exception) {
                }
            }
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
                    // Remove any newlines from the message and add timestamp on same line
                    val cleanMessage = alert.message.replace("\n", " ").trim()
                    alert.copy(message = "$cleanMessage (${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(currentTime))})")
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

private fun buildFormattedSensorData(
    userSensorData: Map<String, MutableList<SensorRecord>>,
    userDisplayNames: Map<String, String>
): String {
    val sb = StringBuilder()

    userSensorData.forEach { (userId, records) ->
        val displayName = userDisplayNames[userId] ?: "Unknown User"
        val latestRecord = records.firstOrNull()

        if (latestRecord != null) {
            val timeSinceLastData = System.currentTimeMillis() - latestRecord.timestamp
            val minutesAgo = timeSinceLastData / 60000

            sb.append("$displayName: ")

            // Add vital signs if available
            latestRecord.heartRate?.let { hr ->
                sb.append("Heart Rate: ${hr.toInt()} bpm, ")
            }

            latestRecord.breathFrequency?.let { breath ->
                sb.append("Breathing: ${breath.toInt()}/min, ")
            }

            latestRecord.gpsSpeed?.let { speed ->
                sb.append("Speed: ${"%.1f".format(speed)} km/h, ")
            }

            latestRecord.intensity?.let { intensity ->
                sb.append("Activity Level: ${"%.1f".format(intensity)}, ")
            }

            // Add time since last data
            when {
                minutesAgo < 1 -> sb.append("Data: Just now")
                minutesAgo < 5 -> sb.append("Data: ${minutesAgo}min ago")
                minutesAgo < 10 -> sb.append("Data: ${minutesAgo}min ago (Getting old)")
                else -> sb.append("Data: ${minutesAgo}min ago (STALE - Possible emergency)")
            }

            sb.append("\n")
        } else {
            sb.append("$displayName: No recent data available\n")
        }
    }

    return sb.toString().trim()
}

private suspend fun startTeamHealthMonitoringWithContext(
    context: Context,
    onAlertsUpdate: (List<AlertData>, Boolean) -> Unit
) = withContext(Dispatchers.IO) {
    val aiHelper = AIHelper(context)
    val userSensorData = mutableMapOf<String, MutableList<SensorRecord>>()
    val userDisplayNames = mutableMapOf<String, String>()
    val listeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()
    var lastAlertMessage = ""
    var consecutiveFailures = 0

    try {
        onAlertsUpdate(emptyList(), true)

        val teamUserIds = getTeamUserIds()
        if (teamUserIds.isEmpty()) {
            onAlertsUpdate(listOf(AlertData("No team members found", "info", true)), false)
            return@withContext
        }

        // Get display names for all team members
        val displayNamesMap = getUserDisplayNames(teamUserIds)
        userDisplayNames.putAll(displayNamesMap)

        Log.d("HomeActivity", "🔍 Starting monitoring for ${teamUserIds.size} team members: ${displayNamesMap.values}")

        val db = FirebaseFirestore.getInstance()
        teamUserIds.forEach { userId ->
            val listener = db.collection("sensor_records")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(10)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("HomeActivity", "Firestore listener error for $userId: ${error.message}")
                        return@addSnapshotListener
                    }

                    snapshot?.documents?.forEach { doc ->
                        try {
                            val sensorRecord = doc.toObject(SensorRecord::class.java)
                            if (sensorRecord != null) {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - sensorRecord.timestamp < 300000) { // Last 5 minutes
                                    if (!userSensorData.containsKey(userId)) {
                                        userSensorData[userId] = mutableListOf()
                                    }
                                    userSensorData[userId]?.add(0, sensorRecord)
                                    if (userSensorData[userId]?.size ?: 0 > 5) {
                                        userSensorData[userId] = userSensorData[userId]?.take(5)?.toMutableList() ?: mutableListOf()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("HomeActivity", "Error parsing sensor record: ${e.message}")
                        }
                    }
                }

            listeners.add(listener)
        }

        // Monitor every minute
        while (true) {
            delay(60000)

            if (userSensorData.isNotEmpty()) {
                try {
                    // Create formatted data with display names
                    val formattedData = buildFormattedSensorData(userSensorData, userDisplayNames)

                    val prompt = """
                    Analyze this team cycling data and determine if anyone needs help:
                    
                    $formattedData
                    
                    The data shows team members with their latest vital signs and sensor readings.
                    
                    You must respond with ONLY one of these formats:
                    - "Everything is perfect and everyone is healthy" if all measurements are normal
                    - A single sentence describing the specific issue like "[Name] has dangerously high heart rate - may need immediate help"
                    
                    Focus on safety-critical issues only:
                    - Heart rate above 180 bpm or below 50 bpm while riding
                    - Breathing frequency above 40/min or below 10/min
                    - No sensor data for over 10 minutes (potential emergency)
                    
                    Use the exact names provided. Do not provide explanations - just the direct assessment.
                    """.trimIndent()

                    val result = aiHelper.sendMessage(prompt)
                    result.fold(
                        onSuccess = { response ->
                            Log.d("HomeActivity", "🤖 AI Response: $response")

                            // Add the emergency detection logic here
                            when {
                                response.contains("immediate help", ignoreCase = true) ||
                                        response.contains("emergency", ignoreCase = true) ||
                                        response.contains("critical", ignoreCase = true) ||
                                        response.contains("danger", ignoreCase = true) ||
                                        response.contains("urgent medical", ignoreCase = true) ||
                                        response.contains("dangerously high", ignoreCase = true) ||
                                        response.contains("may need immediate help", ignoreCase = true) -> {

                                    Log.e("HomeActivity", "🚨 EMERGENCY DETECTED!")

                                    // Send the emergency notification
                                    sendDangerNotification(context, response)

                                    // Also update alerts
                                    onAlertsUpdate(listOf(AlertData(response, "emergency", false)), false)
                                    lastAlertMessage = response
                                }

                                response.contains("perfect", ignoreCase = true) ||
                                        response.contains("healthy", ignoreCase = true) ||
                                        response.contains("Everything is perfect", ignoreCase = true) -> {
                                    Log.d("HomeActivity", "✅ Team health normal")

                                    if (lastAlertMessage.isNotEmpty() && !lastAlertMessage.contains("perfect", ignoreCase = true)) {
                                        // Clear previous alerts when everything becomes normal
                                        onAlertsUpdate(listOf(AlertData("All clear - team health is now normal", "info", true)), false)
                                    }
                                    lastAlertMessage = response
                                }

                                else -> {
                                    // Handle other health warnings (non-emergency)
                                    if (response != lastAlertMessage) {
                                        val alerts = parseAlertsFromResponse(response)
                                        val alertData = alerts.map {
                                            AlertData(it, "health", false)
                                        }
                                        onAlertsUpdate(alertData, false)
                                        lastAlertMessage = response
                                    }
                                }
                            }
                            consecutiveFailures = 0
                        },
                        onFailure = { error ->
                            consecutiveFailures++
                            if (consecutiveFailures <= 3) {
                                Log.e("HomeActivity", "AI analysis failed (attempt $consecutiveFailures): ${error.message}")
                                onAlertsUpdate(listOf(AlertData("AI health analysis temporarily unavailable", "warning", false)), false)
                            }
                        }
                    )
                } catch (e: Exception) {
                    Log.e("HomeActivity", "❌ Exception during AI analysis: ${e.message}")
                }
            }
        }

    } catch (e: Exception) {
        Log.e("HomeActivity", "❌ Error in team health monitoring: ${e.message}")
        onAlertsUpdate(listOf(AlertData("Health monitoring error: ${e.message}", "error", false)), false)
    } finally {
        listeners.forEach { it.remove() }
    }
}

private fun sendDangerNotification(context: Context, message: String) {
    try {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        // Create notification channel for Android 8.0+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "emergency_channel",
                "Emergency Alerts",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical health alerts for team members"
                enableVibration(true)
                enableLights(true)
                lightColor = android.graphics.Color.RED
                setSound(
                    android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM),
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, "emergency_channel")
            .setContentTitle("🚨 EMERGENCY ALERT")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))
            .setLights(android.graphics.Color.RED, 1000, 1000)
            .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM))
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setFullScreenIntent(
                android.app.PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, HomeActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    },
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                ),
                true
            )
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        Log.d("HomeActivity", "🔔 Emergency notification sent: $message")

        // Also show a toast for immediate visibility
        if (context is android.app.Activity) {
            context.runOnUiThread {
                android.widget.Toast.makeText(
                    context,
                    "EMERGENCY: $message",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    } catch (e: Exception) {
        Log.e("HomeActivity", "❌ Failed to send emergency notification: ${e.message}")
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