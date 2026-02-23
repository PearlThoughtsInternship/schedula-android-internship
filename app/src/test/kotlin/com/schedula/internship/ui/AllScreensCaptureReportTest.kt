package com.schedula.internship.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.common.truth.Truth.assertThat
import com.schedula.internship.AddPatientScreen
import com.schedula.internship.AppointmentCancelScreen
import com.schedula.internship.AppointmentDetailsScreen
import com.schedula.internship.AppointmentRescheduleScreen
import com.schedula.internship.BookingConfirmationScreen
import com.schedula.internship.BookingDateScreen
import com.schedula.internship.BookingFailedScreen
import com.schedula.internship.BookingTimeScreen
import com.schedula.internship.ChatScreen
import com.schedula.internship.ConsultingFeedbackScreen
import com.schedula.internship.CopatientCollabScreen
import com.schedula.internship.DoctorProfileScreen
import com.schedula.internship.DoctorSearchScreen
import com.schedula.internship.FriendsFamilyScreen
import com.schedula.internship.GoogleReviewScreen
import com.schedula.internship.LoginScreen
import com.schedula.internship.MyAppointmentsScreen
import com.schedula.internship.PatientDetailsScreen
import com.schedula.internship.RemindersScreen
import com.schedula.internship.RescheduleByDoctorScreen
import com.schedula.internship.SeamlessAppointmentScreen
import com.schedula.internship.SupportScreen
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
import com.schedula.internship.ui.theme.SchedulaInternshipTheme
import java.io.File
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AllScreensCaptureReportTest {

    @Test
    fun generateAllScreenCapturesAndPdfReport() {
        val outputDir = File("build/reports/screen-captures").apply {
            mkdirs()
            listFiles()?.forEach {
                if (it.extension == "png" || it.extension == "pdf" || it.name == "index.md") {
                    it.delete()
                }
            }
        }

        val doctorLavangi = Doctor(
            id = "doctor-lavangi",
            name = "Dr. Lavangi",
            specialty = "Gynacologist",
            hospital = "Schedula Women Care",
            rating = 4.9,
            yearsOfExperience = 15,
            consultationFee = 600.0,
            bio = "Gold Medalist and women health specialist",
            availabilitySummary = "Mon-Fri 10 AM to 1 PM, Sat 2 PM to 5 PM",
        )
        val doctorKumar = doctorLavangi.copy(
            id = "doctor-kumar",
            name = "Dr. Kumar",
            specialty = "Pediatrics",
            yearsOfExperience = 12,
            consultationFee = 550.0,
        )
        val doctorElango = doctorLavangi.copy(
            id = "doctor-elango",
            name = "Dr. Elango",
            specialty = "General Medicine",
            yearsOfExperience = 10,
            consultationFee = 450.0,
        )
        val doctors = listOf(doctorLavangi, doctorKumar, doctorElango)

        val patients = listOf(
            Patient("patient-self", "Muthukumar", 28, "Male", "Self", 73, "General consultation", invited = true),
            Patient("patient-meena", "Meena", 26, "Female", "Wife", 58, "Stomach pain", invited = false),
            Patient("patient-kishore", "Kishore", 21, "Male", "Son", 62, "Fever follow-up", invited = false),
        )

        val slots = listOf(
            Slot("slot-1", doctorLavangi.id, "Tomorrow", "10:00 AM - 11:00 AM", isBooked = false, type = AppointmentType.Regular),
            Slot("slot-2", doctorLavangi.id, "Tomorrow", "11:00 AM - 12:00 PM", isBooked = false, type = AppointmentType.Regular),
            Slot("slot-3", doctorLavangi.id, "Tomorrow", "06:00 PM - 07:00 PM", isBooked = false, type = AppointmentType.Online),
            Slot("slot-4", doctorLavangi.id, "Tomorrow", "07:00 PM - 08:00 PM", isBooked = true, type = AppointmentType.Online),
        )

        val scheduledAppointment = Appointment(
            id = "appointment-1001",
            doctorId = doctorLavangi.id,
            doctorName = doctorLavangi.name,
            doctorSpecialty = doctorLavangi.specialty,
            doctorExperienceYears = doctorLavangi.yearsOfExperience,
            specialty = doctorLavangi.specialty,
            patientId = "patient-meena",
            patientName = "Meena",
            slotId = "slot-1",
            dateLabel = "Tomorrow",
            timeLabel = "10:00 AM - 11:00 AM",
            slotLabel = "Tomorrow 10:00 AM - 11:00 AM",
            tokenNumber = 14,
            channel = "APP",
            complaint = "Stomach pain",
            status = AppointmentStatus.Scheduled,
            type = AppointmentType.Regular,
            paymentStatus = PaymentStatus.Unpaid,
            report = "",
            followUpRequested = false,
            consultingFeedback = null,
            hospitalFeedback = null,
            waitingTimeFeedback = null,
            confirmationCode = "CNF-12345678",
            paymentReference = null,
        )
        val completedAppointment = scheduledAppointment.copy(
            id = "appointment-1002",
            doctorName = doctorKumar.name,
            doctorId = doctorKumar.id,
            doctorSpecialty = doctorKumar.specialty,
            specialty = doctorKumar.specialty,
            status = AppointmentStatus.Completed,
            paymentStatus = PaymentStatus.Paid,
            paymentReference = "PAY-87654321",
            report = "Recovered well",
            tokenNumber = 9,
            confirmationCode = "CNF-87654321",
            type = AppointmentType.Online,
            dateLabel = "1st Oct",
            timeLabel = "11:00 AM - 12:00 PM",
            slotLabel = "1st Oct 11:00 AM - 12:00 PM",
        )

        val reminders = listOf(
            ReminderItem("rem-1", "You have an appointment with Dr. Lavangi at 10:00 AM tomorrow", scheduledAppointment.id),
            ReminderItem("rem-2", "Doctor requested slot change for your appointment", scheduledAppointment.id),
        )

        val doctorNotices = listOf(
            DoctorNotice(
                id = "notice-1",
                appointmentId = scheduledAppointment.id,
                message = "Appointment was rescheduled by clinic. Please choose another slot.",
                suggestedSlotId = "slot-2",
                resolved = false,
            ),
        )

        val chatMessages = listOf(
            ChatMessage("chat-1", ChatThreadType.Patient, ChatSender.Doctor, "Please share recent symptoms.", 1_700_000_001_000),
            ChatMessage("chat-2", ChatThreadType.Patient, ChatSender.User, "Mild pain and nausea.", 1_700_000_002_000),
        )

        val supportTickets = listOf(
            SupportTicket(
                id = "ticket-1",
                subject = "Payment issue",
                message = "Unable to complete payment from app",
                status = SupportTicketStatus.Open,
                createdAtEpochMillis = 1_700_000_003_000,
                externalReference = "SUP-12345678",
                estimatedResolutionHours = 4,
            ),
        )

        val ivrPlans = listOf(
            IvrPlan(
                id = "ivr-1",
                doctorId = doctorLavangi.id,
                patientId = "patient-self",
                ivrAppId = "IVR-APP-1001",
                dateLabel = "Tomorrow",
                slotId = "slot-3",
                paymentConfirmed = true,
                status = IvrPlanStatus.Converted,
                convertedAppointmentId = "appointment-2001",
            ),
        )

        val captures = listOf(
            capture(outputDir, "01-login", "Login") {
                LoginScreen("", "", false, null, {}, {}, {}, {})
            },
            capture(outputDir, "02-doctor-search", "Doctor Search") {
                DoctorSearchScreen("", doctors, {}, {}, {}, {}, {})
            },
            capture(outputDir, "03-doctor-profile", "Doctor Profile") {
                DoctorProfileScreen(doctorLavangi, {}, {})
            },
            capture(outputDir, "04-booking-date", "Booking Date") {
                BookingDateScreen(
                    doctor = doctorLavangi,
                    dateOptions = listOf("Today", "Tomorrow", "Next available day", "1st Oct"),
                    selectedDate = "Tomorrow",
                    selectedType = AppointmentType.Regular,
                    onTypeChange = {},
                    onDateSelected = {},
                    onNext = {},
                    onBack = {},
                )
            },
            capture(outputDir, "05-booking-time", "Booking Time") {
                BookingTimeScreen(doctorLavangi, slots, "slot-1", {}, {}, {}, {})
            },
            capture(outputDir, "06-add-patient-details", "Add Patient Details") {
                AddPatientScreen(patients, "patient-meena", {}, { _, _, _, _, _, _ -> }, {})
            },
            capture(outputDir, "07-booking-confirmation", "Booking Confirmation") {
                BookingConfirmationScreen(scheduledAppointment, {}, {})
            },
            capture(outputDir, "08-booking-failed", "Booking Failed") {
                BookingFailedScreen({}, {}, {})
            },
            capture(outputDir, "09-patient-details", "Patient Details") {
                PatientDetailsScreen(scheduledAppointment, patients[1], {}, {}, {}, {}, {})
            },
            capture(outputDir, "10-patient-chat", "Patient Chat") {
                ChatScreen("Patient chat", chatMessages, {}, {})
            },
            capture(outputDir, "11-my-appointments", "My Appointments") {
                MyAppointmentsScreen(listOf(scheduledAppointment, completedAppointment), {})
            },
            capture(outputDir, "12-appointment-details", "Appointment Details") {
                AppointmentDetailsScreen(scheduledAppointment, {}, {}, {})
            },
            capture(outputDir, "13-appointment-cancel", "Appointment Cancel") {
                AppointmentCancelScreen(scheduledAppointment, {}, {})
            },
            capture(outputDir, "14-appointment-reschedule", "Appointment Reschedule") {
                AppointmentRescheduleScreen(
                    dateOptions = listOf("Today", "Tomorrow", "Next available day", "1st Oct"),
                    selectedDate = "Tomorrow",
                    slots = slots,
                    appointmentType = AppointmentType.Regular,
                    onPickDate = {},
                    onConfirm = {},
                    onBack = {},
                )
            },
            capture(outputDir, "15-consulting-feedback", "Consulting Feedback") {
                ConsultingFeedbackScreen { _, _, _ -> }
            },
            capture(outputDir, "16-reminders", "Reminders") {
                RemindersScreen(reminders, {}, {}, {})
            },
            capture(outputDir, "17-reschedule-by-doctor", "Reschedule By Doctor") {
                RescheduleByDoctorScreen(doctorNotices, {}, {}, {})
            },
            capture(outputDir, "18-reengagement", "Patient Reengagement") {
                ChatScreen("Patient reengagement", chatMessages, {}, {})
            },
            capture(outputDir, "19-seamless-appointment", "Seamless Appointment") {
                SeamlessAppointmentScreen(
                    doctors = doctors,
                    patients = patients,
                    dateOptions = listOf("Today", "Tomorrow", "Next available day", "1st Oct"),
                    slots = slots,
                    selectedDoctorId = doctorLavangi.id,
                    selectedPatientId = "patient-self",
                    selectedDate = "Tomorrow",
                    selectedSlotId = "slot-3",
                    plans = ivrPlans,
                    onPickDoctor = {},
                    onPickPatient = {},
                    onPickDate = {},
                    onPickSlot = {},
                    onSavePlan = {},
                    onConfirmPlan = {},
                    onBack = {},
                )
            },
            capture(outputDir, "20-copatient-collab", "Copatient Collaboration") {
                CopatientCollabScreen(
                    state = CollaborationState(connected = true, groupName = "New Mothers Group"),
                    messages = chatMessages,
                    onConnectChange = {},
                    onSend = {},
                    onBack = {},
                )
            },
            capture(outputDir, "21-support", "Support") {
                SupportScreen(
                    tickets = supportTickets,
                    messages = chatMessages,
                    onCreateTicket = { _, _ -> },
                    onSendChat = {},
                    onFriendsFamily = {},
                    onReview = {},
                    onReengagement = {},
                    onLogout = {},
                )
            },
            capture(outputDir, "22-friends-family", "Friends & Family") {
                FriendsFamilyScreen(patients, { _, _ -> }, {})
            },
            capture(outputDir, "23-google-review", "Google Review") {
                GoogleReviewScreen(
                    state = GoogleReviewState(
                        requested = true,
                        submitted = true,
                        rating = 5,
                        comment = "Excellent service",
                        moderationReference = "REV-12345678",
                    ),
                    onSubmit = { _, _ -> },
                    onBack = {},
                )
            },
        )

        createMarkdownIndex(captures, outputDir)

        assertThat(captures).hasSize(23)
        assertThat(captures.all { it.file.exists() && it.file.length() > 0L }).isTrue()
    }

    private fun capture(
        outputDir: File,
        slug: String,
        title: String,
        content: @androidx.compose.runtime.Composable () -> Unit,
    ): CapturedScreen {
        val activityController = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = activityController.get()
        activity.setContent {
            SchedulaInternshipTheme(darkTheme = false, dynamicColor = false) {
                content()
            }
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val outputFile = File(outputDir, "$slug.png")
        drawActivityToPng(activity, outputFile)
        activityController.pause().stop().destroy()
        return CapturedScreen(title = title, file = outputFile)
    }

    private fun drawActivityToPng(activity: ComponentActivity, outputFile: File) {
        val root = activity.window.decorView
        val width = 1080
        val height = 2200

        root.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        root.layout(0, 0, width, height)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        root.draw(canvas)
        val uniqueColors = countUniqueSampledColors(bitmap, sampleStep = 24)
        assertThat(uniqueColors).isAtLeast(2)
        outputFile.outputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }

    private fun countUniqueSampledColors(bitmap: Bitmap, sampleStep: Int): Int {
        val colors = HashSet<Int>()
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                colors.add(bitmap.getPixel(x, y))
                x += sampleStep
            }
            y += sampleStep
        }
        return colors.size
    }

    private fun createMarkdownIndex(captures: List<CapturedScreen>, outputDir: File) {
        val indexFile = File(outputDir, "index.md")
        val content = buildString {
            appendLine("# Screen Capture Report")
            appendLine()
            appendLine("Generate PDF from PNG files with:")
            appendLine("`magick *.png screen-report.pdf`")
            appendLine()
            captures.forEachIndexed { index, screen ->
                appendLine("${index + 1}. ${screen.title} - `${screen.file.name}`")
            }
        }
        indexFile.writeText(content)
    }

    private data class CapturedScreen(
        val title: String,
        val file: File,
    )
}
