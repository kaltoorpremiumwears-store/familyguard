# Phase 3 Specification & Implementation Review
## Screen Time Engine, App Governance, Dedicated Gaming Vault & Activity Reports

### 1. Overview
Phase 3 delivers granular application management, dedicated gaming controls, automated schedule curfew enforcement, foreground telemetry collection, and aggregated activity analytics.

---

### 2. Core Capabilities Implemented

```
+─────────────────────────────────────────────────────────────────────────────────────────────+
|                                    PHASE 3 ARCHITECTURE                                     |
+─────────────────────────────────────────────────────────────────────────────────────────────+
                                                │
             ┌──────────────────────────────────┴──────────────────────────────────┐
             ▼                                                                     ▼
  ┌─────────────────────┐                                               ┌─────────────────────┐
  │     PARENT APP      │                                               │      CHILD APP      │
  │                     │                                               │                     │
  │ • App Management    │                                               │ • UsageStatsManager │
  │   (Allow/Block/Cap) │       Real-Time Push & Telemetry Ingestion    │   (Foregnd Tracking)│
  │ • Gaming Vault      │ ◄───────────────────────────────────────────► │ • Automatic Curfew  │
  │   (Vault + Curfew)  │                                               │   Enforcement       │
  │ • Activity Reports  │                                               │ • Inventory Scan    │
  │   (Daily & 7-Day)   │                                               │ • Safe Emergency SOS│
  └──────────┬──────────┘                                               └──────────┬──────────┘
             │                                                                     │
             └──────────────────────────────────┬──────────────────────────────────┘
                                                ▼
                                    ┌───────────────────────┐
                                    │    CENTRAL BACKEND    │
                                    │                       │
                                    │ • Apps & Gaming API   │
                                    │ • Daily & Weekly Agg  │
                                    │ • Category Breakdown  │
                                    └───────────────────────┘
```

#### A. Application Management Module
- **Installed Inventory Synchronization**:
  * Child device scans launchable non-system applications and automatically uploads package names and app labels to `POST /api/v1/apps/sync/:childId`.
  * Auto-categorizes apps into `GAMES`, `SOCIAL`, `EDUCATION`, `ENTERTAINMENT`, and `UTILITY`.
- **Granular Modes**:
  * `ALLOWED`: Unlimited usage within overall screen time allowance.
  * `BLOCKED`: Immediately greyscaled and suspended via `DevicePolicyManager.setPackagesSuspended()`.
  * `TIME_LIMITED`: Enforces an individual daily time cap (e.g. 30 minutes for Roblox).
- **Exemptions**:
  * Educational apps can be marked as exempt to encourage study.
  * Telecom dialer, SMS, and emergency numbers (911/112) are hard-coded as **never suspensible**.

#### B. Dedicated Gaming Control Module (Section 4.5)
- **Shared Gaming Vault**:
  * Parents configure a shared daily gaming pool (e.g. 60 minutes total for all games combined).
  * Child app tracks cumulative gaming usage; when the shared bucket is exhausted, all games are suspended simultaneously.
- **Gaming Curfew Rules**:
  * `blockGamesDuringSchool`: Automatically suspends games during school hours (08:30–15:00).
  * `blockGamesDuringBedtime`: Automatically suspends games during bedtime (21:00–07:00).
- **Per-Game Limits**:
  * Parents can configure individual caps per game (e.g. Game A: 30m, Game B: 45m).

#### C. Foreground Usage Tracking & Activity Reports
- **`UsageTrackingManager.kt`**:
  * Uses Android `UsageStatsManager` with `PACKAGE_USAGE_STATS`.
  * Collects exact foreground execution durations without intercepting keystrokes, messages, or screen contents.
  * Regularly evaluates active policies and applies DPM suspensions.
- **`TelemetryService.ts`**:
  * Ingests usage records via `POST /api/v1/telemetry/usage/:childId`.
  * Calculates daily screen time totals, category breakdowns, and top used apps.
  * Aggregates 7-day weekly usage trends.
- **`ActivityReportsScreen.kt`**:
  * Visualizes daily progress against cap, category percentages, top apps leaderboard, and 7-day trend bars.
