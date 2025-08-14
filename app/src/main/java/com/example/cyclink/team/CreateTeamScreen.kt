package com.example.cyclink.com.example.cyclink.team

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cyclink.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTeamScreen(
    onBackPressed: () -> Unit,
    onTeamCreated: () -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var teamName by remember { mutableStateOf("") }
    var teamDescription by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }
    var maxMembers by remember { mutableStateOf("10") }

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

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
                .padding(24.dp)
        ) {
            // Header with back button
            Row(
                modifier = Modifier.fillMaxWidth(),
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

                Text(
                    text = "Create New Team",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.honeydew)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Team Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colorResource(id = R.color.honeydew).copy(alpha = 0.1f))
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Groups,
                    contentDescription = null,
                    tint = colorResource(id = R.color.honeydew),
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = colorResource(id = R.color.honeydew)
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    // Team Name Field
                    OutlinedTextField(
                        value = teamName,
                        onValueChange = { teamName = it },
                        label = { Text("Team Name") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Team Description Field
                    OutlinedTextField(
                        value = teamDescription,
                        onValueChange = { teamDescription = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Max Members Field
                    OutlinedTextField(
                        value = maxMembers,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() } && it.length <= 3) {
                                maxMembers = it
                            }
                        },
                        label = { Text("Max Members") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { keyboardController?.hide() }
                        ),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Create Team Button
            Button(
                onClick = {
                    if (teamName.isNotBlank()) {
                        createTeam(
                            auth = auth,
                            db = db,
                            teamName = teamName,
                            teamDescription = teamDescription,
                            maxMembers = maxMembers.toIntOrNull() ?: 10,
                            context = context as ComponentActivity,
                            onSuccess = {
                                isCreating = false
                                onTeamCreated()
                            },
                            onFailure = { isCreating = false }
                        )
                        isCreating = true
                    } else {
                        Toast.makeText(context, "Please enter a team name", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = teamName.isNotBlank() && !isCreating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.red_pantone),
                    contentColor = colorResource(id = R.color.honeydew),
                    disabledContainerColor = colorResource(id = R.color.non_photo_blue)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = colorResource(id = R.color.honeydew),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Create Team",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info text
            Text(
                text = "You'll be the team leader and can invite members using the team code",
                fontSize = 12.sp,
                color = colorResource(id = R.color.honeydew).copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun createTeam(
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    teamName: String,
    teamDescription: String,
    maxMembers: Int,
    context: ComponentActivity,
    onSuccess: () -> Unit,
    onFailure: () -> Unit
) {
    val user = auth.currentUser
    if (user == null) {
        Toast.makeText(context, "User not authenticated. Please sign in first.", Toast.LENGTH_LONG).show()
        onFailure()
        return
    }

    // Debug logging
    println("Creating team: $teamName for user: ${user.uid}")

    val teamId = db.collection("teams").document().id
    val teamCode = generateTeamCode()

    println("Generated team ID: $teamId, code: $teamCode")

    val teamData = hashMapOf(
        "teamName" to teamName,
        "teamDescription" to teamDescription,
        "teamCode" to teamCode,
        "createdBy" to user.uid,
        "createdByName" to (user.displayName ?: "Unknown"),
        "members" to listOf(user.uid),
        "memberNames" to listOf(user.displayName ?: "Unknown"),
        "maxMembers" to maxMembers,
        "createdAt" to System.currentTimeMillis()
    )

    val userTeamData = hashMapOf(
        "teamId" to teamId,
        "teamName" to teamName,
        "role" to "admin",
        "joinedAt" to System.currentTimeMillis()
    )

    // Save team data
    db.collection("teams").document(teamId).set(teamData)
        .addOnSuccessListener {
            println("Team document created successfully")
            // Update user's team info
            db.collection("users").document(user.uid)
                .set(mapOf("currentTeam" to userTeamData), com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    println("User document updated successfully")
                    Toast.makeText(
                        context,
                        "Team created successfully! Team code: $teamCode",
                        Toast.LENGTH_LONG
                    ).show()
                    onSuccess()
                }
                .addOnFailureListener { exception ->
                    println("Failed to update user document: ${exception.message}")
                    Toast.makeText(context, "Failed to update user data: ${exception.message}", Toast.LENGTH_SHORT).show()
                    onFailure()
                }
        }
        .addOnFailureListener { exception ->
            println("Failed to create team document: ${exception.message}")
            Toast.makeText(context, "Failed to create team: ${exception.message}", Toast.LENGTH_SHORT).show()
            onFailure()
        }
}

private fun generateTeamCode(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    return (1..6)
        .map { chars.random() }
        .joinToString("")
}

@Preview(showBackground = true)
@Composable
fun CreateTeamScreenPreview() {
    CreateTeamScreen(
        onBackPressed = { },
        onTeamCreated = { }
    )
}