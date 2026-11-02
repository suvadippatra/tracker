package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@Composable
fun TestPopup() {
    Popup(
        properties = PopupProperties(focusable = true, clippingEnabled = false, excludeFromSystemGesture = true),
        onDismissRequest = {}
    ) {
    }
}
