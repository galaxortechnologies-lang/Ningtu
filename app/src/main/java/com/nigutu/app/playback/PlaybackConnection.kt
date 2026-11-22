package com.nigutu.app.playback

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.nigutu.app.core.util.NetworkStatus
import com.nigutu.app.domain.model.PlaybackState
import com.nigutu.app.domain.model.RepeatMode
import com.nigutu.app.domain.model.Song
import com.nigutu.app.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Single app-wide connection to [PlaybackService].
 *
 * The UI never touches ExoPlayer directly: it observes [state] and calls the
 * suspend controls, which are forwarded to the media session. That keeps
 * playback alive independently of any screen's lifecycle.
 */
@Singleton
class PlaybackConnection @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val networkStatus: NetworkStatus,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val controllerReady = CompletableDeferred<MediaController>()

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _sleepTimerRemainingMs = MutableStateFlow<Long?>(null)
    val sleepTimerRemainingMs: StateFlow<Long?> = _sleepTimerRemainingMs.asStateFlow()

    private var positionJob: Job? = null
    private var sleepJob: Job? = null
    private var controller: MediaController? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publishState(player)
        }
    }

    init {
        scope.launch {
            try {
                val built = buildController()
                controller = built
                built.addListener(listener)
                controllerReady.complete(built)
                publishState(built)
                startPositionUpdates()
            } catch (t: Throwable) {
                Log.e(TAG, "Unable to connect to playback service", t)
                controllerReady.completeExceptionally(t)
            }
        }
    }

    private suspend fun buildController(): MediaController = suspendCancellableCoroutine { cont ->
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                try {
                    cont.resume(future.get())
                } catch (t: Throwable) {
                    cont.resumeWithException(t)
                }
            },
            ContextCompat.getMainExecutor(context),
        )
        cont.invokeOnCancellation { MediaController.releaseFuture(future) }
    }

    private suspend fun awaitController(): MediaController? = runCatching {
        controllerReady.await()
    }.onFailure { Log.w(TAG, "Playback controller unavailable", it) }.getOrNull()

    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive) {
                controller?.let { publishState(it) }
                delay(POSITION_INTERVAL_MS)
            }
        }
    }

    private fun publishState(player: Player) {
        val current = player.currentMediaItem
        _state.value = PlaybackState(
            currentSong = current?.toSong(),
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.duration.takeIf { it > 0L } ?: current?.toSong()?.durationMs ?: 0L,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            shuffleEnabled = player.shuffleModeEnabled,
            repeatMode = player.repeatMode.toRepeatMode(),
            queue = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).toSong() },
        )
    }

    /**
     * Replaces the queue and starts playback.
     *
     * When "Wi-Fi only streaming" is enabled and the device is on mobile data,
     * online tracks are skipped and the user is told why.
     */
    suspend fun playQueue(songs: List<Song>, startIndex: Int = 0): Boolean {
        val player = awaitController() ?: return false
        if (songs.isEmpty()) return false

        val settings = settingsRepository.settings.first()
        val filtered = if (settings.wifiOnlyStreaming && !networkStatus.isUnmetered()) {
            songs.filter { it.isLocal }
        } else {
            songs
        }
        if (filtered.isEmpty()) {
            _messages.tryEmit("Mobile data streaming is off. Enable it in Settings or play a local track.")
            return false
        }
        if (filtered.size != songs.size) {
            _messages.tryEmit("Skipped online tracks: Wi-Fi only streaming is enabled.")
        }

        val safeIndex = startIndex.coerceIn(0, filtered.lastIndex)
        player.setMediaItems(filtered.map { it.toMediaItem() }, safeIndex, 0L)
        player.prepare()
        player.play()
        publishState(player)
        return true
    }

    /** Plays a single song immediately, replacing the queue. */
    suspend fun playSong(song: Song) = playQueue(listOf(song), 0)

    /** Starts or resumes playback. */
    suspend fun play() {
        awaitController()?.play()
    }

    /** Pauses playback. */
    suspend fun pause() {
        awaitController()?.pause()
    }

    /** Toggles between play and pause. */
    suspend fun togglePlayPause() {
        val player = awaitController() ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    suspend fun next() {
        awaitController()?.seekToNextMediaItem()
    }

    suspend fun previous() {
        val player = awaitController() ?: return
        if (player.currentPosition > RESTART_THRESHOLD_MS) player.seekTo(0L) else player.seekToPreviousMediaItem()
    }

    suspend fun seekTo(positionMs: Long) {
        awaitController()?.seekTo(positionMs.coerceAtLeast(0L))
    }

    suspend fun seekToFraction(fraction: Float) {
        val player = awaitController() ?: return
        val duration = player.duration.takeIf { it > 0L } ?: return
        player.seekTo((duration * fraction.coerceIn(0f, 1f)).toLong())
    }

    suspend fun toggleShuffle() {
        val player = awaitController() ?: return
        player.shuffleModeEnabled = !player.shuffleModeEnabled
        publishState(player)
    }

    suspend fun cycleRepeatMode() {
        val player = awaitController() ?: return
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        publishState(player)
    }

    /** Appends a song to the end of the current queue. */
    suspend fun addToQueue(song: Song) {
        val player = awaitController() ?: return
        player.addMediaItem(song.toMediaItem())
        publishState(player)
    }

    /** Removes the queue entry at [index]; the playing item is protected. */
    suspend fun removeFromQueue(index: Int) {
        val player = awaitController() ?: return
        if (index == player.currentMediaItemIndex || index !in 0 until player.mediaItemCount) return
        player.removeMediaItem(index)
        publishState(player)
    }

    /** Jumps to a queue entry. */
    suspend fun playQueueIndex(index: Int) {
        val player = awaitController() ?: return
        if (index !in 0 until player.mediaItemCount) return
        player.seekToDefaultPosition(index)
        player.play()
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        awaitController()?.setPlaybackSpeed(speed.coerceIn(0.5f, 2f))
    }

    /** Starts a countdown that pauses playback when it reaches zero. */
    fun startSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        val total = minutes.coerceIn(1, 720) * 60_000L
        _sleepTimerRemainingMs.value = total
        sleepJob = scope.launch {
            var remaining = total
            while (remaining > 0 && isActive) {
                delay(1_000L)
                remaining -= 1_000L
                _sleepTimerRemainingMs.value = remaining.coerceAtLeast(0L)
            }
            controller?.pause()
            _sleepTimerRemainingMs.value = null
        }
    }

    /** Cancels an active sleep timer. */
    fun cancelSleepTimer() {
        sleepJob?.cancel()
        sleepJob = null
        _sleepTimerRemainingMs.value = null
    }

    /** Releases the controller; called when the app process is torn down. */
    fun release() {
        positionJob?.cancel()
        sleepJob?.cancel()
        controller?.release()
        controller = null
    }

    private companion object {
        const val TAG = "PlaybackConnection"
        const val POSITION_INTERVAL_MS = 500L
        const val RESTART_THRESHOLD_MS = 3_000L
    }
}
