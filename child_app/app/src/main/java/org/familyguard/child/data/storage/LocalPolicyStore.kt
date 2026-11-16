package org.familyguard.child.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import org.familyguard.child.data.model.ActivePolicyResponse
import org.familyguard.child.data.model.ScreenTimePolicyDto

class LocalPolicyStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("familyguard_child_policy_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveActivePolicy(policy: ActivePolicyResponse) {
        prefs.edit()
            .putString(KEY_CACHED_POLICY, gson.toJson(policy))
            .putLong(KEY_LAST_SYNC_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    fun getActivePolicy(): ActivePolicyResponse? {
        val json = prefs.getString(KEY_CACHED_POLICY, null) ?: return null
        return try {
            gson.fromJson(json, ActivePolicyResponse::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun isInstantPaused(): Boolean {
        val policy = getActivePolicy()
        return policy?.screenTime?.isInstantPause ?: false
    }

    fun setInstantPaused(paused: Boolean) {
        val policy = getActivePolicy() ?: return
        val currentScreenTime = policy.screenTime ?: return
        val updatedScreenTime = currentScreenTime.copy(isInstantPause = paused)
        val updatedPolicy = policy.copy(screenTime = updatedScreenTime)
        saveActivePolicy(updatedPolicy)
    }

    fun getLastSyncTimestamp(): Long {
        return prefs.getLong(KEY_LAST_SYNC_TIMESTAMP, 0L)
    }

    companion object {
        private const val KEY_CACHED_POLICY = "key_cached_policy"
        private const val KEY_LAST_SYNC_TIMESTAMP = "key_last_sync_timestamp"
    }
}
