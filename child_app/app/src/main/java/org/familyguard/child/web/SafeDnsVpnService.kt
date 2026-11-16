package org.familyguard.child.web

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log

class SafeDnsVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }

        startSafeDnsVpn()
        return START_STICKY
    }

    private fun startSafeDnsVpn() {
        if (vpnInterface != null) return

        try {
            val builder = Builder()
                .setSession("FamilyGuard Safe DNS")
                // Cloudflare for Families (1.1.1.3 & 1.0.0.3): Blocks Adult Content and Malware at DNS level
                .addDnsServer("1.1.1.3")
                .addDnsServer("1.0.0.3")
                // CleanBrowsing Family Filter fallback
                .addDnsServer("185.228.168.168")
                .addRoute("0.0.0.0", 0)

            vpnInterface = builder.establish()
            Log.i(TAG, "Safe DNS VpnService successfully established.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to establish Safe DNS VPN: ${e.message}")
        }
    }

    private fun stopVpn() {
        try {
            vpnInterface?.close()
            vpnInterface = null
            stopSelf()
            Log.i(TAG, "Safe DNS VpnService stopped.")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing VPN interface: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
    }

    companion object {
        private const val TAG = "SafeDnsVpnService"
        const val ACTION_STOP = "org.familyguard.child.action.STOP_VPN"
    }
}
