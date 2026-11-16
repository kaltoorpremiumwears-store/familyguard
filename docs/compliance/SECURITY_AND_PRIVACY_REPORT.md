# Security, Privacy & Google Play Families Compliance Report
## Legal Framework, Ethical Standards, and Data Protection Governance

---

### 1. Executive Summary & Philosophy

The FamilyGuard Parental Control Ecosystem is engineered around the principle of **Transparent, Consent-Based Parental Governance**. In stark contrast to surveillance spyware and illicit "stalkerware" tools, FamilyGuard adheres strictly to international child privacy regulations (COPPA, GDPR-K, UK Children's Code) and the Google Play Families Policy.

The ecosystem rejects covert operation:
- **No Hidden Recording**: Zero access to microphone or camera without explicit, active, user-initiated interaction.
- **No Private Message Scraping**: Zero interception of SMS, WhatsApp, Signal, or private chat contents.
- **No Keylogging**: Zero recording of keystrokes, passwords, or personal credentials.
- **No Stealth Mode**: Persistent, non-dismissible notifications clearly signal active parental management on the child's device at all times.

---

### 2. Legal Compliance Framework

#### A. COPPA (Children's Online Privacy Protection Act - 15 U.S.C. §§ 6501–6506)
- **Verifiable Parental Consent (VPC)**: Implemented during parent account registration. Parents must explicitly consent to data processing prior to profile generation (`hasConsentedToParentalTerms: true`).
- **Data Minimization**: Only essential operational data is collected (aggregate screen time minutes, installed application package names, and coarse/fine coordinates for geofence validation). No biometric markers or psychological profiling.
- **Data Portability & Erasure**: Parents possess unilateral rights to export all family telemetry via `GET /api/v1/auth/data-export` and purge all child data permanently via `DELETE /api/v1/auth/account`.

#### B. GDPR-K (General Data Protection Regulation - Article 8) & UK Age Appropriate Design Code
- **Default High Privacy**: Default policies restrict marketing identifiers, ban third-party analytics trackers, and isolate data within private tenant boundaries.
- **Child's Right to Transparency**: The Child App features an on-device "Child Rights & Active Rules Charter" displaying exactly what restrictions are configured, why they exist, and who manages them.
- **Data Retention Policies**: Automated database expiration cleanses location logs and telemetry older than the configured threshold (default 30 days).

#### C. Google Play Families Policy Compliance
- **No Advertising ID (AAID) Collection**: The Child App never collects, requests, or transmits the Google Advertising ID or any persistent hardware identifier (IMEI, MAC address).
- **Legitimate Permission Justifications**:
  * `PACKAGE_USAGE_STATS`: Used solely to calculate screen time allowances and enforce daily limits.
  * `ACCESS_FINE_LOCATION`: Used solely for the parent-authorized safety radar and geofencing.
  * `BIND_DEVICE_ADMIN` / Device Owner: Used exclusively for official Android Enterprise policy management (`setPackagesSuspended`, `setApplicationRestrictions`).

---

### 3. Cryptographic & Security Architecture

```
+─────────────────────────────────────────────────────────────────────────────────────────────+
|                                  SECURITY ARCHITECTURE                                      |
+─────────────────────────────────────────────────────────────────────────────────────────────+
  Client Tier (Parent & Child Apps)
  ├── Jetpack Security EncryptedSharedPreferences (AES-256-GCM) for local token storage
  ├── Android Keystore hardware-backed key generation
  └── ProGuard / R8 code obfuscation & root/tamper detection
         │
         │ TLS 1.3 / HTTPS (ECDHE-ECDSA-AES256-GCM-SHA384) with Certificate Pinning
         ▼
  Transport Tier
  ├── Bearer JWT Authorization (HMAC-SHA256, 7-day expiration)
  ├── Strict CORS Origin verification
  └── Input validation pipes with DTO whitelist sanitization
         │
         ▼
  Backend & Storage Tier
  ├── Password Hashing: bcrypt with cost factor 12
  ├── Foreign key cascade constraints for GDPR Article 17 atomic erasure
  ├── Immutable security AuditLog for all administrative policy mutations
  └── Zero third-party ad SDKs, trackers, or commercial telemetry beacons
```

---

### 4. Data Inventory & Retention Matrix

| Data Category | Purpose | Legal Basis | Storage Location | Retention Period | Exportable? |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Parent Credentials** | Authentication & Account Access | Contractual necessity | Backend Database (`User`) | Duration of account lifecycle | Yes |
| **Child Profile** | Profile grouping & age categorization | Verifiable Parental Consent | Backend Database (`Child`) | Duration of account lifecycle | Yes |
| **Installed Apps Inventory** | Application blocking & time allowance | Parental supervision | Backend Database (`AppRestriction`) | Dynamic sync / Overwritten on update | Yes |
| **Usage Telemetry** | Screen time limit calculation | Parental supervision | Backend Database (`ActivityRecord`) | 30 Days (auto-purged) | Yes |
| **Location Coordinates** | Safety Radar & Geofence triggers | Explicit parental consent | Backend Database (`LocationRecord`) | 7 Days (configurable to 24h) | Yes |
| **Geofence Definitions** | Safe zone perimeter alerts | Safety & Protection of Minor | Backend Database (`GeofenceZone`) | Duration of account lifecycle | Yes |
| **Audit Logs** | Security accountability & tampering audit | Legal compliance / Security | Backend Database (`AuditLog`) | 90 Days | Yes |

---

### 5. Ethical Verification Checklist

- [x] Child application displays an ongoing, persistent notification whenever location tracking or background monitoring is active.
- [x] No covert background audio recording, ambient listening, or remote microphone access.
- [x] No covert remote camera capture or secret screen recording.
- [x] No keystroke logging or password capture.
- [x] Emergency dialing (911/112/999) is guaranteed accessible at all times regardless of policy restrictions.
- [x] Child can inspect active policies directly on their own device interface.
- [x] Complete parental data export and instant account erasure mechanisms verified.
