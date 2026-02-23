package com.schedula.internship.model

data class DoctorNotice(
    val id: String,
    val appointmentId: String,
    val message: String,
    val suggestedSlotId: String?,
    val resolved: Boolean,
)
