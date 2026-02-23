package com.schedula.internship.model

data class GoogleReviewState(
    val requested: Boolean,
    val submitted: Boolean,
    val rating: Int?,
    val comment: String,
)
