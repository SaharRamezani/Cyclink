package com.example.cyclink.team

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cyclink.R
import com.example.cyclink.helpers.MQTTHelper
import com.example.cyclink.helpers.SensorRecord
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TeamDashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TeamDashboardScreen()
            }
        }
    }
}

@Composable
fun TeamDashboardScreen() {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var teamMembers by remember { mutableStateOf<List<TeamMember>>(emptyList()) }
    var teamName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // MQTT helper for real-time data
    val mqttHelper = remember(context) { MQTTHelper(context) }

    // Store real-time sensor data for each user
    var userSensorData by remember { mutableStateOf<Map<String, SensorRecord>>(emptyMap()) }
    var userDisplayNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // Function to determine if user is online based on last data timestamp
    fun isUserOnline(userId: String): String {
        val lastData = userSensorData[userId]
        return if (lastData != null) {
            val timeDiff = System.currentTimeMillis() - lastData.timestamp
            when {
                timeDiff < 5 * 60 * 1000 -> "online" // Last 5 minutes
                timeDiff < 15 * 60 * 1000 -> "riding" // Last 15 minutes
                else -> "offline"
            }
        } else {
            "offline"
        }
    }

    // Load team data and subscribe to MQTT
    LaunchedEffect(Unit) {
        val user = auth.currentUser
        if (user != null) {
            // Get team information
            db.collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener { userDoc ->
                    val teamData = userDoc.get("currentTeam") as? Map<*, *>
                    if (teamData != null) {
                        val teamId = teamData["teamId"] as? String ?: ""
                        teamName = teamData["teamName"] as? String ?: "My Team"

                        if (teamId.isNotEmpty()) {
                            // Get team members
                            db.collection("teams")
                                .document(teamId)
                                .get()
                                .addOnSuccessListener { teamDoc ->
                                    val members = teamDoc.get("members") as? List<*> ?: emptyList<String>()
                                    val memberNames = teamDoc.get("memberNames") as? List<*> ?: emptyList<String>()

                                    // Store display names
                                    val namesMap = mutableMapOf<String, String>()
                                    members.forEachIndexed { index, memberId ->
                                        if (memberId is String && index < memberNames.size) {
                                            val memberName = memberNames[index] as? String ?: "Unknown"
                                            namesMap[memberId] = memberName
                                        }
                                    }
                                    userDisplayNames = namesMap

                                    // Subscribe to MQTT for each team member
                                    members.forEach { memberId ->
                                        if (memberId is String) {
                                            subscribeToUserMqtt(mqttHelper, memberId) { sensorData ->
                                                userSensorData = userSensorData + (memberId to sensorData)
                                                Log.d("TeamDashboard", "📊 Updated data for $memberId: HR=${sensorData.heartRate}, Speed=${sensorData.gpsSpeed}")
                                            }
                                        }
                                    }

                                    isLoading = false
                                }
                                .addOnFailureListener { exception ->
                                    Log.e("TeamDashboard", "Error loading team: ${exception.message}")
                                    isLoading = false
                                }
                        } else {
                            isLoading = false
                        }
                    } else {
                        isLoading = false
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("TeamDashboard", "Error loading user: ${exception.message}")
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

    // Update team members list when sensor data changes
    LaunchedEffect(userSensorData, userDisplayNames) {
        if (userDisplayNames.isNotEmpty()) {
            val updatedMembers = userDisplayNames.map { (userId, displayName) ->
                val sensorData = userSensorData[userId]
                val status = isUserOnline(userId)

                TeamMember(
                    id = userId,
                    name = displayName,
                    status = status,
                    heartRate = sensorData?.heartRate?.toInt() ?: 0,
                    speed = sensorData?.gpsSpeed?.toDouble() ?: 0.0,
                    lastSeen = sensorData?.timestamp ?: 0L,
                    alerts = emptyList() // We can add alert logic later
                )
            }
            teamMembers = updatedMembers
            Log.d("TeamDashboard", "👥 Updated team members: ${teamMembers.size} members")
        }
    }

    // Rest of the UI code remains the same...
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
                IconButton(
                    onClick = {
                        if (context is ComponentActivity) {
                            context.finish()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colorResource(id = R.color.honeydew)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Team Dashboard",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(id = R.color.honeydew)
                    )
                    if (teamName.isNotEmpty()) {
                        Text(
                            text = teamName,
                            fontSize = 14.sp,
                            color = colorResource(id = R.color.non_photo_blue)
                        )
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = colorResource(id = R.color.honeydew)
                    )
                }
            } else {
                // Team Stats Card with real data
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorResource(id = R.color.honeydew)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatColumn("Members", teamMembers.size.toString())
                        StatColumn(
                            "Online",
                            teamMembers.count { it.status == "online" || it.status == "riding" }.toString()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Members List with real data
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    items(teamMembers) { member ->
                        TeamMemberCard(
                            member = member,
                            onMemberClick = {
                                val intent = Intent(context, MemberDetailActivity::class.java).apply {
                                    putExtra("memberId", member.id)
                                    putExtra("memberName", member.name)
                                }
                                context.startActivity(intent)
                            },
                            onLocationClick = {
                                val intent = Intent(context, MemberMapActivity::class.java).apply {
                                    putExtra("memberId", member.id)
                                    putExtra("memberName", member.name)
                                    putExtra("userId", member.id)
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }

    // Cleanup MQTT connections when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            mqttHelper.disconnect()
        }
    }
}

// Helper function to subscribe to a specific user's MQTT data
private fun subscribeToUserMqtt(
    mqttHelper: MQTTHelper,
    userId: String,
    onSensorData: (SensorRecord) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            mqttHelper.connectAndSubscribeToUser(
                userId = userId,
                onConnected = {
                    Log.d("TeamDashboard", "✅ Connected to MQTT for user: $userId")
                },
                onLocationUpdate = { sensorRecord ->
                    Log.d("TeamDashboard", "📍 Received sensor data for $userId: $sensorRecord")
                    onSensorData(sensorRecord)
                },
                onError = { error ->
                    Log.e("TeamDashboard", "❌ MQTT error for user $userId: $error")
                }
            )
        } catch (e: Exception) {

        }
    }
}

@Composable
fun StatColumn(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(id = R.color.berkeley_blue)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = colorResource(id = R.color.berkeley_blue).copy(alpha = 0.7f)
        )
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun TeamMemberCard(
    member: TeamMember,
    onMemberClick: () -> Unit,
    onLocationClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMemberClick() },
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
            // Profile Circle with status indicator
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(getStatusColor(member.status).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = member.name.take(2).uppercase(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = getStatusColor(member.status)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Member Info with real data
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = member.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorResource(id = R.color.berkeley_blue)
                    )

                    // Online status indicator
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(getStatusColor(member.status))
                    )
                }

                Text(
                    text = member.status.replaceFirstChar { it.uppercase() },
                    fontSize = 12.sp,
                    color = colorResource(id = R.color.berkeley_blue).copy(alpha = 0.6f)
                )

                // Show real sensor data if user is online/riding
                if (member.status == "riding" || member.status == "online") {
                    if (member.heartRate > 0 || member.speed > 0) {
                        Text(
                            text = buildString {
                                if (member.heartRate > 0) {
                                    append("♥ ${member.heartRate} bpm")
                                }
                                if (member.speed > 0) {
                                    if (member.heartRate > 0) append(" • ")
                                    append("${String.format("%.1f", member.speed)} km/h")
                                }
                            },
                            fontSize = 12.sp,
                            color = colorResource(id = R.color.berkeley_blue).copy(alpha = 0.8f)
                        )
                    } else {
                        Text(
                            text = "Waiting for sensor data...",
                            fontSize = 11.sp,
                            color = colorResource(id = R.color.berkeley_blue).copy(alpha = 0.5f)
                        )
                    }
                }

                // Show last seen for offline users
                if (member.status == "offline" && member.lastSeen > 0) {
                    val timeDiff = System.currentTimeMillis() - member.lastSeen
                    val lastSeenText = when {
                        timeDiff < 60 * 1000 -> "Just now"
                        timeDiff < 60 * 60 * 1000 -> "${timeDiff / (60 * 1000)} min ago"
                        timeDiff < 24 * 60 * 60 * 1000 -> "${timeDiff / (60 * 60 * 1000)} hr ago"
                        else -> "${timeDiff / (24 * 60 * 60 * 1000)} days ago"
                    }
                    Text(
                        text = "Last seen: $lastSeenText",
                        fontSize = 11.sp,
                        color = colorResource(id = R.color.berkeley_blue).copy(alpha = 0.5f)
                    )
                }

                if (member.alerts.isNotEmpty()) {
                    Text(
                        text = "${member.alerts.size} alert${if (member.alerts.size > 1) "s" else ""}",
                        fontSize = 11.sp,
                        color = colorResource(id = R.color.red_pantone)
                    )
                }
            }

            // Location Button
            IconButton(
                onClick = onLocationClick
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = "View on map",
                    tint = colorResource(id = R.color.berkeley_blue)
                )
            }
        }
    }
}

@Composable
fun getStatusColor(status: String): Color {
    return when (status) {
        "online" -> colorResource(id = R.color.non_photo_blue)
        "riding" -> colorResource(id = R.color.cerulean)
        "offline" -> colorResource(id = R.color.berkeley_blue).copy(alpha = 0.5f)
        else -> colorResource(id = R.color.berkeley_blue)
    }
}