package de.mindmarket.spotifyclone.remote.datasource

import com.google.firebase.firestore.FirebaseFirestore
import de.mindmarket.spotifyclone.remote.SONG_COLLECTION
import de.mindmarket.spotifyclone.remote.dto.Song
import kotlinx.coroutines.tasks.await

class MusicDataSource {
    private val firestore = FirebaseFirestore.getInstance()
    private val songCollection = firestore.collection(SONG_COLLECTION)

    suspend fun getSongCollection():List<Song> {
        return try {
            songCollection.get().await().toObjects(Song::class.java)
        } catch (exception:Exception) {
            emptyList()
        }
    }
}