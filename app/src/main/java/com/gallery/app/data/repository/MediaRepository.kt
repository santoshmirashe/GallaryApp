package com.gallery.app.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.gallery.app.data.model.Album
import com.gallery.app.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository responsible for fetching media (images & videos) from the device
 * using the MediaStore content provider.
 */
class MediaRepository(private val context: Context) {

    /**
     * Loads all images and videos from device storage, sorted by date (newest first).
     */
    suspend fun getAllMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaItems = mutableListOf<MediaItem>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_TAKEN,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.WIDTH,
            MediaStore.Files.FileColumns.HEIGHT,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.BUCKET_ID,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Video.VideoColumns.DURATION
        )

        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ?"
        val selectionArgs = arrayOf("image/%", "video/%")
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_TAKEN} DESC"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val dateTakenCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_TAKEN)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
            val mimeTypeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val mimeType = cursor.getString(mimeTypeCol) ?: continue

                val contentUri: Uri = if (mimeType.startsWith("video/")) {
                    ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                } else {
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                }

                mediaItems.add(
                    MediaItem(
                        id = id,
                        uri = contentUri.toString(),
                        name = cursor.getString(nameCol) ?: "Unknown",
                        dateTaken = cursor.getLong(dateTakenCol),
                        size = cursor.getLong(sizeCol),
                        width = cursor.getInt(widthCol),
                        height = cursor.getInt(heightCol),
                        mimeType = mimeType,
                        bucketId = cursor.getLong(bucketIdCol),
                        bucketName = cursor.getString(bucketNameCol) ?: "Unknown",
                        duration = cursor.getLong(durationCol)
                    )
                )
            }
        }

        mediaItems
    }

    /**
     * Builds an album list from all media items, grouping by bucket/folder.
     */
    suspend fun getAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val allMedia = getAllMedia()
        allMedia
            .groupBy { it.bucketId }
            .map { (bucketId, items) ->
                Album(
                    id = bucketId,
                    name = items.first().bucketName,
                    coverUri = items.first().uri,
                    itemCount = items.size
                )
            }
            .sortedByDescending { it.itemCount }
    }

    /**
     * Returns media items belonging to a specific album (bucket).
     */
    suspend fun getMediaByAlbum(bucketId: Long): List<MediaItem> = withContext(Dispatchers.IO) {
        getAllMedia().filter { it.bucketId == bucketId }
    }

    /**
     * Searches media by filename (case-insensitive).
     */
    suspend fun searchMedia(query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext getAllMedia()
        getAllMedia().filter { it.name.contains(query, ignoreCase = true) }
    }

    /**
     * Deletes a media item from the device.
     * Returns true if deletion was successful.
     * On Android 10+, returns false — caller should use MediaStore.createDeleteRequest().
     */
    fun deleteMedia(item: MediaItem): Boolean {
        return try {
            val uri = Uri.parse(item.uri)
            context.contentResolver.delete(uri, null, null) > 0
        } catch (e: Exception) {
            false
        }
    }
}
