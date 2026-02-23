package com.schedula.internship.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AuthRulesTest {

    @Test
    fun validPhoneRequiresTenDigits() {
        assertThat(AuthRules.isValidPhone("9876543210")).isTrue()
        assertThat(AuthRules.isValidPhone("98765")).isFalse()
        assertThat(AuthRules.isValidPhone("98 76 54 32 10")).isTrue()
    }

    @Test
    fun otpValidationAcceptsOnlyDemoOtp() {
        assertThat(AuthRules.isValidOtp("1234")).isTrue()
        assertThat(AuthRules.isValidOtp("4321")).isFalse()
    }
}
