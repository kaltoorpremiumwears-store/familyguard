# Feasibility Study & Privacy Impact Assessment
## Shared-Device Profile Switching & Biometric Facial Recognition in Parental Control Systems

---

### 1. Executive Summary

This study evaluates two complex technical frontiers in modern parental control architecture:
1. **Shared-Device Profile Switching**: Supporting multiple children (or parent + child) sharing a single Android hardware device.
2. **Biometric Face Recognition**: Evaluating the technical feasibility, regulatory compliance, and ethical safety of facial identification for automatic user switching.

**Key Finding**: While native Android Enterprise Multi-User architecture provides a robust, compliant path for shared tablet environments, continuous camera-based biometric facial recognition is **strongly advised against** due to severe regulatory penalties under COPPA, GDPR Article 9, and BIPA, coupled with Android TEE architectural constraints and extreme battery drain.

---

### 2. Part I: Shared-Device Profile Switching Feasibility Study

#### A. The Real-World Challenge
In many households, siblings of different ages (e.g., an 8-year-old and a 14-year-old) share a single family tablet. Each child requires distinct policies:
- The 8-year-old requires strict 60-minute screen limits, aggressive web filtering, and SafeTube whitelisting.
- The 14-year-old requires expanded educational access, 3-hour limits, and selective social media allowances.

#### B. Architectural Comparison: Android Multi-User vs. App-Level Persona Switching

```
+─────────────────────────────────────────+─────────────────────────────────────────+
|   APPROACH A: NATIVE OS MULTI-USER      |     APPROACH B: IN-APP PERSONA SWAP     |
|   (Android Enterprise DPC Managed)      |          (PIN-Gated Session)            |
+─────────────────────────────────────────+─────────────────────────────────────────+
| • Uses UserManager & dpm.createAnd-     | • Single Android user account.          |
|   ManageUser() to spawn isolated OS     | • Child taps their avatar and inputs a  |
|   profiles.                             |   personal 4-digit PIN or pattern.      |
| • Complete cryptographic app & data     | • DPC dynamically reconfigures policy:  |
|   isolation between siblings.           |   suspends/resumes apps on the fly.     |
| • Each user has their own launcher,     | • Works uniformly across ALL Android    |
|   storage, and app state.               |   devices, including budget smartphones.|
| • Limitation: Heavily disabled by OEMs  | • Limitation: Sibling A can see Sibling |
|   on budget smartphones (MIUI/ColorOS). |   B's app login state unless apps lock. |
+─────────────────────────────────────────+─────────────────────────────────────────+
```

#### C. Recommended Production Implementation Strategy: Hybrid Dual-Engine
1. **Tablet Form-Factors (API 28+)**:
   - DPC inspects `UserManager.supportsMultipleUsers()`.
   - If `true`, the Device Owner provisions managed secondary users via `dpm.createAndManageUser()`.
   - Switching occurs at the system lock screen or via `dpm.switchUser()`.
2. **Smartphone Form-Factors & Budget Devices**:
   - If multi-user is disabled by the OEM, the Child App employs **Session-Based Persona Switching**:
     * An always-accessible PIN switcher allows the active child to sign in.
     * The background engine swaps the active policy matrix (`setPackagesSuspended`, `setApplicationRestrictions`) within 800ms of authentication.
     * When session expires, device locks into a neutral "Family Safe Mode".

---

### 3. Part II: Biometric Face Recognition & Privacy Impact Assessment

#### A. Technical & Hardware Constraints in the Android Security Framework
Modern Android platforms enforce strict hardware-isolated biometric security (Trusted Execution Environment / TEE and StrongBox Keymaster):
1. **Authentication vs. Identification**:
   - Android's native `BiometricPrompt` API is strictly **1-to-1 Authentication** ("Does this face match the single biometric template enrolled in device settings?").
   - Android **never provides 1-to-N Identification** ("Which of the three enrolled children is looking at the screen?").
2. **TEE Sandbox Isolation**:
   - Third-party applications are strictly forbidden from accessing raw camera biometric templates, facial feature vectors, or sensor data.
   - Any attempt to build custom facial identification requires capturing raw camera frames via `Camera2` or `CameraX` API.

#### B. Regulatory & Legal Liabilities of Camera-Based Facial Recognition

| Regulatory Body | Legal Mandate | Compliance Impact on Child Facial Recognition |
| :--- | :--- | :--- |
| **COPPA** (USA) | 15 U.S.C. §§ 6501 | Biometric identifiers of children under 13 are classified as sensitive personal data. Continuous background scanning without verifiable affirmative parental consent carries penalties exceeding **$50,120 per violation**. |
| **GDPR Article 9** (EU/UK) | Special Categories of Data | Processing of biometric data for uniquely identifying a natural person is **expressly prohibited** unless an explicit Article 9(2) exception applies. For minors, the proportionality and necessity tests cannot be satisfied when less invasive methods (PIN/Passcode) exist. |
| **BIPA** (Illinois, USA) | 740 ILCS 14/ | Prohibits collection of biometric identifiers without written release and strict retention schedules. Major tech firms have faced nine-figure class-action settlements over facial geometry scanning. |
| **Google Play Families Policy** | Families Policy Requirements | Explicitly bans covert camera activation, background camera access, and invasive biometric tracking of children. |

#### C. Technical Pitfalls of Custom On-Device ML Facial Scanning
Even if evaluated as an opt-in research prototype using on-device ML (e.g. TensorFlow Lite / MediaPipe Face Mesh):
1. **Severe Battery & Thermal Drain**: Continuous or periodic background camera activation triggers camera sensor power rails and neural processing units, reducing battery life by 60–80%.
2. **High False Acceptance Rates (FAR) Among Siblings**: Pre-pubescent children share high facial geometry similarity. Commercial ML models exhibit unacceptable error rates when distinguishing siblings aged 6 to 10.
3. **Spoofing Vulnerability**: Simple 2D camera feeds are vulnerable to photo presentation attacks (holding up a printed photo or tablet screen of a sibling).

---

### 4. Architectural Recommendation & Conclusion

```
+─────────────────────────────────────────────────────────────────────────────────────────────+
|                                    STRATEGIC DECISION                                       |
+─────────────────────────────────────────────────────────────────────────────────────────────+
|                                                                                             |
|   1. DO NOT implement continuous background camera facial recognition.                      |
|      Reason: Extreme legal liability, Google Play policy violations, battery exhaustion,     |
|      and ethical betrayal of consent-based parental principles.                             |
|                                                                                             |
|   2. IMPLEMENT Managed Multi-User Profiles for Android Tablets.                             |
|      Reason: Uses official Android Enterprise DPM APIs, ensures full OS sandboxing,         |
|      and carries zero privacy risk.                                                         |
|                                                                                             |
|   3. IMPLEMENT PIN / Pattern Persona Switching for Smartphones.                             |
|      Reason: Lightweight, 100% reliable, zero battery drain, and universally compatible         |
|      across all Android OEMs.                                                               |
|                                                                                             |
+─────────────────────────────────────────────────────────────────────────────────────────────+
```
