package com.schedula.internship.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.schedula.internship.data.BookingResult
import com.schedula.internship.data.LocalStoreProvider
import com.schedula.internship.data.SchedulaRepository
import com.schedula.internship.data.SqliteSchedulaRepository
import com.schedula.internship.domain.AuthRules
import com.schedula.internship.model.Appointment
import com.schedula.internship.model.AppointmentStatus
import com.schedula.internship.model.AppointmentType
import com.schedula.internship.model.Doctor
import com.schedula.internship.model.Patient
import com.schedula.internship.model.Slot
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
        val patientId = "patient-${name.lowercase().replace(" ", "-")}".ifBlank { "patient-self" }
        viewModelScope.launch {
            repository.addOrUpdatePatient(
                Patient(
                    id = patientId,
                    name = name,
                    age = age,
                    sex = sex,
                    relation = relation,
                    weightKg = weightKg,
                    complaint = complaint,
                ),
            )
            selectPatient(patientId)
            uiState = uiState.copy(activeScreen = Screen.PatientDetails, statusMessage = "Patient details saved")
        }
    }

    fun bookAppointment(appointmentType: AppointmentType = AppointmentType.Regular) {
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
                    appointmentType = appointmentType,
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

    fun openConsultingFeedback() {
        uiState = uiState.copy(activeScreen = Screen.ConsultingFeedback)
    }

    fun openReminders() {
        uiState = uiState.copy(activeScreen = Screen.Reminders)
    }

    fun openRescheduleByDoctor() {
        uiState = uiState.copy(activeScreen = Screen.RescheduleByDoctor)
    }

    fun openReengagement() {
        uiState = uiState.copy(activeScreen = Screen.Reengagement)
    }

    fun openSeamlessAppointment() {
        uiState = uiState.copy(activeScreen = Screen.SeamlessAppointment)
    }

    fun openCopatientCollab() {
        uiState = uiState.copy(activeScreen = Screen.CopatientCollab)
    }

    fun openSupport() {
        uiState = uiState.copy(activeScreen = Screen.Support)
    }

    fun openFriendsAndFamily() {
        uiState = uiState.copy(activeScreen = Screen.FriendsFamily, activeTab = BottomTab.MyRecords)
    }

    fun openGoogleReview() {
        uiState = uiState.copy(activeScreen = Screen.GoogleReview)
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
