package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.viewmodel.FinancialViewModel
import java.util.UUID

@Composable
fun FullScreenOverlay(
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val overlayId = androidx.compose.runtime.remember { UUID.randomUUID().toString() }
    
    DisposableEffect(overlayId) {
        viewModel.registerSubScreen(overlayId, true)
        onDispose {
            viewModel.registerSubScreen(overlayId, false)
        }
    }
    
    BackHandler {
        onDismiss()
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        DialogEdgeToEdge()
        AppThemeProvider(viewModel = viewModel) {
            Box(
                modifier = modifier.fillMaxSize().zIndex(1000f)
            ) {
                content()
            }
        }
    }
}
