package com.schedula.internship.ui

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.schedula.internship.data.LocalStoreProvider
import com.schedula.internship.model.AppointmentType
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class AppViewModelIntegrationTest {

    private lateinit var app: Application
    private lateinit var viewModel: AppViewModel

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        LocalStoreProvider.resetForTests()
        app.deleteDatabase("schedula-internship.db")
        viewModel = AppViewModel(app)
        waitUntil("viewmodel ready") { viewModel.uiState.isReady }
    }

    @After
    fun tearDown() {
        LocalStoreProvider.resetForTests()
        app.deleteDatabase("schedula-internship.db")
    }

    @Test
    fun loginAndBookingFlowReachesConfirmationWithPersistedMetadata() {
        login()

        viewModel.openDoctorProfile("doctor-kumar")
        viewModel.openBookingDate()
        viewModel.selectDate("Tomorrow")
        viewModel.selectBookingType(AppointmentType.Regular)
        viewModel.openBookingTime()
        viewModel.selectNextAvailableSlot()
        waitUntil("slot selected") { viewModel.uiState.selectedSlotId != null }
        viewModel.selectPatient("patient-self")
        viewModel.bookAppointment()

        waitUntil("booking confirmation screen") { viewModel.uiState.activeScreen == Screen.BookingConfirmation }
        assertThat(viewModel.uiState.selectedAppointmentId).isNotNull()
        assertThat(viewModel.uiState.statusMessage).contains("Appointment confirmed")

        viewModel.openPatientDetails()
        viewModel.markPaymentPaid()
        waitUntil("payment confirmation") {
            viewModel.uiState.statusMessage?.contains("Payment confirmed") == true ||
                viewModel.uiState.statusMessage?.contains("already completed") == true
        }
    }

    @Test
    fun supportAndReviewFlowsRespectValidationAndPersistSuccess() {
        login()

        viewModel.openSupport()
        viewModel.createSupportTicket("abc", "long enough issue text")
        waitUntil("support validation message") {
            viewModel.uiState.statusMessage?.contains("at least 4") == true
        }
        viewModel.consumeStatusMessage()

        viewModel.createSupportTicket("Payment issue", "Unable to complete payment due to bank decline")
        waitUntil("support ticket success") {
            viewModel.uiState.statusMessage?.contains("Support ticket created (SUP-") == true
        }
        viewModel.consumeStatusMessage()

        viewModel.submitGoogleReview(rating = 2, comment = "bad")
        waitUntil("review validation message") {
            viewModel.uiState.statusMessage?.contains("low ratings") == true
        }
        viewModel.consumeStatusMessage()

        viewModel.submitGoogleReview(rating = 5, comment = "Great doctor and smooth consultation")
        waitUntil("review submitted") {
            viewModel.uiState.statusMessage?.contains("Review submitted (REV-") == true
        }
    }

    private fun login() {
        viewModel.updatePhoneNumber("9876543210")
        viewModel.sendOtp()
        viewModel.updateOtp("1234")
        viewModel.verifyOtp()
        waitUntil("logged in") {
            viewModel.uiState.isLoggedIn && viewModel.uiState.activeScreen == Screen.DoctorSearch
        }
    }

    private fun waitUntil(label: String, timeoutMs: Long = 10_000, predicate: () -> Boolean) {
        val start = System.currentTimeMillis()
        while (!predicate()) {
            if (System.currentTimeMillis() - start > timeoutMs) {
                throw AssertionError("Timed out waiting for $label")
            }
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }
    }
}
