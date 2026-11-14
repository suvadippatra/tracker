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
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.detector.Detector
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
        val format: String = "QR_CODE",
        val matrixWidth: Int = 0,
        val matrixBitsHex: String = ""
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

    fun encodeBitMatrixHex(matrix: BitMatrix): Pair<Int, String> {
        val w = matrix.width
        val h = matrix.height
        if (w <= 0 || h <= 0 || w != h) return 0 to ""
        val totalBits = w * h
        val byteLen = (totalBits + 7) / 8
        val bytes = ByteArray(byteLen)
        var bitPos = 0
        for (y in 0 until h) {
            for (x in 0 until w) {
                if (matrix.get(x, y)) {
                    val byteIdx = bitPos ushr 3
                    val bitOffset = 7 - (bitPos and 7)
                    bytes[byteIdx] = (bytes[byteIdx].toInt() or (1 shl bitOffset)).toByte()
                }
                bitPos++
            }
        }
        val hexChars = CharArray(byteLen * 2)
        val digits = "0123456789ABCDEF"
        for (i in 0 until byteLen) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = digits[v ushr 4]
            hexChars[i * 2 + 1] = digits[v and 0x0F]
        }
        return w to String(hexChars)
    }

    fun decodeBitMatrixHex(width: Int, hex: String): BitMatrix? {
        if (width <= 0 || hex.isBlank()) return null
        return runCatching {
            val bytes = hexToBytes(hex)
            val totalBits = width * width
            val minBytes = (totalBits + 7) / 8
            if (bytes.size < minBytes) return@runCatching null
            val matrix = BitMatrix(width, width)
            var bitPos = 0
            for (y in 0 until width) {
                for (x in 0 until width) {
                    val byteIdx = bitPos ushr 3
                    val bitOffset = 7 - (bitPos and 7)
                    if (((bytes[byteIdx].toInt() ushr bitOffset) and 1) != 0) {
                        matrix.set(x, y)
                    }
                    bitPos++
                }
            }
            matrix
        }.getOrNull()
    }

    fun extractDecodedPayload(
        result: Result,
        sourceBitmap: BinaryBitmap? = null
    ): DecodedQrPayload? {
        val base = decodedPayloadFromResult(result) ?: return null
        if (sourceBitmap != null) {
            val detected = runCatching {
                Detector(sourceBitmap.blackMatrix).detect().bits
            }.getOrNull()
            if (detected != null && detected.width > 0 && detected.width == detected.height) {
                val (mw, mx) = encodeBitMatrixHex(detected)
                if (mw > 0 && mx.isNotBlank()) {
                    return base.copy(matrixWidth = mw, matrixBitsHex = mx)
                }
            }
        }
        return base
    }

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

    data class ExtractionResult(
        val payloads: List<DecodedQrPayload>,
        val failedUris: List<String>
    )

    fun extractDecodedQrsFromUri(context: Context?, uriStr: String): List<DecodedQrPayload> {
        return extractDecodedQrsWithFailures(context, uriStr).payloads
    }

    fun extractDecodedQrsWithFailures(context: Context?, uriStr: String): ExtractionResult {
        if (context == null || uriStr.isBlank()) return ExtractionResult(emptyList(), emptyList())
        val uris = uriStr.split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (uris.isEmpty()) return ExtractionResult(emptyList(), emptyList())

        val payloads = mutableListOf<DecodedQrPayload>()
        val failed = mutableListOf<String>()

        uris.forEach { singleUri ->
            val decoded = runCatching {
                val bitmap = decodeScaledBitmap(context, singleUri, maxDim = 1800) ?: return@runCatching null
                try {
                    decodeQrFromBitmap(bitmap)
                } finally {
                    if (!bitmap.isRecycled) bitmap.recycle()
                }
            }.getOrNull()
            if (decoded != null) {
                payloads.add(decoded)
            } else {
                failed.add(singleUri)
            }
        }
        return ExtractionResult(payloads, failed)
    }

    private fun decodeQrFromBitmap(bitmap: Bitmap): DecodedQrPayload? {
        val hints = mapOf(
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)
        )
        val reader = MultiFormatReader().apply { setHints(hints) }

        fun trySource(source: com.google.zxing.LuminanceSource): DecodedQrPayload? {
            val candidates = listOf(
                BinaryBitmap(HybridBinarizer(source)),
                BinaryBitmap(GlobalHistogramBinarizer(source)),
                BinaryBitmap(HybridBinarizer(source.invert())),
                BinaryBitmap(GlobalHistogramBinarizer(source.invert()))
            )
            for (candidate in candidates) {
                try {
                    val result = reader.decodeWithState(candidate)
                    val payload = extractDecodedPayload(result, candidate)
                    if (payload != null) return payload
                } catch (_: Exception) {
                    // Try next binarizer
                } finally {
                    reader.reset()
                }
            }
            return null
        }

        fun tryBitmap(bmp: Bitmap): DecodedQrPayload? {
            val w = bmp.width
            val h = bmp.height
            if (w <= 0 || h <= 0) return null
            val pixels = IntArray(w * h)
            bmp.getPixels(pixels, 0, w, 0, 0, w, h)
            trySource(RGBLuminanceSource(w, h, pixels))?.let { return it }

            // High-contrast thresholded fallback for stylized GPay / PhonePe / Paytm / tinted QRs
            val bwPixels = IntArray(w * h)
            var lumaSum = 0L
            val step = maxOf(1, (w * h) / 4096)
            var count = 0
            var i = 0
            while (i < pixels.size) {
                val c = pixels[i]
                val lum = (((c shr 16) and 0xFF) * 299 + ((c shr 8) and 0xFF) * 587 + (c and 0xFF) * 114) / 1000
                lumaSum += lum
                count++
                i += step
            }
            val threshold = if (count > 0) (lumaSum / count).toInt().coerceIn(70, 200) else 128
            for (idx in pixels.indices) {
                val c = pixels[idx]
                val lum = (((c shr 16) and 0xFF) * 299 + ((c shr 8) and 0xFF) * 587 + (c and 0xFF) * 114) / 1000
                bwPixels[idx] = if (lum > threshold) Color.WHITE else Color.BLACK
            }
            trySource(RGBLuminanceSource(w, h, bwPixels))?.let { return it }
            return null
        }

        // 1. Full image pass
        tryBitmap(bitmap)?.let { return it }

        val w = bitmap.width
        val h = bitmap.height

        // 2. Sub-region crops for tall payment posters (GPay / PhonePe / Paytm)
        val crops = mutableListOf<android.graphics.Rect>()
        val side = minOf(w, h)
        if (side >= 80 && (h > w * 1.15f || w > h * 1.15f)) {
            val cropSide = (side * 0.9f).toInt().coerceAtLeast(64)
            val left = ((w - cropSide) / 2).coerceAtLeast(0)
            // Center crop
            crops.add(android.graphics.Rect(left, ((h - cropSide) / 2).coerceAtLeast(0), (left + cropSide).coerceAtMost(w), (((h - cropSide) / 2) + cropSide).coerceAtMost(h)))
            // Lower-middle crop (typical UPI poster QR position)
            val lowerTop = ((h * 0.28f).toInt()).coerceIn(0, (h - cropSide).coerceAtLeast(0))
            crops.add(android.graphics.Rect(left, lowerTop, (left + cropSide).coerceAtMost(w), (lowerTop + cropSide).coerceAtMost(h)))
            // Upper-middle crop
            val upperTop = ((h * 0.12f).toInt()).coerceIn(0, (h - cropSide).coerceAtLeast(0))
            crops.add(android.graphics.Rect(left, upperTop, (left + cropSide).coerceAtMost(w), (upperTop + cropSide).coerceAtMost(h)))
        } else if (side >= 120) {
            val centerSide = (side * 0.72f).toInt().coerceAtLeast(64)
            val left = ((w - centerSide) / 2).coerceAtLeast(0)
            val top = ((h - centerSide) / 2).coerceAtLeast(0)
            crops.add(android.graphics.Rect(left, top, (left + centerSide).coerceAtMost(w), (top + centerSide).coerceAtMost(h)))
        }

        for (rect in crops) {
            val cw = rect.width()
            val ch = rect.height()
            if (cw <= 20 || ch <= 20) continue
            val cropped = Bitmap.createBitmap(bitmap, rect.left, rect.top, cw, ch)
            try {
                tryBitmap(cropped)?.let { return it }
            } finally {
                if (cropped !== bitmap && !cropped.isRecycled) cropped.recycle()
            }
        }

        // 3. 90 / 180 / 270 rotation passes for skewed/sideways QR photos
        for (angle in floatArrayOf(90f, 180f, 270f)) {
            val matrix = android.graphics.Matrix().apply { postRotate(angle) }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true)
            try {
                tryBitmap(rotated)?.let { return it }
            } finally {
                if (rotated !== bitmap && !rotated.isRecycled) rotated.recycle()
            }
        }

        return null
    }

    /**
     * Binary Eye-inspired exact module matrix generation:
     * Uses the raw detected BitMatrix (if captured) so every module—mask, version, and finder pattern—
     * reconstructs pixel-for-pixel identically. Falls back to QRCodeWriter with original EC level.
     */
    fun reconstructExactQrBitmap(
        payload: DecodedQrPayload,
        targetSize: Int = 900,
        quietZoneModules: Int = 4
    ): Bitmap? {
        return runCatching {
            val capturedMatrix = decodeBitMatrixHex(payload.matrixWidth, payload.matrixBitsHex)
            if (capturedMatrix != null && capturedMatrix.width > 0 && capturedMatrix.height > 0) {
                val mw = capturedMatrix.width
                val mh = capturedMatrix.height
                val qz = quietZoneModules.coerceAtLeast(0)
                val fullW = mw + qz * 2
                val fullH = mh + qz * 2
                val scale = (targetSize / maxOf(fullW, fullH)).coerceAtLeast(4)
                val outW = fullW * scale
                val outH = fullH * scale
                val pixels = IntArray(outW * outH) { Color.WHITE }

                for (my in 0 until mh) {
                    val yOffset = (my + qz) * scale
                    for (mx in 0 until mw) {
                        if (capturedMatrix.get(mx, my)) {
                            val xOffset = (mx + qz) * scale
                            for (dy in 0 until scale) {
                                val rowStart = (yOffset + dy) * outW + xOffset
                                for (dx in 0 until scale) {
                                    pixels[rowStart + dx] = Color.BLACK
                                }
                            }
                        }
                    }
                }
                return@runCatching Bitmap.createBitmap(pixels, outW, outH, Bitmap.Config.ARGB_8888)
            }

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
        val bitmap = reconstructExactQrBitmap(payload, targetSize = targetSize, quietZoneModules = 4) ?: return null
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
        return reconstructExactQrPngFromBitMatrix(context, result, null)
    }

    fun reconstructExactQrPngFromBitMatrix(
        context: Context,
        result: Result,
        sourceBitmap: BinaryBitmap?
    ): String? {
        val payload = extractDecodedPayload(result, sourceBitmap) ?: return null
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
                if (p.matrixWidth > 0 && p.matrixBitsHex.isNotBlank()) {
                    put("mw", p.matrixWidth)
                    put("mx", p.matrixBitsHex)
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
                            charset = obj.optString("cs", "UTF-8"),
                            matrixWidth = obj.optInt("mw", 0),
                            matrixBitsHex = obj.optString("mx", "")
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
        val orientation = runCatching {
            openStreamForUri(context, uri)?.use { stream ->
                android.media.ExifInterface(stream).getAttributeInt(
                    android.media.ExifInterface.TAG_ORIENTATION,
                    android.media.ExifInterface.ORIENTATION_NORMAL
                )
            } ?: android.media.ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(android.media.ExifInterface.ORIENTATION_NORMAL)

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
        val rawDecoded = openStreamForUri(context, uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null

        val rotMatrix = android.graphics.Matrix()
        var needsTransform = true
        when (orientation) {
            android.media.ExifInterface.ORIENTATION_ROTATE_90 -> rotMatrix.postRotate(90f)
            android.media.ExifInterface.ORIENTATION_ROTATE_180 -> rotMatrix.postRotate(180f)
            android.media.ExifInterface.ORIENTATION_ROTATE_270 -> rotMatrix.postRotate(270f)
            android.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> rotMatrix.postScale(-1f, 1f)
            android.media.ExifInterface.ORIENTATION_FLIP_VERTICAL -> rotMatrix.postScale(1f, -1f)
            else -> needsTransform = false
        }

        val oriented = if (needsTransform) {
            val rotated = Bitmap.createBitmap(rawDecoded, 0, 0, rawDecoded.width, rawDecoded.height, rotMatrix, true)
            if (rotated !== rawDecoded) rawDecoded.recycle()
            rotated
        } else {
            rawDecoded
        }

        if (oriented.width <= maxDim && oriented.height <= maxDim) return oriented

        val ratio = oriented.width.toFloat() / oriented.height.toFloat()
        val targetW = if (oriented.width >= oriented.height) maxDim else (maxDim * ratio).toInt().coerceAtLeast(1)
        val targetH = if (oriented.height > oriented.width) maxDim else (maxDim / ratio).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(oriented, targetW, targetH, true)
        if (scaled !== oriented) oriented.recycle()
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
