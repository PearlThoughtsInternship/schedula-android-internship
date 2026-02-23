package com.schedula.internship.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import com.schedula.internship.model.Appointment
import com.schedula.internship.model.AppointmentStatus
import com.schedula.internship.model.AppointmentType
import com.schedula.internship.model.Doctor
import com.schedula.internship.model.Patient
import com.schedula.internship.model.Slot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val MetaLoggedInPhone = "logged_in_phone"

@Entity(tableName = "doctors")
data class DoctorEntity(
    @androidx.room.PrimaryKey val id: String,
    val name: String,
    val specialty: String,
    val hospital: String,
    val rating: Double,
    val yearsOfExperience: Int,
    val consultationFee: Double,
    val bio: String,
    val availabilitySummary: String,
)

@Entity(tableName = "patients")
data class PatientEntity(
    @androidx.room.PrimaryKey val id: String,
    val name: String,
    val age: Int,
    val sex: String,
    val relation: String,
    val weightKg: Int,
    val complaint: String,
)

@Entity(
    tableName = "slots",
    indices = [Index(value = ["doctorId", "dateLabel", "timeOrder"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = DoctorEntity::class,
            parentColumns = ["id"],
            childColumns = ["doctorId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SlotEntity(
    @androidx.room.PrimaryKey val id: String,
    val doctorId: String,
    val dateLabel: String,
    val dateOrder: Int,
    val timeLabel: String,
    val timeOrder: Int,
    val isBooked: Boolean,
    val type: String,
)

@Entity(
    tableName = "appointments",
    indices = [Index("doctorId"), Index("patientId"), Index("slotId")],
    foreignKeys = [
        ForeignKey(
            entity = DoctorEntity::class,
            parentColumns = ["id"],
            childColumns = ["doctorId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = SlotEntity::class,
            parentColumns = ["id"],
            childColumns = ["slotId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
)
data class AppointmentEntity(
    @androidx.room.PrimaryKey val id: String,
    val doctorId: String,
    val patientId: String,
    val slotId: String,
    val tokenNumber: Int,
    val channel: String,
    val complaint: String?,
    val status: String,
    val type: String,
    val createdAtEpochMillis: Long,
)

@Entity(tableName = "app_meta")
data class AppMetaEntity(
    @androidx.room.PrimaryKey val key: String,
    val value: String,
)

data class AppointmentRecord(
    @Embedded val appointment: AppointmentEntity,
    @Relation(parentColumn = "doctorId", entityColumn = "id") val doctor: DoctorEntity,
    @Relation(parentColumn = "patientId", entityColumn = "id") val patient: PatientEntity,
    @Relation(parentColumn = "slotId", entityColumn = "id") val slot: SlotEntity,
)

@Dao
interface SchedulaDao {
    @Query("SELECT COUNT(*) FROM doctors")
    fun countDoctors(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDoctors(doctors: List<DoctorEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPatients(patients: List<PatientEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSlots(slots: List<SlotEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAppointments(appointments: List<AppointmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertMeta(meta: AppMetaEntity)

    @Query("SELECT * FROM doctors ORDER BY name")
    fun observeDoctors(): Flow<List<DoctorEntity>>

    @Query("SELECT * FROM doctors WHERE name LIKE '%' || :query || '%' OR specialty LIKE '%' || :query || '%' ORDER BY name")
    fun searchDoctors(query: String): Flow<List<DoctorEntity>>

    @Query("SELECT * FROM doctors WHERE id = :doctorId LIMIT 1")
    fun observeDoctor(doctorId: String): Flow<DoctorEntity?>

    @Query("SELECT * FROM doctors WHERE id = :doctorId LIMIT 1")
    fun getDoctor(doctorId: String): DoctorEntity?

    @Query("SELECT DISTINCT dateLabel FROM slots WHERE doctorId = :doctorId ORDER BY dateOrder")
    fun observeDateOptions(doctorId: String): Flow<List<String>>

    @Query("SELECT * FROM slots WHERE doctorId = :doctorId AND dateLabel = :dateLabel ORDER BY timeOrder")
    fun observeSlotsForDate(doctorId: String, dateLabel: String): Flow<List<SlotEntity>>

    @Query("SELECT * FROM slots WHERE id = :slotId LIMIT 1")
    fun getSlot(slotId: String): SlotEntity?

    @Query("UPDATE slots SET isBooked = :isBooked WHERE id = :slotId")
    fun setSlotBooked(slotId: String, isBooked: Boolean)

    @Query("SELECT * FROM patients ORDER BY id")
    fun observePatients(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients WHERE id = :patientId LIMIT 1")
    fun observePatient(patientId: String): Flow<PatientEntity?>

    @Query("SELECT * FROM patients WHERE id = :patientId LIMIT 1")
    fun getPatient(patientId: String): PatientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPatient(patient: PatientEntity)

    @Transaction
    @Query("SELECT * FROM appointments ORDER BY createdAtEpochMillis DESC")
    fun observeAppointmentRecords(): Flow<List<AppointmentRecord>>

    @Transaction
    @Query("SELECT * FROM appointments WHERE id = :appointmentId LIMIT 1")
    fun observeAppointmentRecord(appointmentId: String): Flow<AppointmentRecord?>

    @Transaction
    @Query("SELECT * FROM appointments WHERE id = :appointmentId LIMIT 1")
    fun getAppointmentRecord(appointmentId: String): AppointmentRecord?

    @Query("SELECT * FROM appointments WHERE id = :appointmentId LIMIT 1")
    fun getAppointment(appointmentId: String): AppointmentEntity?

    @Query("SELECT MAX(tokenNumber) FROM appointments")
    fun maxToken(): Int?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertAppointment(appointment: AppointmentEntity)

    @Update
    fun updateAppointment(appointment: AppointmentEntity)

    @Query("SELECT value FROM app_meta WHERE key = :key LIMIT 1")
    fun observeMetaValue(key: String): Flow<String?>
}

@Database(
    entities = [DoctorEntity::class, PatientEntity::class, SlotEntity::class, AppointmentEntity::class, AppMetaEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class SchedulaDatabase : RoomDatabase() {
    abstract fun dao(): SchedulaDao

    companion object {
        fun build(context: Context): SchedulaDatabase {
            return Room.databaseBuilder(context, SchedulaDatabase::class.java, "schedula-internship.db").build()
        }
    }
}

object LocalStoreProvider {
    @Volatile
    private var instance: SchedulaDatabase? = null

    fun getDatabase(context: Context): SchedulaDatabase {
        return instance ?: synchronized(this) {
            instance ?: SchedulaDatabase.build(context.applicationContext).also { instance = it }
        }
    }
}

sealed interface BookingResult {
    data class Success(val appointment: Appointment) : BookingResult
    data class SlotUnavailable(val message: String) : BookingResult
    data class Error(val message: String) : BookingResult
}

interface SchedulaRepository {
    suspend fun ensureSeeded()
    fun observeLoggedInPhone(): Flow<String?>
    suspend fun setLoggedInPhone(phone: String?)
    fun observeDoctors(query: String): Flow<List<Doctor>>
    fun observeDoctor(doctorId: String): Flow<Doctor?>
    fun observeDateOptions(doctorId: String): Flow<List<String>>
    fun observeSlots(doctorId: String, dateLabel: String): Flow<List<Slot>>
    fun observePatients(): Flow<List<Patient>>
    fun observePatient(patientId: String): Flow<Patient?>
    suspend fun addOrUpdatePatient(patient: Patient)
    fun observeAppointments(): Flow<List<Appointment>>
    fun observeAppointment(appointmentId: String): Flow<Appointment?>
    suspend fun bookAppointment(
        doctorId: String,
        patientId: String,
        slotId: String,
        appointmentType: AppointmentType,
        channel: String,
    ): BookingResult

    suspend fun cancelAppointment(appointmentId: String)
    suspend fun rescheduleAppointment(appointmentId: String, newSlotId: String): BookingResult
}

class SqliteSchedulaRepository(
    private val database: SchedulaDatabase,
) : SchedulaRepository {
    private val dao = database.dao()

    override suspend fun ensureSeeded() {
        if (dao.countDoctors() > 0) return
        database.runInTransaction {
            dao.insertDoctors(seedDoctors())
            dao.insertPatients(seedPatients())
            dao.insertSlots(seedSlots())
            dao.insertAppointments(seedAppointments())
            dao.upsertMeta(AppMetaEntity(MetaLoggedInPhone, ""))
        }
    }

    override fun observeLoggedInPhone(): Flow<String?> {
        return dao.observeMetaValue(MetaLoggedInPhone).map { it?.takeIf(String::isNotBlank) }
    }

    override suspend fun setLoggedInPhone(phone: String?) {
        dao.upsertMeta(AppMetaEntity(MetaLoggedInPhone, phone.orEmpty()))
    }

    override fun observeDoctors(query: String): Flow<List<Doctor>> {
        val src = if (query.isBlank()) dao.observeDoctors() else dao.searchDoctors(query)
        return src.map { list -> list.map(DoctorEntity::toModel) }
    }

    override fun observeDoctor(doctorId: String): Flow<Doctor?> {
        return dao.observeDoctor(doctorId).map { it?.toModel() }
    }

    override fun observeDateOptions(doctorId: String): Flow<List<String>> = dao.observeDateOptions(doctorId)

    override fun observeSlots(doctorId: String, dateLabel: String): Flow<List<Slot>> {
        return dao.observeSlotsForDate(doctorId, dateLabel).map { list -> list.map(SlotEntity::toModel) }
    }

    override fun observePatients(): Flow<List<Patient>> {
        return dao.observePatients().map { list -> list.map(PatientEntity::toModel) }
    }

    override fun observePatient(patientId: String): Flow<Patient?> {
        return dao.observePatient(patientId).map { it?.toModel() }
    }

    override suspend fun addOrUpdatePatient(patient: Patient) {
        dao.insertPatient(patient.toEntity())
    }

    override fun observeAppointments(): Flow<List<Appointment>> {
        return dao.observeAppointmentRecords().map { rows -> rows.map(AppointmentRecord::toModel) }
    }

    override fun observeAppointment(appointmentId: String): Flow<Appointment?> {
        return dao.observeAppointmentRecord(appointmentId).map { it?.toModel() }
    }

    override suspend fun bookAppointment(
        doctorId: String,
        patientId: String,
        slotId: String,
        appointmentType: AppointmentType,
        channel: String,
    ): BookingResult {
        val doctor = dao.getDoctor(doctorId) ?: return BookingResult.Error("Doctor not found")
        val patient = dao.getPatient(patientId) ?: return BookingResult.Error("Patient not found")
        val slot = dao.getSlot(slotId) ?: return BookingResult.Error("Slot not found")
        if (slot.isBooked) return BookingResult.SlotUnavailable("Selected slot is no longer available")

        val appointmentId = "appointment-${System.currentTimeMillis()}"
        val token = (dao.maxToken() ?: 0) + 1

        database.runInTransaction {
            dao.setSlotBooked(slot.id, true)
            dao.insertAppointment(
                AppointmentEntity(
                    id = appointmentId,
                    doctorId = doctor.id,
                    patientId = patient.id,
                    slotId = slot.id,
                    tokenNumber = token,
                    channel = channel,
                    complaint = patient.complaint,
                    status = AppointmentStatus.Scheduled.name,
                    type = appointmentType.name,
                    createdAtEpochMillis = System.currentTimeMillis(),
                ),
            )
        }

        val booked = dao.getAppointmentRecord(appointmentId)?.toModel()
            ?: return BookingResult.Error("Unable to load booked appointment")
        return BookingResult.Success(booked)
    }

    override suspend fun cancelAppointment(appointmentId: String) {
        val current = dao.getAppointment(appointmentId) ?: return
        database.runInTransaction {
            dao.setSlotBooked(current.slotId, false)
            dao.updateAppointment(
                current.copy(
                    status = AppointmentStatus.Cancelled.name,
                    createdAtEpochMillis = System.currentTimeMillis(),
                ),
            )
        }
    }

    override suspend fun rescheduleAppointment(appointmentId: String, newSlotId: String): BookingResult {
        val current = dao.getAppointment(appointmentId) ?: return BookingResult.Error("Appointment not found")
        val newSlot = dao.getSlot(newSlotId) ?: return BookingResult.Error("Slot not found")
        if (newSlot.isBooked) return BookingResult.SlotUnavailable("Reschedule failed. Slot already booked")

        database.runInTransaction {
            dao.setSlotBooked(current.slotId, false)
            dao.setSlotBooked(newSlot.id, true)
            dao.updateAppointment(
                current.copy(
                    slotId = newSlot.id,
                    status = AppointmentStatus.Rescheduled.name,
                    createdAtEpochMillis = System.currentTimeMillis(),
                ),
            )
        }

        val updated = dao.getAppointmentRecord(appointmentId)?.toModel()
            ?: return BookingResult.Error("Unable to reload appointment")
        return BookingResult.Success(updated)
    }

    private fun seedDoctors(): List<DoctorEntity> {
        return listOf(
            DoctorEntity(
                id = "doctor-lavangi",
                name = "Dr. Lavangi",
                specialty = "Gynacologist",
                hospital = "Schedula Women Care",
                rating = 4.9,
                yearsOfExperience = 15,
                consultationFee = 600.0,
                bio = "Gold Medalist",
                availabilitySummary = "Monday to Friday (10 AM to 1 PM), Saturday (2 PM to 5 PM)",
            ),
            DoctorEntity(
                id = "doctor-kumar",
                name = "Dr. Kumar",
                specialty = "Pediatrics",
                hospital = "City Child Clinic",
                rating = 4.7,
                yearsOfExperience = 12,
                consultationFee = 550.0,
                bio = "Child specialist",
                availabilitySummary = "Monday to Saturday (10 AM to 8 PM)",
            ),
            DoctorEntity(
                id = "doctor-elango",
                name = "Dr. Elango",
                specialty = "General Medicine",
                hospital = "South Health Center",
                rating = 4.5,
                yearsOfExperience = 10,
                consultationFee = 450.0,
                bio = "Family physician",
                availabilitySummary = "Monday to Friday (9 AM to 6 PM)",
            ),
        )
    }

    private fun seedPatients(): List<PatientEntity> {
        return listOf(
            PatientEntity(
                id = "patient-self",
                name = "Muthukumar",
                age = 28,
                sex = "Male",
                relation = "Self",
                weightKg = 73,
                complaint = "General consultation",
            ),
            PatientEntity(
                id = "patient-meena",
                name = "Meena",
                age = 26,
                sex = "Female",
                relation = "Wife",
                weightKg = 58,
                complaint = "Stomach pain",
            ),
            PatientEntity(
                id = "patient-kishore",
                name = "Kishore",
                age = 21,
                sex = "Male",
                relation = "Son",
                weightKg = 62,
                complaint = "Fever follow-up",
            ),
        )
    }

    private fun seedSlots(): List<SlotEntity> {
        val dateOptions = listOf("Today", "Tomorrow", "Next available day", "Next available week", "1st Oct", "2nd Oct", "3rd Oct")
        val times = listOf("10:00 AM - 11:00 AM", "11:00 AM - 12:00 PM", "06:00 PM - 07:00 PM", "07:00 PM - 08:00 PM")
        val doctors = listOf("doctor-lavangi", "doctor-kumar", "doctor-elango")

        return buildList {
            doctors.forEach { doctorId ->
                dateOptions.forEachIndexed { dateIndex, date ->
                    times.forEachIndexed { timeIndex, time ->
                        add(
                            SlotEntity(
                                id = "$doctorId-${date.replace(" ", "-")}-${timeIndex}",
                                doctorId = doctorId,
                                dateLabel = date,
                                dateOrder = dateIndex,
                                timeLabel = time,
                                timeOrder = timeIndex,
                                isBooked = doctorId == "doctor-lavangi" && date == "Today" && timeIndex == 0,
                                type = if (timeIndex <= 1) AppointmentType.Regular.name else AppointmentType.Online.name,
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun seedAppointments(): List<AppointmentEntity> {
        return listOf(
            AppointmentEntity(
                id = "appointment-seed-1",
                doctorId = "doctor-lavangi",
                patientId = "patient-meena",
                slotId = "doctor-lavangi-Today-0",
                tokenNumber = 14,
                channel = "APP",
                complaint = "Stomach pain",
                status = AppointmentStatus.Scheduled.name,
                type = AppointmentType.Regular.name,
                createdAtEpochMillis = System.currentTimeMillis() - 30_000,
            ),
            AppointmentEntity(
                id = "appointment-seed-2",
                doctorId = "doctor-kumar",
                patientId = "patient-self",
                slotId = "doctor-kumar-1st-Oct-1",
                tokenNumber = 9,
                channel = "APP",
                complaint = "General consultation",
                status = AppointmentStatus.Completed.name,
                type = AppointmentType.Online.name,
                createdAtEpochMillis = System.currentTimeMillis() - 86_400_000,
            ),
        )
    }
}

private fun DoctorEntity.toModel(): Doctor {
    return Doctor(id, name, specialty, hospital, rating, yearsOfExperience, consultationFee, bio, availabilitySummary)
}

private fun PatientEntity.toModel(): Patient {
    return Patient(id, name, age, sex, relation, weightKg, complaint)
}

private fun Patient.toEntity(): PatientEntity {
    return PatientEntity(id, name, age, sex, relation, weightKg, complaint)
}

private fun SlotEntity.toModel(): Slot {
    return Slot(id = id, doctorId = doctorId, dateLabel = dateLabel, timeLabel = timeLabel, isBooked = isBooked, type = AppointmentType.valueOf(type))
}

private fun AppointmentRecord.toModel(): Appointment {
    return Appointment(
        id = appointment.id,
        doctorId = doctor.id,
        doctorName = doctor.name,
        doctorSpecialty = doctor.specialty,
        doctorExperienceYears = doctor.yearsOfExperience,
        specialty = doctor.specialty,
        patientId = patient.id,
        patientName = patient.name,
        slotId = slot.id,
        dateLabel = slot.dateLabel,
        timeLabel = slot.timeLabel,
        slotLabel = "${slot.dateLabel} ${slot.timeLabel}",
        tokenNumber = appointment.tokenNumber,
        channel = appointment.channel,
        complaint = appointment.complaint,
        status = AppointmentStatus.valueOf(appointment.status),
        type = AppointmentType.valueOf(appointment.type),
    )
}
