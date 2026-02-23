package com.schedula.internship.model

data class ReminderItem(
    val id: String,
    val message: String,
    val appointmentId: String?,
)
