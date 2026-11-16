package org.familyguard.child.data.api

import org.familyguard.child.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ChildApiService {

    @POST("api/v1/pairing/claim")
    suspend fun claimPairing(@Body request: ClaimPairingRequest): Response<ClaimPairingResponse>

    @GET("api/v1/policies/device/sync")
    suspend fun syncPolicies(@Header("Authorization") deviceToken: String): Response<ActivePolicyResponse>

    @POST("api/v1/apps/sync/{childId}")
    suspend fun syncInstalledApps(
        @Header("Authorization") token: String,
        @Path("childId") childId: String,
        @Body request: SyncInstalledAppsDto
    ): Response<Map<String, Any>>

    @POST("api/v1/telemetry/usage/{childId}")
    suspend fun reportUsage(
        @Header("Authorization") token: String,
        @Path("childId") childId: String,
        @Body request: ReportUsageRequest
    ): Response<Map<String, Any>>

    @POST("api/v1/location/report/{childId}")
    suspend fun reportLocation(
        @Header("Authorization") token: String,
        @Path("childId") childId: String,
        @Body request: ReportLocationRequest
    ): Response<Map<String, Any>>
}
