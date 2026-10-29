package com.gallery.app.data.model

/**
 * Represents a single media item (image or video) from the device.
 */
data class MediaItem(
    val id: Long,
    val uri: String,
    val name: String,
    val dateTaken: Long,
    val size: Long,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val bucketId: Long,
    val bucketName: String,
    val duration: Long = 0L  // For videos only (ms)
) {
    val isVideo: Boolean get() = mimeType.startsWith("video/")
    val isImage: Boolean get() = mimeType.startsWith("image/")
}

/**
 * Represents an album (folder) containing media items.
 */
data class Album(
    val id: Long,
    val name: String,
    val coverUri: String,
    val itemCount: Int
)
