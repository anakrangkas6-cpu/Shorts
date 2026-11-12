
package com.example.shorts

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView

class ShortsAdapter(
    private val videos: List<VideoItem>
) : RecyclerView.Adapter<ShortsAdapter.ShortViewHolder>() {

    private var activePosition = RecyclerView.NO_POSITION
    private var recyclerView: RecyclerView? = null

    private val likedPositions = mutableSetOf<Int>()

    class ShortViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val playerView: PlayerView = view.findViewById(R.id.playerView)
        val tvUsername: TextView = view.findViewById(R.id.tvUsername)
        val tvDesc: TextView = view.findViewById(R.id.tvDesc)
        val tvLikes: TextView = view.findViewById(R.id.tvLikes)
        val tvComments: TextView = view.findViewById(R.id.tvComments)
        val tvShares: TextView = view.findViewById(R.id.tvShares)

        val btnLike: ImageView = view.findViewById(R.id.btnLike)
        val btnShare: ImageView = view.findViewById(R.id.btnShare)
        val btnComment: ImageView = view.findViewById(R.id.btnComment)
        val btnBookmark: ImageView = view.findViewById(R.id.btnBookmark)

        var player: ExoPlayer? = null
    }

    override fun onAttachedToRecyclerView(rv: RecyclerView) {
        super.onAttachedToRecyclerView(rv)
        recyclerView = rv
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

        val player = ExoPlayer.Builder(
            holder.itemView.context
        ).build()

        holder.player = player
        holder.playerView.player = player
        holder.playerView.useController = false

        player.setMediaItem(
            MediaItem.fromUri(video.videoFile.toURI().toString())
        )

        player.repeatMode = ExoPlayer.REPEAT_MODE_ONE
        player.prepare()

        if (position == activePosition) {
            player.play()
        } else {
            player.pause()
        }

        // Like
        holder.btnLike.setColorFilter(
            if (position in likedPositions) Color.RED
            else Color.WHITE
        )

        holder.btnLike.setOnClickListener {
            if (position in likedPositions) {
                likedPositions.remove(position)
            } else {
                likedPositions.add(position)
            }

            holder.btnLike.setColorFilter(
                if (position in likedPositions) Color.RED
                else Color.WHITE
            )
        }

        // Bagikan video melalui FileProvider
        holder.btnShare.setOnClickListener {
            try {
                val context = holder.itemView.context

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    video.videoFile
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "video/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = android.content.ClipData.newUri(
                        context.contentResolver,
                        "Video Shorts",
                        uri
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                context.startActivity(
                    Intent.createChooser(intent, "Bagikan video")
                )

            } catch (e: Exception) {
                Toast.makeText(
                    holder.itemView.context,
                    "Gagal membagikan video",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Komentar dan simpan akan dihubungkan
        // setelah sistem penyimpanan dan komentar dibuat.
    }

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

    override fun onDetachedFromRecyclerView(rv: RecyclerView) {
        for (i in 0 until rv.childCount) {
            val holder = rv.getChildViewHolder(rv.getChildAt(i))
                as? ShortViewHolder

            if (holder != null) {
                releasePlayer(holder)
            }
        }

        recyclerView = null
        super.onDetachedFromRecyclerView(rv)
    }

    override fun getItemCount(): Int = videos.size
}
