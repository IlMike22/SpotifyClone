package de.mindmarket.spotifyclone.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.plcoding.spotifycloneyt.R
import de.mindmarket.spotifyclone.remote.dto.Song
import kotlinx.android.synthetic.main.list_item.view.*
import javax.inject.Inject

class SongAdapter @Inject constructor(
    private val glide: RequestManager,
) : BaseSongAdapter(R.layout.list_item) {
    override val differ: AsyncListDiffer<Song> = AsyncListDiffer(this, diffCallback)

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val currentSong = songs[position]
        holder.itemView.apply {
            tvPrimary.text = currentSong.title
            tvSecondary.text = currentSong.subtitle
            glide.load(currentSong.imageUrl).into(ivItemImage)
            setOnClickListener {
                onItemClickListener?.let { onClicked ->
                    onClicked(currentSong)
                }
            }
        }
    }
}