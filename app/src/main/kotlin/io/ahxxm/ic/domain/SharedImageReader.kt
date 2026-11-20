package io.ahxxm.ic.domain

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File

/**
 * Builds an [ImageItem] from any image URI (share sheet, Open with, or a document picker).
 * Returns null for files that are not JPEG or PNG.
 * Picked/shared URIs are read-only, so the result is saved as a new file.
 */
fun imageItemFrom(context: Context, uri: Uri): ImageItem? {
    val resolver = context.contentResolver
    var name = uri.lastPathSegment?.substringAfterLast('/') ?: "image"
    var size = 0L
    // content:// comes from providers; file:// has no query support, so read the file directly
    if (uri.scheme == ContentResolver.SCHEME_FILE) {
        uri.path?.let { File(it) }?.takeIf { it.isFile }?.let { file ->
            name = file.name
            size = file.length()
        }
    } else {
        try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        name = cursor.getString(0) ?: name
                        if (!cursor.isNull(1)) size = cursor.getLong(1)
                    }
                }
        } catch (_: Exception) {
            // Keep the name from the URI path
        }
    }
    val mime = resolver.getType(uri)
        ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substringAfterLast('.', "").lowercase())
        ?: return null
    return ImageItem(
        uri = uri,
        name = name,
        sizeBytes = size,
        mimeType = mime,
        replaceInPlace = false
    ).takeIf { it.isProcessable }
}
