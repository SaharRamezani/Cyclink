package com.example.cyclink.team

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.cyclink.helpers.MQTTHelper
import com.google.android.gms.maps.CameraUpdateFactory
import androidx.compose.foundation.background
import com.example.cyclink.helpers.SensorRecord
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.example.cyclink.R

class TeamMapActivity : ComponentActivity() {
    private lateinit var mqttHelper: MQTTHelper
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mqttHelper = MQTTHelper(this)

        setContent {
            MaterialTheme {
                TeamMapScreen(mqttHelper = mqttHelper, db = db, auth = auth)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttHelper.disconnect()
    }
}

@Composable
fun TeamMapScreen(mqttHelper: MQTTHelper, db: FirebaseFirestore, auth: FirebaseAuth) {
    val context = LocalContext.current

    // State for team members and their locations
    var teamMembers by remember { mutableStateOf<List<TeamMember>>(emptyList()) }
    var memberLocations by remember { mutableStateOf<Map<String, LatLng>>(emptyMap()) }
    var memberLastSeen by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentTeamId by remember { mutableStateOf<String?>(null) }

    // Map states
    val defaultLocation = LatLng(1.35, 103.87) // Singapore
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 15f)
    }

    var uiSettings by remember {
        mutableStateOf(MapUiSettings(zoomControlsEnabled = true))
    }
    var properties by remember {
        mutableStateOf(MapProperties(mapType = MapType.NORMAL))
    }

    // Load team members
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
                        currentTeamId = teamId

                        if (teamId.isNotEmpty()) {
                            db.collection("teams")
                                .document(teamId)
                                .get()
                                .addOnSuccessListener { teamDoc ->
                                    val members = teamDoc.get("members") as? List<*> ?: emptyList<String>()
                                    val memberNames = teamDoc.get("memberNames") as? List<*> ?: emptyList<String>()

                                    val memberList = mutableListOf<TeamMember>()
                                    members.forEachIndexed { index, memberId ->
                                        if (memberId is String && index < memberNames.size) {
                                            val memberName = memberNames[index] as? String ?: "Unknown"
                                            memberList.add(
                                                TeamMember(
                                                    id = memberId,
                                                    name = memberName,
                                                    status = "online"
                                                )
                                            )
                                        }
                                    }
                                    teamMembers = memberList
                                    isLoading = false

                                    Log.d("TeamMapActivity", "Team members loaded: ${memberList.size}")
                                }
                                .addOnFailureListener { exception ->
                                    Log.e("TeamMapActivity", "Error loading team", exception)
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
                    Log.e("TeamMapActivity", "Error loading user", exception)
                    isLoading = false
                }
        }
    }

    // Subscribe to all team members' MQTT topics
    LaunchedEffect(teamMembers) {
        if (teamMembers.isNotEmpty()) {
            teamMembers.forEach { member ->
                mqttHelper.connectAndSubscribeToUser(
                    userId = member.id,
                    onConnected = {
                        Log.d("TeamMapActivity", "Connected to MQTT for team member: ${member.name}")
                    },
                    onLocationUpdate = { sensorRecord ->
                        val newLocation = LatLng(sensorRecord.latitude, sensorRecord.longitude)
                        memberLocations = memberLocations.toMutableMap().apply {
                            put(member.id, newLocation)
                        }
                        memberLastSeen = memberLastSeen.toMutableMap().apply {
                            put(member.id, System.currentTimeMillis())
                        }

                        Log.d("TeamMapActivity", "Location updated for ${member.name}: ${sensorRecord.latitude}, ${sensorRecord.longitude}")
                    },
                    onError = { error ->
                        Log.e("TeamMapActivity", "MQTT connection error for ${member.name}: $error")
                    }
                )
            }
        }
    }

    // Update camera to show all team members
    LaunchedEffect(memberLocations) {
        if (memberLocations.isNotEmpty()) {
            val locations = memberLocations.values.toList()
            if (locations.size == 1) {
                // Single location - center on it
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(locations.first(), 15f),
                    1000
                )
            } else if (locations.size > 1) {
                // Multiple locations - fit all in view
                val bounds = com.google.android.gms.maps.model.LatLngBounds.builder()
                locations.forEach { bounds.include(it) }

                try {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngBounds(bounds.build(), 100),
                        1000
                    )
                } catch (e: Exception) {
                    Log.e("TeamMapActivity", "Error updating camera bounds", e)
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = properties,
                uiSettings = uiSettings
            ) {
                // Add markers for each team member
                memberLocations.forEach { (userId, location) ->
                    val member = teamMembers.find { it.id == userId }
                    val lastSeen = memberLastSeen[userId] ?: 0
                    val isRecent = (System.currentTimeMillis() - lastSeen) < 30000 // 30 seconds

                    Marker(
                        state = MarkerState(position = location),
                        title = member?.name ?: "Unknown",
                        snippet = if (isRecent) "Live Location" else "Last seen: ${formatLastSeen(lastSeen)}",
                        icon = createBicycleIcon(context, isRecent)
                    )
                }
            }

            // Controls overlay
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                Button(
                    onClick = {
                        properties = properties.copy(
                            mapType = if (properties.mapType == MapType.NORMAL)
                                MapType.SATELLITE else MapType.NORMAL
                        )
                    }
                ) {
                    Text(
                        if (properties.mapType == MapType.NORMAL)
                            "Satellite"
                        else
                            "Normal"
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Team status card
                Card(
                    modifier = Modifier.padding(4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = "Team Members: ${teamMembers.size}",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = "Online: ${memberLocations.size}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            // Team member list overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Team Members",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        teamMembers.forEach { member ->
                            val hasLocation = memberLocations.containsKey(member.id)
                            val lastSeen = memberLastSeen[member.id] ?: 0
                            val isRecent = (System.currentTimeMillis() - lastSeen) < 30000

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = member.name,
                                    style = MaterialTheme.typography.bodySmall
                                )

                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            color = when {
                                                hasLocation && isRecent -> Color.Green
                                                hasLocation -> Color.Yellow
                                                else -> Color.Red
                                            },
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun createBicycleIcon(context: android.content.Context, isRecent: Boolean): BitmapDescriptor {
    return try {
        // Try to create a bicycle icon
        val drawable = VectorDrawableCompat.create(
            context.resources,
            R.drawable.ic_directions_bike_24, // You'll need to add this drawable
            null
        ) ?: run {
            // Fallback to a colored circle if bicycle icon not available
            val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = android.graphics.Paint().apply {
                color = if (isRecent) android.graphics.Color.GREEN else android.graphics.Color.YELLOW
                style = android.graphics.Paint.Style.FILL
            }
            canvas.drawCircle(32f, 32f, 24f, paint)

            // Draw bike symbol
            paint.color = android.graphics.Color.WHITE
            paint.textSize = 20f
            paint.textAlign = android.graphics.Paint.Align.CENTER
            canvas.drawText("🚴", 32f, 40f, paint)

            return BitmapDescriptorFactory.fromBitmap(bitmap)
        }

        drawable.setBounds(0, 0, 64, 64)
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Set color based on recency
        drawable.setTint(if (isRecent) android.graphics.Color.GREEN else android.graphics.Color.YELLOW)
        drawable.draw(canvas)

        BitmapDescriptorFactory.fromBitmap(bitmap)
    } catch (e: Exception) {
        Log.e("TeamMapActivity", "Error creating bicycle icon", e)
        // Fallback to default marker with color
        if (isRecent) {
            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
        } else {
            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
        }
    }
}

private fun formatLastSeen(timestamp: Long): String {
    if (timestamp == 0L) return "Never"

    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / (1000 * 60)

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        else -> "${minutes / 60}h ago"
    }
}