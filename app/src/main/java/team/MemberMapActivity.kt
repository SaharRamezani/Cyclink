package com.example.cyclink.team

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

class MemberMapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { MapScreen() } }
    }
}

@Composable
fun MapScreen() {
    val singapore = LatLng(1.35, 103.87)
    val markerState = rememberMarkerState(position = singapore)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(singapore, 10f)
    }

    var uiSettings by remember {
        mutableStateOf(MapUiSettings(zoomControlsEnabled = true))
    }
    var properties by remember {
        mutableStateOf(MapProperties(mapType = MapType.NORMAL))
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
                title = "Marker in Singapore"
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
                        "Switch to SATELLITE"
                    else
                        "Switch to NORMAL"
                )
            }
        }
    }
}