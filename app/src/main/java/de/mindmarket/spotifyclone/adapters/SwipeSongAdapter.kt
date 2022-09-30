package de.mindmarket.spotifyclone.adapters

import androidx.recyclerview.widget.AsyncListDiffer
import com.plcoding.spotifycloneyt.R
import de.mindmarket.spotifyclone.remote.dto.Song
import kotlinx.android.synthetic.main.swipe_item.view.*

class SwipeSongAdapter() : BaseSongAdapter(R.layout.swipe_item) {
    override val differ: AsyncListDiffer<Song> = AsyncListDiffer(this, diffCallback)

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val currentSong = songs[position]
        holder.itemView.apply {
            val text = "${currentSong.title} - ${currentSong.subtitle}"
            tvPrimary.text = text
            setOnClickListener {
                onItemClickListener?.let { onClicked ->
                    onClicked(currentSong)
                }
            }
        }
    }
}