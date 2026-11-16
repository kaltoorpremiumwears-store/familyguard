package org.familyguard.child.dpc

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager
import android.util.Log

class DevicePolicyManagerWrapper(private val context: Context) {

    private val dpm: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComponent: ComponentName = ParentalDpcReceiver.getComponentName(context)

    val isDeviceOwner: Boolean
        get() = dpm.isDeviceOwnerApp(context.packageName)

    val isDeviceAdmin: Boolean
        get() = dpm.isAdminActive(adminComponent)

    /**
     * Exempts critical emergency and system packages from suspension.
     * Emergency dialing (911/112), phone dialer, and system settings are strictly protected.
     */
    fun isExemptPackage(packageName: String): Boolean {
        if (packageName == context.packageName) return true

        // Essential emergency telecom dialer
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        val defaultDialer = telecomManager?.defaultDialerPackage
        if (packageName == defaultDialer) return true

        // Standard telephony / emergency intents
        val dialIntent = Intent(Intent.ACTION_DIAL)
        val resolveInfo = context.packageManager.resolveActivity(dialIntent, PackageManager.MATCH_DEFAULT_ONLY)
        if (resolveInfo?.activityInfo?.packageName == packageName) return true

        // Android System UI
        if (packageName == "com.android.systemui" || packageName == "android") return true

        return false
    }

    /**
     * Suspend or unsuspend target packages.
     * Suspended apps are greystcaled on the launcher and cannot be opened.
     */
    fun suspendPackages(packages: List<String>, suspend: Boolean): Boolean {
        if (!isDeviceOwner) {
            Log.w(TAG, "Cannot suspend packages: app is not provisioned as Device Owner.")
            return false
        }

        val targetPackages = packages.filterNot { isExemptPackage(it) }
        if (targetPackages.isEmpty()) return true

        return try {
            val failed = dpm.setPackagesSuspended(
                adminComponent,
                targetPackages.toTypedArray(),
                suspend
            )
            if (failed != null && failed.isNotEmpty()) {
                Log.w(TAG, "Failed to suspend some packages: ${failed.joinToString()}")
            } else {
                Log.i(TAG, "Successfully updated suspension for ${targetPackages.size} packages (suspend=$suspend)")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Exception suspending packages: ${e.message}")
            false
        }
    }

    /**
     * Locks device instantly (used when screen time expires or parent triggers instant lock).
     */
    fun lockNow(): Boolean {
        return try {
            dpm.lockNow()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to lock device: ${e.message}")
            false
        }
    }

    /**
     * Ensures network time is enforced so child cannot change system date/time to bypass limits.
     */
    fun enforceNetworkTime(): Boolean {
        if (!isDeviceOwner) return false
        return try {
            dpm.setAutoTimeRequired(adminComponent, true)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enforce network time: ${e.message}")
            false
        }
    }

    /**
     * Prevents uninstalling the protection app.
     */
    fun blockAppUninstall(blocked: Boolean): Boolean {
        if (!isDeviceOwner) return false
        return try {
            dpm.setUninstallBlocked(adminComponent, context.packageName, blocked)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set uninstall blocked: ${e.message}")
            false
        }
    }

    companion object {
        private const val TAG = "DpmWrapper"
    }
}
