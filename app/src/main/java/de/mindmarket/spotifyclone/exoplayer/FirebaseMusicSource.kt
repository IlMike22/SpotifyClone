package de.mindmarket.spotifyclone.exoplayer

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.*
import androidx.core.net.toUri
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import de.mindmarket.spotifyclone.remote.datasource.MusicDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FirebaseMusicSource @Inject constructor(
    private val musicDataSource: MusicDataSource
) {
    var songs = emptyList<MediaMetadataCompat>()
    private val onReadyListeners = mutableListOf<(Boolean) -> Unit>()
    private var state = State.CREATED
        set(value) {
            if (value == State.INITIALIZED || value == State.ERROR) {
                synchronized(onReadyListeners) {
                    field = value
                    onReadyListeners.forEach { listener ->
                        listener(value == State.INITIALIZED)
                    }
                }
            } else {
                field = value
            }
        }

    suspend fun fetchMetadata() = withContext(Dispatchers.IO) {
        state = State.INITIALISING
        val allSongs = musicDataSource.getSongCollection()
        allSongs.map { song ->
            MediaMetadataCompat.Builder()
                .putString(METADATA_KEY_ARTIST, song.subtitle)
                .putString(METADATA_KEY_MEDIA_ID, song.mediaId)
                .putString(METADATA_KEY_TITLE, song.title)
                .putString(METADATA_KEY_DISPLAY_TITLE, song.title)
                .putString(METADATA_KEY_MEDIA_URI, song.songUrl)
                .putString(METADATA_KEY_DISPLAY_ICON_URI, song.imageUrl)
                .putString(METADATA_KEY_ALBUM_ART_URI, song.imageUrl)
                .putString(METADATA_KEY_DISPLAY_SUBTITLE, song.subtitle)
                .putString(METADATA_KEY_DISPLAY_DESCRIPTION, song.subtitle)
                .build()
        }
        state = State.INITIALIZED // since we have defined a setter, each time we set the state again it will be triggered our setter code above
    }

    fun asMediaSource(dataSourceFactory: DefaultDataSourceFactory):ConcatenatingMediaSource {
        val concatenatingMediaSource =  ConcatenatingMediaSource()

        songs.forEach { song ->
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(song.getString(METADATA_KEY_MEDIA_URI).toUri())
            concatenatingMediaSource.addMediaSource(mediaSource)
        }
        return concatenatingMediaSource
    }

    fun asMediaItems() =
        songs.map { song ->
            val description = MediaDescriptionCompat.Builder()
                .setMediaUri(song.getString(METADATA_KEY_MEDIA_URI).toUri())
                .setTitle(song.description.title)
                .setSubtitle(song.description.subtitle)
                .setMediaId(song.description.mediaId)
                .setIconUri(song.description.iconUri)
                .build()
            MediaBrowserCompat.MediaItem(description, FLAG_PLAYABLE)
        }


    fun whenReady(action: (Boolean) -> Unit): Boolean {
        if (state == State.CREATED || state == State.INITIALISING) {
            onReadyListeners += action
            return false
        } else {
            action(state == State.INITIALIZED)
            return true
        }
    }
}

enum class State {
    CREATED,
    INITIALISING,
    INITIALIZED,
    ERROR
}