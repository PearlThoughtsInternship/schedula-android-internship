package com.schedula.internship.model

enum class SupportTicketStatus {
    Open,
    InProgress,
    Resolved,
}

data class SupportTicket(
    val id: String,
    val subject: String,
    val message: String,
    val status: SupportTicketStatus,
    val createdAtEpochMillis: Long,
)
