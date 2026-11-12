
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
import java.io.File

class ShortsAdapter(
    private val videos: List<VideoItem>
) : RecyclerView.Adapter<ShortsAdapter.ShortViewHolder>() {

    private var activePosition = RecyclerView.NO_POSITION
    private var recyclerView: RecyclerView? = null

    class ShortViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val playerView: PlayerView =
            view.findViewById(R.id.playerView)

        val tvUsername: TextView =
            view.findViewById(R.id.tvUsername)

        val tvDesc: TextView =
            view.findViewById(R.id.tvDesc)

        val tvLikes: TextView =
            view.findViewById(R.id.tvLikes)

        val tvComments: TextView =
            view.findViewById(R.id.tvComments)

        val tvShares: TextView =
            view.findViewById(R.id.tvShares)

        val btnLike: ImageView =
            view.findViewById(R.id.btnLike)

        val btnShare: ImageView =
            view.findViewById(R.id.btnShare)

        val btnComment: ImageView =
            view.findViewById(R.id.btnComment)

        val btnBookmark: ImageView =
            view.findViewById(R.id.btnBookmark)

        var player: ExoPlayer? = null
    }

    private fun savedFolder(): File {
        return File(videos.firstOrNull()?.videoFile?.parentFile?.parentFile
            ?: throw IllegalStateException("Folder Shorts tidak ditemukan"),
            "Saved")
    }

    private fun likesPreferences(holder: ShortViewHolder) =
        holder.itemView.context.getSharedPreferences(
            "shorts_likes",
            android.content.Context.MODE_PRIVATE
        )

    private fun isLiked(holder: ShortViewHolder, video: VideoItem): Boolean {
        return likesPreferences(holder)
            .getBoolean(video.videoFile.name, false)
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
        val context = holder.itemView.context

        holder.tvUsername.text = video.username
        holder.tvDesc.text = video.description
        holder.tvLikes.text = video.likes
        holder.tvComments.text = video.comments
        holder.tvShares.text = video.shares

        releasePlayer(holder)

        val player = ExoPlayer.Builder(context).build()

        holder.player = player
        holder.playerView.player = player
        holder.playerView.useController = false

        player.setMediaItem(
            MediaItem.fromUri(
                android.net.Uri.fromFile(video.videoFile)
            )
        )

        player.repeatMode = ExoPlayer.REPEAT_MODE_ONE
        player.prepare()

        if (position == activePosition) {
            player.play()
        } else {
            player.pause()
        }

        // LIKE
        holder.btnLike.setColorFilter(
            if (isLiked(holder, video)) Color.RED
            else Color.WHITE
        )

        holder.btnLike.setOnClickListener {
            val prefs = likesPreferences(holder)
            val liked = !isLiked(holder, video)

            prefs.edit()
                .putBoolean(video.videoFile.name, liked)
                .apply()

            holder.btnLike.setColorFilter(
                if (liked) Color.RED else Color.WHITE
            )

            Toast.makeText(
                context,
                if (liked) "Video disukai" else "Like dibatalkan",
                Toast.LENGTH_SHORT
            ).show()
        }

        // SIMPAN
        val savedDir = File(context.filesDir, "Saved")

        if (!savedDir.exists()) {
            savedDir.mkdirs()
        }

        val savedFile = File(
            savedDir,
            video.videoFile.name
        )

        holder.btnBookmark.setColorFilter(
            if (savedFile.exists()) Color.YELLOW
            else Color.WHITE
        )

        holder.btnBookmark.setOnClickListener {
            try {
                if (savedFile.exists()) {
                    val deleted = savedFile.delete()

                    if (!deleted) {
                        throw Exception("Gagal menghapus video")
                    }

                    holder.btnBookmark.setColorFilter(Color.WHITE)

                    Toast.makeText(
                        context,
                        "Video dihapus dari tersimpan",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {
                    video.videoFile.copyTo(
                        savedFile,
                        overwrite = true
                    )

                    holder.btnBookmark.setColorFilter(Color.YELLOW)

                    Toast.makeText(
                        context,
                        "Video berhasil disimpan",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Gagal menyimpan video",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // BAGIKAN
        holder.btnShare.setOnClickListener {
            try {
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

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }

                context.startActivity(
                    Intent.createChooser(
                        intent,
                        "Bagikan video"
                    )
                )

            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Gagal membagikan video",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Komentar akan dihubungkan ke halaman komentar.
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
