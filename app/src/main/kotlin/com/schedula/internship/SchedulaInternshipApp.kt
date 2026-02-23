package com.schedula.internship

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.schedula.internship.model.Appointment
import com.schedula.internship.model.AppointmentStatus
import com.schedula.internship.model.AppointmentType
import com.schedula.internship.model.ChatMessage
import com.schedula.internship.model.ChatSender
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
import com.schedula.internship.ui.AppViewModel
import com.schedula.internship.ui.BottomTab
import com.schedula.internship.ui.Screen
import com.schedula.internship.ui.ThemeMode
import com.schedula.internship.ui.theme.SchedulaInternshipTheme
import com.schedula.internship.ui.theme.resolveDarkTheme

@Composable
fun SchedulaInternshipApp(
    viewModel: AppViewModel = viewModel(),
) {
    val doctors by viewModel.doctors.collectAsStateWithLifecycle()
    val patients by viewModel.patients.collectAsStateWithLifecycle()
    val slots by viewModel.slots.collectAsStateWithLifecycle()
    val dateOptions by viewModel.dateOptions.collectAsStateWithLifecycle()
    val selectedDoctor by viewModel.selectedDoctor.collectAsStateWithLifecycle()
    val selectedPatient by viewModel.selectedPatient.collectAsStateWithLifecycle()
    val selectedAppointment by viewModel.selectedAppointment.collectAsStateWithLifecycle()
    val appointments by viewModel.appointments.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val doctorNotices by viewModel.doctorNotices.collectAsStateWithLifecycle()
    val patientChat by viewModel.patientChat.collectAsStateWithLifecycle()
    val reengagementChat by viewModel.reengagementChat.collectAsStateWithLifecycle()
    val supportChat by viewModel.supportChat.collectAsStateWithLifecycle()
    val copatientChat by viewModel.copatientChat.collectAsStateWithLifecycle()
    val supportTickets by viewModel.supportTickets.collectAsStateWithLifecycle()
    val ivrPlans by viewModel.ivrPlans.collectAsStateWithLifecycle()
    val collaboration by viewModel.collaborationState.collectAsStateWithLifecycle()
    val reviewState by viewModel.googleReviewState.collectAsStateWithLifecycle()

    val uiState = viewModel.uiState

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeStatusMessage()
        }
    }

    val useDarkTheme = when (uiState.themeMode) {
        ThemeMode.System -> null
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    SchedulaInternshipTheme(
        darkTheme = resolveDarkTheme(useDarkTheme),
        dynamicColor = uiState.dynamicColorEnabled,
    ) {
        if (!uiState.isReady) {
            AppShell(snackbarHostState = snackbarHostState, uiState = uiState, onTab = viewModel::onBottomTab) {
                CenterMessage("Preparing local data...")
            }
            return@SchedulaInternshipTheme
        }

        if (!uiState.isLoggedIn || uiState.activeScreen == Screen.Login) {
            LoginScreen(
                phone = uiState.phoneNumber,
                otp = uiState.otp,
                otpSent = uiState.otpSent,
                error = uiState.authError,
                onPhoneChange = viewModel::updatePhoneNumber,
                onOtpChange = viewModel::updateOtp,
                onSendOtp = viewModel::sendOtp,
                onVerifyOtp = viewModel::verifyOtp,
            )
            return@SchedulaInternshipTheme
        }

        AppShell(
            snackbarHostState = snackbarHostState,
            uiState = uiState,
            onTab = viewModel::onBottomTab,
        ) {
            when (uiState.activeScreen) {
                Screen.Login -> Unit

                Screen.DoctorSearch -> DoctorSearchScreen(
                    query = uiState.searchQuery,
                    doctors = doctors,
                    onQueryChange = viewModel::updateSearchQuery,
                    onDoctorClick = viewModel::openDoctorProfile,
                    onReminders = viewModel::openReminders,
                    onSeamless = viewModel::openSeamlessAppointment,
                    onCopatient = viewModel::openCopatientCollab,
                )

                Screen.DoctorProfile -> DoctorProfileScreen(
                    doctor = selectedDoctor,
                    onBook = viewModel::openBookingDate,
                    onBack = viewModel::backToDoctorSearch,
                )

                Screen.BookingDate -> BookingDateScreen(
                    doctor = selectedDoctor,
                    dateOptions = dateOptions,
                    selectedDate = uiState.selectedDateLabel,
                    selectedType = uiState.bookingType,
                    onTypeChange = viewModel::selectBookingType,
                    onDateSelected = viewModel::selectDate,
                    onNext = viewModel::openBookingTime,
                    onBack = viewModel::backToDoctorSearch,
                )

                Screen.BookingTime -> BookingTimeScreen(
                    doctor = selectedDoctor,
                    slots = slots,
                    selectedSlotId = uiState.selectedSlotId,
                    onSlotClick = viewModel::selectSlot,
                    onNextAvailable = viewModel::selectNextAvailableSlot,
                    onNext = viewModel::openAddPatientDetails,
                    onBack = viewModel::openBookingDate,
                )

                Screen.AddPatientDetails -> AddPatientScreen(
                    patients = patients,
                    selectedPatient = uiState.selectedPatientId,
                    onPatientSelected = viewModel::selectPatient,
                    onSave = viewModel::savePatient,
                    onContinue = viewModel::bookAppointment,
                )

                Screen.BookingConfirmation -> BookingConfirmationScreen(
                    appointment = selectedAppointment,
                    onAddPatientDetails = viewModel::openPatientDetails,
                    onViewAppointments = viewModel::openMyAppointments,
                )

                Screen.BookingFailed -> BookingFailedScreen(
                    onRetry = viewModel::openBookingTime,
                    onBookNext = viewModel::bookUsingNextAvailableSlot,
                    onMyAppointments = viewModel::openMyAppointments,
                )

                Screen.PatientDetails -> PatientDetailsScreen(
                    appointment = selectedAppointment,
                    patient = selectedPatient,
                    onPay = viewModel::markPaymentPaid,
                    onSaveReport = viewModel::saveReport,
                    onFollowUp = viewModel::toggleFollowUp,
                    onChat = viewModel::openPatientChat,
                    onMyAppointments = viewModel::openMyAppointments,
                )

                Screen.PatientChat -> ChatScreen(
                    title = "Patient chat",
                    messages = patientChat,
                    onSend = { viewModel.sendPatientChatMessage(it) },
                    onBack = viewModel::openPatientDetails,
                )

                Screen.MyAppointments -> MyAppointmentsScreen(
                    appointments = appointments,
                    onOpenDetails = viewModel::openAppointmentDetails,
                )

                Screen.AppointmentDetails -> AppointmentDetailsScreen(
                    appointment = selectedAppointment,
                    onCancel = viewModel::openCancelAppointment,
                    onReschedule = viewModel::openRescheduleAppointment,
                    onFeedback = viewModel::openConsultingFeedback,
                )

                Screen.AppointmentCancel -> AppointmentCancelScreen(
                    appointment = selectedAppointment,
                    onConfirm = viewModel::confirmCancelAppointment,
                    onBack = { selectedAppointment?.id?.let(viewModel::openAppointmentDetails) },
                )

                Screen.AppointmentReschedule -> AppointmentRescheduleScreen(
                    slots = slots,
                    onConfirm = viewModel::confirmReschedule,
                    onBack = { selectedAppointment?.id?.let(viewModel::openAppointmentDetails) },
                )

                Screen.ConsultingFeedback -> ConsultingFeedbackScreen(
                    onDone = viewModel::submitFeedback,
                )

                Screen.Reminders -> RemindersScreen(
                    reminders = reminders,
                    onOpenAppointment = { id -> id?.let(viewModel::openAppointmentDetails) },
                    onBack = viewModel::backToDoctorSearch,
                    onRescheduleByDoctor = viewModel::openRescheduleByDoctor,
                )

                Screen.RescheduleByDoctor -> RescheduleByDoctorScreen(
                    notices = doctorNotices,
                    onApply = viewModel::applyNoticeReschedule,
                    onDismiss = viewModel::dismissNotice,
                    onBack = viewModel::openReminders,
                )

                Screen.Reengagement -> ChatScreen(
                    title = "Patient reengagement",
                    messages = reengagementChat,
                    onSend = viewModel::sendReengagementMessage,
                    onBack = viewModel::backToDoctorSearch,
                )

                Screen.SeamlessAppointment -> SeamlessAppointmentScreen(
                    doctors = doctors,
                    patients = patients,
                    dateOptions = dateOptions,
                    slots = slots,
                    selectedDoctorId = uiState.selectedDoctorId,
                    selectedPatientId = uiState.selectedPatientId,
                    selectedDate = uiState.selectedDateLabel,
                    selectedSlotId = uiState.selectedSlotId,
                    plans = ivrPlans,
                    onPickDoctor = viewModel::openDoctorProfile,
                    onPickPatient = viewModel::selectPatient,
                    onPickDate = viewModel::selectDate,
                    onPickSlot = viewModel::selectSlot,
                    onSavePlan = viewModel::saveIvrPlan,
                    onConfirmPlan = viewModel::confirmIvrPlan,
                    onBack = viewModel::backToDoctorSearch,
                )

                Screen.CopatientCollab -> CopatientCollabScreen(
                    state = collaboration,
                    messages = copatientChat,
                    onConnectChange = viewModel::setCollaborationConnected,
                    onSend = viewModel::sendCopatientMessage,
                    onBack = viewModel::backToDoctorSearch,
                )

                Screen.Support -> SupportScreen(
                    tickets = supportTickets,
                    messages = supportChat,
                    onCreateTicket = viewModel::createSupportTicket,
                    onSendChat = viewModel::sendSupportMessage,
                    onFriendsFamily = viewModel::openFriendsAndFamily,
                    onReview = viewModel::openGoogleReview,
                    onReengagement = viewModel::openReengagement,
                    onLogout = viewModel::logout,
                )

                Screen.FriendsFamily -> FriendsFamilyScreen(
                    patients = patients,
                    onToggleInvite = viewModel::togglePatientInvite,
                    onBack = viewModel::openSupport,
                )

                Screen.GoogleReview -> GoogleReviewScreen(
                    state = reviewState,
                    onSubmit = viewModel::submitGoogleReview,
                    onBack = viewModel::openSupport,
                )
            }
        }
    }
}

@Composable
private fun AppShell(
    snackbarHostState: SnackbarHostState,
    uiState: com.schedula.internship.ui.AppUiState,
    onTab: (BottomTab) -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = uiState.activeTab == BottomTab.FindDoctor,
                    onClick = { onTab(BottomTab.FindDoctor) },
                    icon = { Icon(Icons.Outlined.Search, contentDescription = "Find Doctor") },
                    label = { Text("Find") },
                )
                NavigationBarItem(
                    selected = uiState.activeTab == BottomTab.MyRecords,
                    onClick = { onTab(BottomTab.MyRecords) },
                    icon = { Icon(Icons.Outlined.Groups, contentDescription = "My Records") },
                    label = { Text("Records") },
                )
                NavigationBarItem(
                    selected = uiState.activeTab == BottomTab.MyAppt,
                    onClick = { onTab(BottomTab.MyAppt) },
                    icon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = "My Appt") },
                    label = { Text("Appt") },
                )
                NavigationBarItem(
                    selected = uiState.activeTab == BottomTab.Profile,
                    onClick = { onTab(BottomTab.Profile) },
                    icon = { Icon(Icons.Outlined.AccountCircle, contentDescription = "Profile") },
                    label = { Text("Profile") },
                )
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            content()
        }
    }
}

@Composable
private fun LoginScreen(
    phone: String,
    otp: String,
    otpSent: Boolean,
    error: String?,
    onPhoneChange: (String) -> Unit,
    onOtpChange: (String) -> Unit,
    onSendOtp: () -> Unit,
    onVerifyOtp: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Schedula", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Meet your doctor", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(value = phone, onValueChange = onPhoneChange, label = { Text("Mobile number") }, modifier = Modifier.fillMaxWidth())
        if (otpSent) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = otp, onValueChange = onOtpChange, label = { Text("OTP") }, modifier = Modifier.fillMaxWidth())
        }
        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = if (otpSent) onVerifyOtp else onSendOtp, modifier = Modifier.fillMaxWidth()) {
            Text(if (otpSent) "Verify OTP" else "Send OTP")
        }
    }
}

@Composable
private fun DoctorSearchScreen(
    query: String,
    doctors: List<Doctor>,
    onQueryChange: (String) -> Unit,
    onDoctorClick: (String) -> Unit,
    onReminders: () -> Unit,
    onSeamless: () -> Unit,
    onCopatient: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(value = query, onValueChange = onQueryChange, label = { Text("Search Doctor") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = onReminders, label = { Text("Reminders") })
            AssistChip(onClick = onSeamless, label = { Text("IVR Plan") })
            AssistChip(onClick = onCopatient, label = { Text("Co-patient") })
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(doctors, key = { it.id }) { doctor ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(doctor.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${doctor.specialty} • ${doctor.yearsOfExperience} yrs", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(doctor.bio)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { onDoctorClick(doctor.id) }) { Text("View profile") }
                    }
                }
            }
        }
    }
}

@Composable
private fun DoctorProfileScreen(doctor: Doctor?, onBook: () -> Unit, onBack: () -> Unit) {
    if (doctor == null) {
        CenterMessage("Doctor not found")
        return
    }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedButton(onClick = onBack) { Text("Back") }
        Spacer(Modifier.height(12.dp))
        Text(doctor.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("${doctor.specialty} • ${doctor.yearsOfExperience} yrs")
        Text(doctor.bio)
        Spacer(Modifier.height(10.dp))
        Text("Availability: ${doctor.availabilitySummary}")
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBook, modifier = Modifier.fillMaxWidth()) { Text("Book / Plan Appointment") }
    }
}

@Composable
private fun BookingDateScreen(
    doctor: Doctor?,
    dateOptions: List<String>,
    selectedDate: String,
    selectedType: AppointmentType,
    onTypeChange: (AppointmentType) -> Unit,
    onDateSelected: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedButton(onClick = onBack) { Text("Back") }
        Text("Choose date", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
        Text(doctor?.name ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onTypeChange(AppointmentType.Regular) }) {
                Text(if (selectedType == AppointmentType.Regular) "Regular ✓" else "Regular")
            }
            OutlinedButton(onClick = { onTypeChange(AppointmentType.Online) }) {
                Text(if (selectedType == AppointmentType.Online) "Online ✓" else "Online")
            }
        }
        Spacer(Modifier.height(12.dp))
        dateOptions.forEach { date ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(date)
                OutlinedButton(onClick = { onDateSelected(date) }) { Text(if (selectedDate == date) "Selected" else "Select") }
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text("Choose time") }
    }
}

@Composable
private fun BookingTimeScreen(
    doctor: Doctor?,
    slots: List<Slot>,
    selectedSlotId: String?,
    onSlotClick: (String) -> Unit,
    onNextAvailable: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedButton(onClick = onBack) { Text("Back") }
        Text("Choose time", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
        Text(doctor?.name ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onNextAvailable, modifier = Modifier.fillMaxWidth()) {
            Text("Next available slot")
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(slots.filterNot { it.isBooked }, key = { it.id }) { slot ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(slot.timeLabel)
                        OutlinedButton(onClick = { onSlotClick(slot.id) }) {
                            Text(if (selectedSlotId == slot.id) "Selected" else "Select")
                        }
                    }
                }
            }
        }
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth(), enabled = selectedSlotId != null) {
            Text("Add patient details")
        }
    }
}

@Composable
private fun AddPatientScreen(
    patients: List<Patient>,
    selectedPatient: String,
    onPatientSelected: (String) -> Unit,
    onSave: (String, Int, String, String, Int, String) -> Unit,
    onContinue: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var age by rememberSaveable { mutableStateOf("28") }
    var sex by rememberSaveable { mutableStateOf("Female") }
    var relation by rememberSaveable { mutableStateOf("Self") }
    var weight by rememberSaveable { mutableStateOf("60") }
    var complaint by rememberSaveable { mutableStateOf("Stomach pain") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Add patient details", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(patients, key = { it.id }) { patient ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${patient.name} • ${patient.relation}")
                        OutlinedButton(onClick = { onPatientSelected(patient.id) }) {
                            Text(if (selectedPatient == patient.id) "Selected" else "Use")
                        }
                    }
                }
            }
        }
        OutlinedTextField(name, { name = it }, label = { Text("name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(age, { age = it }, label = { Text("age") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(sex, { sex = it }, label = { Text("sex") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(relation, { relation = it }, label = { Text("relation") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(weight, { weight = it }, label = { Text("weight") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(complaint, { complaint = it }, label = { Text("complaint") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { onSave(name, age.toIntOrNull() ?: 0, sex, relation, weight.toIntOrNull() ?: 0, complaint) }, modifier = Modifier.weight(1f)) { Text("Save") }
            Button(onClick = onContinue, modifier = Modifier.weight(1f)) { Text("Book") }
        }
    }
}

@Composable
private fun BookingConfirmationScreen(
    appointment: Appointment?,
    onAddPatientDetails: () -> Unit,
    onViewAppointments: () -> Unit,
) {
    if (appointment == null) {
        CenterMessage("Appointment not found")
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Booking Confirmation", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("Your appointment with ${appointment.doctorName} is confirmed")
        Text("Token #${appointment.tokenNumber} at ${appointment.timeLabel}")
        Text("Please report 15 minutes earlier")
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAddPatientDetails, modifier = Modifier.fillMaxWidth()) { Text("Add patient details") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onViewAppointments, modifier = Modifier.fillMaxWidth()) { Text("View my appointments") }
    }
}

@Composable
private fun BookingFailedScreen(onRetry: () -> Unit, onBookNext: () -> Unit, onMyAppointments: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
        Text("Unable to book", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Selected slot is no longer available.")
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBookNext, modifier = Modifier.fillMaxWidth()) { Text("Book next available slot") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Pick another slot") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onMyAppointments, modifier = Modifier.fillMaxWidth()) { Text("Go to My Appt") }
    }
}

@Composable
private fun PatientDetailsScreen(
    appointment: Appointment?,
    patient: Patient?,
    onPay: () -> Unit,
    onSaveReport: (String) -> Unit,
    onFollowUp: (Boolean) -> Unit,
    onChat: () -> Unit,
    onMyAppointments: () -> Unit,
) {
    var report by remember(appointment?.id) { mutableStateOf(appointment?.report.orEmpty()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Patient details", style = MaterialTheme.typography.titleLarge)
        Text("Doctor: ${appointment?.doctorName.orEmpty()}")
        Text("Patient: ${patient?.name.orEmpty()} • ${patient?.age ?: 0} • ${patient?.sex.orEmpty()}")
        Text("Complaint: ${appointment?.complaint.orEmpty()}")
        Spacer(Modifier.height(12.dp))
        Text("Payment: ${appointment?.paymentStatus ?: PaymentStatus.Unpaid}")
        Button(onClick = onPay, enabled = appointment?.paymentStatus == PaymentStatus.Unpaid, modifier = Modifier.fillMaxWidth()) {
            Text("Pay consultation fee upfront")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = report, onValueChange = { report = it }, label = { Text("Report") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { onSaveReport(report) }, modifier = Modifier.weight(1f)) { Text("Save report") }
            OutlinedButton(onClick = { onFollowUp(!(appointment?.followUpRequested ?: false)) }, modifier = Modifier.weight(1f)) {
                Text(if (appointment?.followUpRequested == true) "Follow-up on" else "Request follow-up")
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onChat, modifier = Modifier.fillMaxWidth()) { Text("Open patient chat") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onMyAppointments, modifier = Modifier.fillMaxWidth()) { Text("My appointments") }
    }
}

@Composable
private fun ChatScreen(
    title: String,
    messages: List<ChatMessage>,
    onSend: (String) -> Unit,
    onBack: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedButton(onClick = onBack) { Text("Back") }
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages, key = { it.id }) { msg ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(msg.sender.name, fontWeight = FontWeight.SemiBold)
                        Text(msg.content)
                    }
                }
            }
        }
        OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Message") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Button(onClick = { onSend(text); text = "" }, modifier = Modifier.fillMaxWidth()) { Text("Send") }
    }
}

@Composable
private fun MyAppointmentsScreen(
    appointments: List<Appointment>,
    onOpenDetails: (String) -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val upcoming = appointments.filter {
        it.status == AppointmentStatus.Scheduled || it.status == AppointmentStatus.Rescheduled
    }
    val past = appointments.filter {
        it.status != AppointmentStatus.Scheduled && it.status != AppointmentStatus.Rescheduled
    }
    val visible = if (tab == 0) upcoming else past

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { tab = 0 }) { Text("Upcoming") }
            OutlinedButton(onClick = { tab = 1 }) { Text("Past") }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(visible, key = { it.id }) { appt ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("${appt.doctorName} - ${appt.patientName}")
                        Text("${appt.dateLabel} ${appt.timeLabel} • Token #${appt.tokenNumber}")
                        Text("Status: ${appt.status}")
                        Button(onClick = { onOpenDetails(appt.id) }) { Text("View") }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppointmentDetailsScreen(
    appointment: Appointment?,
    onCancel: () -> Unit,
    onReschedule: () -> Unit,
    onFeedback: () -> Unit,
) {
    if (appointment == null) {
        CenterMessage("Appointment not found")
        return
    }

    val patientsAhead = (appointment.tokenNumber - 3).coerceAtLeast(0)
    val expectedMinutes = patientsAhead * 5

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Appointment details", style = MaterialTheme.typography.titleLarge)
        Text("${appointment.doctorName} • ${appointment.specialty}")
        Text("Consulting: ${appointment.dateLabel}, ${appointment.timeLabel}")
        Text("Token: #${appointment.tokenNumber}")
        Text("Live tracking: $patientsAhead patients ahead. Expected in ${expectedMinutes} mins")
        Text("Payment: ${appointment.paymentStatus}")
        Text("Report: ${appointment.report.ifBlank { "Not added" }}")
        Text("Follow-up: ${if (appointment.followUpRequested) "Requested" else "Not requested"}")
        Spacer(Modifier.height(12.dp))
        Button(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onReschedule, modifier = Modifier.fillMaxWidth()) { Text("Reschedule") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onFeedback, modifier = Modifier.fillMaxWidth()) { Text("Consulting feedback") }
    }
}

@Composable
private fun AppointmentCancelScreen(
    appointment: Appointment?,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedButton(onClick = onBack) { Text("Back") }
        Spacer(Modifier.height(12.dp))
        Text("Appointment cancel", style = MaterialTheme.typography.titleLarge)
        Text("You are about to cancel appointment with ${appointment?.doctorName.orEmpty()}")
        Spacer(Modifier.height(12.dp))
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) { Text("Confirm cancel") }
    }
}

@Composable
private fun AppointmentRescheduleScreen(
    slots: List<Slot>,
    onConfirm: (String) -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedButton(onClick = onBack) { Text("Back") }
        Spacer(Modifier.height(12.dp))
        Text("Appointment reschedule", style = MaterialTheme.typography.titleLarge)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(slots.filterNot { it.isBooked }, key = { it.id }) { slot ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(slot.timeLabel)
                        Button(onClick = { onConfirm(slot.id) }) { Text("Book") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsultingFeedbackScreen(onDone: (Int, Int, Int) -> Unit) {
    var consulting by rememberSaveable { mutableIntStateOf(4) }
    var hospital by rememberSaveable { mutableIntStateOf(4) }
    var waiting by rememberSaveable { mutableIntStateOf(4) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Consulting feedback", style = MaterialTheme.typography.titleLarge)
        RatingRow("Consulting feedback", consulting) { consulting = it }
        RatingRow("Hospital/Clinic feedback", hospital) { hospital = it }
        RatingRow("Waiting time", waiting) { waiting = it }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onDone(consulting, hospital, waiting) }, modifier = Modifier.fillMaxWidth()) {
            Text("Submit")
        }
    }
}

@Composable
private fun RatingRow(label: String, value: Int, onChange: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text("$label: $value/5")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..5).forEach { star ->
                OutlinedButton(onClick = { onChange(star) }) { Text(star.toString()) }
            }
        }
    }
}

@Composable
private fun RemindersScreen(
    reminders: List<ReminderItem>,
    onOpenAppointment: (String?) -> Unit,
    onBack: () -> Unit,
    onRescheduleByDoctor: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedButton(onClick = onBack) { Text("Back") }
        Spacer(Modifier.height(12.dp))
        Text("Appointment reminders", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(reminders, key = { it.id }) { reminder ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(reminder.message)
                        reminder.appointmentId?.let {
                            OutlinedButton(onClick = { onOpenAppointment(it) }) { Text("Open") }
                        }
                    }
                }
            }
        }
        OutlinedButton(onClick = onRescheduleByDoctor, modifier = Modifier.fillMaxWidth()) {
            Text("Doctor reschedule notices")
        }
    }
}

@Composable
private fun RescheduleByDoctorScreen(
    notices: List<DoctorNotice>,
    onApply: (String) -> Unit,
    onDismiss: (String) -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedButton(onClick = onBack) { Text("Back") }
        Spacer(Modifier.height(12.dp))
        Text("Appointment reschedule by doctor", style = MaterialTheme.typography.titleLarge)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(notices, key = { it.id }) { notice ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(notice.message)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onApply(notice.id) }) { Text("Apply suggested") }
                            OutlinedButton(onClick = { onDismiss(notice.id) }) { Text("Dismiss") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeamlessAppointmentScreen(
    doctors: List<Doctor>,
    patients: List<Patient>,
    dateOptions: List<String>,
    slots: List<Slot>,
    selectedDoctorId: String?,
    selectedPatientId: String,
    selectedDate: String,
    selectedSlotId: String?,
    plans: List<IvrPlan>,
    onPickDoctor: (String) -> Unit,
    onPickPatient: (String) -> Unit,
    onPickDate: (String) -> Unit,
    onPickSlot: (String) -> Unit,
    onSavePlan: (String) -> Unit,
    onConfirmPlan: (String) -> Unit,
    onBack: () -> Unit,
) {
    var ivrAppId by rememberSaveable { mutableStateOf("IVR-APP-") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedButton(onClick = onBack) { Text("Back") }
        Spacer(Modifier.height(10.dp))
        Text("Seamless appointment", style = MaterialTheme.typography.titleLarge)
        Text("Plan via IVR App ID")

        OutlinedTextField(
            value = ivrAppId,
            onValueChange = { ivrAppId = it },
            label = { Text("IVR App ID") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))
        Text("Doctor")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            doctors.forEach { doctor ->
                OutlinedButton(onClick = { onPickDoctor(doctor.id) }) {
                    Text(if (selectedDoctorId == doctor.id) "${doctor.name} ✓" else doctor.name)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("Patient")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            patients.forEach { patient ->
                OutlinedButton(onClick = { onPickPatient(patient.id) }) {
                    Text(if (selectedPatientId == patient.id) "${patient.name} ✓" else patient.name)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("Date")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            dateOptions.take(4).forEach { date ->
                OutlinedButton(onClick = { onPickDate(date) }) {
                    Text(if (selectedDate == date) "$date ✓" else date)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("Slot")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.height(140.dp)) {
            items(slots.filterNot { it.isBooked }, key = { it.id }) { slot ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(slot.timeLabel)
                    OutlinedButton(onClick = { onPickSlot(slot.id) }) {
                        Text(if (selectedSlotId == slot.id) "Selected" else "Select")
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { onSavePlan(ivrAppId) }, modifier = Modifier.weight(1f), enabled = selectedDoctorId != null && selectedSlotId != null) {
                Text("Save plan")
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("Planned IVR appointments")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(plans, key = { it.id }) { plan ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("${plan.ivrAppId} • ${plan.status}")
                        Text("Doctor: ${plan.doctorId}, Patient: ${plan.patientId}")
                        Text("Date: ${plan.dateLabel}")
                        if (plan.status == IvrPlanStatus.Planned || plan.status == IvrPlanStatus.Confirmed) {
                            Button(onClick = { onConfirmPlan(plan.id) }) { Text("Confirm payment + convert") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CopatientCollabScreen(
    state: CollaborationState,
    messages: List<ChatMessage>,
    onConnectChange: (Boolean) -> Unit,
    onSend: (String) -> Unit,
    onBack: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedButton(onClick = onBack) { Text("Back") }
        Spacer(Modifier.height(10.dp))
        Text("Copatient collaboration", style = MaterialTheme.typography.titleLarge)
        Text("Group: ${state.groupName}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onConnectChange(!state.connected) }) {
                Text(if (state.connected) "Disconnect" else "Connect")
            }
            Text(if (state.connected) "Connected" else "Not connected")
        }

        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages, key = { it.id }) { msg ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(msg.sender.name, fontWeight = FontWeight.SemiBold)
                        Text(msg.content)
                    }
                }
            }
        }

        OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Message") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Button(onClick = { onSend(text); text = "" }, modifier = Modifier.fillMaxWidth(), enabled = state.connected) { Text("Send") }
    }
}

@Composable
private fun SupportScreen(
    tickets: List<SupportTicket>,
    messages: List<ChatMessage>,
    onCreateTicket: (String, String) -> Unit,
    onSendChat: (String) -> Unit,
    onFriendsFamily: () -> Unit,
    onReview: () -> Unit,
    onReengagement: () -> Unit,
    onLogout: () -> Unit,
) {
    var subject by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var chat by rememberSaveable { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Patient - Customer Support", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = onFriendsFamily, label = { Text("Friends & Family") })
            AssistChip(onClick = onReview, label = { Text("Google Review") })
            AssistChip(onClick = onReengagement, label = { Text("Reengagement") })
        }
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(subject, { subject = it }, label = { Text("Ticket subject") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(message, { message = it }, label = { Text("Ticket message") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { onCreateTicket(subject, message); subject = ""; message = "" }, modifier = Modifier.fillMaxWidth()) {
            Text("Create support ticket")
        }

        Spacer(Modifier.height(8.dp))
        Text("Tickets", fontWeight = FontWeight.SemiBold)
        LazyColumn(modifier = Modifier.height(130.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(tickets, key = { it.id }) { ticket ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("${ticket.subject} • ${ticket.status}")
                        Text(ticket.message)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("Support chat", fontWeight = FontWeight.SemiBold)
        LazyColumn(modifier = Modifier.height(130.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(messages, key = { it.id }) { msg ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(msg.sender.name)
                        Text(msg.content)
                    }
                }
            }
        }

        OutlinedTextField(chat, { chat = it }, label = { Text("Chat message") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { onSendChat(chat); chat = "" }, modifier = Modifier.fillMaxWidth()) { Text("Send") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Logout") }
    }
}

@Composable
private fun FriendsFamilyScreen(
    patients: List<Patient>,
    onToggleInvite: (String, Boolean) -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedButton(onClick = onBack) { Text("Back") }
        Spacer(Modifier.height(12.dp))
        Text("Friends & Family", style = MaterialTheme.typography.titleLarge)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(patients, key = { it.id }) { patient ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${patient.name} | ${patient.sex} | ${patient.age} | ${patient.relation}")
                        OutlinedButton(onClick = { onToggleInvite(patient.id, !patient.invited) }) {
                            Text(if (patient.invited) "Invited" else "Invite")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoogleReviewScreen(
    state: GoogleReviewState,
    onSubmit: (Int, String) -> Unit,
    onBack: () -> Unit,
) {
    var rating by rememberSaveable { mutableIntStateOf(state.rating ?: 5) }
    var comment by rememberSaveable { mutableStateOf(state.comment) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onBack) { Text("Back") }
        Text("Google Review Requested", style = MaterialTheme.typography.titleLarge)
        Text("Express your gratitude for the doctor")
        Text("Status: ${if (state.submitted) "Submitted" else "Pending"}")
        RatingRow("Rating", rating) { rating = it }
        OutlinedTextField(comment, { comment = it }, label = { Text("Comment") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { onSubmit(rating, comment) }, modifier = Modifier.fillMaxWidth()) { Text("Submit review") }
    }
}

@Composable
private fun CenterMessage(message: String) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text(message, modifier = Modifier.padding(24.dp))
    }
}
