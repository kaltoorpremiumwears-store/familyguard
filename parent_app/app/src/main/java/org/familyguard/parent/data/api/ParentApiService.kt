package org.familyguard.parent.data.api

import org.familyguard.parent.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ParentApiService {

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("api/v1/children")
    suspend fun getChildren(@Header("Authorization") token: String): Response<List<ChildDto>>

    @POST("api/v1/children")
    suspend fun createChild(
        @Header("Authorization") token: String,
        @Body request: CreateChildRequest
    ): Response<ChildDto>

    @POST("api/v1/pairing/generate")
    suspend fun generatePairing(
        @Header("Authorization") token: String,
        @Body request: GeneratePairingRequest
    ): Response<PairingResponse>

    @GET("api/v1/pairing/status/{pairingId}")
    suspend fun getPairingStatus(
        @Header("Authorization") token: String,
        @Path("pairingId") pairingId: String
    ): Response<PairingStatusResponse>

    @PUT("api/v1/policies/child/{childId}/screentime")
    suspend fun updateScreenTime(
        @Header("Authorization") token: String,
        @Path("childId") childId: String,
        @Body request: UpdateScreenTimeRequest
    ): Response<Map<String, Any>>

    // Application Management
    @GET("api/v1/apps/{childId}")
    suspend fun getAppRestrictions(
        @Header("Authorization") token: String,
        @Path("childId") childId: String
    ): Response<List<AppRestrictionDto>>

    @PUT("api/v1/apps/{childId}/restrictions")
    suspend fun updateAppRestrictions(
        @Header("Authorization") token: String,
        @Path("childId") childId: String,
        @Body request: BulkUpdateAppRestrictionsRequest
    ): Response<Map<String, Any>>

    // Gaming Control Center
    @GET("api/v1/apps/{childId}/gaming")
    suspend fun getGamingOverview(
        @Header("Authorization") token: String,
        @Path("childId") childId: String
    ): Response<GamingOverviewDto>

    @PUT("api/v1/apps/{childId}/gaming/schedule")
    suspend fun updateGamingSchedule(
        @Header("Authorization") token: String,
        @Path("childId") childId: String,
        @Body request: UpdateGamingScheduleRequest
    ): Response<Map<String, Any>>

    // Telemetry & Activity Reports
    @GET("api/v1/telemetry/reports/{childId}/daily")
    suspend fun getDailyReport(
        @Header("Authorization") token: String,
        @Path("childId") childId: String,
        @Query("date") date: String? = null
    ): Response<DailyReportDto>

    @GET("api/v1/telemetry/reports/{childId}/weekly")
    suspend fun getWeeklyTrends(
        @Header("Authorization") token: String,
        @Path("childId") childId: String
    ): Response<WeeklyTrendsDto>

    // Web & Browser Filtering
    @GET("api/v1/content/{childId}/web-safety")
    suspend fun getWebSafety(
        @Header("Authorization") token: String,
        @Path("childId") childId: String
    ): Response<WebSafetyOverviewDto>

    @PUT("api/v1/content/{childId}/web-safety")
    suspend fun updateWebSafety(
        @Header("Authorization") token: String,
        @Path("childId") childId: String,
        @Body request: UpdateWebSafetyRequest
    ): Response<Map<String, Any>>

    @POST("api/v1/content/{childId}/website-rules")
    suspend fun addWebsiteRule(
        @Header("Authorization") token: String,
        @Path("childId") childId: String,
        @Body request: AddWebsiteRuleRequest
    ): Response<Map<String, Any>>

    @DELETE("api/v1/content/{childId}/website-rules/{ruleId}")
    suspend fun deleteWebsiteRule(
        @Header("Authorization") token: String,
        @Path("childId") childId: String,
        @Path("ruleId") ruleId: String
    ): Response<Map<String, Any>>

    // YouTube Content Management
    @GET("api/v1/youtube/{childId}/approved-library")
    suspend fun getYouTubeLibrary(
        @Header("Authorization") token: String,
        @Path("childId") childId: String
    ): Response<YouTubeLibraryOverviewDto>

    @POST("api/v1/youtube/{childId}/approved-library")
    suspend fun addYouTubeItem(
        @Header("Authorization") token: String,
        @Path("childId") childId: String,
        @Body request: AddYouTubeItemRequest
    ): Response<Map<String, Any>>

    @DELETE("api/v1/youtube/{childId}/approved-library/{externalId}")
    suspend fun removeYouTubeItem(
        @Header("Authorization") token: String,
        @Path("childId") childId: String,
        @Path("externalId") externalId: String
    ): Response<Map<String, Any>>

    // Location Radar & Geofencing
    @GET("api/v1/location/latest/{childId}")
    suspend fun getLatestLocation(
        @Header("Authorization") token: String,
        @Path("childId") childId: String
    ): Response<LocationOverviewDto>

    @POST("api/v1/location/geofences/{childId}")
    suspend fun createGeofence(
        @Header("Authorization") token: String,
        @Path("childId") childId: String,
        @Body request: CreateGeofenceRequest
    ): Response<GeofenceZoneDto>

    @DELETE("api/v1/location/geofences/{childId}/{geofenceId}")
    suspend fun deleteGeofence(
        @Header("Authorization") token: String,
        @Path("childId") childId: String,
        @Path("geofenceId") geofenceId: String
    ): Response<Map<String, Any>>

    // Notifications Center
    @GET("api/v1/notifications")
    suspend fun getNotifications(
        @Header("Authorization") token: String
    ): Response<List<NotificationDto>>

    @PUT("api/v1/notifications/{id}/read")
    suspend fun markNotificationRead(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<NotificationDto>

    @PUT("api/v1/notifications/read-all")
    suspend fun markAllNotificationsRead(
        @Header("Authorization") token: String
    ): Response<Map<String, Any>>
}
