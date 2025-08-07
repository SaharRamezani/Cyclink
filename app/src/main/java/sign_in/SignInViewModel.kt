package com.example.cyclink.sign_in

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SignInViewModel: ViewModel() {

    private val _state = MutableStateFlow(SignInState())
    val state = _state.asStateFlow()

    fun onSignInResult(result: SignInResult) {
        _state.value = _state.value.copy(
            isSignInSuccessful = result.data != null,
            signInError = result.errorMessage
        )

        if (result.data != null) {
            Log.d("SignInViewModel", "User signed in: ${result.data.userId}")
        } else {
            Log.e("SignInViewModel", "Sign-in failed: ${result.errorMessage}")
        }
    }

    fun resetState() {
        _state.update { SignInState() }
    }
}