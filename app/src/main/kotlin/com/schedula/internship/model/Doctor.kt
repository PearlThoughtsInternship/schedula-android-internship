package com.schedula.internship.model

data class Doctor(
    val id: String,
    val name: String,
    val specialty: String,
    val hospital: String,
    val rating: Double,
    val yearsOfExperience: Int,
    val consultationFee: Double,
    val bio: String,
    val availabilitySummary: String,
)
