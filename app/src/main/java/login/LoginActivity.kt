package com.example.cyclink.login

import com.example.cyclink.Team.TeamActivity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.*
import androidx.credentials.exceptions.*
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth
        credentialManager = CredentialManager.create(this)

        // Check if user is already signed in
        if (auth.currentUser != null) {
            updateUI(auth.currentUser)
            return
        }

        startGoogleSignIn()
    }

    private fun startGoogleSignIn() {
        val webClientId = getString(R.string.default_web_client_id)
        if (webClientId.isBlank()) {
            Log.e("LoginActivity", "Web client ID not configured")
            return
        }

        // Try with authorized accounts first
        trySignInWithAuthorizedAccounts(webClientId)
    }

    private fun trySignInWithAuthorizedAccounts(webClientId: String) {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(webClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(this@LoginActivity, request)
                handleSignInResult(result.credential)
            } catch (e: NoCredentialException) {
                // No authorized accounts, try with all accounts
                trySignInWithAllAccounts(webClientId)
            } catch (e: Exception) {
                Log.e("LoginActivity", "Credential error: ${e.message}")
                // Handle error appropriately - show error message to user
            }
        }
    }

    private fun trySignInWithAllAccounts(webClientId: String) {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(this@LoginActivity, request)
                handleSignInResult(result.credential)
            } catch (e: Exception) {
                Log.e("LoginActivity", "Sign-in failed: ${e.message}")
                // Show error to user
            }
        }
    }

    private fun handleSignInResult(credential: Credential) {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {

            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            firebaseAuthWithGoogle(googleCredential.idToken)
        } else {
            Log.w("LoginActivity", "Unexpected credential type")
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    Log.w("LoginActivity", "signInWithCredential failed", task.exception)
                    updateUI(null)
                }
            }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            startActivity(Intent(this, TeamActivity::class.java))
            finish()
        } else {
            Log.e("LoginActivity", "User is null after sign-in")
            // Show error message to user
        }
    }
}