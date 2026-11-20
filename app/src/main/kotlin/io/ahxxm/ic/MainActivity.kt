package io.ahxxm.ic

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.ahxxm.ic.domain.ImageItem
import io.ahxxm.ic.ui.AppNavigation
import io.ahxxm.ic.ui.theme.ImageCompressorTheme

class MainActivity : ComponentActivity() {

    // Images received via the share sheet, waiting for the navigation to pick them up
    private var incomingShared by mutableStateOf<List<ImageItem>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        incomingShared = sharedImagesFrom(intent)
        setContent {
            ImageCompressorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Screens below have their own Scaffold, which would apply these insets again.
                    AppNavigation(
                        modifier = Modifier
                            .consumeWindowInsets(innerPadding)
                            .padding(innerPadding),
                        incomingShared = incomingShared,
                        onIncomingSharedConsumed = { incomingShared = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        sharedImagesFrom(intent)?.let { incomingShared = it }
    }

    private fun sharedImagesFrom(intent: Intent?): List<ImageItem>? {
        if (intent == null) return null
        val uris = when (intent.action) {
            Intent.ACTION_SEND -> listOfNotNull(intent.streamExtra())
            Intent.ACTION_SEND_MULTIPLE -> intent.streamListExtra()
            else -> return null
        }
        return uris.mapNotNull { it.toSharedImage() }.takeIf { it.isNotEmpty() }
    }

    private fun Uri.toSharedImage(): ImageItem? {
        val mime = contentResolver.getType(this) ?: return null
        var name = "image"
        var size = 0L
        contentResolver.query(this, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    name = cursor.getString(0) ?: name
                    if (!cursor.isNull(1)) size = cursor.getLong(1)
                }
            }
        return ImageItem(
            uri = this,
            name = name,
            sizeBytes = size,
            mimeType = mime,
            replaceInPlace = false
        ).takeIf { it.isProcessable }
    }

    @Suppress("DEPRECATION")
    private fun Intent.streamExtra(): Uri? =
        if (Build.VERSION.SDK_INT >= 33) getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        else getParcelableExtra(Intent.EXTRA_STREAM)

    @Suppress("DEPRECATION")
    private fun Intent.streamListExtra(): List<Uri> {
        val list = if (Build.VERSION.SDK_INT >= 33) getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
        else getParcelableArrayListExtra(Intent.EXTRA_STREAM)
        return list ?: emptyList()
    }
}
