package de.mindmarket.spotifyclone.ui.viewModels

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.mindmarket.spotifyclone.exoplayer.MusicServiceConnection
import de.mindmarket.spotifyclone.exoplayer.isPlayEnabled
import de.mindmarket.spotifyclone.exoplayer.isPlaying
import de.mindmarket.spotifyclone.exoplayer.isPrepared
import de.mindmarket.spotifyclone.other.Resource
import de.mindmarket.spotifyclone.remote.MEDIA_ROOT_IT
import de.mindmarket.spotifyclone.remote.dto.Song

class MainViewModel @ViewModelInject constructor(
    private val musicServiceConnection: MusicServiceConnection
) : ViewModel() {

    private val _mediaItems = MutableLiveData<Resource<List<Song>>>()
    val mediaItems: LiveData<Resource<List<Song>>> = _mediaItems

    val isConnected = musicServiceConnection.isConnected
    val networkError = musicServiceConnection.networkError
    val playbackState = musicServiceConnection.playbackState
    val currentlyPlayingSong = musicServiceConnection.currentlyPlayingSong

    init {
        _mediaItems.postValue(Resource.loading(null))
        musicServiceConnection.subscribe(
            MEDIA_ROOT_IT,
            object : MediaBrowserCompat.SubscriptionCallback() {
                override fun onChildrenLoaded(
                    parentId: String,
                    children: MutableList<MediaBrowserCompat.MediaItem>
                ) {
                    val items = children.map {
                        Song(
                            mediaId = it.mediaId!!,
                            title = it.description.title.toString(),
                            subtitle = it.description.subtitle.toString(),
                            songUrl = it.description.mediaUri.toString(),
                            imageUrl = it.description.iconUri.toString()
                        )
                    }
                    _mediaItems.postValue(Resource.success(items))
                }
            })
    }

    override fun onCleared() {
        musicServiceConnection.unsubscribe(
            MEDIA_ROOT_IT,
            object : MediaBrowserCompat.SubscriptionCallback() {})
    }

    fun skipToNextSong() {
        musicServiceConnection.transportControls.skipToNext()
    }

    fun skipToPreviousSong() {
        musicServiceConnection.transportControls.skipToPrevious()
    }

    fun seekTo(position: Long) {
        musicServiceConnection.transportControls.seekTo(position)
    }

    fun playOrToggleSong(mediaItem: Song, toggle: Boolean = false) {
        val isPlayerPrepared = playbackState.value?.isPrepared ?: false
        if (isPlayerPrepared
            && mediaItem.mediaId == currentlyPlayingSong.value?.getString(METADATA_KEY_MEDIA_ID)
        ) {
            playbackState.value?.let { playbackState ->
                when {
                    playbackState.isPlaying -> if (toggle) musicServiceConnection.transportControls.pause()
                    playbackState.isPlayEnabled -> musicServiceConnection.transportControls.play()
                    else -> Unit
                }
            }
        } else {
            musicServiceConnection.transportControls.playFromMediaId(mediaItem.mediaId, null)
        }
    }
}