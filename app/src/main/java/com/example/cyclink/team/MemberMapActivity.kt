package com.example.cyclink.com.example.cyclink.team

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.cyclink.R
import com.example.cyclink.helpers.FirestoreHelper
import com.example.cyclink.helpers.SensorRecord
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class MemberMapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val memberId = intent.getStringExtra("memberId") ?: ""
        val memberName = intent.getStringExtra("memberName") ?: "Unknown Member"
        val userId = intent.getStringExtra("userId") ?: memberId

        setContent {
            MaterialTheme {
                MemberMapScreen(
                    memberName = memberName,
                    userId = userId,
                    onBackPressed = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberMapScreen(
    memberName: String,
    userId: String,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    var lastSensorRecord by remember { mutableStateOf<SensorRecord?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }

    // Fetch the last sensor record with GPS data for this user
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            Log.d("MemberMapActivity", "🔍 Fetching last GPS data for user: $userId")

            db.collection("sensor_records")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener { documents ->
                    val validDocuments = documents.filter { document ->
                        val latitude = document.get("latitude")
                        val longitude = document.get("longitude")
                        latitude != null && longitude != null
                    }

                    if (validDocuments.isNotEmpty()) {
                        val document = validDocuments.first()
                        val sensorRecord = document.toObject(SensorRecord::class.java)

                        if (sensorRecord != null &&
                            sensorRecord.latitude != null &&
                            sensorRecord.longitude != null) {

                            lastSensorRecord = sensorRecord
                            Log.d("MemberMapActivity", "📍 Found GPS data: lat=${sensorRecord.latitude}, lon=${sensorRecord.longitude}")

                            // Update map with location
                            googleMap?.let { map ->
                                val location = LatLng(sensorRecord.latitude, sensorRecord.longitude)
                                map.clear()
                                map.addMarker(
                                    MarkerOptions()
                                        .position(location)
                                        .title(memberName)
                                        .snippet("Last known location")
                                )
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
                            }
                        } else {
                            errorMessage = "No GPS coordinates found in sensor record"
                        }
                    } else {
                        errorMessage = "No GPS data found for this member"
                    }
                    isLoading = false
                }
                .addOnFailureListener { exception ->
                    errorMessage = "Failed to load location data: ${exception.message}"
                    Log.e("MemberMapActivity", "❌ Error fetching GPS data", exception)
                    isLoading = false
                }
        } else {
            errorMessage = "Invalid user ID"
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
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$memberName's Location",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(id = R.color.honeydew)
                    )

                    lastSensorRecord?.let { record ->
                        val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                        Text(
                            text = "Last seen: ${dateFormat.format(Date(record.timestamp))}",
                            fontSize = 12.sp,
                            color = colorResource(id = R.color.non_photo_blue)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = colorResource(id = R.color.honeydew)
                )
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = colorResource(id = R.color.honeydew)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading location...",
                                color = colorResource(id = R.color.honeydew)
                            )
                        }
                    }
                }

                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = colorResource(id = R.color.honeydew)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = colorResource(id = R.color.berkeley_blue),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Location Not Available",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorResource(id = R.color.berkeley_blue)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = errorMessage!!,
                                    fontSize = 14.sp,
                                    color = colorResource(id = R.color.berkeley_blue).copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                else -> {
                    // Map View with proper lifecycle management
                    // Replace the AndroidView block in MemberMapScreen with this:
                    AndroidView(
                        factory = { context ->
                            MapView(context).apply {
                                mapView = this
                                onCreate(null)
                                onStart()
                                onResume()

                                getMapAsync { map ->
                                    googleMap = map
                                    map.uiSettings.isZoomControlsEnabled = true
                                    map.uiSettings.isMyLocationButtonEnabled = false

                                    // Set a default location
                                    val defaultLocation = LatLng(37.7749, -122.4194)
                                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))

                                    // Show user location if available
                                    lastSensorRecord?.let { record ->
                                        if (record.latitude != null && record.longitude != null) {
                                            val location = LatLng(record.latitude, record.longitude)
                                            map.clear()
                                            map.addMarker(
                                                MarkerOptions()
                                                    .position(location)
                                                    .title(memberName)
                                                    .snippet("Last known location")
                                            )
                                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { view ->
                        // This update block is called on recomposition
                        view.onResume()
                    }
                }
            }
        }

        // Sensor Data Overlay
        lastSensorRecord?.let { record ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(id = R.color.honeydew)
                ),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Latest Sensor Data",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(id = R.color.berkeley_blue)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (record.heartRate != null) {
                            SensorValue("Heart Rate", "${record.heartRate.toInt()} bpm")
                        }
                        if (record.phoneSpeed != null) {
                            SensorValue("Speed", "${record.phoneSpeed.toInt()} km/h")
                        }
                        if (record.gpsAccuracy != null) {
                            SensorValue("GPS Accuracy", "${record.gpsAccuracy.toInt()}m")
                        }
                    }

                    if (record.latitude != null && record.longitude != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Coordinates: ${String.format("%.6f", record.latitude)}, ${String.format("%.6f", record.longitude)}",
                            fontSize = 12.sp,
                            color = colorResource(id = R.color.berkeley_blue).copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }

    DisposableEffect(mapView) {
        mapView?.onStart()
        onDispose {
            mapView?.let { view ->
                view.onPause()
                view.onStop()
                view.onDestroy()
            }
        }
    }
}

@Composable
fun SensorValue(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(id = R.color.berkeley_blue)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = colorResource(id = R.color.berkeley_blue).copy(alpha = 0.7f)
        )
    }
}