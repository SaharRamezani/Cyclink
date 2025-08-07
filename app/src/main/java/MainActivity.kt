package com.example.cyclink

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.cyclink.home.HomeActivity
import com.example.cyclink.misc.WelcomeActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser

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