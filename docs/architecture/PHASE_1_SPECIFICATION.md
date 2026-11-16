# Phase 1 Specification & Implementation Review
## Consent-Based Android Parental Control Ecosystem

### Overview
Phase 1 delivers the complete architectural foundation, backend core, parent authentication, child profile management, and cryptographic device pairing engine for the FamilyGuard Parental Control Ecosystem.

---

### Core Components Delivered

```
d:\New Project App\
├── backend/
│   ├── prisma/
│   │   └── schema.prisma              # Database schema (SQLite dev / Postgres prod)
│   ├── src/
│   │   ├── auth/                      # Parent Auth + Verifiable Parental Consent
│   │   ├── children/                  # Child Profile lifecycle + baseline policies
│   │   ├── pairing/                   # Cryptographic 6-char & QR token pairing engine
│   │   ├── policies/                  # Remote ScreenTime rules & instant pause
│   │   ├── prisma/                    # Global Prisma Client service
│   │   ├── app.module.ts              # Root NestJS module
│   │   └── main.ts                    # Bootstrap with Swagger & CORS
│   └── .env                           # Configured for zero-cost SQLite
│
├── parent_app/                        # Kotlin + Jetpack Compose Parent Dashboard
│   ├── app/src/main/
│   │   ├── AndroidManifest.xml
│   │   └── java/org/familyguard/parent/
│   │       ├── data/                  # Retrofit API service & DTO models
│   │       ├── ui/screens/
│   │       │   ├── LoginScreen.kt     # Guardian Auth
│   │       │   ├── RegisterScreen.kt  # VPC consent flow & legal terms
│   │       │   ├── DashboardScreen.kt # Child carousel, screen time donut, pause button
│   │       │   ├── CreateChildScreen.kt# Profile setup with age presets
│   │       │   └── PairingScreen.kt   # QR generator & live status poller
│   │       └── MainActivity.kt        # Compose Navigation Controller
│   └── build.gradle.kts
│
├── child_app/                         # Kotlin + Jetpack Compose + DPC Child App
│   ├── app/src/main/
│   │   ├── AndroidManifest.xml        # DPC DeviceAdminReceiver declarations
│   │   ├── res/xml/device_admin_receiver.xml
│   │   └── java/org/familyguard/child/
│   │       ├── dpc/ParentalDpcReceiver.kt # Android Enterprise DPC Receiver
│   │       ├── data/                  # Child Retrofit API client
│   │       ├── ui/screens/
│   │       │   ├── EnrollmentScreen.kt# Transparent rights & permissions charter
│   │       │   ├── PairingClaimScreen.kt# Code entry & hardware handshake
│   │       │   └── ChildStatusScreen.kt# Remaining screen time & Emergency 911 SOS
│   │       └── MainActivity.kt
│   └── build.gradle.kts
│
└── docs/
    ├── architecture/
    │   └── parental_control_architecture_and_spec.md
    └── setup/
        └── FREE_RESOURCES_GUIDE.md    # Zero-cost local & cloud deployment guide
```

---

### Security & Pairing Cryptography

1. **Verifiable Parental Consent (VPC)**:
   - Registration strictly requires `hasConsentedToParentalTerms: true`.
   - Generates a unique family code (e.g. `FAM-X8K2M9`).
2. **Device Pairing Handshake**:
   - Time-limited (15-minute) cryptographic pairing session.
   - Dual-mode input: 6-character human-friendly code or high-density QR payload.
   - Prevents replay attacks by invalidating previous pending tokens upon new code generation.
   - Upon successful claim, yields a distinct, long-lived `CHILD_DEVICE` token ensuring that child devices can only sync their assigned policies and report telemetry, but cannot alter restrictions.
3. **Emergency Call Preservation**:
   - The child application explicitly and permanently exempts standard telecom dialers (`ACTION_DIAL`) and Emergency SOS (911 / 112) from any screen time or app lock constraints.
