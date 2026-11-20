package io.ahxxm.ic.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.ahxxm.ic.domain.CompressionOptions
import io.ahxxm.ic.domain.ImageCompressionPreview
import io.ahxxm.ic.domain.ImageItem
import io.ahxxm.ic.domain.ImageSource
import kotlinx.serialization.Serializable

@Serializable data object FolderBrowser
@Serializable data object Options
@Serializable data object SavingsPreview
@Serializable data object Progress
@Serializable data object Done

/**
 * @param incomingShared images received from a share sheet; when non-null the app jumps to Options.
 * @param onIncomingSharedConsumed called once the shared images have been taken over.
 */
@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    incomingShared: List<ImageItem>? = null,
    onIncomingSharedConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    var currentSource by remember { mutableStateOf<ImageSource?>(null) }
    var currentOptions by remember { mutableStateOf(CompressionOptions()) }
    var selectedPreviews by remember { mutableStateOf<List<ImageCompressionPreview>>(emptyList()) }
    var completionCount by remember { mutableStateOf(0) }
    var completionSavedBytes by remember { mutableStateOf(0L) }

    LaunchedEffect(incomingShared) {
        val items = incomingShared ?: return@LaunchedEffect
        currentSource = ImageSource.Shared(items)
        navController.navigate(Options) {
            popUpTo<FolderBrowser> { inclusive = false }
        }
        onIncomingSharedConsumed()
    }

    NavHost(
        navController = navController,
        startDestination = FolderBrowser,
        modifier = modifier
    ) {
        composable<FolderBrowser> {
            FolderBrowserScreen(
                onSourceSelected = { source ->
                    currentSource = source
                    navController.navigate(Options)
                }
            )
        }

        composable<Options> {
            currentSource?.let { source ->
                OptionsScreen(
                    source = source,
                    onConfirm = { options ->
                        currentOptions = options
                        navController.navigate(SavingsPreview)
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable<SavingsPreview> {
            currentSource?.let { source ->
                SavingsPreviewScreen(
                    source = source,
                    options = currentOptions,
                    onConfirm = { previews ->
                        selectedPreviews = previews
                        navController.navigate(Progress)
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable<Progress> {
            ProgressScreen(
                selectedPreviews = selectedPreviews,
                onComplete = { count, savedBytes ->
                    completionCount = count
                    completionSavedBytes = savedBytes
                    navController.navigate(Done) {
                        popUpTo<FolderBrowser>()
                    }
                },
                onCancel = { navController.popBackStack() }
            )
        }

        composable<Done> {
            DoneScreen(
                count = completionCount,
                savedBytes = completionSavedBytes,
                onFinish = {
                    navController.navigate(FolderBrowser) {
                        popUpTo<FolderBrowser> { inclusive = true }
                    }
                }
            )
        }
    }
}
