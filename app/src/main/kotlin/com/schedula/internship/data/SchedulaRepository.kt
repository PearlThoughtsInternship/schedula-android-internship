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
import com.schedula.internship.model.ChatMessage
import com.schedula.internship.model.ChatSender
import com.schedula.internship.model.ChatThreadType
import com.schedula.internship.model.CollaborationState
import com.schedula.internship.model.Doctor
import com.schedula.internship.model.DoctorNotice
import com.schedula.internship.model.GoogleReviewState
import com.schedula.internship.model.IvrPlan
import com.schedula.internship.model.IvrPlanStatus
import com.schedula.internship.model.Patient
import com.schedula.internship.model.PaymentStatus
import com.schedula.internship.model.ReminderItem
import com.schedula.internship.model.Slot
import com.schedula.internship.model.SupportTicket
import com.schedula.internship.model.SupportTicketStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
    val invited: Boolean,
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
    val paymentStatus: String,
    val confirmationCode: String,
    val paymentReference: String?,
    val report: String,
    val followUpRequested: Boolean,
    val consultingFeedback: Int?,
    val hospitalFeedback: Int?,
    val waitingTimeFeedback: Int?,
    val createdAtEpochMillis: Long,
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @androidx.room.PrimaryKey val id: String,
    val threadType: String,
    val sender: String,
    val content: String,
    val createdAtEpochMillis: Long,
)

@Entity(tableName = "support_tickets")
data class SupportTicketEntity(
    @androidx.room.PrimaryKey val id: String,
    val subject: String,
    val message: String,
    val status: String,
    val externalReference: String,
    val estimatedResolutionHours: Int,
    val createdAtEpochMillis: Long,
)

@Entity(tableName = "doctor_notices")
data class DoctorNoticeEntity(
    @androidx.room.PrimaryKey val id: String,
    val appointmentId: String,
    val message: String,
    val suggestedSlotId: String?,
    val resolved: Boolean,
)

@Entity(tableName = "ivr_plans")
data class IvrPlanEntity(
    @androidx.room.PrimaryKey val id: String,
    val doctorId: String,
    val patientId: String,
    val ivrAppId: String,
    val dateLabel: String,
    val slotId: String,
    val paymentConfirmed: Boolean,
    val status: String,
    val convertedAppointmentId: String?,
)

@Entity(tableName = "collaboration_state")
data class CollaborationEntity(
    @androidx.room.PrimaryKey val id: String,
    val connected: Boolean,
    val groupName: String,
)

@Entity(tableName = "google_review_state")
data class GoogleReviewEntity(
    @androidx.room.PrimaryKey val id: String,
    val requested: Boolean,
    val submitted: Boolean,
    val rating: Int?,
    val comment: String,
    val moderationReference: String?,
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
    fun insertChatMessages(messages: List<ChatMessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDoctorNotices(notices: List<DoctorNoticeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertIvrPlans(plans: List<IvrPlanEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertCollaboration(entity: CollaborationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertGoogleReview(entity: GoogleReviewEntity)

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

    @Query("SELECT * FROM slots WHERE doctorId = :doctorId AND isBooked = 0 ORDER BY dateOrder, timeOrder LIMIT 1")
    fun getNextAvailableSlot(doctorId: String): SlotEntity?

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

    @Update
    fun updatePatient(patient: PatientEntity)

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

    @Query("SELECT * FROM chat_messages WHERE threadType = :threadType ORDER BY createdAtEpochMillis")
    fun observeChatMessages(threadType: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertChatMessage(message: ChatMessageEntity)

    @Query("SELECT * FROM support_tickets ORDER BY createdAtEpochMillis DESC")
    fun observeSupportTickets(): Flow<List<SupportTicketEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSupportTicket(ticket: SupportTicketEntity)

    @Query("SELECT * FROM doctor_notices WHERE resolved = 0")
    fun observeDoctorNotices(): Flow<List<DoctorNoticeEntity>>

    @Query("SELECT * FROM doctor_notices WHERE id = :noticeId LIMIT 1")
    fun getDoctorNotice(noticeId: String): DoctorNoticeEntity?

    @Update
    fun updateDoctorNotice(notice: DoctorNoticeEntity)

    @Query("SELECT * FROM ivr_plans ORDER BY id DESC")
    fun observeIvrPlans(): Flow<List<IvrPlanEntity>>

    @Query("SELECT * FROM ivr_plans WHERE id = :planId LIMIT 1")
    fun getIvrPlan(planId: String): IvrPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertIvrPlan(plan: IvrPlanEntity)

    @Query("SELECT * FROM collaboration_state WHERE id = 'default' LIMIT 1")
    fun observeCollaboration(): Flow<CollaborationEntity?>

    @Query("SELECT * FROM google_review_state WHERE id = 'default' LIMIT 1")
    fun observeGoogleReview(): Flow<GoogleReviewEntity?>

    @Query("SELECT value FROM app_meta WHERE key = :key LIMIT 1")
    fun observeMetaValue(key: String): Flow<String?>
}

@Database(
    entities = [
        DoctorEntity::class,
        PatientEntity::class,
        SlotEntity::class,
        AppointmentEntity::class,
        ChatMessageEntity::class,
        SupportTicketEntity::class,
        DoctorNoticeEntity::class,
        IvrPlanEntity::class,
        CollaborationEntity::class,
        GoogleReviewEntity::class,
        AppMetaEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class SchedulaDatabase : RoomDatabase() {
    abstract fun dao(): SchedulaDao

    companion object {
        fun build(context: Context): SchedulaDatabase {
            return Room.databaseBuilder(context, SchedulaDatabase::class.java, "schedula-internship.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
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

sealed interface OperationResult {
    data class Success(val message: String) : OperationResult
    data class Error(val message: String) : OperationResult
}

interface SchedulaRepository {
    suspend fun ensureSeeded()
    fun observeLoggedInPhone(): Flow<String?>
    suspend fun setLoggedInPhone(phone: String?)

    fun observeDoctors(query: String): Flow<List<Doctor>>
    fun observeDoctor(doctorId: String): Flow<Doctor?>
    fun observeDateOptions(doctorId: String): Flow<List<String>>
    fun observeSlots(doctorId: String, dateLabel: String): Flow<List<Slot>>
    suspend fun getNextAvailableSlot(doctorId: String): Slot?

    fun observePatients(): Flow<List<Patient>>
    fun observePatient(patientId: String): Flow<Patient?>
    suspend fun addOrUpdatePatient(patient: Patient)
    suspend fun setPatientInvite(patientId: String, invited: Boolean)

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

    suspend fun markPaymentPaid(appointmentId: String): OperationResult
    suspend fun saveAppointmentReport(appointmentId: String, report: String)
    suspend fun setFollowUpRequested(appointmentId: String, requested: Boolean)
    suspend fun submitConsultingFeedback(appointmentId: String, consulting: Int, hospital: Int, waiting: Int)

    fun observeReminders(): Flow<List<ReminderItem>>
    fun observeDoctorNotices(): Flow<List<DoctorNotice>>
    suspend fun resolveDoctorNotice(noticeId: String)

    fun observeChat(threadType: ChatThreadType): Flow<List<ChatMessage>>
    suspend fun sendChatMessage(threadType: ChatThreadType, sender: ChatSender, content: String)

    fun observeSupportTickets(): Flow<List<SupportTicket>>
    suspend fun createSupportTicket(subject: String, message: String): OperationResult

    fun observeIvrPlans(): Flow<List<IvrPlan>>
    suspend fun upsertIvrPlan(plan: IvrPlan)
    suspend fun confirmIvrPlan(planId: String): BookingResult

    fun observeCollaborationState(): Flow<CollaborationState>
    suspend fun setCollaborationConnected(connected: Boolean)

    fun observeGoogleReviewState(): Flow<GoogleReviewState>
    suspend fun submitGoogleReview(rating: Int, comment: String): OperationResult
}

class SqliteSchedulaRepository(
    private val database: SchedulaDatabase,
    private val schedulingApi: SchedulingApi = LocalMockSchedulingApi(),
    private val billingApi: BillingApi = LocalMockBillingApi(),
    private val supportApi: SupportApi = LocalMockSupportApi(),
    private val reviewApi: ReviewApi = LocalMockReviewApi(),
) : SchedulaRepository {
    private val dao = database.dao()

    override suspend fun ensureSeeded() {
        if (dao.countDoctors() > 0) return
        database.runInTransaction {
            dao.insertDoctors(seedDoctors())
            dao.insertPatients(seedPatients())
            dao.insertSlots(seedSlots())
            dao.insertAppointments(seedAppointments())
            dao.insertChatMessages(seedChatMessages())
            dao.insertDoctorNotices(seedDoctorNotices())
            dao.insertIvrPlans(seedIvrPlans())
            dao.upsertCollaboration(CollaborationEntity(id = "default", connected = false, groupName = "New Mothers Group"))
            dao.upsertGoogleReview(
                GoogleReviewEntity(
                    id = "default",
                    requested = true,
                    submitted = false,
                    rating = null,
                    comment = "",
                    moderationReference = null,
                ),
            )
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
        return src.map { rows -> rows.map(DoctorEntity::toModel) }
    }

    override fun observeDoctor(doctorId: String): Flow<Doctor?> {
        return dao.observeDoctor(doctorId).map { it?.toModel() }
    }

    override fun observeDateOptions(doctorId: String): Flow<List<String>> {
        return dao.observeDateOptions(doctorId)
    }

    override fun observeSlots(doctorId: String, dateLabel: String): Flow<List<Slot>> {
        return dao.observeSlotsForDate(doctorId, dateLabel).map { rows -> rows.map(SlotEntity::toModel) }
    }

    override suspend fun getNextAvailableSlot(doctorId: String): Slot? {
        return dao.getNextAvailableSlot(doctorId)?.toModel()
    }

    override fun observePatients(): Flow<List<Patient>> {
        return dao.observePatients().map { rows -> rows.map(PatientEntity::toModel) }
    }

    override fun observePatient(patientId: String): Flow<Patient?> {
        return dao.observePatient(patientId).map { it?.toModel() }
    }

    override suspend fun addOrUpdatePatient(patient: Patient) {
        dao.insertPatient(patient.toEntity())
    }

    override suspend fun setPatientInvite(patientId: String, invited: Boolean) {
        val current = dao.getPatient(patientId) ?: return
        dao.updatePatient(current.copy(invited = invited))
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
        if (slot.type != appointmentType.name) {
            return BookingResult.Error("Selected slot does not support ${appointmentType.name.lowercase()} appointments.")
        }

        when (val availability = schedulingApi.checkSlotAvailability(doctorId = doctor.id, slotId = slot.id)) {
            is ApiResult.Error -> return BookingResult.Error(availability.message)
            is ApiResult.Success -> {
                if (!availability.data.available) {
                    return BookingResult.SlotUnavailable(availability.data.reason ?: "Slot is unavailable")
                }
            }
        }

        val confirmation = when (
            val response = schedulingApi.confirmBooking(
                doctorId = doctor.id,
                patientId = patient.id,
                slotId = slot.id,
                channel = channel,
            )
        ) {
            is ApiResult.Error -> return BookingResult.Error(response.message)
            is ApiResult.Success -> response.data
        }

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
                    paymentStatus = PaymentStatus.Unpaid.name,
                    confirmationCode = confirmation.confirmationCode,
                    paymentReference = null,
                    report = "",
                    followUpRequested = false,
                    consultingFeedback = null,
                    hospitalFeedback = null,
                    waitingTimeFeedback = null,
                    createdAtEpochMillis = System.currentTimeMillis(),
                ),
            )
            dao.insertChatMessage(
                ChatMessageEntity(
                    id = "chat-${System.currentTimeMillis()}",
                    threadType = ChatThreadType.Patient.name,
                    sender = ChatSender.System.name,
                    content = "Appointment confirmed for ${patient.name}. Confirmation: ${confirmation.confirmationCode}.",
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
        if (newSlot.type != current.type) {
            return BookingResult.Error("Please select a ${current.type.lowercase()} slot for this appointment.")
        }

        when (val availability = schedulingApi.checkSlotAvailability(current.doctorId, newSlot.id)) {
            is ApiResult.Error -> return BookingResult.Error(availability.message)
            is ApiResult.Success -> {
                if (!availability.data.available) {
                    return BookingResult.SlotUnavailable(availability.data.reason ?: "Selected slot is not available")
                }
            }
        }
        val confirmation = when (val response = schedulingApi.confirmReschedule(appointmentId, newSlot.id)) {
            is ApiResult.Error -> return BookingResult.Error(response.message)
            is ApiResult.Success -> response.data
        }

        database.runInTransaction {
            dao.setSlotBooked(current.slotId, false)
            dao.setSlotBooked(newSlot.id, true)
            dao.updateAppointment(
                current.copy(
                    slotId = newSlot.id,
                    status = AppointmentStatus.Rescheduled.name,
                    confirmationCode = confirmation.confirmationCode,
                    createdAtEpochMillis = System.currentTimeMillis(),
                ),
            )
        }

        val updated = dao.getAppointmentRecord(appointmentId)?.toModel()
            ?: return BookingResult.Error("Unable to reload appointment")
        return BookingResult.Success(updated)
    }

    override suspend fun markPaymentPaid(appointmentId: String): OperationResult {
        val current = dao.getAppointment(appointmentId) ?: return OperationResult.Error("Appointment not found")
        val appointmentRecord = dao.getAppointmentRecord(appointmentId)
            ?: return OperationResult.Error("Appointment data not found")
        if (current.paymentStatus == PaymentStatus.Paid.name) {
            return OperationResult.Success("Payment already completed")
        }

        val payment = when (
            val response = billingApi.capturePayment(
                appointmentId = appointmentId,
                amount = appointmentRecord.doctor.consultationFee,
            )
        ) {
            is ApiResult.Error -> return OperationResult.Error(response.message)
            is ApiResult.Success -> response.data
        }

        dao.updateAppointment(
            current.copy(
                paymentStatus = PaymentStatus.Paid.name,
                paymentReference = payment.paymentReference,
                createdAtEpochMillis = System.currentTimeMillis(),
            ),
        )
        return OperationResult.Success("Payment confirmed (${payment.paymentReference})")
    }

    override suspend fun saveAppointmentReport(appointmentId: String, report: String) {
        val current = dao.getAppointment(appointmentId) ?: return
        dao.updateAppointment(current.copy(report = report, createdAtEpochMillis = System.currentTimeMillis()))
    }

    override suspend fun setFollowUpRequested(appointmentId: String, requested: Boolean) {
        val current = dao.getAppointment(appointmentId) ?: return
        dao.updateAppointment(current.copy(followUpRequested = requested, createdAtEpochMillis = System.currentTimeMillis()))
    }

    override suspend fun submitConsultingFeedback(appointmentId: String, consulting: Int, hospital: Int, waiting: Int) {
        val current = dao.getAppointment(appointmentId) ?: return
        dao.updateAppointment(
            current.copy(
                consultingFeedback = consulting,
                hospitalFeedback = hospital,
                waitingTimeFeedback = waiting,
                createdAtEpochMillis = System.currentTimeMillis(),
            ),
        )
    }

    override fun observeReminders(): Flow<List<ReminderItem>> {
        return combine(observeAppointments(), observeDoctorNotices()) { appts, notices ->
            val appointmentReminders = appts
                .filter { it.status == AppointmentStatus.Scheduled || it.status == AppointmentStatus.Rescheduled }
                .mapIndexed { index, appt ->
                    ReminderItem(
                        id = "reminder-appt-${appt.id}",
                        message = "#${index + 1}. You have an appointment with ${appt.doctorName} at ${appt.timeLabel} for ${appt.patientName}",
                        appointmentId = appt.id,
                    )
                }

            val noticeReminders = notices.map { notice ->
                ReminderItem(
                    id = "reminder-notice-${notice.id}",
                    message = notice.message,
                    appointmentId = notice.appointmentId,
                )
            }

            appointmentReminders + noticeReminders
        }
    }

    override fun observeDoctorNotices(): Flow<List<DoctorNotice>> {
        return dao.observeDoctorNotices().map { rows -> rows.map(DoctorNoticeEntity::toModel) }
    }

    override suspend fun resolveDoctorNotice(noticeId: String) {
        val current = dao.getDoctorNotice(noticeId) ?: return
        dao.updateDoctorNotice(current.copy(resolved = true))
    }

    override fun observeChat(threadType: ChatThreadType): Flow<List<ChatMessage>> {
        return dao.observeChatMessages(threadType.name).map { rows -> rows.map(ChatMessageEntity::toModel) }
    }

    override suspend fun sendChatMessage(threadType: ChatThreadType, sender: ChatSender, content: String) {
        if (content.isBlank()) return
        dao.insertChatMessage(
            ChatMessageEntity(
                id = "chat-${System.currentTimeMillis()}",
                threadType = threadType.name,
                sender = sender.name,
                content = content.trim(),
                createdAtEpochMillis = System.currentTimeMillis(),
            ),
        )
    }

    override fun observeSupportTickets(): Flow<List<SupportTicket>> {
        return dao.observeSupportTickets().map { rows -> rows.map(SupportTicketEntity::toModel) }
    }

    override suspend fun createSupportTicket(subject: String, message: String): OperationResult {
        if (subject.isBlank() || message.isBlank()) return OperationResult.Error("Subject and message are required")
        val ack = when (val response = supportApi.acknowledgeTicket(subject.trim(), message.trim())) {
            is ApiResult.Error -> return OperationResult.Error(response.message)
            is ApiResult.Success -> response.data
        }

        dao.insertSupportTicket(
            SupportTicketEntity(
                id = "ticket-${System.currentTimeMillis()}",
                subject = subject.trim(),
                message = message.trim(),
                status = SupportTicketStatus.Open.name,
                externalReference = ack.externalReference,
                estimatedResolutionHours = ack.estimatedResolutionHours,
                createdAtEpochMillis = System.currentTimeMillis(),
            ),
        )
        sendChatMessage(
            ChatThreadType.Support,
            ChatSender.Support,
            "Support ticket created: ${subject.trim()} (${ack.externalReference}), ETA ${ack.estimatedResolutionHours}h",
        )
        return OperationResult.Success("Support ticket created (${ack.externalReference})")
    }

    override fun observeIvrPlans(): Flow<List<IvrPlan>> {
        return dao.observeIvrPlans().map { rows -> rows.map(IvrPlanEntity::toModel) }
    }

    override suspend fun upsertIvrPlan(plan: IvrPlan) {
        dao.upsertIvrPlan(plan.toEntity())
    }

    override suspend fun confirmIvrPlan(planId: String): BookingResult {
        val plan = dao.getIvrPlan(planId) ?: return BookingResult.Error("IVR plan not found")
        val slot = dao.getSlot(plan.slotId) ?: return BookingResult.Error("IVR slot not found")
        val confirmed = plan.copy(paymentConfirmed = true, status = IvrPlanStatus.Confirmed.name)
        dao.upsertIvrPlan(confirmed)
        val booking = bookAppointment(
            doctorId = confirmed.doctorId,
            patientId = confirmed.patientId,
            slotId = confirmed.slotId,
            appointmentType = AppointmentType.valueOf(slot.type),
            channel = "IVR",
        )
        if (booking is BookingResult.Success) {
            dao.upsertIvrPlan(
                confirmed.copy(
                    status = IvrPlanStatus.Converted.name,
                    convertedAppointmentId = booking.appointment.id,
                ),
            )
        }
        return booking
    }

    override fun observeCollaborationState(): Flow<CollaborationState> {
        return dao.observeCollaboration().map { row ->
            (row ?: CollaborationEntity(id = "default", connected = false, groupName = "New Mothers Group")).toModel()
        }
    }

    override suspend fun setCollaborationConnected(connected: Boolean) {
        dao.upsertCollaboration(CollaborationEntity(id = "default", connected = connected, groupName = "New Mothers Group"))
    }

    override fun observeGoogleReviewState(): Flow<GoogleReviewState> {
        return dao.observeGoogleReview().map { row ->
            (row ?: GoogleReviewEntity(
                id = "default",
                requested = true,
                submitted = false,
                rating = null,
                comment = "",
                moderationReference = null,
            )).toModel()
        }
    }

    override suspend fun submitGoogleReview(rating: Int, comment: String): OperationResult {
        val reviewAck = when (val response = reviewApi.submitReview(rating, comment)) {
            is ApiResult.Error -> return OperationResult.Error(response.message)
            is ApiResult.Success -> response.data
        }
        dao.upsertGoogleReview(
            GoogleReviewEntity(
                id = "default",
                requested = true,
                submitted = true,
                rating = rating,
                comment = comment,
                moderationReference = reviewAck.moderationReference,
            ),
        )
        return OperationResult.Success("Review submitted (${reviewAck.moderationReference})")
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
                invited = true,
            ),
            PatientEntity(
                id = "patient-meena",
                name = "Meena",
                age = 26,
                sex = "Female",
                relation = "Wife",
                weightKg = 58,
                complaint = "Stomach pain",
                invited = false,
            ),
            PatientEntity(
                id = "patient-kishore",
                name = "Kishore",
                age = 21,
                sex = "Male",
                relation = "Son",
                weightKg = 62,
                complaint = "Fever follow-up",
                invited = false,
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
                paymentStatus = PaymentStatus.Unpaid.name,
                confirmationCode = "CNF-SEED-0001",
                paymentReference = null,
                report = "",
                followUpRequested = false,
                consultingFeedback = null,
                hospitalFeedback = null,
                waitingTimeFeedback = null,
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
                paymentStatus = PaymentStatus.Paid.name,
                confirmationCode = "CNF-SEED-0002",
                paymentReference = "PAY-SEED-0002",
                report = "Recovered well",
                followUpRequested = false,
                consultingFeedback = 5,
                hospitalFeedback = 4,
                waitingTimeFeedback = 4,
                createdAtEpochMillis = System.currentTimeMillis() - 86_400_000,
            ),
        )
    }

    private fun seedChatMessages(): List<ChatMessageEntity> {
        return listOf(
            ChatMessageEntity(
                id = "chat-seed-patient-1",
                threadType = ChatThreadType.Patient.name,
                sender = ChatSender.Doctor.name,
                content = "I'm sorry to hear you're not feeling well. Please use warm compress and rest.",
                createdAtEpochMillis = System.currentTimeMillis() - 50_000,
            ),
            ChatMessageEntity(
                id = "chat-seed-reengagement-1",
                threadType = ChatThreadType.Reengagement.name,
                sender = ChatSender.System.name,
                content = "You had an appointment yesterday. How are you feeling today?",
                createdAtEpochMillis = System.currentTimeMillis() - 25_000,
            ),
            ChatMessageEntity(
                id = "chat-seed-support-1",
                threadType = ChatThreadType.Support.name,
                sender = ChatSender.Support.name,
                content = "Support is available 24x7. Share your issue and we will help.",
                createdAtEpochMillis = System.currentTimeMillis() - 20_000,
            ),
            ChatMessageEntity(
                id = "chat-seed-copatient-1",
                threadType = ChatThreadType.Copatient.name,
                sender = ChatSender.System.name,
                content = "Would you like to connect with other new mothers visiting this doctor?",
                createdAtEpochMillis = System.currentTimeMillis() - 15_000,
            ),
        )
    }

    private fun seedDoctorNotices(): List<DoctorNoticeEntity> {
        return listOf(
            DoctorNoticeEntity(
                id = "notice-1",
                appointmentId = "appointment-seed-1",
                message = "Appointment was rescheduled by clinic. Please choose another slot.",
                suggestedSlotId = "doctor-lavangi-Tomorrow-1",
                resolved = false,
            ),
        )
    }

    private fun seedIvrPlans(): List<IvrPlanEntity> {
        return listOf(
            IvrPlanEntity(
                id = "ivr-plan-1",
                doctorId = "doctor-lavangi",
                patientId = "patient-self",
                ivrAppId = "IVR-APP-1001",
                dateLabel = "Tomorrow",
                slotId = "doctor-lavangi-Tomorrow-2",
                paymentConfirmed = false,
                status = IvrPlanStatus.Planned.name,
                convertedAppointmentId = null,
            ),
        )
    }
}

private fun DoctorEntity.toModel(): Doctor {
    return Doctor(id, name, specialty, hospital, rating, yearsOfExperience, consultationFee, bio, availabilitySummary)
}

private fun PatientEntity.toModel(): Patient {
    return Patient(id, name, age, sex, relation, weightKg, complaint, invited)
}

private fun Patient.toEntity(): PatientEntity {
    return PatientEntity(id, name, age, sex, relation, weightKg, complaint, invited)
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
        paymentStatus = PaymentStatus.valueOf(appointment.paymentStatus),
        report = appointment.report,
        followUpRequested = appointment.followUpRequested,
        consultingFeedback = appointment.consultingFeedback,
        hospitalFeedback = appointment.hospitalFeedback,
        waitingTimeFeedback = appointment.waitingTimeFeedback,
        confirmationCode = appointment.confirmationCode,
        paymentReference = appointment.paymentReference,
    )
}

private fun ChatMessageEntity.toModel(): ChatMessage {
    return ChatMessage(
        id = id,
        threadType = ChatThreadType.valueOf(threadType),
        sender = ChatSender.valueOf(sender),
        content = content,
        createdAtEpochMillis = createdAtEpochMillis,
    )
}

private fun SupportTicketEntity.toModel(): SupportTicket {
    return SupportTicket(
        id = id,
        subject = subject,
        message = message,
        status = SupportTicketStatus.valueOf(status),
        createdAtEpochMillis = createdAtEpochMillis,
        externalReference = externalReference,
        estimatedResolutionHours = estimatedResolutionHours,
    )
}

private fun DoctorNoticeEntity.toModel(): DoctorNotice {
    return DoctorNotice(
        id = id,
        appointmentId = appointmentId,
        message = message,
        suggestedSlotId = suggestedSlotId,
        resolved = resolved,
    )
}

private fun IvrPlanEntity.toModel(): IvrPlan {
    return IvrPlan(
        id = id,
        doctorId = doctorId,
        patientId = patientId,
        ivrAppId = ivrAppId,
        dateLabel = dateLabel,
        slotId = slotId,
        paymentConfirmed = paymentConfirmed,
        status = IvrPlanStatus.valueOf(status),
        convertedAppointmentId = convertedAppointmentId,
    )
}

private fun IvrPlan.toEntity(): IvrPlanEntity {
    return IvrPlanEntity(
        id = id,
        doctorId = doctorId,
        patientId = patientId,
        ivrAppId = ivrAppId,
        dateLabel = dateLabel,
        slotId = slotId,
        paymentConfirmed = paymentConfirmed,
        status = status.name,
        convertedAppointmentId = convertedAppointmentId,
    )
}

private fun CollaborationEntity.toModel(): CollaborationState {
    return CollaborationState(connected = connected, groupName = groupName)
}

private fun GoogleReviewEntity.toModel(): GoogleReviewState {
    return GoogleReviewState(
        requested = requested,
        submitted = submitted,
        rating = rating,
        comment = comment,
        moderationReference = moderationReference,
    )
}
