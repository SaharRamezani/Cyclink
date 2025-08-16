package com.example.cyclink.home

import android.annotation.SuppressLint
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cyclink.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

data class RideRecord(
    val id: String = "",
    val duration: Long = 0L,
    val distance: Double = 0.0,
    val date: String = "",
    val startHour: String = "",
    val finishHour: String = "",
    val participants: List<String> = emptyList(),
    val startTime: Long = 0L,
    val finishTime: Long = 0L
)

class RideHistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                RideHistoryScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideHistoryScreen() {
    var rideRecords by remember { mutableStateOf<List<RideRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        loadRideHistory { records ->
            rideRecords = records
            isLoading = false
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
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { /* Handle back navigation */ }
                ) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colorResource(id = R.color.honeydew)
                    )
                }
                Text(
                    text = "Ride History",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.honeydew),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = colorResource(id = R.color.honeydew)
                    )
                }
            } else if (rideRecords.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No ride records found",
                        color = colorResource(id = R.color.honeydew),
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(rideRecords) { record ->
                        RideRecordCard(record = record)
                    }
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun RideRecordCard(record: RideRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colorResource(id = R.color.honeydew)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Date and time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.date,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.berkeley_blue)
                )
                Text(
                    text = "${record.startHour} - ${record.finishHour}",
                    fontSize = 14.sp,
                    color = colorResource(id = R.color.cerulean)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(
                    icon = Icons.Filled.Schedule,
                    label = "Duration",
                    value = formatDurationFromMillis(record.duration)
                )
                StatColumn(
                    icon = Icons.Filled.Straighten,
                    label = "Distance",
                    value = "${String.format("%.2f", record.distance)} km"
                )
                StatColumn(
                    icon = Icons.Filled.Group,
                    label = "Riders",
                    value = record.participants.size.toString()
                )
            }
        }
    }
}

@Composable
fun StatColumn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = colorResource(id = R.color.cerulean),
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
            color = colorResource(id = R.color.cerulean)
        )
    }
}

private fun loadRideHistory(onResult: (List<RideRecord>) -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    if (currentUser == null) {
        onResult(emptyList())
        return
    }

    db.collection("ride_records")
        .whereEqualTo("userId", currentUser.uid)
        .orderBy("finishTime", Query.Direction.DESCENDING)
        .get()
        .addOnSuccessListener { documents ->
            val records = documents.mapNotNull { doc ->
                try {
                    RideRecord(
                        id = doc.id,
                        duration = doc.getLong("duration") ?: 0L,
                        distance = doc.getDouble("distance") ?: 0.0,
                        date = doc.getString("date") ?: "",
                        startHour = doc.getString("startHour") ?: "",
                        finishHour = doc.getString("finishHour") ?: "",
                        participants = doc.get("participants") as? List<String> ?: emptyList(),
                        startTime = doc.getLong("startTime") ?: 0L,
                        finishTime = doc.getLong("finishTime") ?: 0L
                    )
                } catch (e: Exception) {
                    Log.w("RideHistory", "Failed to parse ride record", e)
                    null
                }
            }
            onResult(records)
        }
        .addOnFailureListener { e ->
            Log.e("RideHistory", "Error loading ride records", e)
            onResult(emptyList())
        }
}

private fun formatDurationFromMillis(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = (millis / (1000 * 60 * 60))

    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}