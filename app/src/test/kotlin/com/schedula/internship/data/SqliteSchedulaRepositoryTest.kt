package com.schedula.internship.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.schedula.internship.model.AppointmentStatus
import com.schedula.internship.model.AppointmentType
import com.schedula.internship.model.ChatSender
import com.schedula.internship.model.ChatThreadType
import com.schedula.internship.model.IvrPlanStatus
import com.schedula.internship.model.PaymentStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SqliteSchedulaRepositoryTest {

    private lateinit var db: SchedulaDatabase
    private lateinit var repository: SqliteSchedulaRepository

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SchedulaDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = SqliteSchedulaRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun bookingAvailableSlotCreatesScheduledAppointment() = runTest {
        repository.ensureSeeded()

        val slot = repository.observeSlots("doctor-kumar", "Today").first().first { !it.isBooked }
        val result = repository.bookAppointment(
            doctorId = "doctor-kumar",
            patientId = "patient-self",
            slotId = slot.id,
            appointmentType = AppointmentType.Regular,
            channel = "APP",
        )

        assertThat(result).isInstanceOf(BookingResult.Success::class.java)

        val upcoming = repository.observeAppointments().first().filter {
            it.status == AppointmentStatus.Scheduled || it.status == AppointmentStatus.Rescheduled
        }
        assertThat(upcoming).isNotEmpty()
        assertThat(upcoming.first().slotId).isEqualTo(slot.id)
    }

    @Test
    fun bookingSameSlotTwiceReturnsUnavailable() = runTest {
        repository.ensureSeeded()

        val slot = repository.observeSlots("doctor-elango", "Today").first().first { !it.isBooked }
        val first = repository.bookAppointment(
            doctorId = "doctor-elango",
            patientId = "patient-self",
            slotId = slot.id,
            appointmentType = AppointmentType.Regular,
            channel = "APP",
        )
        val second = repository.bookAppointment(
            doctorId = "doctor-elango",
            patientId = "patient-meena",
            slotId = slot.id,
            appointmentType = AppointmentType.Regular,
            channel = "APP",
        )

        assertThat(first).isInstanceOf(BookingResult.Success::class.java)
        assertThat(second).isInstanceOf(BookingResult.SlotUnavailable::class.java)
    }

    @Test
    fun cancelAndRescheduleUpdateAppointmentState() = runTest {
        repository.ensureSeeded()

        val bookedSlot = repository.observeSlots("doctor-kumar", "Tomorrow").first().first { !it.isBooked }
        val booked = repository.bookAppointment(
            doctorId = "doctor-kumar",
            patientId = "patient-self",
            slotId = bookedSlot.id,
            appointmentType = AppointmentType.Regular,
            channel = "APP",
        ) as BookingResult.Success

        repository.cancelAppointment(booked.appointment.id)
        val canceled = repository.observeAppointment(booked.appointment.id).first()
        assertThat(canceled?.status).isEqualTo(AppointmentStatus.Cancelled)

        val replacement = repository.observeSlots("doctor-kumar", "2nd Oct").first().first { !it.isBooked }
        val rebooked = repository.bookAppointment(
            doctorId = "doctor-kumar",
            patientId = "patient-self",
            slotId = replacement.id,
            appointmentType = AppointmentType.Online,
            channel = "APP",
        ) as BookingResult.Success

        val nextSlot = repository.observeSlots("doctor-kumar", "3rd Oct").first().first { !it.isBooked }
        val rescheduled = repository.rescheduleAppointment(rebooked.appointment.id, nextSlot.id)
        assertThat(rescheduled).isInstanceOf(BookingResult.Success::class.java)

        val updated = repository.observeAppointment(rebooked.appointment.id).first()
        assertThat(updated?.status).isEqualTo(AppointmentStatus.Rescheduled)
        assertThat(updated?.slotId).isEqualTo(nextSlot.id)
    }

    @Test
    fun cancelingAppointmentFreesSlotForAnotherBooking() = runTest {
        repository.ensureSeeded()

        val targetSlot = repository.observeSlots("doctor-kumar", "Today").first().first { !it.isBooked }
        val firstBooking = repository.bookAppointment(
            doctorId = "doctor-kumar",
            patientId = "patient-self",
            slotId = targetSlot.id,
            appointmentType = AppointmentType.Regular,
            channel = "APP",
        ) as BookingResult.Success

        repository.cancelAppointment(firstBooking.appointment.id)

        val secondBooking = repository.bookAppointment(
            doctorId = "doctor-kumar",
            patientId = "patient-meena",
            slotId = targetSlot.id,
            appointmentType = AppointmentType.Online,
            channel = "APP",
        )

        assertThat(secondBooking).isInstanceOf(BookingResult.Success::class.java)
    }

    @Test
    fun doctorSearchFiltersByNameAndSpecialty() = runTest {
        repository.ensureSeeded()

        val byName = repository.observeDoctors("Lavangi").first()
        val bySpecialty = repository.observeDoctors("Pediatrics").first()

        assertThat(byName.map { it.id }).containsExactly("doctor-lavangi")
        assertThat(bySpecialty.map { it.id }).containsExactly("doctor-kumar")
    }

    @Test
    fun loginPhoneStatePersistsInMetaStore() = runTest {
        repository.ensureSeeded()

        repository.setLoggedInPhone("9876543210")
        val loggedIn = repository.observeLoggedInPhone().first()
        assertThat(loggedIn).isEqualTo("9876543210")

        repository.setLoggedInPhone(null)
        val loggedOut = repository.observeLoggedInPhone().first()
        assertThat(loggedOut).isNull()
    }

    @Test
    fun paymentReportFollowUpAndFeedbackPersistOnAppointment() = runTest {
        repository.ensureSeeded()

        val slot = repository.observeSlots("doctor-kumar", "Next available day").first().first { !it.isBooked }
        val booked = repository.bookAppointment(
            doctorId = "doctor-kumar",
            patientId = "patient-self",
            slotId = slot.id,
            appointmentType = AppointmentType.Regular,
            channel = "APP",
        ) as BookingResult.Success

        repository.markPaymentPaid(booked.appointment.id)
        repository.saveAppointmentReport(booked.appointment.id, "Vitals stable")
        repository.setFollowUpRequested(booked.appointment.id, true)
        repository.submitConsultingFeedback(booked.appointment.id, consulting = 5, hospital = 4, waiting = 3)

        val updated = repository.observeAppointment(booked.appointment.id).first()
        assertThat(updated?.paymentStatus).isEqualTo(PaymentStatus.Paid)
        assertThat(updated?.report).isEqualTo("Vitals stable")
        assertThat(updated?.followUpRequested).isTrue()
        assertThat(updated?.consultingFeedback).isEqualTo(5)
        assertThat(updated?.hospitalFeedback).isEqualTo(4)
        assertThat(updated?.waitingTimeFeedback).isEqualTo(3)
    }

    @Test
    fun supportTicketAndChatAreStored() = runTest {
        repository.ensureSeeded()

        repository.createSupportTicket("Payment issue", "Unable to complete payment")
        repository.sendChatMessage(ChatThreadType.Support, ChatSender.User, "Need help quickly")

        val tickets = repository.observeSupportTickets().first()
        val supportChat = repository.observeChat(ChatThreadType.Support).first()

        assertThat(tickets).isNotEmpty()
        assertThat(tickets.first().subject).isEqualTo("Payment issue")
        assertThat(supportChat.last().content).contains("Need help quickly")
    }

    @Test
    fun ivrPlanCanBeConfirmedAndConverted() = runTest {
        repository.ensureSeeded()

        val plan = repository.observeIvrPlans().first().first()
        val result = repository.confirmIvrPlan(plan.id)
        assertThat(result).isInstanceOf(BookingResult.Success::class.java)

        val updatedPlan = repository.observeIvrPlans().first().first { it.id == plan.id }
        assertThat(updatedPlan.status).isEqualTo(IvrPlanStatus.Converted)
    }

    @Test
    fun inviteAndReviewAndCollaborationPersist() = runTest {
        repository.ensureSeeded()

        repository.setPatientInvite("patient-meena", true)
        repository.setCollaborationConnected(true)
        repository.submitGoogleReview(rating = 5, comment = "Excellent consultation")

        val meena = repository.observePatient("patient-meena").first()
        val collaboration = repository.observeCollaborationState().first()
        val review = repository.observeGoogleReviewState().first()

        assertThat(meena?.invited).isTrue()
        assertThat(collaboration.connected).isTrue()
        assertThat(review.submitted).isTrue()
        assertThat(review.rating).isEqualTo(5)
    }

    @Test
    fun remindersContainAppointmentsAndDoctorNotices() = runTest {
        repository.ensureSeeded()

        val reminders = repository.observeReminders().first()

        assertThat(reminders).isNotEmpty()
        assertThat(reminders.any { it.id.startsWith("reminder-appt-") }).isTrue()
        assertThat(reminders.any { it.id.startsWith("reminder-notice-") }).isTrue()
    }
}
