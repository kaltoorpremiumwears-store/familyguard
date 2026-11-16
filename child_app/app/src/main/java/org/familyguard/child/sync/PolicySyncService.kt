package org.familyguard.child.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import org.familyguard.child.data.api.ChildApiClient
import org.familyguard.child.data.storage.LocalPolicyStore
import org.familyguard.child.dpc.DevicePolicyManagerWrapper

class PolicySyncService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var policyStore: LocalPolicyStore
    private lateinit var dpmWrapper: DevicePolicyManagerWrapper
    private lateinit var usageTracker: org.familyguard.child.usage.UsageTrackingManager
    private lateinit var appInventoryManager: org.familyguard.child.inventory.AppInventoryManager
    private var syncJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        policyStore = LocalPolicyStore(this)
        dpmWrapper = DevicePolicyManagerWrapper(this)
        usageTracker = org.familyguard.child.usage.UsageTrackingManager(this)
        appInventoryManager = org.familyguard.child.inventory.AppInventoryManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)

        startPeriodicSync()
        return START_STICKY
    }

    private fun startPeriodicSync() {
        syncJob?.cancel()
        syncJob = serviceScope.launch {
            while (isActive) {
                try {
                    syncPolicyDelta()
                } catch (e: Exception) {
                    Log.w(TAG, "Sync loop error: ${e.message}")
                }
                delay(30_000) // Poll every 30 seconds (or immediately on push)
            }
        }
    }

    private suspend fun syncPolicyDelta() {
        val token = ChildApiClient.deviceAccessToken ?: return
        try {
            val response = ChildApiClient.service.syncPolicies(token)
            if (response.isSuccessful && response.body() != null) {
                val policy = response.body()!!
                policyStore.saveActivePolicy(policy)

                // Enforce instant pause if requested by parent
                val isPaused = policy.screenTime?.isInstantPause ?: false
                if (isPaused) {
                    Log.i(TAG, "Instant pause detected! Enforcing restriction...")
                }

                // Enforce tamper protections
                dpmWrapper.enforceNetworkTime()
                dpmWrapper.blockAppUninstall(true)

                // Enforce Screen Time & Gaming Limits
                usageTracker.evaluateAndEnforcePolicies()

                // Sync app inventory
                appInventoryManager.scanAndSyncInventory(policy.childId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync policy: ${e.message}")
        }
    }

    private fun getBatteryPercentage(): Int {
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "FamilyGuard Protection Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows transparent active management and synchronization status."
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FamilyGuard Protection Active")
            .setContentText("Parental policies synchronized and active.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "PolicySyncService"
        private const val CHANNEL_ID = "familyguard_protection_channel"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, PolicySyncService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
