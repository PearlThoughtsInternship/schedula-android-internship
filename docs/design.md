# Design

## UX Goals

- Keep navigation predictable.
- Keep forms short and direct.
- Keep booking flow linear and recoverable.
- Preserve all key wireframe capabilities as navigable screens.

## Functional Areas

1. Authentication (OTP)
2. Doctor discovery
3. Booking (date, time, patient, confirmation/failure)
4. Appointment management (details, cancel, reschedule)
5. Patient engagement (chat, reminders, reengagement)
6. Support & social features (friends/family, review, collaboration)

## Booking Interaction

```mermaid
sequenceDiagram
  participant U as User
  participant UI as Compose Screen
  participant VM as AppViewModel
  participant R as Repository
  participant DB as Room DB

  U->>UI: Select doctor/date/time/patient
  UI->>VM: bookAppointment(...)
  VM->>R: bookAppointment(...)
  R->>DB: validate slot + create appointment + mark slot booked
  DB-->>R: committed row
  R-->>VM: BookingResult.Success
  VM-->>UI: activeScreen = BookingConfirmation
```

## Reschedule Interaction

```mermaid
sequenceDiagram
  participant U as User
  participant VM as AppViewModel
  participant R as Repository
  participant DB as Room DB

  U->>VM: confirmReschedule(newSlot)
  VM->>R: rescheduleAppointment(id, newSlot)
  R->>DB: unbook old slot
  R->>DB: book new slot
  R->>DB: update appointment status=Rescheduled
  R-->>VM: BookingResult.Success
  VM-->>U: Appointment details updated
```

## Error Strategy

- Slot race/conflict: returns `BookingResult.SlotUnavailable`.
- Validation errors: handled in ViewModel before repository call.
- UI feedback: snackbar + fallback screen for booking failure.
