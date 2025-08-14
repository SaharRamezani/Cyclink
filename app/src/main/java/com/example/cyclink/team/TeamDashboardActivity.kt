package com.example.cyclink.com.example.cyclink.team

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TeamDashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TeamDashboardScreen(
                    onBackPressed = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDashboardScreen(onBackPressed: () -> Unit) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var teamMembers by remember { mutableStateOf<List<TeamMember>>(emptyList()) }
    var teamName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var teamId by remember { mutableStateOf("") }

    // Load team data
    LaunchedEffect(Unit) {
        val user = auth.currentUser
        if (user != null) {
            db.collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener { userDoc ->
                    val teamData = userDoc.get("currentTeam") as? Map<String, Any>
                    if (teamData != null) {
                        teamId = teamData["teamId"] as? String ?: ""
                        teamName = teamData["teamName"] as? String ?: ""

                        // Load team members
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
                                                    status = if (memberId == user.uid) "online" else listOf("online", "riding", "offline").random(),
                                                    heartRate = (60..90).random(),
                                                    speed = (0..25).random().toDouble(),
                                                    alerts = if ((0..10).random() < 3) listOf("Low battery", "High heart rate").take((1..2).random()) else emptyList()
                                                )
                                            )
                                        }
                                    }
                                    teamMembers = memberList
                                    isLoading = false
                                }
                        }
                    } else {
                        isLoading = false
                    }
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
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackPressed
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

                IconButton(
                    onClick = { /* Refresh team data */ }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh",
                        tint = colorResource(id = R.color.honeydew)
                    )
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
                // Team Stats Card
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
                        StatColumn("Active", teamMembers.count { it.status == "online" || it.status == "riding" }.toString())
                        StatColumn("Avg Speed", "${teamMembers.filter { it.status == "riding" }.map { it.speed }.average().takeIf { !it.isNaN() }?.toInt() ?: 0} km/h")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Members List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                    putExtra("userId", member.id) // For Firestore queries
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
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
            // Profile Circle
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

            // Member Info
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

                    Spacer(modifier = Modifier.width(8.dp))

                    // Status Indicator
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

                if (member.status == "riding" || member.status == "online") {
                    Text(
                        text = "♥ ${member.heartRate} bpm • ${member.speed.toInt()} km/h",
                        fontSize = 12.sp,
                        color = colorResource(id = R.color.berkeley_blue).copy(alpha = 0.8f)
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