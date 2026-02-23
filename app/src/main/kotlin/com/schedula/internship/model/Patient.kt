package com.schedula.internship.model

data class Patient(
    val id: String,
    val name: String,
    val age: Int,
    val sex: String,
    val relation: String,
    val weightKg: Int,
    val complaint: String,
)
