package com.schedula.internship.model

data class Slot(
    val id: String,
    val doctorId: String,
    val dateLabel: String,
    val timeLabel: String,
    val isBooked: Boolean,
    val type: AppointmentType,
)
