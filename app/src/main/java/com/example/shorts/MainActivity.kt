
package com.example.shorts

import android.net.Uri
import android.os.Bundle
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: ShortsAdapter
    private lateinit var shortsFolder: File

    private val videoPicker =
        registerForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments()
        ) { uris ->

            if (uris.isNotEmpty()) {
                var imported = 0

                uris.forEach { uri ->
                    if (importVideo(uri)) imported++
                }

                loadVideos()

                Toast.makeText(
                    this,
                    "$imported video berhasil ditambahkan",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewPager = findViewById(R.id.viewPager)

        // Folder privat Shorts
        shortsFolder = File(filesDir, "Shorts")

        if (!shortsFolder.exists()) {
            shortsFolder.mkdirs()
        }

        viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL
        viewPager.offscreenPageLimit = 1

        // Hubungkan perpindahan halaman dengan pemutar video
        viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    if (::adapter.isInitialized) {
                        adapter.setActivePosition(position)
                    }
                }
            }
        )

        // Tombol tambah video
        findViewById<ImageView>(R.id.navAdd)
            .setOnClickListener {
                videoPicker.launch(arrayOf("video/*"))
            }

        loadVideos()
    }

    private fun importVideo(uri: Uri): Boolean {
        return try {
            val mimeType = contentResolver.getType(uri)
                ?: return false

            if (!mimeType.startsWith("video/")) {
                return false
            }

            val extension = MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(mimeType)
                ?: "mp4"

            val file = File(
                shortsFolder,
                "video_${System.currentTimeMillis()}.$extension"
            )

            contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return false

            file.exists() && file.length() > 0

        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun loadVideos() {

        val videoFiles = shortsFolder.listFiles()
            ?.filter {
                it.isFile &&
                it.extension.lowercase() in listOf(
                    "mp4", "mkv", "webm", "3gp", "mov"
                ) &&
                it.length() > 0
            }
            ?.sortedBy { it.name }
            ?: emptyList()

        val videos = videoFiles.map { file ->
            VideoItem(
                videoFile = file,
                username = "@username",
                description = file.nameWithoutExtension,
                likes = "0",
                comments = "0",
                shares = "0"
            )
        }

        adapter = ShortsAdapter(videos)
        viewPager.adapter = adapter

        if (videos.isNotEmpty()) {
            adapter.setActivePosition(0)
        }
    }
}
