package com.example.ui.components

import android.util.Log

fun testQr() {
    try {
        val bytes = ByteArray(750) { it.toByte() }
        val bitmap = generateQrCode(bytes, 400)
        Log.d("TEST_QR", "Bitmap: $bitmap")
    } catch (e: Exception) {
        Log.e("TEST_QR", "Error", e)
    }
}
