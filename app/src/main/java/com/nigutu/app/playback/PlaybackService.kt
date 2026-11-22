package com.nigutu.app.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.nigutu.app.MainActivity
import com.nigutu.app.domain.repository.MusicRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground media service that owns the ExoPlayer instance.
 *
 * Media3 publishes the media notification and handles audio focus, noisy
 * (headphone unplug) events and lock-screen controls through the session.
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var musicRepository: MusicRepository
    @Inject lateinit var audioEffects: AudioEffectsController

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()

        val exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        player = exoPlayer
        audioEffects.attach(exoPlayer)

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        exoPlayer.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (mediaItem != null && reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                    val song = mediaItem.toSong()
                    serviceScope.launch {
                        runCatching { musicRepository.recordPlay(song) }
                    }
                }
            }
        })

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(sessionActivity)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val currentPlayer = player
        if (currentPlayer == null || !currentPlayer.playWhenReady || currentPlayer.mediaItemCount == 0) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        player = null
        audioEffects.release()
        super.onDestroy()
    }
}
