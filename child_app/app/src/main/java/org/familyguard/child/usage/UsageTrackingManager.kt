package org.familyguard.child.usage

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log
import org.familyguard.child.data.storage.LocalPolicyStore
import org.familyguard.child.dpc.DevicePolicyManagerWrapper
import java.text.SimpleDateFormat
import java.util.*

class UsageTrackingManager(private val context: Context) {

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    private val policyStore = LocalPolicyStore(context)
    private val dpmWrapper = DevicePolicyManagerWrapper(context)

    /**
     * Checks if PACKAGE_USAGE_STATS permission has been granted.
     */
    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Collects foreground app usage statistics for today.
     */
    fun getTodayUsageStats(): Map<String, Long> {
        if (!hasUsageStatsPermission() || usageStatsManager == null) {
            Log.w(TAG, "UsageStats permission not granted.")
            return emptyMap()
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: return emptyMap()

        val usageMap = mutableMapOf<String, Long>()
        for (stat in stats) {
            if (stat.totalTimeInForeground > 0) {
                usageMap[stat.packageName] = stat.totalTimeInForeground / 1000 // In seconds
            }
        }
        return usageMap
    }

    /**
     * Evaluates active screen time, gaming limits, and schedules.
     * Enforces app suspensions via DevicePolicyManager when rules are violated.
     */
    fun evaluateAndEnforcePolicies() {
        val policy = policyStore.getActivePolicy() ?: return
        val schedule = policy.screenTime ?: return
        val usageMap = getTodayUsageStats()

        val packagesToSuspend = mutableListOf<String>()

        // 1. Instant Pause Enforcement
        if (schedule.isInstantPause) {
            Log.i(TAG, "Instant pause is active. Suspending non-exempt apps.")
            // Suspend non-exempt apps
            val installedNonExempt = getInstalledNonSystemPackages().filterNot { dpmWrapper.isExemptPackage(it) }
            dpmWrapper.suspendPackages(installedNonExempt, true)
            return
        }

        // 2. Curfew / Schedule checks
        val now = Calendar.getInstance()
        val currentHourMinute = String.format("%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))

        val isSchoolTime = isTimeBetween(currentHourMinute, schedule.schoolStart, schedule.schoolEnd)
        val isBedTime = isTimeBetween(currentHourMinute, schedule.bedtimeStart, schedule.bedtimeEnd)

        // 3. Screen Time Cap Check
        val totalScreenTimeSeconds = usageMap.values.sum()
        val totalScreenTimeMinutes = totalScreenTimeSeconds / 60
        val dailyCapWithBonus = schedule.dailyLimitMinutes + schedule.bonusMinutesToday

        val dailyLimitExceeded = totalScreenTimeMinutes >= dailyCapWithBonus

        // 4. Per-app and Gaming Vault checks
        var totalGamingSeconds = 0L
        val gamingLimitSeconds = (schedule.dailyGamingLimitMinutes.toLong()) * 60

        // If daily limit exceeded or bedtime active, suspend non-exempt packages
        if (dailyLimitExceeded || isBedTime) {
            val toBlock = getInstalledNonSystemPackages().filterNot { dpmWrapper.isExemptPackage(it) }
            packagesToSuspend.addAll(toBlock)
        } else {
            // Check individual app restrictions and gaming rules
            for ((pkg, durationSec) in usageMap) {
                if (dpmWrapper.isExemptPackage(pkg)) continue

                // Check if gaming and during school hours
                if (isSchoolTime && schedule.blockGamesDuringSchool && isGamePackage(pkg)) {
                    packagesToSuspend.add(pkg)
                }

                if (isGamePackage(pkg)) {
                    totalGamingSeconds += durationSec
                }
            }

            // If shared gaming vault exceeded, suspend all games
            if (totalGamingSeconds >= gamingLimitSeconds) {
                val gamePackages = getInstalledNonSystemPackages().filter { isGamePackage(it) }
                packagesToSuspend.addAll(gamePackages)
            }
        }

        // Apply suspension via DevicePolicyManager
        dpmWrapper.suspendPackages(packagesToSuspend.distinct(), true)
    }

    private fun isTimeBetween(current: String, start: String, end: String): Boolean {
        return try {
            if (start <= end) {
                current in start..end
            } else {
                // Overnight window (e.g. 21:00 to 07:00)
                current >= start || current <= end
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun isGamePackage(packageName: String): Boolean {
        val pkg = packageName.toLowerCase(Locale.ROOT)
        return pkg.contains("game") ||
                pkg.contains("roblox") ||
                pkg.contains("minecraft") ||
                pkg.contains("clash") ||
                pkg.contains("subway") ||
                pkg.contains("pubg") ||
                pkg.contains("candy")
    }

    private fun getInstalledNonSystemPackages(): List<String> {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(0)
        return packages.map { it.packageName }
    }

    companion object {
        private const val TAG = "UsageTrackingManager"
    }
}
