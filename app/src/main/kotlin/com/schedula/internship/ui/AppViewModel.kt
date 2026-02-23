package com.schedula.internship.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.schedula.internship.data.BookingResult
import com.schedula.internship.data.LocalStoreProvider
import com.schedula.internship.data.OperationResult
import com.schedula.internship.data.SchedulaRepository
import com.schedula.internship.data.SqliteSchedulaRepository
import com.schedula.internship.domain.AuthRules
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
import com.schedula.internship.model.ReminderItem
import com.schedula.internship.model.Slot
import com.schedula.internship.model.SupportTicket
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ThemeMode {
    System,
    Light,
    Dark,
}

enum class BottomTab {
    FindDoctor,
    MyRecords,
    MyAppt,
    Profile,
}

enum class Screen {
    Login,
    DoctorSearch,
    DoctorProfile,
    BookingDate,
    BookingTime,
    BookingConfirmation,
    BookingFailed,
    AddPatientDetails,
    PatientDetails,
    PatientChat,
    MyAppointments,
    AppointmentDetails,
    AppointmentCancel,
    AppointmentReschedule,
    ConsultingFeedback,
    Reminders,
    RescheduleByDoctor,
    Reengagement,
    SeamlessAppointment,
    CopatientCollab,
    Support,
    FriendsFamily,
    GoogleReview,
}

data class AppUiState(
    val isReady: Boolean = false,
    val isLoggedIn: Boolean = false,
    val phoneNumber: String = "",
    val otp: String = "",
    val otpSent: Boolean = false,
    val authError: String? = null,
    val activeTab: BottomTab = BottomTab.FindDoctor,
    val activeScreen: Screen = Screen.Login,
    val searchQuery: String = "",
    val selectedDoctorId: String? = null,
    val selectedDateLabel: String = "Today",
    val selectedSlotId: String? = null,
    val selectedPatientId: String = "patient-self",
    val selectedAppointmentId: String? = null,
    val selectedNoticeId: String? = null,
    val bookingType: AppointmentType = AppointmentType.Regular,
    val themeMode: ThemeMode = ThemeMode.System,
    val dynamicColorEnabled: Boolean = true,
    val statusMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SchedulaRepository = SqliteSchedulaRepository(
        LocalStoreProvider.getDatabase(application),
    )

    private val searchQueryFlow = MutableStateFlow("")
    private val selectedDoctorIdFlow = MutableStateFlow<String?>(null)
    private val selectedDateFlow = MutableStateFlow("Today")
    private val selectedPatientIdFlow = MutableStateFlow("patient-self")
    private val selectedAppointmentIdFlow = MutableStateFlow<String?>(null)

    val doctors: StateFlow<List<Doctor>> = searchQueryFlow
        .flatMapLatest(repository::observeDoctors)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedDoctor: StateFlow<Doctor?> = selectedDoctorIdFlow
        .flatMapLatest { id -> if (id == null) flowOf(null) else repository.observeDoctor(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val dateOptions: StateFlow<List<String>> = selectedDoctorIdFlow
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repository.observeDateOptions(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val slots: StateFlow<List<Slot>> = combine(selectedDoctorIdFlow, selectedDateFlow) { doctorId, date ->
        doctorId to date
    }.flatMapLatest { (doctorId, dateLabel) ->
        if (doctorId == null) flowOf(emptyList()) else repository.observeSlots(doctorId, dateLabel)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val patients: StateFlow<List<Patient>> = repository.observePatients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedPatient: StateFlow<Patient?> = selectedPatientIdFlow
        .flatMapLatest(repository::observePatient)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val appointments: StateFlow<List<Appointment>> = repository.observeAppointments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedAppointment: StateFlow<Appointment?> = selectedAppointmentIdFlow
        .flatMapLatest { id -> if (id == null) flowOf(null) else repository.observeAppointment(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val reminders: StateFlow<List<ReminderItem>> = repository.observeReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val doctorNotices: StateFlow<List<DoctorNotice>> = repository.observeDoctorNotices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val patientChat: StateFlow<List<ChatMessage>> = repository.observeChat(ChatThreadType.Patient)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val reengagementChat: StateFlow<List<ChatMessage>> = repository.observeChat(ChatThreadType.Reengagement)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val supportChat: StateFlow<List<ChatMessage>> = repository.observeChat(ChatThreadType.Support)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val copatientChat: StateFlow<List<ChatMessage>> = repository.observeChat(ChatThreadType.Copatient)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val supportTickets: StateFlow<List<SupportTicket>> = repository.observeSupportTickets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val ivrPlans: StateFlow<List<IvrPlan>> = repository.observeIvrPlans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val collaborationState: StateFlow<CollaborationState> = repository.observeCollaborationState()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            CollaborationState(connected = false, groupName = "New Mothers Group"),
        )

    val googleReviewState: StateFlow<GoogleReviewState> = repository.observeGoogleReviewState()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            GoogleReviewState(requested = true, submitted = false, rating = null, comment = "", moderationReference = null),
        )

    var uiState by mutableStateOf(AppUiState())
        private set

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            repository.observeLoggedInPhone().collect { phone ->
                val loggedIn = !phone.isNullOrBlank()
                uiState = uiState.copy(
                    isReady = true,
                    isLoggedIn = loggedIn,
                    activeScreen = if (loggedIn) Screen.DoctorSearch else Screen.Login,
                )
            }
        }
    }

    fun updatePhoneNumber(value: String) {
        uiState = uiState.copy(phoneNumber = value, authError = null)
    }

    fun updateOtp(value: String) {
        uiState = uiState.copy(otp = value, authError = null)
    }

    fun sendOtp() {
        if (!AuthRules.isValidPhone(uiState.phoneNumber)) {
            uiState = uiState.copy(authError = "Enter a valid 10-digit phone number")
            return
        }
        uiState = uiState.copy(otpSent = true, authError = null, statusMessage = "OTP sent. Use 1234")
    }

    fun verifyOtp() {
        if (!AuthRules.isValidOtp(uiState.otp)) {
            uiState = uiState.copy(authError = "Invalid OTP")
            return
        }
        viewModelScope.launch {
            repository.setLoggedInPhone(uiState.phoneNumber)
            uiState = uiState.copy(
                isLoggedIn = true,
                otpSent = false,
                phoneNumber = "",
                otp = "",
                authError = null,
                activeScreen = Screen.DoctorSearch,
                activeTab = BottomTab.FindDoctor,
                statusMessage = "Welcome to Schedula",
            )
        }
    }

    fun updateSearchQuery(value: String) {
        uiState = uiState.copy(searchQuery = value)
        searchQueryFlow.value = value
    }

    fun openDoctorProfile(doctorId: String) {
        selectedDoctorIdFlow.value = doctorId
        selectedDateFlow.value = "Today"
        uiState = uiState.copy(
            selectedDoctorId = doctorId,
            selectedDateLabel = "Today",
            selectedSlotId = null,
            activeScreen = Screen.DoctorProfile,
            activeTab = BottomTab.FindDoctor,
        )
    }

    fun openBookingDate() {
        uiState = uiState.copy(activeScreen = Screen.BookingDate)
    }

    fun selectBookingType(type: AppointmentType) {
        uiState = uiState.copy(bookingType = type)
    }

    fun selectDate(dateLabel: String) {
        selectedDateFlow.value = dateLabel
        uiState = uiState.copy(selectedDateLabel = dateLabel, selectedSlotId = null)
    }

    fun openBookingTime() {
        uiState = uiState.copy(activeScreen = Screen.BookingTime)
    }

    fun selectSlot(slotId: String) {
        uiState = uiState.copy(selectedSlotId = slotId)
    }

    fun selectNextAvailableSlot() {
        val doctorId = uiState.selectedDoctorId ?: return
        viewModelScope.launch {
            val next = repository.getNextAvailableSlot(doctorId)
            if (next == null) {
                uiState = uiState.copy(statusMessage = "No available slots")
            } else {
                selectedDateFlow.value = next.dateLabel
                uiState = uiState.copy(
                    selectedDateLabel = next.dateLabel,
                    selectedSlotId = next.id,
                    statusMessage = "Selected next available slot",
                )
            }
        }
    }

    fun openAddPatientDetails() {
        uiState = uiState.copy(activeScreen = Screen.AddPatientDetails)
    }

    fun selectPatient(patientId: String) {
        selectedPatientIdFlow.value = patientId
        uiState = uiState.copy(selectedPatientId = patientId)
    }

    fun savePatient(
        name: String,
        age: Int,
        sex: String,
        relation: String,
        weightKg: Int,
        complaint: String,
    ) {
        val normalized = name.trim().lowercase().replace(" ", "-")
        val patientId = if (normalized.isBlank()) "patient-${System.currentTimeMillis()}" else "patient-$normalized"
        viewModelScope.launch {
            repository.addOrUpdatePatient(
                Patient(
                    id = patientId,
                    name = name.ifBlank { "Unknown" },
                    age = age,
                    sex = sex,
                    relation = relation,
                    weightKg = weightKg,
                    complaint = complaint,
                    invited = false,
                ),
            )
            selectPatient(patientId)
            uiState = uiState.copy(activeScreen = Screen.PatientDetails, statusMessage = "Patient details saved")
        }
    }

    fun togglePatientInvite(patientId: String, invited: Boolean) {
        viewModelScope.launch {
            repository.setPatientInvite(patientId, invited)
            uiState = uiState.copy(statusMessage = if (invited) "Invite sent" else "Invite removed")
        }
    }

    fun bookAppointment() {
        val doctorId = uiState.selectedDoctorId ?: return
        val slotId = uiState.selectedSlotId ?: run {
            uiState = uiState.copy(statusMessage = "Please select a slot")
            return
        }

        viewModelScope.launch {
            when (
                val result = repository.bookAppointment(
                    doctorId = doctorId,
                    patientId = uiState.selectedPatientId,
                    slotId = slotId,
                    appointmentType = uiState.bookingType,
                    channel = "APP",
                )
            ) {
                is BookingResult.Success -> {
                    selectedAppointmentIdFlow.value = result.appointment.id
                    uiState = uiState.copy(
                        selectedAppointmentId = result.appointment.id,
                        activeScreen = Screen.BookingConfirmation,
                        statusMessage = "Appointment confirmed",
                    )
                }

                is BookingResult.SlotUnavailable -> {
                    uiState = uiState.copy(activeScreen = Screen.BookingFailed, statusMessage = result.message)
                }

                is BookingResult.Error -> {
                    uiState = uiState.copy(activeScreen = Screen.BookingFailed, statusMessage = result.message)
                }
            }
        }
    }

    fun bookUsingNextAvailableSlot() {
        val doctorId = uiState.selectedDoctorId ?: return
        viewModelScope.launch {
            val next = repository.getNextAvailableSlot(doctorId)
            if (next == null) {
                uiState = uiState.copy(statusMessage = "No next available slot")
                return@launch
            }
            selectedDateFlow.value = next.dateLabel
            uiState = uiState.copy(selectedDateLabel = next.dateLabel, selectedSlotId = next.id)
            bookAppointment()
        }
    }

    fun openPatientDetails() {
        uiState = uiState.copy(activeScreen = Screen.PatientDetails)
    }

    fun openPatientChat() {
        uiState = uiState.copy(activeScreen = Screen.PatientChat)
    }

    fun openMyAppointments() {
        uiState = uiState.copy(activeScreen = Screen.MyAppointments, activeTab = BottomTab.MyAppt)
    }

    fun openAppointmentDetails(appointmentId: String) {
        appointments.value.firstOrNull { it.id == appointmentId }?.let { appointment ->
            selectedDoctorIdFlow.value = appointment.doctorId
            selectedDateFlow.value = appointment.dateLabel
            uiState = uiState.copy(
                selectedDoctorId = appointment.doctorId,
                selectedDateLabel = appointment.dateLabel,
                selectedSlotId = appointment.slotId,
            )
        }
        selectedAppointmentIdFlow.value = appointmentId
        uiState = uiState.copy(selectedAppointmentId = appointmentId, activeScreen = Screen.AppointmentDetails)
    }

    fun openCancelAppointment() {
        uiState = uiState.copy(activeScreen = Screen.AppointmentCancel)
    }

    fun confirmCancelAppointment() {
        val appointmentId = uiState.selectedAppointmentId ?: return
        viewModelScope.launch {
            repository.cancelAppointment(appointmentId)
            uiState = uiState.copy(activeScreen = Screen.MyAppointments, statusMessage = "Appointment canceled")
        }
    }

    fun openRescheduleAppointment() {
        selectedAppointment.value?.let { appointment ->
            selectedDoctorIdFlow.value = appointment.doctorId
            selectedDateFlow.value = appointment.dateLabel
            uiState = uiState.copy(
                selectedDoctorId = appointment.doctorId,
                selectedDateLabel = appointment.dateLabel,
            )
        }
        uiState = uiState.copy(activeScreen = Screen.AppointmentReschedule)
    }

    fun confirmReschedule(slotId: String) {
        val appointmentId = uiState.selectedAppointmentId ?: return
        viewModelScope.launch {
            when (val result = repository.rescheduleAppointment(appointmentId, slotId)) {
                is BookingResult.Success -> {
                    selectedAppointmentIdFlow.value = result.appointment.id
                    uiState = uiState.copy(activeScreen = Screen.AppointmentDetails, statusMessage = "Appointment rescheduled")
                }

                is BookingResult.SlotUnavailable -> {
                    uiState = uiState.copy(activeScreen = Screen.BookingFailed, statusMessage = result.message)
                }

                is BookingResult.Error -> {
                    uiState = uiState.copy(activeScreen = Screen.BookingFailed, statusMessage = result.message)
                }
            }
        }
    }

    fun markPaymentPaid() {
        val appointmentId = uiState.selectedAppointmentId ?: return
        viewModelScope.launch {
            when (val result = repository.markPaymentPaid(appointmentId)) {
                is OperationResult.Success -> uiState = uiState.copy(statusMessage = result.message)
                is OperationResult.Error -> uiState = uiState.copy(statusMessage = result.message)
            }
        }
    }

    fun saveReport(report: String) {
        val appointmentId = uiState.selectedAppointmentId ?: return
        viewModelScope.launch {
            repository.saveAppointmentReport(appointmentId, report)
            uiState = uiState.copy(statusMessage = "Report saved")
        }
    }

    fun toggleFollowUp(requested: Boolean) {
        val appointmentId = uiState.selectedAppointmentId ?: return
        viewModelScope.launch {
            repository.setFollowUpRequested(appointmentId, requested)
            uiState = uiState.copy(statusMessage = if (requested) "Follow-up requested" else "Follow-up removed")
        }
    }

    fun openConsultingFeedback() {
        uiState = uiState.copy(activeScreen = Screen.ConsultingFeedback)
    }

    fun submitFeedback(consulting: Int, hospital: Int, waiting: Int) {
        val appointmentId = uiState.selectedAppointmentId ?: return
        viewModelScope.launch {
            repository.submitConsultingFeedback(appointmentId, consulting, hospital, waiting)
            uiState = uiState.copy(activeScreen = Screen.AppointmentDetails, statusMessage = "Feedback submitted")
        }
    }

    fun openReminders() {
        uiState = uiState.copy(activeScreen = Screen.Reminders)
    }

    fun openRescheduleByDoctor() {
        uiState = uiState.copy(activeScreen = Screen.RescheduleByDoctor)
    }

    fun applyNoticeReschedule(noticeId: String) {
        val notice = doctorNotices.value.firstOrNull { it.id == noticeId } ?: return
        val appointmentId = notice.appointmentId
        val suggestedSlot = notice.suggestedSlotId
        viewModelScope.launch {
            if (suggestedSlot != null) {
                when (val result = repository.rescheduleAppointment(appointmentId, suggestedSlot)) {
                    is BookingResult.Success -> {
                        repository.resolveDoctorNotice(noticeId)
                        selectedAppointmentIdFlow.value = appointmentId
                        uiState = uiState.copy(
                            selectedAppointmentId = appointmentId,
                            activeScreen = Screen.AppointmentDetails,
                            statusMessage = "Doctor notice applied",
                        )
                    }

                    is BookingResult.SlotUnavailable -> uiState = uiState.copy(statusMessage = result.message)
                    is BookingResult.Error -> uiState = uiState.copy(statusMessage = result.message)
                }
            }
        }
    }

    fun dismissNotice(noticeId: String) {
        viewModelScope.launch {
            repository.resolveDoctorNotice(noticeId)
            uiState = uiState.copy(statusMessage = "Notice dismissed")
        }
    }

    fun openReengagement() {
        uiState = uiState.copy(activeScreen = Screen.Reengagement)
    }

    fun sendReengagementMessage(content: String) {
        viewModelScope.launch {
            repository.sendChatMessage(ChatThreadType.Reengagement, ChatSender.User, content)
        }
    }

    fun openSeamlessAppointment() {
        uiState = uiState.copy(activeScreen = Screen.SeamlessAppointment)
    }

    fun saveIvrPlan(ivrAppId: String) {
        val doctorId = uiState.selectedDoctorId ?: return
        val slotId = uiState.selectedSlotId ?: return
        if (ivrAppId.isBlank()) {
            uiState = uiState.copy(statusMessage = "IVR App ID is required")
            return
        }
        val plan = IvrPlan(
            id = "ivr-${System.currentTimeMillis()}",
            doctorId = doctorId,
            patientId = uiState.selectedPatientId,
            ivrAppId = ivrAppId.trim(),
            dateLabel = uiState.selectedDateLabel,
            slotId = slotId,
            paymentConfirmed = false,
            status = IvrPlanStatus.Planned,
            convertedAppointmentId = null,
        )
        viewModelScope.launch {
            repository.upsertIvrPlan(plan)
            uiState = uiState.copy(statusMessage = "IVR plan saved")
        }
    }

    fun confirmIvrPlan(planId: String) {
        viewModelScope.launch {
            when (val result = repository.confirmIvrPlan(planId)) {
                is BookingResult.Success -> {
                    selectedAppointmentIdFlow.value = result.appointment.id
                    uiState = uiState.copy(
                        selectedAppointmentId = result.appointment.id,
                        activeScreen = Screen.BookingConfirmation,
                        statusMessage = "IVR appointment confirmed",
                    )
                }

                is BookingResult.SlotUnavailable -> uiState = uiState.copy(statusMessage = result.message)
                is BookingResult.Error -> uiState = uiState.copy(statusMessage = result.message)
            }
        }
    }

    fun openCopatientCollab() {
        uiState = uiState.copy(activeScreen = Screen.CopatientCollab)
    }

    fun setCollaborationConnected(connected: Boolean) {
        viewModelScope.launch {
            repository.setCollaborationConnected(connected)
            uiState = uiState.copy(statusMessage = if (connected) "Collaboration enabled" else "Collaboration disabled")
        }
    }

    fun sendCopatientMessage(content: String) {
        viewModelScope.launch {
            repository.sendChatMessage(ChatThreadType.Copatient, ChatSender.User, content)
        }
    }

    fun openSupport() {
        uiState = uiState.copy(activeScreen = Screen.Support, activeTab = BottomTab.Profile)
    }

    fun createSupportTicket(subject: String, message: String) {
        viewModelScope.launch {
            when (val result = repository.createSupportTicket(subject, message)) {
                is OperationResult.Success -> uiState = uiState.copy(statusMessage = result.message)
                is OperationResult.Error -> uiState = uiState.copy(statusMessage = result.message)
            }
        }
    }

    fun sendSupportMessage(content: String) {
        viewModelScope.launch {
            repository.sendChatMessage(ChatThreadType.Support, ChatSender.User, content)
        }
    }

    fun openFriendsAndFamily() {
        uiState = uiState.copy(activeScreen = Screen.FriendsFamily, activeTab = BottomTab.MyRecords)
    }

    fun openGoogleReview() {
        uiState = uiState.copy(activeScreen = Screen.GoogleReview)
    }

    fun submitGoogleReview(rating: Int, comment: String) {
        viewModelScope.launch {
            when (val result = repository.submitGoogleReview(rating, comment)) {
                is OperationResult.Success -> uiState = uiState.copy(statusMessage = result.message)
                is OperationResult.Error -> uiState = uiState.copy(statusMessage = result.message)
            }
        }
    }

    fun sendPatientChatMessage(content: String) {
        viewModelScope.launch {
            repository.sendChatMessage(ChatThreadType.Patient, ChatSender.User, content)
        }
    }

    fun onBottomTab(tab: BottomTab) {
        uiState = when (tab) {
            BottomTab.FindDoctor -> uiState.copy(activeTab = tab, activeScreen = Screen.DoctorSearch)
            BottomTab.MyRecords -> uiState.copy(activeTab = tab, activeScreen = Screen.FriendsFamily)
            BottomTab.MyAppt -> uiState.copy(activeTab = tab, activeScreen = Screen.MyAppointments)
            BottomTab.Profile -> uiState.copy(activeTab = tab, activeScreen = Screen.Support)
        }
    }

    fun backToDoctorSearch() {
        uiState = uiState.copy(activeScreen = Screen.DoctorSearch, activeTab = BottomTab.FindDoctor)
    }

    fun updateThemeMode(mode: ThemeMode) {
        uiState = uiState.copy(themeMode = mode)
    }

    fun updateDynamicColor(enabled: Boolean) {
        uiState = uiState.copy(dynamicColorEnabled = enabled)
    }

    fun logout() {
        viewModelScope.launch {
            repository.setLoggedInPhone(null)
            uiState = AppUiState(isReady = true)
        }
    }

    fun consumeStatusMessage() {
        if (uiState.statusMessage != null) uiState = uiState.copy(statusMessage = null)
    }

    fun upcomingAppointments(): List<Appointment> {
        return appointments.value.filter { it.status == AppointmentStatus.Scheduled || it.status == AppointmentStatus.Rescheduled }
    }

    fun pastAppointments(): List<Appointment> {
        return appointments.value.filter { it.status == AppointmentStatus.Completed || it.status == AppointmentStatus.Cancelled }
    }
}
