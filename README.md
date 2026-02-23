# Schedula Android

Android application for appointment discovery, booking, follow-up, and patient support workflows.

## Quick Start

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
cd app/build/reports/screen-captures && magick $(ls -1 0[1-9]-*.png 1[0-9]-*.png 2[0-3]-*.png | sort) screen-report.pdf
```

Demo login OTP: `1234`

## Project Structure

- `app/` Android application module
- `docs/` architecture, design, diagrams, and implementation references

## Documentation

- [Architecture](docs/architecture.md)
- [Design](docs/design.md)
- [State Flow](docs/state-flow.md)
- [Flowcharts](docs/flowcharts.md)
- [Data Model](docs/data-model.md)
- [Wireframe Mapping](docs/wireframe-map.md)
- [Testing](docs/testing.md)

## Core Capabilities

- OTP login and persisted local session
- Doctor search and profile discovery
- Date/time/patient-based booking workflow
- Booking confirmation and failure handling
- Appointment details, cancel, reschedule, and feedback
- Reminder, reengagement, support, collaboration, and family workflows
- Local-first persistence with Room (SQLite)
- API-bounded behavior via deterministic mock contracts (availability, confirmation, payment, support, review)
