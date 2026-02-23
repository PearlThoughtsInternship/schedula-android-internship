package com.schedula.internship.domain

object AuthRules {
    private const val DemoOtp = "1234"

    fun isValidPhone(phoneNumber: String): Boolean {
        val digits = phoneNumber.filter(Char::isDigit)
        return digits.length == 10
    }

    fun isValidOtp(otp: String): Boolean = otp == DemoOtp
}
