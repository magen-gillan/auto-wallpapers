package com.anthonyla.paperize.service.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.anthonyla.paperize.service.worker.WallpaperScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Broadcast receiver to reschedule wallpaper changes on device boot
 *
 * Uses WorkManager for scheduling instead of AlarmManager
 * Uses goAsync() to ensure coroutine completes before receiver is destroyed
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var wallpaperScheduler: WallpaperScheduler

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == Intent.ACTION_TIME_CHANGED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED ||
            intent.action == "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
        ) {
            Log.d(TAG, "Boot completed, rescheduling wallpaper changes")

            // Use goAsync() to extend receiver lifecycle for async work
            // This prevents the system from killing the receiver before coroutine completes
            val pendingResult = goAsync()

            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    wallpaperScheduler.reschedulePersistedSchedules()
                    Log.d(TAG, "Wallpaper schedules restored successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling wallpaper changes", e)
                } finally {
                    // Must call finish() to release the async operation
                    pendingResult.finish()
                }
            }
        }
    }
}
