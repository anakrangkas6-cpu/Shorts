package com.example.shorts

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.viewpager2.widget.ViewPager2
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var pager: ViewPager2
    private lateinit var adapter: ShortsAdapter
    private lateinit var emptyPanel: LinearLayout
    private lateinit var profileButton: TextView
    private val prefs by lazy { getSharedPreferences("shorts_prefs", MODE_PRIVATE) }
    private val shortsDir by lazy { File(filesDir, "Shorts").apply { mkdirs() } }

    private val pickVideos = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (!uris.isNullOrEmpty()) {
            var copied = 0
            uris.forEachIndexed { index, uri ->
                try {
                    contentResolver.openInputStream(uri)?.use { input ->
                        val guessed = queryDisplayName(uri) ?: "video_${System.currentTimeMillis()}_$index.mp4"
                        val safeName = guessed.replace(Regex("[^A-Za-z0-9._-]"), "_")
                        val target = uniqueFile(File(shortsDir, safeName))
                        target.outputStream().use { output -> input.copyTo(output) }
                        copied++
                    }
                } catch (_: Exception) { }
            }
            loadVideos()
            Toast.makeText(this, "$copied video ditambahkan ke folder Shorts", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = 0
        buildScreen()
        loadVideos()
    }

    override fun onPause() {
        if (::adapter.isInitialized) adapter.pauseAll()
        super.onPause()
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF09090D.toInt())
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(10), dp(14), dp(10))
            setBackgroundColor(0xFF09090D.toInt())
        }
        val title = TextView(this).apply {
            text = "Shorts"
            textSize = 23f
            setTextColor(0xFFFFFFFF.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        header.addView(title, LinearLayout.LayoutParams(0, dp(42), 1f))
        val add = TextView(this).apply {
            text = "＋ Tambah video"
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(0xFF292933.toInt())
            setOnClickListener { pickVideos.launch(arrayOf("video/*")) }
        }
        header.addView(add)
        root.addView(header)

        val content = FrameLayout(this).apply {
            setBackgroundColor(0xFF000000.toInt())
        }
        pager = ViewPager2(this).apply {
            orientation = ViewPager2.ORIENTATION_VERTICAL
            offscreenPageLimit = 1
        }
        content.addView(pager, FrameLayout.LayoutParams(-1, -1))

        emptyPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(20), dp(28), dp(20))
            setBackgroundColor(0xFF111118.toInt())
            visibility = View.GONE
        }
        val emptyTitle = TextView(this).apply {
            text = "Belum ada video"
            textSize = 22f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
        }
        val emptyDesc = TextView(this).apply {
            text = "Tambahkan video ke folder Shorts untuk mulai menonton. Video disimpan dan diputar secara offline di aplikasi ini."
            textSize = 14f
            setTextColor(0xFFB8B8C2.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(18))
        }
        val emptyAdd = Button(this).apply {
            text = "Pilih video"
            setOnClickListener { pickVideos.launch(arrayOf("video/*")) }
        }
        emptyPanel.addView(emptyTitle)
        emptyPanel.addView(emptyDesc)
        emptyPanel.addView(emptyAdd)
        content.addView(emptyPanel, FrameLayout.LayoutParams(-1, -1))
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(8))
            setBackgroundColor(0xFF09090D.toInt())
        }
        val home = navItem("⌂\nBeranda") { pager.setCurrentItem(0, true) }
        val folder = navItem("▣\nFolder") { showFolder() }
        profileButton = navItem("●\nProfil") { showProfile() }
        nav.addView(home, LinearLayout.LayoutParams(0, dp(54), 1f))
        nav.addView(folder, LinearLayout.LayoutParams(0, dp(54), 1f))
        nav.addView(profileButton, LinearLayout.LayoutParams(0, dp(54), 1f))
        root.addView(nav)
        setContentView(root)
    }

    private fun navItem(label: String, action: () -> Unit): TextView =
        TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = 12f
            setTextColor(0xFFECECF2.toInt())
            setOnClickListener { action() }
        }

    private fun loadVideos() {
        val videos = shortsDir.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in listOf("mp4", "mkv", "webm", "3gp", "mov", "m4v") }
            ?.sortedByDescending { it.lastModified() }
            ?.map { VideoItem(it, it.nameWithoutExtension) }
            ?: emptyList()

        if (!::pager.isInitialized) return
        adapter = ShortsAdapter(
            context = this,
            items = videos,
            isLiked = { key -> prefs.getBoolean("like_$key", false) },
            setLiked = { key, value -> prefs.edit().putBoolean("like_$key", value).apply() },
            isSaved = { key -> prefs.getBoolean("saved_$key", false) },
            setSaved = { key, value -> prefs.edit().putBoolean("saved_$key", value).apply() },
            onComment = { item -> showComments(item) },
            onShare = { item -> shareVideo(item) }
        )
        pager.adapter = adapter
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                adapter.playAt(position)
            }
        })
        emptyPanel.visibility = if (videos.isEmpty()) View.VISIBLE else View.GONE
        pager.visibility = if (videos.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showFolder() {
        val files = shortsDir.listFiles()?.filter { it.isFile }?.sortedBy { it.name } ?: emptyList()
        val names = if (files.isEmpty()) arrayOf("Folder Shorts kosong") else files.map { it.name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Folder penyimpanan Shorts")
            .setItems(names) { _, which ->
                if (files.isNotEmpty()) {
                    val file = files[which]
                    AlertDialog.Builder(this)
                        .setTitle(file.name)
                        .setItems(arrayOf("Putar dari beranda", "Hapus video")) { _, action ->
                            if (action == 0) {
                                loadVideos()
                                val index = (shortsDir.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() }
                                    ?.indexOfFirst { it.name == file.name } ?: 0).coerceAtLeast(0)
                                pager.setCurrentItem(index, true)
                            } else {
                                AlertDialog.Builder(this).setMessage("Hapus video ini dari folder Shorts?")
                                    .setNegativeButton("Batal", null)
                                    .setPositiveButton("Hapus") { _, _ ->
                                        file.delete()
                                        loadVideos()
                                        Toast.makeText(this, "Video dihapus", Toast.LENGTH_SHORT).show()
                                    }.show()
                            }
                        }.show()
                }
            }
            .setPositiveButton("Tambah video") { _, _ -> pickVideos.launch(arrayOf("video/*")) }
            .setNegativeButton("Tutup", null)
            .show()
    }

    private fun showProfile() {
        val nameInput = EditText(this).apply {
            setText(prefs.getString("profile_name", "Pengguna Shorts"))
            hint = "Nama profil"
            setSingleLine()
        }
        val imagePreview = ImageView(this).apply {
            val savedImage = prefs.getString("profile_image", null)
            if (!savedImage.isNullOrBlank()) {
                try { setImageURI(Uri.parse(savedImage)) } catch (_: Exception) { }
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(0xFF292933.toInt())
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(imagePreview, LinearLayout.LayoutParams(dp(88), dp(88)))
            addView(nameInput, LinearLayout.LayoutParams(-1, -2))
        }
        AlertDialog.Builder(this)
            .setTitle("Profil")
            .setView(box)
            .setPositiveButton("Simpan") { _, _ ->
                prefs.edit().putString("profile_name", nameInput.text.toString().ifBlank { "Pengguna Shorts" }).apply()
                Toast.makeText(this, "Nama profil disimpan", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Pilih foto") { _, _ ->
                pickProfileImage.launch(arrayOf("image/*"))
            }
            .setNegativeButton("Tutup", null)
            .show()
    }

    private val pickProfileImage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            prefs.edit().putString("profile_image", uri.toString()).apply()
            Toast.makeText(this, "Foto profil tersimpan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showComments(item: VideoItem) {
        val key = item.file.name
        val comments = prefs.getString("comments_$key", "") ?: ""
        val input = EditText(this).apply {
            hint = "Tulis komentar..."
            setText(comments)
            minLines = 2
        }
        AlertDialog.Builder(this)
            .setTitle("Komentar lokal · ${item.title}")
            .setView(input)
            .setPositiveButton("Simpan") { _, _ ->
                prefs.edit().putString("comments_$key", input.text.toString()).apply()
                Toast.makeText(this, "Komentar disimpan di perangkat", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun shareVideo(item: VideoItem) {
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", item.file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "video/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(send, "Bagikan video"))
        } catch (e: Exception) {
            Toast.makeText(this, "Tidak dapat membagikan video: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        } catch (_: Exception) { null }
    }

    private fun uniqueFile(file: File): File {
        if (!file.exists()) return file
        val base = file.nameWithoutExtension
        val ext = file.extension
        var i = 1
        while (true) {
            val candidate = File(file.parentFile, "${base}_$i.$ext")
            if (!candidate.exists()) return candidate
            i++
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
