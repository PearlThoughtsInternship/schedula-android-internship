package com.schedula.internship.data

import kotlinx.coroutines.delay

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Error(val message: String) : ApiResult<Nothing>
}

data class AvailabilityPayload(
    val available: Boolean,
    val reason: String?,
)

data class BookingConfirmationPayload(
    val confirmationCode: String,
)

data class PaymentPayload(
    val paymentReference: String,
)

data class SupportTicketPayload(
    val externalReference: String,
    val estimatedResolutionHours: Int,
)

data class ReviewPayload(
    val moderationReference: String,
)

interface SchedulingApi {
    suspend fun checkSlotAvailability(doctorId: String, slotId: String): ApiResult<AvailabilityPayload>
    suspend fun confirmBooking(doctorId: String, patientId: String, slotId: String, channel: String): ApiResult<BookingConfirmationPayload>
    suspend fun confirmReschedule(appointmentId: String, newSlotId: String): ApiResult<BookingConfirmationPayload>
}

interface BillingApi {
    suspend fun capturePayment(appointmentId: String, amount: Double): ApiResult<PaymentPayload>
}

interface SupportApi {
    suspend fun acknowledgeTicket(subject: String, message: String): ApiResult<SupportTicketPayload>
}

interface ReviewApi {
    suspend fun submitReview(rating: Int, comment: String): ApiResult<ReviewPayload>
}

class LocalMockSchedulingApi : SchedulingApi {
    override suspend fun checkSlotAvailability(doctorId: String, slotId: String): ApiResult<AvailabilityPayload> {
        delay(60)
        if (slotId.endsWith("-3")) {
            return ApiResult.Success(
                AvailabilityPayload(
                    available = false,
                    reason = "Hospital feed reports this slot is no longer available.",
                ),
            )
        }
        return ApiResult.Success(AvailabilityPayload(available = true, reason = null))
    }

    override suspend fun confirmBooking(
        doctorId: String,
        patientId: String,
        slotId: String,
        channel: String,
    ): ApiResult<BookingConfirmationPayload> {
        delay(90)
        if (channel == "IVR" && slotId.contains("Next-available-week")) {
            return ApiResult.Error("IVR booking is currently unavailable for this slot. Please select another slot.")
        }
        val confirmationCode = "CNF-${System.currentTimeMillis().toString().takeLast(8)}"
        return ApiResult.Success(BookingConfirmationPayload(confirmationCode = confirmationCode))
    }

    override suspend fun confirmReschedule(appointmentId: String, newSlotId: String): ApiResult<BookingConfirmationPayload> {
        delay(90)
        if (newSlotId.endsWith("-3")) {
            return ApiResult.Error("Reschedule rejected by hospital feed for this slot.")
        }
        val confirmationCode = "RCNF-${System.currentTimeMillis().toString().takeLast(8)}"
        return ApiResult.Success(BookingConfirmationPayload(confirmationCode = confirmationCode))
    }
}

class LocalMockBillingApi : BillingApi {
    override suspend fun capturePayment(appointmentId: String, amount: Double): ApiResult<PaymentPayload> {
        delay(80)
        if (amount <= 0.0) {
            return ApiResult.Error("Payment gateway rejected invalid amount.")
        }
        val paymentReference = "PAY-${System.currentTimeMillis().toString().takeLast(8)}"
        return ApiResult.Success(PaymentPayload(paymentReference = paymentReference))
    }
}

class LocalMockSupportApi : SupportApi {
    override suspend fun acknowledgeTicket(subject: String, message: String): ApiResult<SupportTicketPayload> {
        delay(70)
        if (subject.trim().length < 4) {
            return ApiResult.Error("Subject should be at least 4 characters.")
        }
        if (message.trim().length < 8) {
            return ApiResult.Error("Issue details should be at least 8 characters.")
        }
        return ApiResult.Success(
            SupportTicketPayload(
                externalReference = "SUP-${System.currentTimeMillis().toString().takeLast(8)}",
                estimatedResolutionHours = 4,
            ),
        )
    }
}

class LocalMockReviewApi : ReviewApi {
    override suspend fun submitReview(rating: Int, comment: String): ApiResult<ReviewPayload> {
        delay(50)
        if (rating !in 1..5) {
            return ApiResult.Error("Rating should be between 1 and 5.")
        }
        if (rating <= 2 && comment.trim().length < 8) {
            return ApiResult.Error("Please add a short comment for low ratings.")
        }
        return ApiResult.Success(
            ReviewPayload(
                moderationReference = "REV-${System.currentTimeMillis().toString().takeLast(8)}",
            ),
        )
    }
}
