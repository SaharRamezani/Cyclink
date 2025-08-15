package com.example.cyclink.team

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Group
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cyclink.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinTeamScreen(
    onBackPressed: () -> Unit,
    onTeamJoined: () -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var teamCode by remember { mutableStateOf("") }
    var isJoining by remember { mutableStateOf(false) }

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
                    onClick = onBackPressed,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = colorResource(id = R.color.honeydew)
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Join Team",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.honeydew)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Team Join Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colorResource(id = R.color.honeydew).copy(alpha = 0.1f))
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Group,
                    contentDescription = null,
                    tint = colorResource(id = R.color.honeydew),
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Enter Team Code",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorResource(id = R.color.honeydew),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Ask your team leader for the 6-character team code",
                fontSize = 14.sp,
                color = colorResource(id = R.color.non_photo_blue),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

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
                    // Team Code Field
                    OutlinedTextField(
                        value = teamCode,
                        onValueChange = {
                            if (it.length <= 6) {
                                teamCode = it.uppercase()
                            }
                        },
                        label = { Text("Team Code") },
                        placeholder = { Text("ABC123") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colorResource(id = R.color.red_pantone),
                            focusedLabelColor = colorResource(id = R.color.red_pantone),
                            cursorColor = colorResource(id = R.color.red_pantone)
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { keyboardController?.hide() }
                        ),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Build,
                                contentDescription = null,
                                tint = colorResource(id = R.color.berkeley_blue).copy(alpha = 0.6f)
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Helper text
                    Text(
                        text = "Team codes are 6 characters long (letters and numbers)",
                        fontSize = 12.sp,
                        color = colorResource(id = R.color.berkeley_blue).copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Join Team Button
            Button(
                onClick = {
                    if (teamCode.length == 6) {
                        joinTeam(
                            auth = auth,
                            db = db,
                            teamCode = teamCode,
                            context = context as ComponentActivity,
                            onSuccess = {
                                isJoining = false
                                onTeamJoined()
                            },
                            onFailure = { isJoining = false }
                        )
                        isJoining = true
                    } else {
                        Toast.makeText(context, "Please enter a valid 6-character team code", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = teamCode.length == 6 && !isJoining,
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
                if (isJoining) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = colorResource(id = R.color.honeydew),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Joining Team...")
                } else {
                    Icon(
                        imageVector = Icons.Filled.Face,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Join Team",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun joinTeam(
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    teamCode: String,
    context: ComponentActivity,
    onSuccess: () -> Unit,
    onFailure: () -> Unit
) {
    val user = auth.currentUser ?: return

    // First, find the team with the given code
    db.collection("teams")
        .whereEqualTo("teamCode", teamCode)
        .get()
        .addOnSuccessListener { querySnapshot ->
            if (querySnapshot.isEmpty) {
                Toast.makeText(context, "Invalid team code", Toast.LENGTH_SHORT).show()
                onFailure()
                return@addOnSuccessListener
            }

            val teamDoc = querySnapshot.documents.first()
            val teamId = teamDoc.id
            val teamData = teamDoc.data!!
            val currentMembers = teamData["members"] as? List<*> ?: emptyList<String>()
            val currentMemberNames = teamData["memberNames"] as? List<*> ?: emptyList<String>()
            val maxMembers = (teamData["maxMembers"] as? Long)?.toInt() ?: 10

            // Check if user is already in the team
            if (currentMembers.contains(user.uid)) {
                Toast.makeText(context, "You're already a member of this team", Toast.LENGTH_SHORT).show()
                onFailure()
                return@addOnSuccessListener
            }

            // Check if team is full
            if (currentMembers.size >= maxMembers) {
                Toast.makeText(context, "Team is full", Toast.LENGTH_SHORT).show()
                onFailure()
                return@addOnSuccessListener
            }

            // Add user to team
            val userTeamData = hashMapOf(
                "teamId" to teamId,
                "teamName" to teamData["teamName"],
                "role" to "member",
                "joinedAt" to System.currentTimeMillis()
            )

            // Update team with new member
            db.collection("teams").document(teamId)
                .update(
                    "members", FieldValue.arrayUnion(user.uid),
                    "memberNames", FieldValue.arrayUnion(user.displayName ?: "Unknown")
                )
                .addOnSuccessListener {
                    // Update user's team info
                    db.collection("users").document(user.uid)
                        .update("currentTeam", userTeamData)
                        .addOnSuccessListener {
                            println("User document updated successfully")
                            onSuccess()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed to update user data", Toast.LENGTH_SHORT).show()
                            onFailure()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to join team", Toast.LENGTH_SHORT).show()
                    onFailure()
                }
        }
        .addOnFailureListener {
            Toast.makeText(context, "Error searching for team", Toast.LENGTH_SHORT).show()
            onFailure()
        }
}

@Preview(showBackground = true)
@Composable
fun JoinTeamScreenPreview() {
    JoinTeamScreen(
        onBackPressed = { },
        onTeamJoined = { }
    )
}