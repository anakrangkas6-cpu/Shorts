package com.example.shorts

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.VideoView
import androidx.recyclerview.widget.RecyclerView

class ShortsAdapter(private val list: List<VideoItem>) : RecyclerView.Adapter<ShortsAdapter.Holder>() {
    class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val video: VideoView = v.findViewById(R.id.videoView)
        val likes: TextView = v.findViewById(R.id.tvLikes)
        val subscribe: TextView = v.findViewById(R.id.btnSubscribe)
        val likeBtn: View = v.findViewById(R.id.btnLike)
    }
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = Holder(LayoutInflater.from(p.context).inflate(R.layout.item_short, p, false))
    override fun getItemCount() = list.size
    override fun onBindViewHolder(h: Holder, pos: Int) {
        val item = list[pos]
        h.likes.text = item.likes
        h.video.setVideoURI(Uri.parse(item.url))
        h.video.setOnPreparedListener { it.isLooping = true; h.video.start() }
        var liked = false
        h.likeBtn.setOnClickListener {
            liked = !liked
            h.likeBtn.alpha = if (liked) 1f else 0.7f
        }
        var sub = false
        h.subscribe.setOnClickListener {
            sub = !sub
            h.subscribe.text = if (sub) "Subscribed" else "Subscribe"
        }
    }
    override fun onViewRecycled(h: Holder) { h.video.stopPlayback(); super.onViewRecycled(h) }
}
