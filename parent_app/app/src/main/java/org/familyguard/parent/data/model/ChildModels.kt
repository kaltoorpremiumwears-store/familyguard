package org.familyguard.parent.data.model

data class CreateChildRequest(
    val name: String,
    val birthDate: String,
    val avatarUrl: String? = null
)

data class ChildDto(
    val id: String,
    val parentId: String,
    val name: String,
    val birthDate: String,
    val avatarUrl: String?,
    val devices: List<DeviceSummaryDto>? = emptyList(),
    val screenTimeSchedule: ScreenTimeScheduleDto? = null
)

data class DeviceSummaryDto(
    val id: String,
    val deviceName: String,
    val hardwareModel: String,
    val isOnline: Boolean,
    val batteryLevel: Int?,
    val deviceOwnerEnrolled: Boolean
)

data class ScreenTimeScheduleDto(
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

data class UpdateScreenTimeRequest(
    val dailyLimitMinutes: Int? = null,
    val weekendLimitMinutes: Int? = null,
    val isInstantPause: Boolean? = null,
    val bonusMinutesToday: Int? = null
)

data class AppRestrictionDto(
    val id: String,
    val childId: String,
    val packageName: String,
    val appName: String,
    val category: String,
    val mode: String,
    val dailyLimitMinutes: Int
)

data class UpdateAppRestrictionDto(
    val packageName: String,
    val appName: String,
    val category: String,
    val mode: String,
    val dailyLimitMinutes: Int
)

data class BulkUpdateAppRestrictionsRequest(
    val restrictions: List<UpdateAppRestrictionDto>
)

data class GamingOverviewDto(
    val childId: String,
    val dailyGamingLimitMinutes: Int,
    val blockGamesDuringSchool: Boolean,
    val blockGamesDuringBedtime: Boolean,
    val games: List<AppRestrictionDto>
)

data class UpdateGamingScheduleRequest(
    val dailyGamingLimitMinutes: Int? = null,
    val blockGamesDuringSchool: Boolean? = null,
    val blockGamesDuringBedtime: Boolean? = null
)

data class DailyReportDto(
    val childId: String,
    val childName: String,
    val date: String,
    val totalScreenTimeMinutes: Int,
    val dailyAllowanceMinutes: Int,
    val percentUsed: Int,
    val categories: CategoryBreakdownDto,
    val topApps: List<AppUsageBreakdownDto>
)

data class CategoryBreakdownDto(
    val gamesMinutes: Int,
    val educationMinutes: Int,
    val socialMinutes: Int,
    val entertainmentMinutes: Int,
    val utilityMinutes: Int
)

data class AppUsageBreakdownDto(
    val packageName: String,
    val appName: String,
    val category: String,
    val durationMinutes: Int
)

data class WeeklyTrendsDto(
    val childId: String,
    val childName: String,
    val weeklyAllowanceMinutes: Int,
    val trends: List<DailyTrendItemDto>
)

data class DailyTrendItemDto(
    val date: String,
    val screenTimeMinutes: Int
)

// Web Safety Models
data class WebSafetyConfigDto(
    val id: String,
    val childId: String,
    val safeSearchEnabled: Boolean,
    val blockAdultContent: Boolean,
    val blockGambling: Boolean,
    val blockViolence: Boolean,
    val blockMalicious: Boolean,
    val unapprovedBrowsersBlocked: Boolean,
    val youtubeMode: String,
    val dailyYouTubeLimitMinutes: Int
)

data class WebsiteRuleDto(
    val id: String,
    val childId: String,
    val domain: String,
    val isBlocked: Boolean,
    val category: String
)

data class WebSafetyOverviewDto(
    val config: WebSafetyConfigDto,
    val websiteRules: List<WebsiteRuleDto>,
    val managedChromePolicy: Map<String, Any>?
)

data class UpdateWebSafetyRequest(
    val safeSearchEnabled: Boolean? = null,
    val blockAdultContent: Boolean? = null,
    val blockGambling: Boolean? = null,
    val blockViolence: Boolean? = null,
    val blockMalicious: Boolean? = null,
    val unapprovedBrowsersBlocked: Boolean? = null,
    val youtubeMode: String? = null,
    val dailyYouTubeLimitMinutes: Int? = null
)

data class AddWebsiteRuleRequest(
    val domain: String,
    val isBlocked: Boolean,
    val category: String = "CUSTOM"
)

// YouTube Management Models
data class YouTubeApprovedItemDto(
    val id: String,
    val childId: String,
    val type: String, // CHANNEL, VIDEO
    val externalId: String,
    val title: String,
    val description: String?,
    val thumbnailUrl: String?,
    val isWhitelisted: Boolean
)

data class CuratedChannelDto(
    val channelId: String,
    val title: String,
    val category: String,
    val description: String,
    val thumbnailUrl: String
)

data class YouTubeLibraryOverviewDto(
    val childId: String,
    val youtubeMode: String,
    val dailyLimitMinutes: Int,
    val approvedItems: List<YouTubeApprovedItemDto>,
    val curatedSuggestions: List<CuratedChannelDto>
)

data class AddYouTubeItemRequest(
    val type: String,
    val externalId: String,
    val title: String,
    val description: String? = null,
    val thumbnailUrl: String? = null
)

// Location Radar & Geofence Models
data class LocationFixDto(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val batteryPercentage: Int?,
    val recordedAt: String,
    val isOnline: Boolean
)

data class GeofenceZoneDto(
    val id: String,
    val childId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
    val notifyOnExit: Boolean,
    val notifyOnEnter: Boolean
)

data class LocationOverviewDto(
    val childId: String,
    val childName: String,
    val latestFix: LocationFixDto?,
    val geofences: List<GeofenceZoneDto>
)

data class CreateGeofenceRequest(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 200f,
    val notifyOnExit: Boolean = true,
    val notifyOnEnter: Boolean = true
)

// Notification Models
data class NotificationDto(
    val id: String,
    val parentId: String,
    val childId: String?,
    val type: String,
    val title: String,
    val message: String,
    val isRead: Boolean,
    val createdAt: String
)
