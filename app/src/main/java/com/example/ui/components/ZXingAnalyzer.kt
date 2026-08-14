package com.example.ui.components

import android.graphics.ImageFormat
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

class ZXingAnalyzer(private val onQrCodeScanned: (ByteArray) -> Unit) : ImageAnalysis.Analyzer {
    private val reader = MultiFormatReader().apply {
        setHints(mapOf(DecodeHintType.CHARACTER_SET to "ISO-8859-1"))
    }

    override fun analyze(image: ImageProxy) {
        if (image.format == ImageFormat.YUV_420_888) {
            val buffer = image.planes[0].buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)
            
            val source = PlanarYUVLuminanceSource(
                data, image.width, image.height,
                0, 0, image.width, image.height, false
            )
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            
            try {
                val result = reader.decodeWithState(binaryBitmap)
                val rawBytes = result.text.toByteArray(Charsets.ISO_8859_1)
                onQrCodeScanned(rawBytes)
            } catch (e: Exception) {
                // Not found
            } finally {
                reader.reset()
            }
        }
        image.close()
    }
}
