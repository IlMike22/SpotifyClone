package de.mindmarket.spotifyclone.exoplayer

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import dagger.hilt.android.AndroidEntryPoint
import de.mindmarket.spotifyclone.exoplayer.callbacks.MusicPlaybackPreparer
import de.mindmarket.spotifyclone.exoplayer.callbacks.MusicPlayerEventListener
import de.mindmarket.spotifyclone.exoplayer.callbacks.MusicPlayerNotificationListener
import de.mindmarket.spotifyclone.remote.MEDIA_ROOT_IT
import de.mindmarket.spotifyclone.remote.NETWORK_ERROR
import kotlinx.coroutines.*
import javax.inject.Inject

private const val SERVICE_TAG = "MusicService"

@AndroidEntryPoint
class MusicService : MediaBrowserServiceCompat() {
    @Inject
    lateinit var dataSourceFactory: DefaultDataSourceFactory

    @Inject
    lateinit var exoPlayer: SimpleExoPlayer

    @Inject
    lateinit var firebaseMusicSource: FirebaseMusicSource

    lateinit var musicNotificationManager: MusicNotificationManager

    private val serviceJob = Job()

    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var musicPlayerEventListener: MusicPlayerEventListener

    private lateinit var mediaSessionConnector: MediaSessionConnector
    var isForegroundService = false
    private var currentPlayingSong: MediaMetadataCompat? = null
    private var isPlayerInititialized = false

    override fun onCreate() {
        super.onCreate()

        serviceScope.launch {
            firebaseMusicSource.fetchMetadata()
        }

        val activityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, 0)
        }

        mediaSession = MediaSessionCompat(this, SERVICE_TAG).apply {
            setSessionActivity(activityIntent)
            isActive = true
        }

        musicNotificationManager = MusicNotificationManager(
            context = this,
            sessionToken = mediaSession.sessionToken,
            notificationListener = MusicPlayerNotificationListener(this)
        ) {
            // new song callback lambda block
            currentSongDuration = exoPlayer.duration
        }

        val musicPlaybackPreparer = MusicPlaybackPreparer(
            firebaseMusicSource = firebaseMusicSource
        ) {
            currentPlayingSong = it
            preparePlayer(
                songs = firebaseMusicSource.songs,
                itemToPlay = it,
                isPlayNow = true
            )
        }

        sessionToken = mediaSession.sessionToken
        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlaybackPreparer(musicPlaybackPreparer)
        mediaSessionConnector.setQueueNavigator(MusicQueueNavigator())
        mediaSessionConnector.setPlayer(exoPlayer)

        musicPlayerEventListener = MusicPlayerEventListener(this)
        exoPlayer.addListener(musicPlayerEventListener)
        musicNotificationManager.showNotification(exoPlayer)

    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return BrowserRoot(MEDIA_ROOT_IT, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        when (parentId) {
            MEDIA_ROOT_IT -> {
                val resultsSend = firebaseMusicSource.whenReady { isInitialized ->
                    if (isInitialized) {
                        result.sendResult(firebaseMusicSource.asMediaItems())

                        if (isPlayerInititialized.not() && firebaseMusicSource.songs.isNotEmpty()) {
                            preparePlayer(
                                firebaseMusicSource.songs,
                                firebaseMusicSource.songs[0],
                                false
                            )
                            isPlayerInititialized = true
                        }
                    } else { // music source ready but not initialized -> error occured
                        mediaSession.sendSessionEvent(NETWORK_ERROR, null)
                        result.sendResult(null)
                    }
                }
                if (resultsSend.not()) {
                    result.detach()
                }
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        exoPlayer.stop()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
        exoPlayer.removeListener(musicPlayerEventListener)
        exoPlayer.release()
    }

    private fun preparePlayer(
        songs: List<MediaMetadataCompat>,
        itemToPlay: MediaMetadataCompat?,
        isPlayNow: Boolean
    ) {
        val currentSongIndex = if (currentPlayingSong == null) 0 else songs.indexOf(itemToPlay)
        exoPlayer.prepare(firebaseMusicSource.asMediaSource(dataSourceFactory))
        exoPlayer.seekTo(currentSongIndex, 0L)
        exoPlayer.playWhenReady = isPlayNow
    }

    private inner class MusicQueueNavigator : TimelineQueueNavigator(mediaSession) {
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            return firebaseMusicSource.songs[windowIndex].description
        }
    }

    companion object {
        var currentSongDuration = 0L
            private set
    }
}