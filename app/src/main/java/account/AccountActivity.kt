package com.example.cyclink.account

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cyclink.R
import com.example.cyclink.home.HomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AccountActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AccountScreen(
                    onBackPressed = { finish() }
                )
            }
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = colorResource(id = R.color.berkeley_blue).copy(alpha = 0.7f),
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = colorResource(id = R.color.berkeley_blue),
            textAlign = TextAlign.End
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(onBackPressed: () -> Unit) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var userName by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var teamCode by remember { mutableStateOf("") }
    var teamName by remember { mutableStateOf("") }
    var teamDescription by remember { mutableStateOf("") }
    var userRole by remember { mutableStateOf("member") }
    var teamId by remember { mutableStateOf("") }
    var isEditingName by remember { mutableStateOf(false) }
    var isEditingTeamName by remember { mutableStateOf(false) }
    var isEditingTeamDescription by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var isSavingTeam by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }
    var tempTeamName by remember { mutableStateOf("") }
    var tempTeamDescription by remember { mutableStateOf("") }

    val user = auth.currentUser

    // Load user data
    LaunchedEffect(Unit) {
        if (user != null) {
            userName = user.displayName ?: ""
            userEmail = user.email ?: ""
            tempName = userName

            // Get team data
            db.collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    val teamData = document.get("currentTeam") as? Map<String, Any>
                    if (teamData != null) {
                        teamName = teamData["teamName"] as? String ?: ""
                        userRole = teamData["role"] as? String ?: "member"
                        teamId = teamData["teamId"] as? String ?: ""
                        tempTeamName = teamName

                        // Get team details from team document
                        if (teamId.isNotEmpty()) {
                            db.collection("teams")
                                .document(teamId)
                                .get()
                                .addOnSuccessListener { teamDoc ->
                                    teamCode = teamDoc.getString("teamCode") ?: ""
                                    teamDescription = teamDoc.getString("teamDescription") ?: ""
                                    tempTeamDescription = teamDescription
                                    isLoading = false
                                }
                                .addOnFailureListener {
                                    isLoading = false
                                }
                        } else {
                            isLoading = false
                        }
                    } else {
                        isLoading = false
                    }
                }
                .addOnFailureListener {
                    isLoading = false
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header
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
                        contentDescription = "Back"
                    )
                }

                Text(
                    text = "Account Settings",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(id = R.color.honeydew),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = colorResource(id = R.color.honeydew)
                    )
                }
            } else {
                // Profile Section (existing code remains the same)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = colorResource(id = R.color.honeydew)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Profile Picture
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(colorResource(id = R.color.berkeley_blue).copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = colorResource(id = R.color.berkeley_blue),
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Editable Name
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isEditingName) {
                                OutlinedTextField(
                                    value = tempName,
                                    onValueChange = { tempName = it },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colorResource(id = R.color.red_pantone),
                                        focusedLabelColor = colorResource(id = R.color.red_pantone),
                                        cursorColor = colorResource(id = R.color.red_pantone)
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(
                                        onDone = { keyboardController?.hide() }
                                    ),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                IconButton(
                                    onClick = {
                                        if (tempName.isNotBlank()) {
                                            updateUserName(
                                                auth = auth,
                                                db = db,
                                                newName = tempName,
                                                teamId = teamId,
                                                context = context as ComponentActivity,
                                                onSuccess = {
                                                    userName = tempName
                                                    isEditingName = false
                                                    isSaving = false
                                                },
                                                onFailure = { isSaving = false }
                                            )
                                            isSaving = true
                                        }
                                    },
                                    enabled = tempName.isNotBlank() && !isSaving
                                ) {
                                    if (isSaving) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = colorResource(id = R.color.berkeley_blue)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.Save,
                                            contentDescription = "Save",
                                            tint = colorResource(id = R.color.berkeley_blue)
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = userName.ifBlank { "No Name" },
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorResource(id = R.color.berkeley_blue)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                IconButton(
                                    onClick = { isEditingName = true }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Edit Name",
                                        tint = colorResource(id = R.color.berkeley_blue)
                                    )
                                }
                            }
                        }

                        Text(
                            text = userEmail,
                            fontSize = 14.sp,
                            color = colorResource(id = R.color.berkeley_blue).copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Team Information Section with editable fields
                if (teamName.isNotEmpty()) {
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
                                    imageVector = Icons.Filled.Group,
                                    contentDescription = null,
                                    tint = colorResource(id = R.color.berkeley_blue),
                                    modifier = Modifier.size(24.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = "Team Information",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorResource(id = R.color.berkeley_blue)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Editable Team Name
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isEditingTeamName) {
                                    OutlinedTextField(
                                        value = tempTeamName,
                                        onValueChange = { tempTeamName = it },
                                        label = { Text("Team Name") },
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = colorResource(id = R.color.red_pantone),
                                            focusedLabelColor = colorResource(id = R.color.red_pantone),
                                            cursorColor = colorResource(id = R.color.red_pantone)
                                        ),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(
                                            onDone = { keyboardController?.hide() }
                                        ),
                                        singleLine = true
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    IconButton(
                                        onClick = {
                                            if (tempTeamName.isNotBlank()) {
                                                updateTeamInfo(
                                                    db = db,
                                                    auth = auth,
                                                    teamId = teamId,
                                                    newName = tempTeamName,
                                                    newDescription = teamDescription,
                                                    context = context as ComponentActivity,
                                                    onSuccess = {
                                                        teamName = tempTeamName
                                                        isEditingTeamName = false
                                                        isSavingTeam = false
                                                    },
                                                    onFailure = { isSavingTeam = false }
                                                )
                                                isSavingTeam = true
                                            }
                                        },
                                        enabled = tempTeamName.isNotBlank() && !isSavingTeam
                                    ) {
                                        if (isSavingTeam) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = colorResource(id = R.color.berkeley_blue)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Filled.Save,
                                                contentDescription = "Save",
                                                tint = colorResource(id = R.color.berkeley_blue)
                                            )
                                        }
                                    }
                                } else {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Team Name",
                                            fontSize = 14.sp,
                                            color = colorResource(id = R.color.berkeley_blue).copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = teamName,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colorResource(id = R.color.berkeley_blue)
                                        )
                                    }

                                    IconButton(
                                        onClick = { isEditingTeamName = true }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = "Edit Team Name",
                                            tint = colorResource(id = R.color.berkeley_blue)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Editable Team Description
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = if (isEditingTeamDescription) Alignment.Top else Alignment.CenterVertically
                            ) {
                                if (isEditingTeamDescription) {
                                    OutlinedTextField(
                                        value = tempTeamDescription,
                                        onValueChange = { tempTeamDescription = it },
                                        label = { Text("Team Description") },
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = colorResource(id = R.color.red_pantone),
                                            focusedLabelColor = colorResource(id = R.color.red_pantone),
                                            cursorColor = colorResource(id = R.color.red_pantone)
                                        ),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(
                                            onDone = { keyboardController?.hide() }
                                        ),
                                        maxLines = 3
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    IconButton(
                                        onClick = {
                                            updateTeamInfo(
                                                db = db,
                                                auth = auth,
                                                teamId = teamId,
                                                newName = teamName,
                                                newDescription = tempTeamDescription,
                                                context = context as ComponentActivity,
                                                onSuccess = {
                                                    teamDescription = tempTeamDescription
                                                    isEditingTeamDescription = false
                                                    isSavingTeam = false
                                                },
                                                onFailure = { isSavingTeam = false }
                                            )
                                            isSavingTeam = true
                                        },
                                        enabled = !isSavingTeam
                                    ) {
                                        if (isSavingTeam) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = colorResource(id = R.color.berkeley_blue)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Filled.Save,
                                                contentDescription = "Save",
                                                tint = colorResource(id = R.color.berkeley_blue)
                                            )
                                        }
                                    }
                                } else {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Team Description",
                                            fontSize = 14.sp,
                                            color = colorResource(id = R.color.berkeley_blue).copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = teamDescription.ifBlank { "No description" },
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colorResource(id = R.color.berkeley_blue)
                                        )
                                    }

                                    IconButton(
                                        onClick = { isEditingTeamDescription = true }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = "Edit Team Description",
                                            tint = colorResource(id = R.color.berkeley_blue)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Team Code with copy functionality (existing code)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Team Code",
                                        fontSize = 14.sp,
                                        color = colorResource(id = R.color.berkeley_blue).copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = teamCode,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colorResource(id = R.color.berkeley_blue)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(teamCode))
                                        Toast.makeText(context, "Team code copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ContentCopy,
                                        contentDescription = "Copy Team Code",
                                        tint = colorResource(id = R.color.berkeley_blue)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Current Role
                            InfoRow(
                                label = "Current Role",
                                value = userRole.replaceFirstChar { it.uppercase() }
                            )
                        }
                    }

                    // Rest of your existing code for role management...
                    Spacer(modifier = Modifier.height(16.dp))

                    // Role Management Section (existing code remains the same)
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
                                    imageVector = Icons.Filled.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = colorResource(id = R.color.berkeley_blue),
                                    modifier = Modifier.size(24.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = "Role Management",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorResource(id = R.color.berkeley_blue)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Change your role within the team. Admin role provides additional management capabilities.",
                                fontSize = 14.sp,
                                color = colorResource(id = R.color.berkeley_blue).copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    val newRole = if (userRole == "admin") "member" else "admin"
                                    updateUserRole(
                                        db = db,
                                        auth = auth,
                                        teamId = teamId,
                                        newRole = newRole,
                                        context = context as ComponentActivity,
                                        onSuccess = { userRole = newRole }
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorResource(id = R.color.red_pantone),
                                    contentColor = colorResource(id = R.color.honeydew)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Switch to ${if (userRole == "admin") "Member" else "Admin"}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                } else {
                    // No team card (existing code remains the same)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = colorResource(id = R.color.honeydew)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No Team",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colorResource(id = R.color.berkeley_blue)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "You're not currently part of any team. Create or join a team to get started.",
                                fontSize = 14.sp,
                                color = colorResource(id = R.color.berkeley_blue).copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// Add this new function to update team information
private fun updateTeamInfo(
    db: FirebaseFirestore,
    auth: FirebaseAuth,
    teamId: String,
    newName: String,
    newDescription: String,
    context: ComponentActivity,
    onSuccess: () -> Unit,
    onFailure: () -> Unit
) {
    val user = auth.currentUser ?: return

    val updates = hashMapOf<String, Any>(
        "teamName" to newName,
        "teamDescription" to newDescription
    )

    // Update team document
    db.collection("teams")
        .document(teamId)
        .update(updates)
        .addOnSuccessListener {
            // Update user's currentTeam.teamName
            db.collection("users")
                .document(user.uid)
                .update("currentTeam.teamName", newName)
                .addOnSuccessListener {
                    // Update all team members' currentTeam.teamName
                    db.collection("teams")
                        .document(teamId)
                        .get()
                        .addOnSuccessListener { teamDoc ->
                            val members = teamDoc.get("members") as? List<*> ?: emptyList<String>()
                            members.forEach { memberId ->
                                if (memberId is String && memberId != user.uid) {
                                    db.collection("users")
                                        .document(memberId)
                                        .update("currentTeam.teamName", newName)
                                }
                            }
                        }

                    Toast.makeText(context, "Team information updated successfully", Toast.LENGTH_SHORT).show()
                    onSuccess()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to update user team name", Toast.LENGTH_SHORT).show()
                    onFailure()
                }
        }
        .addOnFailureListener {
            Toast.makeText(context, "Failed to update team information", Toast.LENGTH_SHORT).show()
            onFailure()
        }
}

private fun updateUserName(
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    newName: String,
    teamId: String,
    context: ComponentActivity,
    onSuccess: () -> Unit,
    onFailure: () -> Unit
) {
    val user = auth.currentUser ?: return

    // Update Firebase Auth profile
    val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
        displayName = newName
    }

    user.updateProfile(profileUpdates)
        .addOnSuccessListener {
            // If user is in a team, update team member names
            if (teamId.isNotEmpty()) {
                db.collection("teams")
                    .document(teamId)
                    .get()
                    .addOnSuccessListener { teamDoc ->
                        val members = teamDoc.get("members") as? List<*> ?: emptyList<String>()
                        val memberNames = teamDoc.get("memberNames") as? MutableList<*> ?: mutableListOf<String>()

                        val userIndex = members.indexOf(user.uid)
                        if (userIndex != -1 && userIndex < memberNames.size) {
                            (memberNames as MutableList<String>)[userIndex] = newName

                            db.collection("teams")
                                .document(teamId)
                                .update("memberNames", memberNames)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Name updated successfully", Toast.LENGTH_SHORT).show()
                                    onSuccess()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Failed to update team member name", Toast.LENGTH_SHORT).show()
                                    onFailure()
                                }
                        } else {
                            onSuccess()
                        }
                    }
            } else {
                Toast.makeText(context, "Name updated successfully", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
        }
        .addOnFailureListener {
            Toast.makeText(context, "Failed to update name", Toast.LENGTH_SHORT).show()
            onFailure()
        }
}

private fun updateUserRole(
    db: FirebaseFirestore,
    auth: FirebaseAuth,
    teamId: String,
    newRole: String,
    context: ComponentActivity,
    onSuccess: () -> Unit
) {
    val user = auth.currentUser ?: return

    // Update user's role in their profile
    db.collection("users")
        .document(user.uid)
        .update("currentTeam.role", newRole)
        .addOnSuccessListener {
            Toast.makeText(context, "Role updated to ${newRole.replaceFirstChar { it.uppercase() }}", Toast.LENGTH_SHORT).show()
            onSuccess()
        }
        .addOnFailureListener {
            Toast.makeText(context, "Failed to update role", Toast.LENGTH_SHORT).show()
        }
}