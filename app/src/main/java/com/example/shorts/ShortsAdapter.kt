
package com.example.shorts

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView

class ShortsAdapter(
    private val videos: List<VideoItem>
) : RecyclerView.Adapter<ShortsAdapter.ShortViewHolder>() {

    private var activePosition = RecyclerView.NO_POSITION
    private var recyclerView: RecyclerView? = null

    class ShortViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val playerView: PlayerView = view.findViewById(R.id.playerView)
        val tvUsername: TextView = view.findViewById(R.id.tvUsername)
        val tvDesc: TextView = view.findViewById(R.id.tvDesc)
        val tvLikes: TextView = view.findViewById(R.id.tvLikes)
        val tvComments: TextView = view.findViewById(R.id.tvComments)
        val tvShares: TextView = view.findViewById(R.id.tvShares)

        val btnLike: ImageView = view.findViewById(R.id.btnLike)
        val btnShare: ImageView = view.findViewById(R.id.btnShare)

        var player: ExoPlayer? = null
        var liked = false
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ShortViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_short, parent, false)

        return ShortViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ShortViewHolder,
        position: Int
    ) {
        val video = videos[position]

        holder.tvUsername.text = video.username
        holder.tvDesc.text = video.description
        holder.tvLikes.text = video.likes
        holder.tvComments.text = video.comments
        holder.tvShares.text = video.shares

        releasePlayer(holder)

        val player = ExoPlayer.Builder(holder.itemView.context).build()
        holder.player = player
        holder.playerView.player = player
        holder.playerView.useController = false

        // Memutar file video lokal
        player.setMediaItem(
            MediaItem.fromUri(Uri.fromFile(video.videoFile))
        )

        player.repeatMode = ExoPlayer.REPEAT_MODE_ONE
        player.prepare()

        // Hanya halaman aktif yang diputar
        if (position == activePosition) {
            player.play()
        } else {
            player.pause()
        }

        // Tombol Like
        holder.liked = false

        holder.btnLike.setColorFilter(Color.WHITE)

        holder.btnLike.setOnClickListener {
            holder.liked = !holder.liked

            holder.btnLike.setColorFilter(
                if (holder.liked) Color.RED else Color.WHITE
            )
        }

        // Tombol Bagikan
        holder.btnShare.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/*"
                putExtra(
                    Intent.EXTRA_STREAM,
                    Uri.fromFile(video.videoFile)
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            holder.itemView.context.startActivity(
                Intent.createChooser(intent, "Bagikan video")
            )
        }
    }

    // Dipanggil ketika pengguna menggeser Shorts
    fun setActivePosition(position: Int) {
        if (position !in videos.indices) return

        activePosition = position

        recyclerView?.let { rv ->
            for (i in 0 until rv.childCount) {
                val child = rv.getChildAt(i)
                val holder = rv.getChildViewHolder(child)
                    as? ShortViewHolder ?: continue

                if (holder.bindingAdapterPosition == position) {
                    holder.player?.play()
                } else {
                    holder.player?.pause()
                }
            }
        }
    }

    private fun releasePlayer(holder: ShortViewHolder) {
        holder.playerView.player = null
        holder.player?.release()
        holder.player = null
    }

    override fun onViewRecycled(holder: ShortViewHolder) {
        releasePlayer(holder)
        super.onViewRecycled(holder)
    }

    override fun onDetachedFromRecyclerView(
        recyclerView: RecyclerView
    ) {
        for (i in 0 until recyclerView.childCount) {
            val holder = recyclerView.getChildViewHolder(
                recyclerView.getChildAt(i)
            ) as? ShortViewHolder

            if (holder != null) {
                releasePlayer(holder)
            }
        }

        this.recyclerView = null
        super.onDetachedFromRecyclerView(recyclerView)
    }

    override fun getItemCount(): Int = videos.size
}
