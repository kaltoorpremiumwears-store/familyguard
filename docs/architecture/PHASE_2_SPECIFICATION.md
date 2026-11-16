# Phase 2 Specification & Architecture Review
## Managed-Device Enrollment, Android Enterprise Device Owner Provisioning & Real-Time Sync

### 1. Overview
Phase 2 establishes the Android Enterprise foundation and real-time bidirectional synchronization pipeline between the Central Backend, Parent Dashboard, and Child Device Policy Controller (DPC).

---

### 2. Core Capabilities Implemented

```
+─────────────────────────────────────────────────────────────────────────────────────────────+
|                                    PHASE 2 ARCHITECTURE                                     |
+─────────────────────────────────────────────────────────────────────────────────────────────+
                                                │
             ┌──────────────────────────────────┴──────────────────────────────────┐
             ▼                                                                     ▼
  ┌─────────────────────┐                                               ┌─────────────────────┐
  │     PARENT APP      │                                               │      CHILD APP      │
  │                     │                                               │                     │
  │ • Enterprise 6-Tap  │                                               │ • DeviceOwner (DPC) │
  │   QR Generator      │       Bidirectional WebSocket & REST Sync     │ • DpmWrapper (Lock, │
  │ • ADB Provisioning  │ ◄───────────────────────────────────────────► │   Suspend, Time)    │
  │   Quick Setup       │                                               │ • Foreground Sync   │
  │ • Real-time Actions │                                               │   Service           │
  └──────────┬──────────┘                                               └──────────┬──────────┘
             │                                                                     │
             └──────────────────────────────────┬──────────────────────────────────┘
                                                ▼
                                    ┌───────────────────────┐
                                    │    CENTRAL BACKEND    │
                                    │                       │
                                    │ • Socket.IO Gateway   │
                                    │ • Device Integrity API│
                                    │ • Instant Policy Push │
                                    └───────────────────────┘
```

#### A. Android Enterprise Device Owner Provisioning Engine
- **Method 1: Zero-Touch 6-Tap QR Provisioning** (`GET /api/v1/enrollment/enterprise-qr`):
  Generates standard Google Android Enterprise provisioning JSON payload containing:
  * `android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME`: `"org.familyguard.child/.dpc.ParentalDpcReceiver"`
  * `android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED`: `true`
  * Admin extras bundle carrying `server_url` and `family_code`.
- **Method 2: ADB Shell Provisioning Scripts**:
  * [provision_device_owner.ps1](file:///d:/New%20Project%20App/scripts/provision_device_owner.ps1) (PowerShell)
  * [provision_device_owner.sh](file:///d:/New%20Project%20App/scripts/provision_device_owner.sh) (Bash)
  * Script verifies connected devices, detects existing accounts, installs APK, and runs `adb shell dpm set-device-owner`.

#### B. Child App Device Policy Controller Engine
- **`DevicePolicyManagerWrapper.kt`**:
  * `suspendPackages(packages: List<String>, suspend: Boolean)`: Suspends target apps using native `DevicePolicyManager.setPackagesSuspended()`.
  * `isExemptPackage(packageName: String)`: Guaranteed safety rule that Telecom dialer (`TelecomManager.defaultDialerPackage`), emergency SOS (`911`/`112`), and system UI are **NEVER** locked or suspended.
  * `enforceNetworkTime()`: Locks device clock to network time, preventing children from advancing device time to bypass daily caps.
  * `blockAppUninstall(true)`: Blocks app uninstallation by children or unauthorized third parties.
- **`PolicySyncService.kt`**:
  * Runs as an Android compliant foreground service (`specialUse` subtype).
  * Displays persistent, non-dismissible notification meeting Google Play transparency & anti-stalkerware rules.
  * Synchronizes active policy diffs and enforces instant pauses.
- **`LocalPolicyStore.kt`**:
  * Local cache ensuring policies, timers, and restriction schedules persist across device reboots and offline periods.

#### C. Backend Real-Time Events Gateway (`backend/src/realtime/`)
- Socket.IO gateway running on `/ws/events`.
- Room isolation: `parent_<parentId>` and `child_<childId>`.
- Real-time broadcasts:
  * `instant_pause_command`: Broadcasts instant pause toggles directly to child devices.
  * `policy_updated`: Pushes schedule updates immediately without polling lag.
  * `device_heartbeat`: Ingests live battery level and management integrity reports.
- `EnrollmentService` & `EnrollmentController`:
  * Generates enterprise QR codes.
  * Ingests and verifies hardware integrity reports (`verifyIntegrity`).
  * Logs security audit events when tampering or root is detected.

#### D. Parent App Provisioning Studio (`parent_app/`)
- **`ProvisioningGuideScreen.kt`**:
  * Tabbed interface offering visual 6-tap QR setup or 1-click copy-able ADB test commands.
  * Integrated directly into Parent Dashboard via top navigation.
