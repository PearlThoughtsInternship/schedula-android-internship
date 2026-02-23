package com.schedula.internship.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.schedula.internship.model.AppointmentStatus
import com.schedula.internship.model.AppointmentType
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
}
