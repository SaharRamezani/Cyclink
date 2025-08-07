package com.example.cyclink.team

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TeamActivity : ComponentActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(onClick = { createTeam() }) {
                            Text("Create New Team")
                        }
                        Button(onClick = { joinTeam() }) {
                            Text("Join Existing Team")
                        }
                    }
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
                Toast.makeText(this, "Team created!", Toast.LENGTH_SHORT).show()
                // TODO: Navigate to HomeActivity or TeamScreen
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to create team", Toast.LENGTH_SHORT).show()
            }
    }

    private fun joinTeam() {
        // TODO: Implement team join logic (input code or select from list)
        Toast.makeText(this, "Join team logic here", Toast.LENGTH_SHORT).show()
    }
}