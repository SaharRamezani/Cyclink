package com.example.cyclink

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.example.cyclink.home.HomeActivity
import com.example.cyclink.misc.WelcomeActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("TestLog", "onCreate called")

        val user = FirebaseAuth.getInstance().currentUser
        Log.d("AuthCheck", "Already signed in: ${user?.uid ?: "No user"}")

        if (user != null) {
            // User already signed in → go to Home
            startActivity(Intent(this, HomeActivity::class.java))
        } else {
            // First launch or signed out → go to Welcome (then Login)
            startActivity(Intent(this, WelcomeActivity::class.java))
        }

        finish()
    }
}