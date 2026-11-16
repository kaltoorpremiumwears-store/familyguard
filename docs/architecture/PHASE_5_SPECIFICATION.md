# Phase 5 Specification & Implementation Review
## Location Radar, Geofencing, OpenStreetMap Integration & Centralized Notifications

### 1. Overview
Phase 5 implements a transparent, battery-aware, consent-driven Location Management & Geofencing System using 100% free, open-source cartography (OpenStreetMap + Leaflet.js), coupled with a real-time Centralized Notification & Audit System.

---

### 2. Architectural Blueprint

```
+─────────────────────────────────────────────────────────────────────────────────────────────+
|                                    PHASE 5 ARCHITECTURE                                     |
+─────────────────────────────────────────────────────────────────────────────────────────────+
                                                │
             ┌──────────────────────────────────┴──────────────────────────────────┐
             ▼                                                                     ▼
  ┌─────────────────────┐                                               ┌─────────────────────┐
  │     PARENT APP      │                                               │      CHILD APP      │
  │                     │                                               │                     │
  │ • Location Radar    │                                               │ • LocationTracker   │
  │   - OpenStreetMap   │         Real-Time Push & Geofence Sync        │   Service (GPS/Net) │
  │   - Leaflet.js Map  │ ◄───────────────────────────────────────────► │ • Battery-Aware     │
  │   - Geofence Editor │                                               │   Adaptive Interval │
  │ • Notification Hub  │                                               │ • Transparent Sticky│
  │   - Alerts & Audit  │                                               │   Foreground Notif  │
  └──────────┬──────────┘                                               └──────────┬──────────┘
             │                                                                     │
             └──────────────────────────────────┬──────────────────────────────────┘
                                                ▼
                                    ┌───────────────────────┐
                                    │    CENTRAL BACKEND    │
                                    │                       │
                                    │ • Location Logs & API │
                                    │ • Haversine Geofence  │
                                    │ • Boundary Transitions│
                                    │ • Push Notifications  │
                                    │ • WebSocket Gateway   │
                                    └───────────────────────┘
```

---

### 3. Core Modules & Implementation Details

#### A. Child Location Tracker Service (`LocationTrackerService.kt`)
- **Battery-Aware Strategy**: Dynamically adjusts GPS and Network polling intervals based on battery percentage and charging state (`ACTION_BATTERY_CHANGED` broadcast receiver):
  - Normal (>20%): 3-minute poll interval, 30m minimum distance delta.
  - Low Battery (<=20%): 10-minute poll interval, 100m minimum distance delta.
  - Charging: 1-minute high-frequency mode.
- **Transparency & Legal Mandates**:
  - Bound to foreground service with visible persistent notification: `"FamilyGuard Active Location - Sharing location with parent"`.
  - Android 14 compliant with `android:foregroundServiceType="location"`.
- **Dual-Provider Fallback**: Requests updates from both `LocationManager.GPS_PROVIDER` and `LocationManager.NETWORK_PROVIDER` for fast TTFF (Time-to-First-Fix).

#### B. Free & Open-Source Location Radar (`LocationRadarScreen.kt`)
- **Zero Google Maps Billing / API Keys**:
  - Uses an Android `WebView` rendering **Leaflet 1.9.4** and **OpenStreetMap Carto tiles** (`https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png`).
  - Native Kotlin-to-JavaScript bridge handles live updates (`L.circle`, `L.marker`, and accuracy rings).
- **Interactive Geofence Management**:
  - Parents can visually view registered safe zones (Home, School, Grandma's House, Park) rendered as colored boundary rings.
  - Modal bottom sheet to create new circular geofence zones by specifying Latitude, Longitude, Radius (meters), and Alert triggers (ENTER / EXIT).

#### C. Backend Geofencing Engine & Haversine Distance Calculation (`location.service.ts`)
- **Spherical Haversine Algorithm**:
  Computes precise great-circle distance between child coordinates and all registered geofence centroids:
  $$\Delta\sigma = 2 \arcsin \left( \sqrt{\sin^2\left(\frac{\Delta\phi}{2}\right) + \cos(\phi_1)\cos(\phi_2)\sin^2\left(\frac{\Delta\lambda}{2}\right)} \right)$$
  $$d = R \cdot \Delta\sigma$$
- **Transition Detection**:
  - Evaluates whether previous state was `INSIDE` or `OUTSIDE`.
  - Emits real-time notifications on entry or departure:
    * `"GEOFENCE_EXIT"`: Alert indicating the child departed a safe zone.
    * `"GEOFENCE_ENTER"`: Confirmation that child arrived safely at school/home.

#### D. Centralized Notification Center (`notifications.service.ts`, `NotificationsCenterScreen.kt`)
- **Unified Alert Categories**:
  - `GEOFENCE_ENTER` / `GEOFENCE_EXIT`
  - `TAMPER_ATTEMPT` / `PERMISSION_REVOKED`
  - `SCREEN_TIME_EXPIRED` / `GAMING_LIMIT_REACHED`
  - `BLOCKED_APP_OPENED` / `UNAPPROVED_BROWSER`
- **Real-Time Push**: Delivered instantly to Parent App UI via WebSockets (`EventsGateway`) and stored in database for historical audit logs.
- **Actionable UI**: Dismiss, Mark All Read, and category filtering badges.

---

### 4. Verification & Testing Evidence
- **Backend Unit Tests**: 9 test suites passing (21 tests) covering location logging, geofence boundary math, notifications broadcast, and auth.
- **Backend Build**: Clean compilation with `npm run build` and `nest build`.
- **Parent & Child App Integration**: Services declared in AndroidManifest, UI linked into `DashboardScreen` and `MainActivity` routes.
