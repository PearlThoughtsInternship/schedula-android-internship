package com.schedula.internship.model

enum class ChatThreadType {
    Patient,
    Reengagement,
    Support,
    Copatient,
}

enum class ChatSender {
    User,
    Doctor,
    System,
    Support,
}

data class ChatMessage(
    val id: String,
    val threadType: ChatThreadType,
    val sender: ChatSender,
    val content: String,
    val createdAtEpochMillis: Long,
)
