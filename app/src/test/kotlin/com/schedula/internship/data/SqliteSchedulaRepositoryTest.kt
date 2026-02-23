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
}
