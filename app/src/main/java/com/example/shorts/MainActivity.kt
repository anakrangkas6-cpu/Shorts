package com.example.shorts

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val videos = listOf(
            VideoItem("https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", "@nurealam-h4z", "#shortsvideos", "74 rb", "298", "72 rb"),
            VideoItem("https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4", "@nurealam-h4z", "#shortsvideos", "74 rb", "298", "72 rb"),
            VideoItem("https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4", "@nurealam-h4z", "#shortsvideos", "74 rb", "298", "72 rb")
        )
        viewPager.adapter = ShortsAdapter(videos)
        viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL
    }
}
