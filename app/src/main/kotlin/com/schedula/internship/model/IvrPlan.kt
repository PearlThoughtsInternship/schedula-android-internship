package com.schedula.internship.model

enum class IvrPlanStatus {
    Planned,
    Confirmed,
    Converted,
}

data class IvrPlan(
    val id: String,
    val doctorId: String,
    val patientId: String,
    val ivrAppId: String,
    val dateLabel: String,
    val slotId: String,
    val paymentConfirmed: Boolean,
    val status: IvrPlanStatus,
)
