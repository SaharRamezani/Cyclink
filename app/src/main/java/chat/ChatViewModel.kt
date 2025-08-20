package com.example.cyclink.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.cyclink.helpers.AIHelper
import com.example.cyclink.helpers.ChatMessage
import java.util.UUID

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val aiHelper = AIHelper(application.applicationContext)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // Add welcome message
        addMessage(ChatMessage(
            id = UUID.randomUUID().toString(),
            text = "Hello! I'm your cycling assistant. How can I help you today?",
            isFromUser = false
        ))
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        // Add user message
        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = text,
            isFromUser = true
        )
        addMessage(userMessage)

        // Get AI response
        _isLoading.value = true
        viewModelScope.launch {
            aiHelper.sendMessage(text).fold(
                onSuccess = { response ->
                    val aiMessage = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        text = response,
                        isFromUser = false
                    )
                    addMessage(aiMessage)
                },
                onFailure = { error ->
                    val errorMessage = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        text = "Sorry, I encountered an error: ${error.message}",
                        isFromUser = false
                    )
                    addMessage(errorMessage)
                }
            )
            _isLoading.value = false
        }
    }

    private fun addMessage(message: ChatMessage) {
        _messages.value = _messages.value + message
    }
}