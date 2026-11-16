package org.familyguard.child.data.model

data class ClaimPairingRequest(
    val pairingKey: String,
    val deviceName: String,
    val hardwareModel: String,
    val osVersion: String,
    val deviceOwnerEnrolled: Boolean,
    val fcmToken: String? = null
)

data class ClaimPairingResponse(
    val message: String,
    val deviceAccessToken: String,
    val device: DeviceInfoDto,
    val child: ChildInfoDto,
    val initialPolicy: InitialPolicyDto?
)

data class DeviceInfoDto(
    val id: String,
    val name: String,
    val deviceOwnerEnrolled: Boolean
)

data class ChildInfoDto(
    val id: String,
    val name: String
)

data class InitialPolicyDto(
    val screenTimeSchedule: ScreenTimePolicyDto?
)

data class ScreenTimePolicyDto(
    val id: String,
    val dailyLimitMinutes: Int,
    val weekendLimitMinutes: Int,
    val dailyGamingLimitMinutes: Int = 60,
    val blockGamesDuringSchool: Boolean = true,
    val blockGamesDuringBedtime: Boolean = true,
    val schoolStart: String,
    val schoolEnd: String,
    val bedtimeStart: String,
    val bedtimeEnd: String,
    val isInstantPause: Boolean,
    val bonusMinutesToday: Int
)

data class ActivePolicyResponse(
    val deviceId: String,
    val childId: String,
    val childName: String,
    val screenTime: ScreenTimePolicyDto?,
    val timestamp: String
)

data class InstalledAppItemDto(
    val packageName: String,
    val appName: String,
    val category: String? = null
)

data class SyncInstalledAppsDto(
    val apps: List<InstalledAppItemDto>
)

data class AppUsageRecordDto(
    val packageName: String,
    val appName: String,
    val category: String,
    val durationSeconds: Long,
    val recordedDate: String
)

data class ReportUsageRequest(
    val deviceId: String,
    val records: List<AppUsageRecordDto>
)

data class ReportLocationRequest(
    val deviceId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float? = null,
    val batteryPercentage: Int? = null
)
