package com.anthonyla.paperize.service.alarm

import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import com.anthonyla.paperize.core.ScreenType
import com.anthonyla.paperize.core.WallpaperMode
import com.anthonyla.paperize.domain.model.ScheduleSettings
import com.anthonyla.paperize.domain.repository.SettingsRepository
import com.anthonyla.paperize.domain.repository.WallpaperRepository
import com.anthonyla.paperize.service.WallpaperChangeLock
import com.anthonyla.paperize.service.wallpaper.WallpaperChangeService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * Handles USER_PRESENT without opening the Activity. The next source URI is applied directly
 * while the receiver is alive; the foreground service remains only as a fallback.
 */
@AndroidEntryPoint
class ScreenEventReceiver : BroadcastReceiver() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var wallpaperRepository: WallpaperRepository
    @Inject lateinit var wallpaperChangeLock: WallpaperChangeLock

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_USER_PRESENT) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = settingsRepository.getScheduleSettings()
                val isStaticMode = settingsRepository.getWallpaperMode() == WallpaperMode.STATIC
                if (isStaticMode && settings.changeOnLockUnlock) {
                    val changed = wallpaperChangeLock.mutex.withLock {
                        applyImmediately(context, settings)
                    }
                    if (!changed) {
                        startFallbackService(context)
                    }
                    Log.d(TAG, "Unlock wallpaper path completed immediately=$changed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unable to apply wallpaper after unlock", e)
                runCatching { startFallbackService(context) }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun applyImmediately(context: Context, settings: ScheduleSettings): Boolean {
        val homeAlbumId = settings.homeAlbumId.takeIf { settings.homeEnabled }
        val lockAlbumId = settings.lockAlbumId.takeIf { settings.lockEnabled }
        if (homeAlbumId == null && lockAlbumId == null) return false

        val synchronized = homeAlbumId != null && homeAlbumId == lockAlbumId &&
            !settings.separateSchedules
        return if (synchronized) {
            applyOne(
                context = context,
                albumId = homeAlbumId,
                queueScreen = ScreenType.HOME,
                completedScreens = listOf(ScreenType.HOME, ScreenType.LOCK),
                which = WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK,
                shuffle = settings.shuffleEnabled
            )
        } else {
            var changed = false
            homeAlbumId?.let {
                changed = applyOne(
                    context,
                    it,
                    ScreenType.HOME,
                    listOf(ScreenType.HOME),
                    WallpaperManager.FLAG_SYSTEM,
                    settings.shuffleEnabled
                ) || changed
            }
            lockAlbumId?.let {
                changed = applyOne(
                    context,
                    it,
                    ScreenType.LOCK,
                    listOf(ScreenType.LOCK),
                    WallpaperManager.FLAG_LOCK,
                    settings.shuffleEnabled
                ) || changed
            }
            changed
        }
    }

    private suspend fun applyOne(
        context: Context,
        albumId: String,
        queueScreen: ScreenType,
        completedScreens: List<ScreenType>,
        which: Int,
        shuffle: Boolean
    ): Boolean {
        if (wallpaperRepository.getNextWallpaperInQueue(albumId, queueScreen) == null) {
            wallpaperRepository.buildWallpaperQueue(albumId, queueScreen, shuffle)
        }
        val candidate = wallpaperRepository.getAndDequeueWallpaper(albumId, queueScreen)
            ?: return false
        return try {
            val input = context.contentResolver.openInputStream(Uri.parse(candidate.uri))
                ?: throw IllegalStateException("Unable to open wallpaper URI")
            input.use { WallpaperManager.getInstance(context).setStream(it, null, true, which) }
            completedScreens.forEach { screen ->
                wallpaperRepository.setCurrentWallpaper(albumId, screen, candidate.id)
                wallpaperRepository.removeWallpaperFromQueue(albumId, screen, candidate.id)
            }
            true
        } catch (e: Exception) {
            wallpaperRepository.restoreWallpaperToQueueFront(albumId, queueScreen, candidate.id)
            Log.e(TAG, "Direct wallpaper application failed", e)
            false
        }
    }

    private fun startFallbackService(context: Context) {
        val fallback = Intent(context, WallpaperChangeService::class.java).apply {
            action = WallpaperChangeService.ACTION_CHANGE_WALLPAPER_FAST
            putExtra(WallpaperChangeService.EXTRA_SCREEN_TYPE, ScreenType.BOTH.name)
        }
        ContextCompat.startForegroundService(context, fallback)
    }

    private companion object {
        const val TAG = "ScreenEventReceiver"
    }
}
