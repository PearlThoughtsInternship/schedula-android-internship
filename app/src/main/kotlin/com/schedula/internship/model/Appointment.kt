package com.schedula.internship.model

enum class AppointmentStatus {
    Scheduled,
    Rescheduled,
    Completed,
    Cancelled,
}

enum class AppointmentType {
    Online,
    Regular,
}

enum class PaymentStatus {
    Unpaid,
    Paid,
}

data class Appointment(
    val id: String,
    val doctorId: String,
    val doctorName: String,
    val doctorSpecialty: String,
    val doctorExperienceYears: Int,
    val specialty: String,
    val patientId: String,
    val patientName: String,
    val slotId: String,
    val dateLabel: String,
    val timeLabel: String,
    val slotLabel: String,
    val tokenNumber: Int,
    val channel: String,
    val complaint: String?,
    val status: AppointmentStatus,
    val type: AppointmentType,
    val paymentStatus: PaymentStatus,
    val report: String,
    val followUpRequested: Boolean,
    val consultingFeedback: Int?,
    val hospitalFeedback: Int?,
    val waitingTimeFeedback: Int?,
    val confirmationCode: String,
    val paymentReference: String?,
)
