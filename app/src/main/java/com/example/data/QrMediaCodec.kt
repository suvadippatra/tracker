package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.util.Base64
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.Result
import com.google.zxing.ResultMetadataType
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Handles zero-Base64 payment QR payload extraction and Binary Eye-inspired exact
 * module-for-module pixel reconstruction into lossless PNG files.
 */
object QrMediaCodec {

    data class DecodedQrPayload(
        val text: String,
        val rawByteHex: String = "",
        val ecLevel: String = "M",
        val charset: String = "UTF-8",
        val format: String = "QR_CODE"
    )

    data class PaymentQrTransferItem(
        val key: String,
        val entityType: String,
        val entityName: String,
        val originalId: Int,
        val slotIndex: Int,
        val totalSlots: Int,
        val decodedQr: DecodedQrPayload
    )

    fun decodedPayloadFromResult(result: Result): DecodedQrPayload? {
        if (result.barcodeFormat != BarcodeFormat.QR_CODE) return null
        val text = result.text ?: ""
        val metadata = result.resultMetadata

        val ecLevel = metadata?.get(ResultMetadataType.ERROR_CORRECTION_LEVEL)
            ?.toString()
            ?.uppercase()
            ?.takeIf { it == "L" || it == "M" || it == "Q" || it == "H" }
            ?: "M"

        @Suppress("UNCHECKED_CAST")
        val byteSegments = metadata?.get(ResultMetadataType.BYTE_SEGMENTS) as? List<ByteArray>
        val rawBytes = if (!byteSegments.isNullOrEmpty()) {
            val totalLen = byteSegments.sumOf { it.size }
            ByteArray(totalLen).also { out ->
                var offset = 0
                for (seg in byteSegments) {
                    System.arraycopy(seg, 0, out, offset, seg.size)
                    offset += seg.size
                }
            }
        } else null

        val utf8Bytes = text.toByteArray(Charsets.UTF_8)
        val useRawHex = rawBytes != null && rawBytes.isNotEmpty() && !rawBytes.contentEquals(utf8Bytes)
        val hex = if (useRawHex && rawBytes != null) {
            rawBytes.joinToString("") { "%02X".format(it) }
        } else {
            ""
        }
        val charset = if (useRawHex) "ISO-8859-1" else "UTF-8"

        if (text.isEmpty() && hex.isEmpty()) return null
        return DecodedQrPayload(
            text = text,
            rawByteHex = hex,
            ecLevel = ecLevel,
            charset = charset,
            format = BarcodeFormat.QR_CODE.name
        )
    }

    fun extractDecodedQrsFromUri(context: Context?, uriStr: String): List<DecodedQrPayload> {
        if (context == null || uriStr.isBlank()) return emptyList()
        val uris = uriStr.split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (uris.isEmpty()) return emptyList()

        return uris.mapNotNull { singleUri ->
            runCatching {
                val bitmap = decodeScaledBitmap(context, singleUri, maxDim = 1600) ?: return@runCatching null
                try {
                    decodeQrFromBitmap(bitmap)
                } finally {
                    if (!bitmap.isRecycled) bitmap.recycle()
                }
            }.getOrNull()
        }
    }

    private fun decodeQrFromBitmap(bitmap: Bitmap): DecodedQrPayload? {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return null

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val source = RGBLuminanceSource(width, height, pixels)

        val hints = mapOf(
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)
        )
        val reader = MultiFormatReader().apply { setHints(hints) }

        val candidates = listOf(
            BinaryBitmap(HybridBinarizer(source)),
            BinaryBitmap(GlobalHistogramBinarizer(source)),
            BinaryBitmap(HybridBinarizer(source.invert()))
        )

        for (candidate in candidates) {
            try {
                val result = reader.decodeWithState(candidate)
                val payload = decodedPayloadFromResult(result)
                if (payload != null) return payload
            } catch (_: Exception) {
                // Try next binarizer
            } finally {
                reader.reset()
            }
        }
        return null
    }

    /**
     * Binary Eye-inspired exact module matrix generation:
     * Encodes with margin and original error correction level at (0, 0) to obtain the exact
     * native BitMatrix, then integer-scales each module into crisp black/white pixels.
     */
    fun reconstructExactQrBitmap(
        payload: DecodedQrPayload,
        targetSize: Int = 900,
        quietZoneModules: Int = 2
    ): Bitmap? {
        return runCatching {
            val ec = when (payload.ecLevel.uppercase()) {
                "L" -> ErrorCorrectionLevel.L
                "Q" -> ErrorCorrectionLevel.Q
                "H" -> ErrorCorrectionLevel.H
                else -> ErrorCorrectionLevel.M
            }

            val content: String
            val charset: String
            if (payload.rawByteHex.isNotBlank()) {
                val bytes = hexToBytes(payload.rawByteHex)
                content = String(bytes, Charsets.ISO_8859_1)
                charset = "ISO-8859-1"
            } else {
                content = payload.text
                charset = payload.charset.ifBlank { "UTF-8" }
            }

            if (content.isEmpty()) return@runCatching null

            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ec,
                EncodeHintType.CHARACTER_SET to charset,
                EncodeHintType.MARGIN to quietZoneModules
            )

            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
            val mw = matrix.width
            val mh = matrix.height
            if (mw <= 0 || mh <= 0) return@runCatching null

            val scale = (targetSize / maxOf(mw, mh)).coerceAtLeast(4)
            val outW = mw * scale
            val outH = mh * scale
            val pixels = IntArray(outW * outH)

            for (my in 0 until mh) {
                val yOffset = my * scale
                for (mx in 0 until mw) {
                    val color = if (matrix.get(mx, my)) Color.BLACK else Color.WHITE
                    val xOffset = mx * scale
                    for (dy in 0 until scale) {
                        val rowStart = (yOffset + dy) * outW + xOffset
                        for (dx in 0 until scale) {
                            pixels[rowStart + dx] = color
                        }
                    }
                }
            }

            Bitmap.createBitmap(pixels, outW, outH, Bitmap.Config.ARGB_8888)
        }.getOrNull()
    }

    fun saveExactQrPng(context: Context, payload: DecodedQrPayload, targetSize: Int = 900): String? {
        val bitmap = reconstructExactQrBitmap(payload, targetSize = targetSize, quietZoneModules = 2) ?: return null
        return try {
            val dir = File(context.filesDir, "qr_codes").apply { mkdirs() }
            val outFile = File(dir, "qr_exact_${UUID.randomUUID()}.png")
            outFile.outputStream().use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            Uri.fromFile(outFile).toString()
        } catch (_: Exception) {
            null
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    fun reconstructExactQrPngFromResult(context: Context, result: Result): String? {
        val payload = decodedPayloadFromResult(result) ?: return null
        return saveExactQrPng(context, payload)
    }

    /**
     * Zero-Base64 export:
     * - In QR stream exports (`includeQrPayloads = false`), writes nothing so QRs never carry Base64/image bloat.
     * - In JSON/ZIP file exports (`includeQrPayloads = true`), writes compact decoded QR descriptors (`qrPayloads`)
     *   for exact pixel-free reconstruction on the receiver.
     */
    fun appendQrDataToJson(
        json: JSONObject,
        context: Context?,
        qrCodeUri: String,
        includeQrPayloads: Boolean = true
    ) {
        if (!includeQrPayloads || qrCodeUri.isBlank() || context == null) return
        val payloads = extractDecodedQrsFromUri(context, qrCodeUri)
        if (payloads.isEmpty()) return

        val arr = JSONArray()
        payloads.forEach { p ->
            arr.put(JSONObject().apply {
                put("t", p.text)
                put("ec", p.ecLevel)
                put("cs", p.charset)
                if (p.rawByteHex.isNotBlank()) {
                    put("hx", p.rawByteHex)
                }
            })
        }
        json.put("qrPayloads", arr)
    }

    fun resolveImportedQrUri(json: JSONObject, context: Context?): String {
        val rawFallbackUri = json.optString("qrCodeUri", "")
        if (context != null) {
            val restoredList = mutableListOf<String>()

            // 1. Primary zero-Base64 exact Binary Eye reconstruction from compact decoded QR descriptors
            if (json.has("qrPayloads")) {
                val arr = json.optJSONArray("qrPayloads")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val payload = DecodedQrPayload(
                            text = obj.optString("t", ""),
                            rawByteHex = obj.optString("hx", ""),
                            ecLevel = obj.optString("ec", "M"),
                            charset = obj.optString("cs", "UTF-8")
                        )
                        saveExactQrPng(context, payload)?.let { restoredList.add(it) }
                    }
                }
            }

            // 2. Read-only backward compatibility for legacy Base64 JSON backups
            if (restoredList.isEmpty() && json.has("qrCodeDataList")) {
                val arr = json.optJSONArray("qrCodeDataList")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val b64 = arr.optString(i, "")
                        decodeLegacyBase64ToLocalUri(context, b64)?.let { restoredList.add(it) }
                    }
                }
            }
            if (restoredList.isEmpty() && json.has("qrCodeData")) {
                val b64 = json.optString("qrCodeData", "")
                decodeLegacyBase64ToLocalUri(context, b64)?.let { restoredList.add(it) }
            }

            if (restoredList.isNotEmpty()) {
                return restoredList.joinToString(",")
            }
        }
        return filterLocallyAccessibleUris(rawFallbackUri)
    }

    private fun hexToBytes(hex: String): ByteArray {
        val clean = hex.trim()
        val len = clean.length / 2
        val out = ByteArray(len)
        for (i in 0 until len) {
            val idx = i * 2
            out[i] = clean.substring(idx, idx + 2).toInt(16).toByte()
        }
        return out
    }

    private fun filterLocallyAccessibleUris(raw: String): String {
        if (raw.isBlank()) return ""
        val valid = raw.split(",").map { it.trim() }.filter { uriStr ->
            if (uriStr.isBlank()) false
            else if (uriStr.startsWith("file://")) {
                val path = runCatching { Uri.parse(uriStr).path }.getOrNull()
                !path.isNullOrBlank() && File(path).exists()
            } else if (uriStr.startsWith("/")) {
                File(uriStr).exists()
            } else {
                true
            }
        }
        return valid.joinToString(",")
    }

    private fun decodeLegacyBase64ToLocalUri(context: Context, base64Str: String): String? {
        if (base64Str.isBlank()) return null
        return runCatching {
            val bytes = Base64.decode(base64Str, Base64.DEFAULT)
            if (bytes.isEmpty()) return@runCatching null
            val dir = File(context.filesDir, "qr_codes").apply { mkdirs() }
            val outFile = File(dir, "qr_imp_${UUID.randomUUID()}.jpg")
            outFile.outputStream().use { it.write(bytes) }
            Uri.fromFile(outFile).toString()
        }.getOrNull()
    }

    private fun decodeScaledBitmap(context: Context, uriStr: String, maxDim: Int): Bitmap? {
        val uri = when {
            uriStr.startsWith("/") -> Uri.fromFile(File(uriStr))
            else -> Uri.parse(uriStr)
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStreamForUri(context, uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val w = bounds.outWidth
        val h = bounds.outHeight
        if (w <= 0 || h <= 0) return null

        var sample = 1
        while ((w / sample) > maxDim * 2 || (h / sample) > maxDim * 2) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val raw = openStreamForUri(context, uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
        if (raw.width <= maxDim && raw.height <= maxDim) return raw

        val ratio = raw.width.toFloat() / raw.height.toFloat()
        val targetW = if (raw.width >= raw.height) maxDim else (maxDim * ratio).toInt().coerceAtLeast(1)
        val targetH = if (raw.height > raw.width) maxDim else (maxDim / ratio).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(raw, targetW, targetH, true)
        if (scaled !== raw) raw.recycle()
        return scaled
    }

    private fun openStreamForUri(context: Context, uri: Uri): java.io.InputStream? {
        return when (uri.scheme) {
            "file", null -> uri.path?.let { path ->
                val f = File(path)
                if (f.exists()) f.inputStream() else null
            }
            else -> context.contentResolver.openInputStream(uri)
        }
    }
}
