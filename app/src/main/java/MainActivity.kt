package com.example.cyclink

import com.example.cyclink.misc.WelcomeActivity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.cyclink.sign_in.LoginActivity

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs: SharedPreferences = getSharedPreferences("CyclinkPrefs", MODE_PRIVATE)
        val isFirstTime = prefs.getBoolean("isFirstTime", true)

        if (isFirstTime) {
            prefs.edit().putBoolean("isFirstTime", false).apply()
            startActivity(Intent(this, WelcomeActivity::class.java))
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        finish()
    }
}