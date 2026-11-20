package io.ahxxm.ic.domain

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Where the images to compress come from. */
sealed class ImageSource {
    abstract val name: String
    abstract suspend fun loadImages(context: Context): List<ImageItem>

    /** A folder from the MediaStore gallery index (the folder list in the app). */
    data class Bucket(val bucketId: Long, override val name: String) : ImageSource() {
        override suspend fun loadImages(context: Context): List<ImageItem> =
            ImageRepository(context.contentResolver).getImagesInFolder(bucketId)
    }

    /** A folder chosen with the system folder picker (any storage provider). */
    data class Tree(val treeUri: Uri, override val name: String) : ImageSource() {
        override suspend fun loadImages(context: Context): List<ImageItem> = withContext(Dispatchers.IO) {
            val folder = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
            folder.listFiles()
                .filter { it.isFile }
                .mapNotNull { doc ->
                    val mime = doc.type ?: return@mapNotNull null
                    val docName = doc.name ?: return@mapNotNull null
                    ImageItem(
                        uri = doc.uri,
                        name = docName,
                        sizeBytes = doc.length(),
                        mimeType = mime
                    )
                }
                .filter { it.isProcessable }
                .sortedBy { it.name.lowercase() }
        }
    }

    /** Images handed to the app from another app's share sheet. */
    data class Shared(val items: List<ImageItem>) : ImageSource() {
        override val name: String = "Shared images"
        override suspend fun loadImages(context: Context): List<ImageItem> = items
    }
}
