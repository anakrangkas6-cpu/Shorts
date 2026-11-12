package com.example.shorts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView

class ShortsAdapter(private val videos: List<VideoItem>) : RecyclerView.Adapter<ShortsAdapter.ShortViewHolder>() {

    class ShortViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val playerView: PlayerView = view.findViewById(R.id.playerView)
        val tvUsername: TextView = view.findViewById(R.id.tvUsername)
        val tvDesc: TextView = view.findViewById(R.id.tvDesc)
        val tvLikes: TextView = view.findViewById(R.id.tvLikes)
        val tvComments: TextView = view.findViewById(R.id.tvComments)
        val tvShares: TextView = view.findViewById(R.id.tvShares)
        var player: ExoPlayer? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShortViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_short, parent, false)
        return ShortViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShortViewHolder, position: Int) {
        val video = videos[position]
        holder.tvUsername.text = video.username
        holder.tvDesc.text = video.description
        holder.tvLikes.text = video.likes
        holder.tvComments.text = video.comments
        holder.tvShares.text = video.shares

        holder.player?.release()
        val player = ExoPlayer.Builder(holder.itemView.context).build()
        holder.player = player
        holder.playerView.player = player
        holder.playerView.useController = false
        player.setMediaItem(MediaItem.fromUri(video.videoUrl))
        player.repeatMode = ExoPlayer.REPEAT_MODE_ONE
        player.prepare()
        player.play()
    }

    override fun onViewRecycled(holder: ShortViewHolder) {
        super.onViewRecycled(holder)
        holder.player?.release()
        holder.player = null
    }

    override fun getItemCount() = videos.size
}
