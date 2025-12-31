package me.bmax.apatch.util

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.bmax.apatch.Natives
import org.json.JSONArray

/**
 * AppData - Data management center for badge counts
 * Manages counts for superuser and APM modules
 */
object AppData {
    private const val TAG = "AppData"

    object DataRefreshManager {
        // Private state flows for counts
        private val _superuserCount = MutableStateFlow(0)
        private val _apmModuleCount = MutableStateFlow(0)

        // Public read-only state flows
        val superuserCount: StateFlow<Int> = _superuserCount.asStateFlow()
        val apmModuleCount: StateFlow<Int> = _apmModuleCount.asStateFlow()

        /**
         * Refresh all data counts
         */
        fun refreshData() {
            _superuserCount.value = getSuperuserCount()
            _apmModuleCount.value = getApmModuleCount()
        }
    }

    /**
     * Get superuser count
     */
    private fun getSuperuserCount(): Int {
        return try {
            val uids = Natives.suUids()
            uids.size
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get superuser count", e)
            0
        }
    }

    /**
     * Get APM module count
     */
    private fun getApmModuleCount(): Int {
        return try {
            val result = listModules()
            val array = JSONArray(result)
            array.length()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get APM module count", e)
            0
        }
    }
}


