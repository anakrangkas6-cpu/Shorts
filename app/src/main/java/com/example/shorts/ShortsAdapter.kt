package com.example.shorts

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView

class ShortsAdapter(
    private val context: Context,
    private val items: List<VideoItem>,
    private val isLiked: (String) -> Boolean,
    private val setLiked: (String, Boolean) -> Unit,
    private val isSaved: (String) -> Boolean,
    private val setSaved: (String, Boolean) -> Unit,
    private val onComment: (VideoItem) -> Unit,
    private val onShare: (VideoItem) -> Unit
) : RecyclerView.Adapter<ShortsAdapter.VideoHolder>() {

    private val holders = mutableMapOf<Int, VideoHolder>()
    private var activePosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoHolder {
        return VideoHolder(context)
    }

    override fun onBindViewHolder(holder: VideoHolder, position: Int) {
        holders[position] = holder
        holder.bind(items[position], position)
        if (position == activePosition) holder.play() else holder.pause()
    }

    override fun getItemCount() = items.size

    override fun onViewRecycled(holder: VideoHolder) {
        holders.entries.removeAll { it.value === holder }
        holder.release()
        super.onViewRecycled(holder)
    }

    fun playAt(position: Int) {
        activePosition = position
        holders.forEach { (index, holder) ->
            if (index == position) holder.play() else holder.pause()
        }
    }

    fun pauseAll() {
        holders.values.forEach { it.pause() }
    }

    inner class VideoHolder(private val ctx: Context) : RecyclerView.ViewHolder(
        FrameLayout(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(-1, -1)
            setBackgroundColor(Color.BLACK)
        }
    ) {
        private val container = itemView as FrameLayout
        private val playerView = PlayerView(ctx)
        private var player: ExoPlayer? = null
        private var currentItem: VideoItem? = null
        private val likeButton = actionButton("♡\nSuka")
        private val commentButton = actionButton("▢\nKomentar")
        private val saveButton = actionButton("☆\nSimpan")
        private val shareButton = actionButton("↗\nBagikan")
        private val caption = TextView(ctx)

        init {
            playerView.useController = false
            playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            container.addView(playerView, FrameLayout.LayoutParams(-1, -1))
            val shade = View(ctx).apply {
                setBackgroundColor(0x33000000)
            }
            container.addView(shade, FrameLayout.LayoutParams(-1, -1))
            val rightActions = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }
            listOf(likeButton, commentButton, saveButton, shareButton).forEach {
                rightActions.addView(it, LinearLayout.LayoutParams(dp(76), dp(68)))
            }
            val actionParams = FrameLayout.LayoutParams(dp(82), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.END or Gravity.CENTER_VERTICAL)
            actionParams.marginEnd = dp(8)
            container.addView(rightActions, actionParams)

            caption.apply {
                setTextColor(Color.WHITE)
                textSize = 15f
                setShadowLayer(4f, 1f, 1f, Color.BLACK)
                setPadding(dp(16), dp(12), dp(100), dp(24))
            }
            val captionParams = FrameLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM)
            container.addView(caption, captionParams)

            container.setOnClickListener {
                player?.let {
                    if (it.isPlaying) it.pause() else it.play()
                }
            }
        }

        fun bind(item: VideoItem, position: Int) {
            currentItem = item
            player?.release()
            player = ExoPlayer.Builder(ctx).build().also { exo ->
                playerView.player = exo
                exo.setMediaItem(MediaItem.fromUri(Uri.fromFile(item.file)))
                exo.repeatMode = Player.REPEAT_MODE_ONE
                exo.prepare()
                exo.playWhenReady = false
            }
            caption.text = item.title
            updateButtons(item)
            likeButton.setOnClickListener {
                val next = !isLiked(item.file.name)
                setLiked(item.file.name, next)
                updateButtons(item)
            }
            commentButton.setOnClickListener { onComment(item) }
            saveButton.setOnClickListener {
                val next = !isSaved(item.file.name)
                setSaved(item.file.name, next)
                updateButtons(item)
            }
            shareButton.setOnClickListener { onShare(item) }
        }

        private fun updateButtons(item: VideoItem) {
            likeButton.text = if (isLiked(item.file.name)) "♥\nDisukai" else "♡\nSuka"
            likeButton.setTextColor(if (isLiked(item.file.name)) 0xFFFF496C.toInt() else Color.WHITE)
            saveButton.text = if (isSaved(item.file.name)) "★\nTersimpan" else "☆\nSimpan"
        }

        fun play() { player?.playWhenReady = true }
        fun pause() { player?.playWhenReady = false }
        fun release() {
            playerView.player = null
            player?.release()
            player = null
        }

        private fun actionButton(label: String) = TextView(ctx).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = 12f
            setTextColor(Color.WHITE)
            setShadowLayer(5f, 1f, 1f, Color.BLACK)
            isClickable = true
            isFocusable = true
        }

        private fun dp(value: Int) = (value * ctx.resources.displayMetrics.density).toInt()
    }

    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()
}
