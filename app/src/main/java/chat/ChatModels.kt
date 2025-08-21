package com.example.cyclink.chat

import kotlinx.serialization.Serializable

data class ChatMessage(
    val id: String,
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class AIResponse(
    val candidates: List<Candidate>
)

@Serializable
data class Candidate(
    val content: Content
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String
)

@Serializable
data class AIRequest(
    val contents: List<RequestContent>
)

@Serializable
data class RequestContent(
    val parts: List<RequestPart>
)

@Serializable
data class RequestPart(
    val text: String
)