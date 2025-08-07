package com.example.cyclink.team

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.res.colorResource
import com.example.cyclink.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TeamActivity : ComponentActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                TeamSelectionScreen(
                    onCreateTeam = { createTeam() },
                    onJoinTeam = { joinTeam() }
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun TeamSelectionScreen(
        onCreateTeam: () -> Unit,
        onJoinTeam: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colorResource(id = R.color.berkeley_blue),
                            colorResource(id = R.color.non_photo_blue)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Header Section
                Text(
                    text = "Welcome to CycLink!",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.honeydew),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Choose how you'd like to get started with your cycling team",
                    fontSize = 16.sp,
                    color = colorResource(id = R.color.non_photo_blue),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 48.dp)
                )

                // Action Cards
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Create Team Card
                    TeamActionCard(
                        title = "Create New Team",
                        subtitle = "Start your own cycling group and invite friends",
                        icon = Icons.Filled.Add,
                        onClick = onCreateTeam,
                        backgroundColor = colorResource(id = R.color.honeydew),
                        contentColor = colorResource(id = R.color.berkeley_blue)
                    )

                    // Join Team Card
                    TeamActionCard(
                        title = "Join Existing Team",
                        subtitle = "Connect with an existing cycling group",
                        icon = Icons.Filled.Person,
                        onClick = onJoinTeam,
                        backgroundColor = colorResource(id = R.color.honeydew),
                        contentColor = colorResource(id = R.color.berkeley_blue)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Footer text
                Text(
                    text = "Ready to start your cycling journey?",
                    fontSize = 14.sp,
                    color = colorResource(id = R.color.honeydew).copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun TeamActionCard(
        title: String,
        subtitle: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        onClick: () -> Unit,
        backgroundColor: Color,
        contentColor: Color
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(contentColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Text Section
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        color = contentColor.copy(alpha = 0.7f),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }

    private fun createTeam() {
        val user = auth.currentUser ?: return

        val teamId = db.collection("teams").document().id
        val teamData = hashMapOf(
            "teamName" to "${user.displayName}'s Team",
            "createdBy" to user.uid,
            "members" to listOf(user.uid)
        )

        db.collection("teams").document(teamId).set(teamData)
            .addOnSuccessListener {
                Toast.makeText(this, "Team created successfully! 🎉", Toast.LENGTH_SHORT).show()
                // TODO: Navigate to HomeActivity or TeamScreen
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to create team. Please try again.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun joinTeam() {
        // TODO: Implement team join logic (input code or select from list)
        Toast.makeText(this, "Team joining feature coming soon! 🚴‍♀️", Toast.LENGTH_SHORT).show()
    }
}