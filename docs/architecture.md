# Architecture

## System Overview

```mermaid
flowchart LR
  U[Patient User] --> UI[Compose UI Layer]
  UI --> VM[AppViewModel]
  VM --> REPO[SqliteSchedulaRepository]
  REPO --> API[Mock API Contracts]
  REPO --> DB[(Room SQLite DB)]

  DB --> D1[doctors]
  DB --> D2[patients]
  DB --> D3[slots]
  DB --> D4[appointments]
  DB --> D5[support_tickets]
  DB --> D6[ivr_plans]
  DB --> D7[google_review_state]
  DB --> D8[app_meta]

  API --> A1[SchedulingApi]
  API --> A2[BillingApi]
  API --> A3[SupportApi]
  API --> A4[ReviewApi]
```

## Module Structure

```mermaid
flowchart TB
  A[app/src/main] --> M1[model]
  A --> M2[data]
  A --> M3[domain]
  A --> M4[ui]

  M4 --> S1[Screen State Machine]
  M4 --> S2[Bottom Navigation]
  M4 --> S3[Screen Composables]

  M2 --> R1[Repository Interface]
  M2 --> R2[Room Entities + DAO]
  M2 --> R3[API Contracts + Local Mocks]
  M2 --> R4[Seed Data]
```

## Local-First Boundary

- Source of truth: local Room database.
- App boot: `ensureSeeded()` populates local tables for first-run demo behavior.
- All read models in UI are `Flow` streams from DAO observers.
- Login persistence uses `app_meta.logged_in_phone`.
- API-dependent operations run through deterministic local mocks (availability, booking confirmation, payment capture, support acknowledgment, review moderation) while keeping all user state local.

## Why This Shape

- Interns can trace each feature end-to-end in one module.
- Data layer still uses production-like boundaries (DAO -> repository -> ViewModel).
- No hidden framework magic: explicit state, explicit transitions.
