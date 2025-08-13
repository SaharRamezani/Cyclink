package com.example.cyclink.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cyclink.helpers.AIHelper
import com.example.cyclink.helpers.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

class ChatViewModel : ViewModel() {
    private val aiHelper = AIHelper()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun sendMessage(text: String) {
        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = text,
            isFromUser = true
        )

        _messages.value = _messages.value + userMessage
        _isLoading.value = true

        viewModelScope.launch {
            aiHelper.sendMessage(text)
                .onSuccess { response ->
                    val aiMessage = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        text = response,
                        isFromUser = false
                    )
                    _messages.value = _messages.value + aiMessage
                }
                .onFailure { error ->
                    val errorMessage = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        text = "Sorry, I'm having trouble connecting right now. Please try again later. Error: ${error.message}",
                        isFromUser = false
                    )
                    _messages.value = _messages.value + errorMessage
                }

            _isLoading.value = false
        }
    }
}