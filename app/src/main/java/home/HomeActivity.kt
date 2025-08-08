package com.example.cyclink.home

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
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
fun HomeScreen() {
    val scrollState = rememberScrollState()
    var userRole by remember { mutableStateOf("member") }
    var isRiding by remember { mutableStateOf(false) }

    // Get user role from Firestore
    LaunchedEffect(Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    val teamData = document.get("currentTeam") as? Map<String, Any>
                    userRole = teamData?.get("role") as? String ?: "member"
                }
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
                .padding(16.dp)
        ) {
            // Header
            HeaderSection()

            Spacer(modifier = Modifier.height(16.dp))

            // Session Summary
            SessionSummarySection(isRiding = isRiding) { isRiding = it }

            Spacer(modifier = Modifier.height(16.dp))

            // Personal Status Section
            PersonalStatusSection()

            Spacer(modifier = Modifier.height(16.dp))

            // Warnings/Alerts Panel
            AlertsSection()

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Actions
            QuickActionsSection()

            Spacer(modifier = Modifier.height(16.dp))

            // Team Dashboard (Only for admin)
            if (userRole == "admin") {
                TeamDashboardSection()
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))
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
                text = "CycLink",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.honeydew)
            )
            Text(
                text = "Ready to ride together",
                fontSize = 14.sp,
                color = colorResource(id = R.color.non_photo_blue)
            )
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colorResource(id = R.color.honeydew).copy(alpha = 0.2f))
                .clickable {
                    val intent = Intent(context, com.example.cyclink.account.AccountActivity::class.java)
                    context.startActivity(intent)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "Profile",
                tint = colorResource(id = R.color.honeydew),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun PersonalStatusSection() {
    StatusCard(
        title = "Personal Status",
        icon = Icons.Filled.Favorite
    ) {
        Column {
            // Vitals Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                VitalMetric("Heart Rate", "72 bpm", Icons.Filled.Favorite)
                VitalMetric("Breathing", "18 /min", Icons.Filled.Favorite)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                VitalMetric("Speed", "0 km/h", Icons.Filled.Speed)
                VitalMetric("Cadence", "0 rpm", Icons.Filled.Refresh)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // GPS Status
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = colorResource(id = R.color.non_photo_blue),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GPS Connected • Sensors Active",
                    fontSize = 12.sp,
                    color = colorResource(id = R.color.berkeley_blue).copy(alpha = 0.7f)
                )
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

    // Load real team data
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
                // Real team members list
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
fun AlertsSection() {
    StatusCard(
        title = "Alerts & Warnings",
        icon = Icons.Filled.Warning
    ) {
        Column {
            AlertItem("All sensors connected", "Good", true)
            AlertItem("Heart rate normal", "Good", true)
            AlertItem("Hydration reminder", "Info", false)
        }
    }
}

@Composable
fun SessionSummarySection(
    isRiding: Boolean,
    onRideToggle: (Boolean) -> Unit
) {
    StatusCard(
        title = if (isRiding) "Current Ride" else "Session Summary",
        icon = Icons.Filled.DirectionsBike
    ) {
        Column {
            if (isRiding) {
                // Current ride stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem("Duration", "45:30")
                    StatItem("Distance", "12.5 km")
                    StatItem("Elevation", "150 m")
                }
            } else {
                Text(
                    text = "Ready to start your next adventure?",
                    fontSize = 14.sp,
                    color = colorResource(id = R.color.berkeley_blue).copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onRideToggle(!isRiding) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRiding) colorResource(id = R.color.red_pantone) else colorResource(id = R.color.non_photo_blue),
                    contentColor = colorResource(id = R.color.honeydew)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (isRiding) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRiding) "End Ride" else "Start Ride",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
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
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colorResource(id = R.color.berkeley_blue),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorResource(id = R.color.berkeley_blue)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            content()
        }
    }
}

@Composable
fun VitalMetric(label: String, value: String, icon: ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colorResource(id = R.color.red_pantone),
            modifier = Modifier.size(20.dp)
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
            color = colorResource(id = R.color.berkeley_blue).copy(alpha = 0.6f)
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colorResource(id = R.color.non_photo_blue).copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.first().toString(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.berkeley_blue)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorResource(id = R.color.berkeley_blue)
                )
                Text(
                    text = distance,
                    fontSize = 12.sp,
                    color = colorResource(id = R.color.berkeley_blue).copy(alpha = 0.6f)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(
                    when (healthStatus) {
                        "Good" -> colorResource(id = R.color.non_photo_blue)
                        "Warning" -> colorResource(id = R.color.cerulean)
                        else -> colorResource(id = R.color.red_pantone)
                    }
                )
        )
    }
}

@Composable
fun AlertItem(message: String, type: String, isGood: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            fontSize = 14.sp,
            color = colorResource(id = R.color.berkeley_blue),
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = if (isGood) Icons.Filled.CheckCircle else Icons.Filled.Info,
            contentDescription = null,
            tint = if (isGood) colorResource(id = R.color.non_photo_blue) else colorResource(id = R.color.red_pantone),
            modifier = Modifier.size(16.dp)
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
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(id = R.color.berkeley_blue)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = colorResource(id = R.color.berkeley_blue).copy(alpha = 0.6f)
        )
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