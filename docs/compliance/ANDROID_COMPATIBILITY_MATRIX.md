# Android Compatibility Matrix & Device Management Architecture
## Android 9 (API 28) Through Android 14 / 15 (API 34 / 35)

### 1. Architectural Scope & API Version Overview

The FamilyGuard Parental Control Ecosystem is engineered to operate strictly within official Android Enterprise Device Owner (DO) specifications, avoiding non-standard, fragile accessibility hacks or hidden reflection calls.

| Android Version | API Level | DPC Mode | Key Parental APIs Supported | Known OS Enforcements & Mitigations |
| :--- | :--- | :--- | :--- | :--- |
| **Android 9 (Pie)** | 28 | Device Owner / Device Admin | `setPackagesSuspended`, `addUserRestriction`, `setApplicationRestrictions` | Basic background execution limits; JobScheduler recommended. |
| **Android 10 (Q)** | 29 | Device Owner | Scoped Storage introduction, `ACCESS_BACKGROUND_LOCATION` introduced | Background location requires explicit two-step user permission or Device Owner automated grant via `setPermissionGrantState`. |
| **Android 11 (R)** | 30 | Device Owner | Package Visibility (`<queries>` tag in manifest mandatory), Fine/Coarse location separation | Child DPC must declare `<queries>` for all installed apps or use `DevicePolicyManager.getInstalledApplications()` (exempt from package visibility). |
| **Android 12 / 12L (S)**| 31 / 32 | Device Owner | Exact Alarms restriction (`SCHEDULE_EXACT_ALARM`), Foreground Service start restrictions from background | Foreground services (`PolicySyncService`, `LocationTrackerService`) must be initiated while in foreground or triggered via DPC device admin event callbacks. |
| **Android 13 (Tiramisu)** | 33 | Device Owner | Runtime Notification Permission (`POST_NOTIFICATIONS`) | Device Owner automatically pre-grants `POST_NOTIFICATIONS` to itself to maintain continuous child transparency notification without dismissal. |
| **Android 14 (Upside Down Cake)** | 34 | Device Owner | Mandatory Foreground Service Types (`foregroundServiceType="specialUse"` / `"location"`), Broadcast receiver export flags | Must explicitly declare `RECEIVER_NOT_EXPORTED` in dynamic receivers. Specific `foregroundServiceType` tags declared in AndroidManifest. |
| **Android 15 (Vanilla Ice Cream)** | 35 | Device Owner | Private Space isolation, stricter background execution | Device Owner policies automatically propagate across default and restricted secondary profiles. |

---

### 2. Android Enterprise Device Owner vs. Legacy Accessibility Hacks

```
+───────────────────────────────────+───────────────────────────────────────────────────+
|      LEGACY / HACK APPROACH       |       FAMILYGUARD ENTERPRISE DEVICE OWNER         |
+───────────────────────────────────+───────────────────────────────────────────────────+
| • Relies on AccessibilityService  | • Uses DevicePolicyManager.setPackagesSuspended()  |
|   to scrape on-screen package name|   directly in the Android OS framework.           |
| • Severely penalized by Google    | • Officially supported by Google Enterprise API.  |
|   Play Policies (frequent bans).  | • 100% Google Play Policy compliant.              |
| • Constant battery drain due to   | • Zero idle CPU/battery drain (handled at the     |
|   continuous event polling.       |   system package manager level).                  |
| • Trivial for children to bypass  | • Impossible to bypass or uninstall without       |
|   in Accessibility Settings.      |   parent authorization.                           |
| • Laggy overlay screens with      | • Clean native Android OS dialog: "This app has   |
|   flickering visual glitches.     |   been suspended by your administrator".          |
+───────────────────────────────────+───────────────────────────────────────────────────+
```

---

### 3. Manufacturer-Specific Battery Optimization ("Battery Killers")

Many Android OEMs (notably Xiaomi/MIUI/HyperOS, Huawei/EMUI, Samsung/OneUI, Oppo/ColorOS) implement aggressive proprietary battery-saving daemons that kill background services.

#### OEM Mitigation Strategies:
1. **Device Owner Exemption**:
   When provisioned as **Device Owner**, the application is treated as a managed system policy controller. The OS exempts the DPC from aggressive task killing.
2. **Battery Optimization Whitelist**:
   The Child App invokes:
   ```kotlin
   val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
       data = Uri.parse("package:$packageName")
   }
   context.startActivity(intent)
   ```
   Under Device Owner mode, the DPC can programmatically verify battery exemption.
3. **Foreground Services with Sticky Notifications**:
   Both `PolicySyncService` and `LocationTrackerService` invoke `startForeground()` within 5 seconds of lifecycle start with non-dismissible notifications (`FLAG_ONGOING_EVENT`), ensuring Linux OOM-killer priority remains equivalent to a foreground application.

---

### 4. Emergency Exemption Architecture (Mandatory Safety Rule)

Android Enterprise policy requires that critical device capabilities remain operational under all circumstances:
1. **Emergency Call Exemption**:
   `EmergencyCallExemptions.isExempt(packageName)` strictly exempts `com.android.phone`, `com.google.android.dialer`, `com.android.server.telecom`, and emergency dialer intents from suspension.
2. **SMS & SOS Exemption**:
   Carrier default emergency SMS services and SOS alert features cannot be suspended.
3. **Settings Exemption**:
   While `UserManager.DISALLOW_CONFIG_BLUETOOTH` or `DISALLOW_SAFE_BOOT` may be applied, fundamental system access is maintained to permit legal parental compliance.
