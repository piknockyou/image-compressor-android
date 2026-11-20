package io.ahxxm.ic

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import io.ahxxm.ic.domain.imageItemFrom
import io.ahxxm.ic.ui.AppNavigation
import io.ahxxm.ic.ui.theme.ImageCompressorTheme

class MainActivity : ComponentActivity() {

    // Images received via the share sheet or Open with, waiting for the navigation to pick them up
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
            // Open with: some apps pass several files as ClipData, so read them all
            Intent.ACTION_VIEW -> intent.viewUris()
            else -> return null
        }
        return uris.mapNotNull { imageItemFrom(this, it) }.takeIf { it.isNotEmpty() }
    }

    private fun Intent.viewUris(): List<Uri> {
        val clip = clipData
        if (clip != null && clip.itemCount > 0) {
            return (0 until clip.itemCount).mapNotNull { clip.getItemAt(it).uri }
        }
        return listOfNotNull(data)
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
