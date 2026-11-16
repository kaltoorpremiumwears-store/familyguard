package org.familyguard.child.web

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import org.familyguard.child.dpc.DevicePolicyManagerWrapper
import org.familyguard.child.dpc.ParentalDpcReceiver

class ManagedBrowserEnforcer(private val context: Context) {

    private val dpm: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComponent: ComponentName = ParentalDpcReceiver.getComponentName(context)
    private val dpmWrapper = DevicePolicyManagerWrapper(context)

    /**
     * Common browsers that do not support Android Enterprise Managed App Restrictions
     * or could be used to bypass web filtering if unmanaged.
     */
    private val unmanagedBrowserPackages = listOf(
        "org.mozilla.firefox",
        "org.mozilla.firefox_beta",
        "com.opera.browser",
        "com.opera.mini.native",
        "com.brave.browser",
        "com.duckduckgo.mobile.android",
        "com.UCMobile.intl",
        "org.torproject.torbrowser"
    )

    /**
     * Enforces Managed Configuration for Google Chrome.
     * Google Chrome natively parses `URLBlocklist`, `URLAllowlist`, and `ForceGoogleSafeSearch`.
     */
    fun applyChromeRestrictions(
        urlBlocklist: List<String>,
        urlAllowlist: List<String>,
        forceSafeSearch: Boolean = true,
        strictYouTube: Boolean = true
    ): Boolean {
        if (!dpmWrapper.isDeviceOwner) {
            Log.w(TAG, "Cannot apply Chrome restrictions: not Device Owner.")
            return false
        }

        val restrictions = Bundle().apply {
            putStringArray("URLBlocklist", urlBlocklist.toTypedArray())
            putStringArray("URLAllowlist", urlAllowlist.toTypedArray())
            putBoolean("ForceGoogleSafeSearch", forceSafeSearch)
            putInt("ForceYouTubeRestrict", if (strictYouTube) 2 else 0) // 2 = Strict
            putInt("IncognitoModeAvailability", 1) // 1 = Disabled incognito mode
            putInt("BrowserSignin", 0) // Disable account switching to bypass
        }

        return try {
            dpm.setApplicationRestrictions(adminComponent, "com.android.chrome", restrictions)
            Log.i(TAG, "Successfully enforced Managed Chrome restrictions.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply Chrome restrictions: ${e.message}")
            false
        }
    }

    /**
     * Suspends unmanaged browsers so child cannot bypass filtered Chrome.
     */
    fun suspendUnmanagedBrowsers(suspend: Boolean) {
        if (!dpmWrapper.isDeviceOwner) return
        val pm = context.packageManager
        val installedUnmanaged = unmanagedBrowserPackages.filter { pkg ->
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }

        if (installedUnmanaged.isNotEmpty()) {
            dpmWrapper.suspendPackages(installedUnmanaged, suspend)
            Log.i(TAG, "Unmanaged browsers suspension state updated (suspend=$suspend): ${installedUnmanaged.joinToString()}")
        }
    }

    companion object {
        private const val TAG = "ManagedBrowserEnforcer"
    }
}
