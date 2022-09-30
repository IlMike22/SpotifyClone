package de.mindmarket.spotifyclone.exoplayer

import android.support.v4.media.MediaMetadataCompat
import de.mindmarket.spotifyclone.remote.dto.Song

fun MediaMetadataCompat.toSong(): Song =
    Song(
        mediaId = this.description?.mediaId?:"",
        title = this.description?.title.toString(),
        imageUrl = this.description?.iconUri.toString(),
        songUrl = this.description?.mediaUri.toString(),
        subtitle = this.description?.subtitle.toString()
    )