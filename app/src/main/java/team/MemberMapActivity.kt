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
import com.example.cyclink.helpers.SensorRecord
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.serialization.json.Json
import android.util.Log

class MemberMapActivity : ComponentActivity() {
    private lateinit var mqttHelper: MQTTHelper
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get userId from intent
        userId = intent.getStringExtra("userId")

        mqttHelper = MQTTHelper(this)

        setContent {
            MaterialTheme {
                MapScreen(mqttHelper = mqttHelper, userId = userId ?: "")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttHelper.disconnect()
    }
}

@Composable
fun MapScreen(mqttHelper: MQTTHelper, userId: String) {
    val context = LocalContext.current

    // State for user location
    var userLocation by remember { mutableStateOf(LatLng(1.35, 103.87)) } // Default Singapore
    var isLocationReceived by remember { mutableStateOf(false) }

    val markerState = rememberMarkerState(position = userLocation)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation, 15f)
    }

    var uiSettings by remember {
        mutableStateOf(MapUiSettings(zoomControlsEnabled = true))
    }
    var properties by remember {
        mutableStateOf(MapProperties(mapType = MapType.NORMAL))
    }

    // Connect to MQTT and subscribe to user-specific topic
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            mqttHelper.connectAndSubscribeToUser(
                userId = userId,
                onConnected = {
                    Log.d("MemberMapActivity", "Connected to MQTT for user: $userId")
                },
                onLocationUpdate = { sensorRecord ->
                    // Update location when new sensor data is received
                    val newLocation = LatLng(sensorRecord.latitude, sensorRecord.longitude)
                    userLocation = newLocation
                    isLocationReceived = true

                    // Update marker position
                    markerState.position = newLocation

                    Log.d("MemberMapActivity", "Location updated: ${sensorRecord.latitude}, ${sensorRecord.longitude}")
                },
                onError = { error ->
                    Log.e("MemberMapActivity", "MQTT connection error: $error")
                }
            )
        }
    }

    // Update camera position when location changes
    LaunchedEffect(userLocation) {
        if (isLocationReceived) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(userLocation, 15f),
                1000
            )
        }
    }

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = properties,
            uiSettings = uiSettings
        ) {
            Marker(
                state = markerState,
                title = "User: $userId",
                snippet = if (isLocationReceived) "Live Location" else "Default Location"
            )
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

            // Status indicator
            Card(
                modifier = Modifier.padding(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isLocationReceived)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = if (isLocationReceived) "Live" else "Waiting...",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}