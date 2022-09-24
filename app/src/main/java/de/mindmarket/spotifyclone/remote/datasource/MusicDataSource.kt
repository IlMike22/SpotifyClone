package de.mindmarket.spotifyclone.remote.datasource

import de.mindmarket.spotifyclone.remote.dto.Song

class MusicDataSource {
    private val songCollection = firestore.collection(SONG_COLLECTION)

    suspend fun getSongCollection():List<Song> {
        return try {
            songCollection.get().await().toObjects(Song::class.java)
        } catch (exception:Exception) {
            emptyList()
        }
    }
}