package org.familyguard.child.inventory

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.familyguard.child.data.api.ChildApiClient
import org.familyguard.child.data.model.InstalledAppItemDto
import org.familyguard.child.data.model.SyncInstalledAppsDto

class AppInventoryManager(private val context: Context) {

    /**
     * Scans installed applications on the device and syncs inventory with backend.
     */
    suspend fun scanAndSyncInventory(childId: String): Boolean = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        val appList = mutableListOf<InstalledAppItemDto>()

        for (app in apps) {
            // Include launchable user applications or common non-system utilities
            val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystem = (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            val launchIntent = pm.getLaunchIntentForPackage(app.packageName)

            if (launchIntent != null || !isSystem || isUpdatedSystem) {
                val label = pm.getApplicationLabel(app).toString()
                val category = guessCategory(app.packageName)
                appList.add(
                    InstalledAppItemDto(
                        packageName = app.packageName,
                        appName = label,
                        category = category
                    )
                )
            }
        }

        try {
            val token = ChildApiClient.deviceAccessToken ?: return@withContext false
            val response = ChildApiClient.service.syncInstalledApps(
                token = token,
                childId = childId,
                request = SyncInstalledAppsDto(apps = appList)
            )
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload app inventory: ${e.message}")
            false
        }
    }

    private fun guessCategory(packageName: String): String {
        val pkg = packageName.lowercase()
        return when {
            pkg.contains("game") || pkg.contains("roblox") || pkg.contains("minecraft") || pkg.contains("clash") -> "GAMES"
            pkg.contains("duolingo") || pkg.contains("khan") || pkg.contains("learn") -> "EDUCATION"
            pkg.contains("instagram") || pkg.contains("tiktok") || pkg.contains("snapchat") || pkg.contains("whatsapp") -> "SOCIAL"
            pkg.contains("youtube") || pkg.contains("netflix") || pkg.contains("music") -> "ENTERTAINMENT"
            else -> "UTILITY"
        }
    }

    companion object {
        private const val TAG = "AppInventoryManager"
    }
}
