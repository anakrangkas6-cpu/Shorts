
package com.example.shorts

import java.io.File

data class VideoItem(
    val videoFile: File,
    val username: String = "@username",
    val description: String = "",
    val likes: String = "0",
    val comments: String = "0",
    val shares: String = "0"
)
