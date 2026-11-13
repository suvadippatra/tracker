package com.example.ui.components

import android.graphics.ImageFormat
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer

class ZXingAnalyzer(
    private val onQrCodeScanned: (ByteArray) -> Unit,
    private val onQrResult: ((Result) -> Unit)? = null
) : ImageAnalysis.Analyzer {
    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.CHARACTER_SET to "ISO-8859-1",
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.TRY_HARDER to true
            )
        )
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
            val normalBitmap = BinaryBitmap(HybridBinarizer(source))

            try {
                val result = try {
                    reader.decodeWithState(normalBitmap)
                } catch (_: Exception) {
                    reader.reset()
                    val invertedBitmap = BinaryBitmap(HybridBinarizer(source.invert()))
                    reader.decodeWithState(invertedBitmap)
                }
                onQrResult?.invoke(result)
                val rawBytes = (result.text ?: "").toByteArray(Charsets.ISO_8859_1)
                if (rawBytes.isNotEmpty()) {
                    onQrCodeScanned(rawBytes)
                }
            } catch (_: Exception) {
                // Not found
            } finally {
                reader.reset()
            }
        }
        image.close()
    }
}
