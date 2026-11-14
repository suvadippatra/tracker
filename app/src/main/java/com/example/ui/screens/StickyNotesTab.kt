@file:Suppress("DEPRECATION")
package com.example.ui.screens

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.StickyNoteEntity
import com.example.ui.components.*
import com.example.ui.theme.AppBoldFontFamily
import com.example.ui.theme.AppFontFamily
import com.example.ui.theme.ArchivoBlackFontFamily
import com.example.ui.theme.StickyBoldBaseFontFamily
import com.example.ui.viewmodel.FinancialViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Curated Material 3 Sticky Note Palette (Index 0 is Default White/Black according to Theme)
data class StickyColorPalette(
    val index: Int,
    val nameEn: String,
    val nameBn: String,
    val cardBgLight: Color,
    val cardBgDark: Color,
    val accentBorder: Color
)

val STICKY_COLOR_PALETTES = listOf(
    StickyColorPalette(0, "Default (Theme)", "ডিফল্ট", Color(0xFFFFFFFF), Color(0xFF141822), Color(0xFF64748B)),
    StickyColorPalette(1, "Warm Yellow", "হলুদ", Color(0xFFFFF8C5), Color(0xFF3B3510), Color(0xFFFBC02D)),
    StickyColorPalette(2, "Cyan Breeze", "সায়ান", Color(0xFFE0F7FA), Color(0xFF0E3338), Color(0xFF00ACC1)),
    StickyColorPalette(3, "Rose Pink", "গোলাপী", Color(0xFFFCE4EC), Color(0xFF3D1424), Color(0xFFD81B60)),
    StickyColorPalette(4, "Mint Green", "সবুজ", Color(0xFFE8F5E9), Color(0xFF14361C), Color(0xFF43A047)),
    StickyColorPalette(5, "Sunset Orange", "কমলা", Color(0xFFFFF3E0), Color(0xFF3E270E), Color(0xFFFB8C00)),
    StickyColorPalette(6, "Lavender Purple", "বেগুনি", Color(0xFFF3E5F5), Color(0xFF2E1438), Color(0xFF8E24AA))
)

fun getPaletteForIndex(index: Int): StickyColorPalette {
    val safeIdx = index.mod(STICKY_COLOR_PALETTES.size)
    return STICKY_COLOR_PALETTES[safeIdx]
}

// Serialization helpers for Table JSON
data class StickyTableData(
    val rows: Int,
    val cols: Int,
    val cells: List<List<String>>
) {
    fun toJsonObject(): JSONObject {
        val obj = JSONObject()
        obj.put("rows", rows)
        obj.put("cols", cols)
        val arr = JSONArray()
        for (r in 0 until rows) {
            val rowArr = JSONArray()
            for (c in 0 until cols) {
                rowArr.put(cells.getOrNull(r)?.getOrNull(c) ?: "")
            }
            arr.put(rowArr)
        }
        obj.put("cells", arr)
        return obj
    }

    fun toJson(): String = toJsonObject().toString()

    companion object {
        fun fromJsonObject(obj: JSONObject?): StickyTableData? {
            if (obj == null) return null
            val r = obj.optInt("rows", 0)
            val c = obj.optInt("cols", 0)
            if (r <= 0 || c <= 0) return null
            val arr = obj.optJSONArray("cells") ?: return null
            val matrix = mutableListOf<List<String>>()
            for (i in 0 until r) {
                val rowArr = arr.optJSONArray(i)
                val rowList = mutableListOf<String>()
                for (j in 0 until c) {
                    rowList.add(rowArr?.optString(j, "") ?: "")
                }
                matrix.add(rowList)
            }
            return StickyTableData(r, c, matrix)
        }

        fun fromJson(json: String): StickyTableData? {
            if (json.isBlank() || !json.trim().startsWith("{")) return null
            return try {
                fromJsonObject(JSONObject(json))
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Envelope stored inside `StickyNoteEntity.subtitle` that preserves:
 * 1. `subHeading` (displayed before the main text)
 * 2. `pinIcon` ("📌" or "📎", synced with the landing card UI)
 * 3. `table` (borderless visual grid table)
 * Fully backward-compatible with legacy plain-text subtitles and legacy `{"rows":...}` table JSON.
 */
data class StickySubtitleEnvelope(
    val subHeading: String = "",
    val pinIcon: String = "📌",
    val table: StickyTableData? = null
) {
    fun toSerializedString(): String {
        val obj = JSONObject()
        obj.put("sub", subHeading)
        obj.put("pin", pinIcon.ifBlank { "📌" })
        if (table != null) {
            obj.put("table", table.toJsonObject())
        }
        return obj.toString()
    }

    companion object {
        fun parse(rawSubtitle: String): StickySubtitleEnvelope {
            val trimmed = rawSubtitle.trim()
            if (trimmed.isEmpty()) return StickySubtitleEnvelope()
            if (trimmed.startsWith("{")) {
                return try {
                    val obj = JSONObject(trimmed)
                    if (obj.has("sub") || obj.has("pin") || obj.has("table")) {
                        val sub = obj.optString("sub", "")
                        val pin = obj.optString("pin", "📌").ifBlank { "📌" }
                        val tbl = StickyTableData.fromJsonObject(obj.optJSONObject("table"))
                        StickySubtitleEnvelope(subHeading = sub, pinIcon = pin, table = tbl)
                    } else if (obj.has("rows") && obj.has("cols")) {
                        StickySubtitleEnvelope(
                            subHeading = "",
                            pinIcon = "📌",
                            table = StickyTableData.fromJsonObject(obj)
                        )
                    } else {
                        StickySubtitleEnvelope(subHeading = rawSubtitle)
                    }
                } catch (_: Exception) {
                    StickySubtitleEnvelope(subHeading = rawSubtitle)
                }
            }
            return StickySubtitleEnvelope(subHeading = rawSubtitle)
        }
    }
}

// Serialization helpers for Attachments JSON (compatible with StickyNoteEntity.attachmentsJson)
data class StickyAttachmentItem(
    val uri: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("uri", uri)
            put("name", name)
            put("mimeType", mimeType)
            put("type", if (mimeType.startsWith("image/")) "IMAGE" else "FILE")
            put("sizeBytes", sizeBytes)
        }
    }

    companion object {
        fun listToJson(list: List<StickyAttachmentItem>): String {
            if (list.isEmpty()) return "[]"
            val arr = JSONArray()
            list.forEach { arr.put(it.toJsonObject()) }
            return arr.toString()
        }

        fun listFromJson(json: String): List<StickyAttachmentItem> {
            if (json.isBlank() || json == "[]") return emptyList()
            return try {
                val arr = JSONArray(json)
                val res = mutableListOf<StickyAttachmentItem>()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val uri = o.optString("uri", "")
                    if (uri.isNotBlank()) {
                        val fallbackMime = if (o.optString("type") == "IMAGE") "image/*" else "*/*"
                        res.add(
                            StickyAttachmentItem(
                                uri = uri,
                                name = o.optString("name", "Attachment"),
                                mimeType = o.optString("mimeType", fallbackMime),
                                sizeBytes = o.optLong("sizeBytes", 0L)
                            )
                        )
                    }
                }
                res
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.US, "%.2f MB", mb)
}

/**
 * Per-line alignment serialization stored in StickyNoteEntity.textAlignment.
 * Format: "0:START,1:CENTER,2:END,3:JUSTIFY"
 */
fun parseLineAlignments(serialized: String): Map<Int, String> {
    if (serialized.isBlank()) return emptyMap()
    if (!serialized.contains(":")) {
        return if (serialized == "START") emptyMap() else mapOf(0 to serialized)
    }
    val map = mutableMapOf<Int, String>()
    serialized.split(",").forEach { entry ->
        val parts = entry.split(":")
        if (parts.size == 2) {
            val idx = parts[0].trim().toIntOrNull()
            val align = parts[1].trim()
            if (idx != null && align in listOf("START", "CENTER", "END", "JUSTIFY")) {
                map[idx] = align
            }
        }
    }
    return map
}

fun serializeLineAlignments(map: Map<Int, String>): String {
    val filtered = map.filterValues { it != "START" }
    if (filtered.isEmpty()) return "START"
    return filtered.entries.sortedBy { it.key }.joinToString(",") { "${it.key}:${it.value}" }
}

fun mapStringToTextAlign(align: String): TextAlign {
    return when (align) {
        "CENTER" -> TextAlign.Center
        "END" -> TextAlign.End
        "JUSTIFY" -> TextAlign.Justify
        else -> TextAlign.Start
    }
}

/**
 * Extracts legacy inline markdown tables (`| a | b |`) and parses StickySubtitleEnvelope.
 */
fun parseNoteBodyAndEnvelope(rawBody: String, rawSubtitle: String): Pair<String, StickySubtitleEnvelope> {
    val env = StickySubtitleEnvelope.parse(rawSubtitle)
    if (env.table != null || !rawBody.contains("|")) return rawBody to env
    val lines = rawBody.lines()
    val nonTableLines = mutableListOf<String>()
    val tableRows = mutableListOf<List<String>>()
    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.length >= 3) {
            val cells = trimmed.substring(1, trimmed.length - 1).split("|").map { it.trim() }
            if (cells.isNotEmpty()) {
                tableRows.add(cells)
            }
        } else {
            nonTableLines.add(line)
        }
    }
    if (tableRows.isEmpty()) return rawBody to env
    val cols = tableRows.maxOf { it.size }.coerceAtLeast(1)
    val normalized = tableRows.map { r ->
        List(cols) { c -> r.getOrNull(c) ?: "" }
    }
    val extractedTable = StickyTableData(normalized.size, cols, normalized)
    return nonTableLines.joinToString("\n").trim() to env.copy(table = extractedTable)
}

/**
 * VisualTransformation for Sticky Note editor:
 * - Renders `*bold*` and `_italic_` inline
 * - Applies per-line ParagraphStyle(textAlign = ...) so each sentence/line has its own alignment!
 * - Preserves 1:1 character length (`OffsetMapping.Identity`) so cursor placement never jumps.
 */
private val STICKY_BOLD_REGEX = Regex("""\*([^\*\n]+)\*""")
private val STICKY_ITALIC_REGEX = Regex("""_([^_\n]+)_""")

/**
 * VisualTransformation for Sticky Note editor:
 * - Renders `*bold*` and `_italic_` inline
 * - Applies per-line ParagraphStyle(textAlign = ...) so each sentence/line has its own alignment!
 * - Preserves 1:1 character length (`OffsetMapping.Identity`) so cursor placement never jumps.
 */
class StickyFormattingVisualTransformation(
    private val isLight: Boolean,
    private val isBoldMode: Boolean,
    private val baseColor: Color,
    private val accentColor: Color,
    private val lineAlignments: Map<Int, String>
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty() || (!raw.contains('*') && !raw.contains('_') && lineAlignments.isEmpty())) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        val markerColor = baseColor.copy(alpha = 0.25f)
        val boldSpanFont = if (isBoldMode) ArchivoBlackFontFamily else AppBoldFontFamily

        val styled = buildAnnotatedString {
            append(raw)

            if (lineAlignments.isNotEmpty()) {
                var lineStart = 0
                var lineIdx = 0
                while (lineStart <= raw.length) {
                    val nextNewline = raw.indexOf('\n', lineStart)
                    val lineEnd = if (nextNewline == -1) raw.length else nextNewline + 1
                    val alignStr = lineAlignments[lineIdx] ?: "START"
                    if (alignStr != "START" && lineStart < lineEnd) {
                        addStyle(
                            style = ParagraphStyle(textAlign = mapStringToTextAlign(alignStr)),
                            start = lineStart,
                            end = lineEnd
                        )
                    }
                    if (nextNewline == -1) break
                    lineStart = nextNewline + 1
                    lineIdx++
                }
            }

            if (raw.contains('*') || raw.contains('_')) {
                var lineOffset = 0
                val lines = raw.split('\n')
                for (lineIdx in lines.indices) {
                    val line = lines[lineIdx]
                    val boldRanges = mutableListOf<IntRange>()
                    val italicRanges = mutableListOf<IntRange>()

                    var bi = 0
                    while (bi < line.length) {
                        if (line[bi] == '*') {
                            val close = line.indexOf('*', bi + 1)
                            if (close > bi + 1) {
                                boldRanges.add(bi..close)
                                bi = close + 1
                                continue
                            }
                        }
                        bi++
                    }

                    var ii = 0
                    while (ii < line.length) {
                        if (line[ii] == '_') {
                            val close = line.indexOf('_', ii + 1)
                            if (close > ii + 1) {
                                italicRanges.add(ii..close)
                                ii = close + 1
                                continue
                            }
                        }
                        ii++
                    }

                    // Apply Italic first, then Bold, then dim all outer/inner markers with uniform font size
                    for (r in italicRanges) {
                        val absStart = lineOffset + r.first
                        val absEnd = lineOffset + r.last
                        addStyle(
                            SpanStyle(
                                fontStyle = FontStyle.Italic,
                                color = baseColor.copy(alpha = 0.92f)
                            ),
                            absStart + 1,
                            absEnd
                        )
                    }
                    for (r in boldRanges) {
                        val absStart = lineOffset + r.first
                        val absEnd = lineOffset + r.last
                        addStyle(
                            SpanStyle(
                                fontFamily = boldSpanFont,
                                fontWeight = FontWeight.Black,
                                color = baseColor
                            ),
                            absStart + 1,
                            absEnd
                        )
                    }
                    for (r in italicRanges) {
                        val absStart = lineOffset + r.first
                        val absEnd = lineOffset + r.last
                        addStyle(SpanStyle(color = markerColor), absStart, absStart + 1)
                        addStyle(SpanStyle(color = markerColor), absEnd, absEnd + 1)
                    }
                    for (r in boldRanges) {
                        val absStart = lineOffset + r.first
                        val absEnd = lineOffset + r.last
                        addStyle(SpanStyle(color = markerColor), absStart, absStart + 1)
                        addStyle(SpanStyle(color = markerColor), absEnd, absEnd + 1)
                    }

                    lineOffset += line.length + 1
                }
            }
        }

        return TransformedText(styled, OffsetMapping.Identity)
    }
}

data class StickyInlineSpan(
    val start: Int,
    val end: Int,
    val isBold: Boolean
)

data class ParsedStickyFormattedText(
    val plainText: String,
    val inlineSpans: List<StickyInlineSpan>,
    val lineParagraphRanges: List<Triple<Int, Int, String>>
) {
    val hasCustomFormatting: Boolean
        get() = inlineSpans.isNotEmpty() || lineParagraphRanges.isNotEmpty()
}

fun parseStickyFormattedText(
    rawText: String,
    lineAlignments: Map<Int, String> = emptyMap()
): ParsedStickyFormattedText {
    if (rawText.isEmpty()) {
        return ParsedStickyFormattedText("", emptyList(), emptyList())
    }
    if (!rawText.contains('*') && !rawText.contains('_') && lineAlignments.isEmpty()) {
        return ParsedStickyFormattedText(rawText, emptyList(), emptyList())
    }

    val sb = StringBuilder(rawText.length)
    val spans = ArrayList<StickyInlineSpan>(4)
    val paraRanges = ArrayList<Triple<Int, Int, String>>(4)

    val lines = rawText.split('\n')
    for (lineIdx in lines.indices) {
        val lineText = lines[lineIdx]
        val paraStart = sb.length
        var inBold = false
        var boldStart = -1
        var inItalic = false
        var italicStart = -1

        var i = 0
        val n = lineText.length
        while (i < n) {
            val ch = lineText[i]
            if (ch == '*') {
                if (inBold) {
                    if (sb.length > boldStart) {
                        spans.add(StickyInlineSpan(boldStart, sb.length, isBold = true))
                    }
                    inBold = false
                    i++
                    continue
                } else {
                    val close = lineText.indexOf('*', i + 1)
                    if (close > i + 1) {
                        inBold = true
                        boldStart = sb.length
                        i++
                        continue
                    }
                }
            } else if (ch == '_') {
                if (inItalic) {
                    if (sb.length > italicStart) {
                        spans.add(StickyInlineSpan(italicStart, sb.length, isBold = false))
                    }
                    inItalic = false
                    i++
                    continue
                } else {
                    val close = lineText.indexOf('_', i + 1)
                    if (close > i + 1) {
                        inItalic = true
                        italicStart = sb.length
                        i++
                        continue
                    }
                }
            }
            sb.append(ch)
            i++
        }
        if (lineIdx < lines.lastIndex) {
            sb.append('\n')
        }
        val paraEnd = sb.length
        val alignStr = lineAlignments[lineIdx]
        if (alignStr != null && alignStr != "START" && paraStart < paraEnd) {
            paraRanges.add(Triple(paraStart, paraEnd, alignStr))
        }
    }

    return ParsedStickyFormattedText(
        plainText = sb.toString(),
        inlineSpans = spans,
        lineParagraphRanges = paraRanges
    )
}

fun buildAnnotatedFromParsedSticky(
    parsed: ParsedStickyFormattedText,
    isBoldMode: Boolean,
    baseColor: Color,
    accentColor: Color
): AnnotatedString {
    if (!parsed.hasCustomFormatting) {
        return AnnotatedString(parsed.plainText)
    }
    val boldSpanFont = if (isBoldMode) ArchivoBlackFontFamily else AppBoldFontFamily
    return buildAnnotatedString {
        append(parsed.plainText)
        for (span in parsed.inlineSpans) {
            if (span.isBold) {
                addStyle(
                    style = SpanStyle(
                        fontFamily = boldSpanFont,
                        fontWeight = FontWeight.Black,
                        color = baseColor
                    ),
                    start = span.start,
                    end = span.end
                )
            } else {
                addStyle(
                    style = SpanStyle(
                        fontStyle = FontStyle.Italic,
                        color = baseColor.copy(alpha = 0.92f)
                    ),
                    start = span.start,
                    end = span.end
                )
            }
        }
        for ((pStart, pEnd, alignStr) in parsed.lineParagraphRanges) {
            addStyle(
                style = ParagraphStyle(textAlign = mapStringToTextAlign(alignStr)),
                start = pStart,
                end = pEnd
            )
        }
    }
}

private fun remapLineAlignmentsOnEdit(
    oldText: String,
    newText: String,
    alignments: Map<Int, String>
): Map<Int, String> {
    if (alignments.isEmpty() || oldText == newText) return alignments

    var prefixLen = 0
    val minLen = minOf(oldText.length, newText.length)
    while (prefixLen < minLen && oldText[prefixLen] == newText[prefixLen]) {
        prefixLen++
    }

    var suffixLen = 0
    val maxSuffix = minOf(oldText.length - prefixLen, newText.length - prefixLen)
    while (suffixLen < maxSuffix &&
        oldText[oldText.length - 1 - suffixLen] == newText[newText.length - 1 - suffixLen]
    ) {
        suffixLen++
    }

    val oldMiddle = oldText.substring(prefixLen, oldText.length - suffixLen)
    val newMiddle = newText.substring(prefixLen, newText.length - suffixLen)
    val deletedNewlines = oldMiddle.count { it == '\n' }
    val insertedNewlines = newMiddle.count { it == '\n' }
    val delta = insertedNewlines - deletedNewlines
    if (delta == 0) return alignments

    val editStartLine = oldText.substring(0, prefixLen).count { it == '\n' }
    val totalNewLines = newText.count { it == '\n' } + 1
    val updated = mutableMapOf<Int, String>()

    if (delta > 0) {
        for ((idx, align) in alignments) {
            if (idx < editStartLine) {
                updated[idx] = align
            } else if (idx == editStartLine) {
                updated[idx] = align
                if (newMiddle == "\n" && oldMiddle.isEmpty() && idx + 1 < totalNewLines) {
                    updated[idx + 1] = align
                }
            } else {
                val target = idx + delta
                if (target < totalNewLines) updated[target] = align
            }
        }
    } else {
        val removedLines = -delta
        for ((idx, align) in alignments) {
            when {
                idx < editStartLine -> updated[idx] = align
                idx == editStartLine -> updated[idx] = align
                idx > editStartLine + removedLines -> {
                    val target = idx + delta
                    if (target in 0 until totalNewLines) updated[target] = align
                }
            }
        }
        if (editStartLine !in updated) {
            val mergedAlign = alignments[editStartLine + removedLines]
            if (mergedAlign != null && editStartLine < totalNewLines) {
                updated[editStartLine] = mergedAlign
            }
        }
    }
    return updated
}

/**
 * Builds an AnnotatedString for viewer/card display that strips `*` and `_` markers
 * and applies per-line ParagraphStyle alignments with minimal SpanStyle runs.
 */
@Composable
fun buildFormattedStickyAnnotatedString(
    rawText: String,
    isBoldMode: Boolean,
    baseColor: Color,
    accentColor: Color,
    lineAlignments: Map<Int, String> = emptyMap()
): AnnotatedString {
    val parsed = remember(rawText, lineAlignments) {
        parseStickyFormattedText(rawText, lineAlignments)
    }
    return remember(parsed, isBoldMode, baseColor, accentColor) {
        buildAnnotatedFromParsedSticky(parsed, isBoldMode, baseColor, accentColor)
    }
}

data class StickyCardPreviewModel(
    val noteId: Int,
    val updatedAt: Long,
    val subHeading: String,
    val pinIcon: String,
    val tableData: StickyTableData?,
    val parsedPreview: ParsedStickyFormattedText,
    val attachmentsCount: Int,
    val tagsList: List<String>,
    val staticTiltDegrees: Float
)

internal object StickyCardPreviewCache {
    private val map = java.util.concurrent.ConcurrentHashMap<String, StickyCardPreviewModel>()

    fun prewarmAll(notes: List<StickyNoteEntity>, previewLines: Int = 6) {
        for (note in notes) {
            getOrCompute(note, previewLines)
        }
    }

    fun getOrCompute(note: StickyNoteEntity, maxPreviewLines: Int): StickyCardPreviewModel {
        val key = "${note.id}_${note.updatedAt}_$maxPreviewLines"
        map[key]?.let { return it }
        val computed = compute(note, maxPreviewLines)
        map[key] = computed
        return computed
    }

    fun compute(note: StickyNoteEntity, maxPreviewLines: Int): StickyCardPreviewModel {
        val (cleanedBody, envelope) = parseNoteBodyAndEnvelope(note.bodyText, note.subtitle)
        val tableRows = envelope.table?.rows ?: 0
        val maxLinesToKeep = (maxPreviewLines - minOf(tableRows, maxPreviewLines)).coerceAtLeast(1) + 1
        val bodyLines = cleanedBody.lineSequence().take(maxLinesToKeep).joinToString("\n")
        val truncatedBody = if (bodyLines.length > 340) bodyLines.substring(0, 340) + "…" else bodyLines
        val alignments = parseLineAlignments(note.textAlignment).filterKeys { it < maxLinesToKeep }
        val parsedPreview = parseStickyFormattedText(truncatedBody, alignments)
        val attCount = StickyAttachmentItem.listFromJson(note.attachmentsJson).size
        val tags = if (note.tags.isBlank()) emptyList() else note.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val tiltAngles = floatArrayOf(-0.65f, 0.55f, -0.40f, 0.70f, -0.50f, 0.45f)
        val staticTilt = tiltAngles[note.id.mod(tiltAngles.size)]
        return StickyCardPreviewModel(
            noteId = note.id,
            updatedAt = note.updatedAt,
            subHeading = envelope.subHeading,
            pinIcon = envelope.pinIcon,
            tableData = envelope.table,
            parsedPreview = parsedPreview,
            attachmentsCount = attCount,
            tagsList = tags,
            staticTiltDegrees = staticTilt
        )
    }
}

@Composable
fun StickyNotesTab(viewModel: FinancialViewModel) {
    val allNotes by viewModel.allStickyNotes.collectAsStateWithLifecycle()
    val selectedTag by viewModel.selectedStickyTag.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isBn = userProfile.language == "bn"
    val isBoldMode = LocalBoldTextEnabled.current
    val cardWidthPercent = LocalStickyCardWidthPercent.current.coerceIn(50, 80)
    val maxPreviewLines = LocalStickyPreviewLines.current.coerceIn(4, 10)
    val haptic = LocalHapticFeedback.current

    // Keep preview cache warm for custom preview lines setting on background thread without triggering redundant UI recomposition
    LaunchedEffect(allNotes, maxPreviewLines) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            StickyCardPreviewCache.prewarmAll(allNotes, maxPreviewLines)
        }
    }

    val notes = remember(allNotes, selectedTag, maxPreviewLines) {
        if (selectedTag == "ALL" || selectedTag.isBlank()) {
            allNotes
        } else {
            allNotes.filter { note ->
                val cachedTags = StickyCardPreviewCache.getOrCompute(note, maxPreviewLines).tagsList
                cachedTags.any { it.equals(selectedTag, ignoreCase = true) }
            }
        }
    }

    // Active Note Editor / Viewer Modal State
    var openedNote by remember { mutableStateOf<StickyNoteEntity?>(null) }
    var isCreatingNewNote by remember { mutableStateOf(false) }
    var openInEditMode by remember { mutableStateOf(false) }

    // Long-press rearrange mode state
    var rearrangingNoteId by remember { mutableStateOf<Int?>(null) }
    var localNotesOrder by remember(notes) { mutableStateOf(notes) }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp
    val isTablet = screenWidthDp >= 600.dp
    val containerWidth = if (isTablet) (screenWidthDp - 96.dp).coerceAtLeast(320.dp) else screenWidthDp
    val isWallMode = screenWidthDp >= 600.dp || screenWidthDp > screenHeightDp

    val stickyWallListState = androidx.compose.runtime.saveable.rememberSaveable(
        saver = androidx.compose.foundation.lazy.LazyListState.Saver
    ) { androidx.compose.foundation.lazy.LazyListState() }
    val stickyMobileListState = androidx.compose.runtime.saveable.rememberSaveable(
        saver = androidx.compose.foundation.lazy.LazyListState.Saver
    ) { androidx.compose.foundation.lazy.LazyListState() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (rearrangingNoteId != null) {
                    Modifier.pointerInput(rearrangingNoteId) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            rearrangingNoteId = null
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp),
                    cornerRadius = 24.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(AppAccentColor().copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.StickyNote2,
                                contentDescription = null,
                                tint = AppAccentColor(),
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isBn) "কোনো স্টিকি নোট নেই" else "No Sticky Notes Yet",
                            color = AppTextColor(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isBn)
                                "নিচের '+' বাটনে ট্যাপ করে নতুন স্টিকি নোট, টেবিল বা ফাইল যুক্ত করুন।"
                            else
                                "Tap the '+' button below to create floating sticky notes with visual grid tables, line alignment, and attachments.",
                            color = AppSecondaryTextColor(),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 19.sp
                        )
                    }
                }
            }
        } else if (isWallMode) {
            // Tablet / Landscape Wall Mode: Virtualized LazyColumn of multi-column rows with native hardware-accelerated fling
            val horizontalPad = 16.dp
            val colSpacing = 12.dp
            val usableWidth = (containerWidth - horizontalPad * 2).coerceAtLeast(240.dp)
            val columnCount = when {
                usableWidth >= 900.dp -> 4
                usableWidth >= 560.dp -> 3
                else -> 2
            }
            val maxColWidth = (usableWidth - colSpacing * (columnCount - 1)) / columnCount
            val widthFractionInCol = (0.78f + ((cardWidthPercent - 50) / 30f) * 0.22f).coerceIn(0.78f, 1.0f)
            val cardTargetWidth = (maxColWidth * widthFractionInCol).coerceAtMost(maxColWidth)

            val chunkedRows = remember(localNotesOrder, columnCount) {
                localNotesOrder.mapIndexed { idx, note -> idx to note }.chunked(columnCount)
            }

            LazyColumn(
                state = stickyWallListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = horizontalPad,
                    end = horizontalPad,
                    top = 12.dp,
                    bottom = 120.dp + LocalBottomBarInset.current + rememberSplitScreenSafeBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(
                    items = chunkedRows,
                    key = { rowIdx, rowItems -> "wall_row_${rowIdx}_${rowItems.firstOrNull()?.second?.id ?: 0}" },
                    contentType = { _, _ -> "sticky_wall_row" }
                ) { _, rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(colSpacing),
                        verticalAlignment = Alignment.Top
                    ) {
                        for (colIndex in 0 until columnCount) {
                            val pair = rowItems.getOrNull(colIndex)
                            val staggeredTopOffset = when (colIndex % 4) {
                                0 -> 2.dp
                                1 -> 14.dp
                                2 -> 6.dp
                                else -> 18.dp
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(top = staggeredTopOffset),
                                contentAlignment = if (pair != null && pair.first % 2 == 0) Alignment.CenterStart else Alignment.CenterEnd
                            ) {
                                if (pair != null) {
                                    val (globalIdx, note) = pair
                                    StickyFloatingCard(
                                        note = note,
                                        index = globalIdx,
                                        cardWidth = cardTargetWidth,
                                        maxPreviewLines = maxPreviewLines,
                                        isRearranging = rearrangingNoteId == note.id,
                                        isBoldMode = isBoldMode,
                                        isBn = isBn,
                                        onTap = {
                                            if (rearrangingNoteId != null) {
                                                rearrangingNoteId = null
                                            } else {
                                                openedNote = note
                                                openInEditMode = false
                                            }
                                        },
                                        onLongPressRearrange = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            rearrangingNoteId = note.id
                                        },
                                        onMovePrev = {
                                            if (globalIdx > 0) {
                                                val mutable = localNotesOrder.toMutableList()
                                                val item = mutable.removeAt(globalIdx)
                                                mutable.add(globalIdx - 1, item)
                                                localNotesOrder = mutable
                                                viewModel.reorderStickyNotes(mutable)
                                            }
                                        },
                                        onMoveNext = {
                                            if (globalIdx < localNotesOrder.lastIndex) {
                                                val mutable = localNotesOrder.toMutableList()
                                                val item = mutable.removeAt(globalIdx)
                                                mutable.add(globalIdx + 1, item)
                                                localNotesOrder = mutable
                                                viewModel.reorderStickyNotes(mutable)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Mobile Portrait Mode: Alternating Leftward & Rightward arrangement with native hardware-accelerated fling
            val usableWidth = (containerWidth - 28.dp).coerceAtLeast(220.dp)
            val cardTargetWidth = usableWidth * (cardWidthPercent / 100f)

            LazyColumn(
                state = stickyMobileListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 14.dp,
                    end = 14.dp,
                    top = 12.dp,
                    bottom = 120.dp + LocalBottomBarInset.current + rememberSplitScreenSafeBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(
                    items = localNotesOrder,
                    key = { _, n -> n.id },
                    contentType = { _, _ -> "sticky_mobile_card" }
                ) { idx, note ->
                    val isLeftward = idx % 2 == 0
                    val rowAlignment = if (isLeftward) Alignment.CenterStart else Alignment.CenterEnd

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = rowAlignment
                    ) {
                        StickyFloatingCard(
                            note = note,
                            index = idx,
                            cardWidth = cardTargetWidth,
                            maxPreviewLines = maxPreviewLines,
                            isRearranging = rearrangingNoteId == note.id,
                            isBoldMode = isBoldMode,
                            isBn = isBn,
                            onTap = {
                                if (rearrangingNoteId != null) {
                                    rearrangingNoteId = null
                                } else {
                                    openedNote = note
                                    openInEditMode = false
                                }
                            },
                            onLongPressRearrange = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                rearrangingNoteId = note.id
                            },
                            onMovePrev = {
                                if (idx > 0) {
                                    val mutable = localNotesOrder.toMutableList()
                                    val item = mutable.removeAt(idx)
                                    mutable.add(idx - 1, item)
                                    localNotesOrder = mutable
                                    viewModel.reorderStickyNotes(mutable)
                                }
                            },
                            onMoveNext = {
                                if (idx < localNotesOrder.lastIndex) {
                                    val mutable = localNotesOrder.toMutableList()
                                    val item = mutable.removeAt(idx)
                                    mutable.add(idx + 1, item)
                                    localNotesOrder = mutable
                                    viewModel.reorderStickyNotes(mutable)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Unified Floating Action Button to add new Sticky Note
        FloatingActionButton(
            onClick = {
                openedNote = null
                openInEditMode = true
                isCreatingNewNote = true
            },
            containerColor = AppAccentColor(),
            contentColor = AppOnAccentColor(),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 20.dp,
                    bottom = LocalBottomBarInset.current.let { inset ->
                        if (inset > 0.dp) inset + 18.dp else 24.dp + rememberSplitScreenSafeBottomPadding()
                    }
                )
                .size(58.dp)
                .testTag("add_sticky_note_fab")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Sticky Note",
                modifier = Modifier.size(28.dp)
            )
        }
    }

    if (isCreatingNewNote || openedNote != null) {
        StickyNoteFullScreenDialog(
            initialNote = openedNote,
            startInEditMode = isCreatingNewNote || openInEditMode,
            viewModel = viewModel,
            onDismiss = {
                isCreatingNewNote = false
                openedNote = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StickyFloatingCard(
    note: StickyNoteEntity,
    index: Int,
    cardWidth: Dp,
    maxPreviewLines: Int,
    isRearranging: Boolean,
    isBoldMode: Boolean,
    isBn: Boolean,
    onTap: () -> Unit,
    onLongPressRearrange: () -> Unit,
    onMovePrev: () -> Unit,
    onMoveNext: () -> Unit
) {
    val isLight = isAppLightMode()
    val palette = getPaletteForIndex(note.colorIndex)
    val cardBg = if (isLight) palette.cardBgLight else palette.cardBgDark
    val effectiveAccent = if (note.colorIndex == 0) AppAccentColor() else palette.accentBorder
    val borderCol = if (isRearranging) AppAccentColor() else palette.accentBorder.copy(alpha = if (note.colorIndex == 0) 0.32f else 0.65f)
    val textColor = if (isLight) Color(0xFF1E293B) else Color(0xFFF8FAFC)
    val regularFont = if (isBoldMode) StickyBoldBaseFontFamily else AppFontFamily

    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    val shape = RoundedCornerShape(20.dp)
    val previewModel = remember(note.id, note.updatedAt, maxPreviewLines) {
        StickyCardPreviewCache.getOrCompute(note, maxPreviewLines)
    }
    val parsedPreview = previewModel.parsedPreview
    val subHeading = previewModel.subHeading
    val pinIcon = previewModel.pinIcon
    val tableData = previewModel.tableData
    val attachmentsCount = previewModel.attachmentsCount
    val noteTags = previewModel.tagsList

    val tableRowsToDisplay = remember(tableData, maxPreviewLines) {
        if (tableData == null) 0 else minOf(tableData.rows, maxPreviewLines)
    }
    val remainingTextLines = (maxPreviewLines - tableRowsToDisplay).coerceAtLeast(1)

    Box(
        modifier = Modifier
            .width(cardWidth)
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .then(
                if (isRearranging) {
                    Modifier
                        .graphicsLayer {
                            translationX = dragOffset.x
                            translationY = dragOffset.y
                            scaleX = 1.02f
                            scaleY = 1.02f
                        }
                        .pointerInput(note.id) {
                            detectDragGestures(
                                onDragEnd = {
                                    if (dragOffset.y < -45f || dragOffset.x < -45f) {
                                        onMovePrev()
                                    } else if (dragOffset.y > 45f || dragOffset.x > 45f) {
                                        onMoveNext()
                                    }
                                    dragOffset = Offset.Zero
                                },
                                onDragCancel = { dragOffset = Offset.Zero },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset += dragAmount
                                }
                            )
                        }
                } else {
                    Modifier.combinedClickable(
                        onClick = onTap,
                        onLongClick = onLongPressRearrange
                    )
                }
            )
            .testTag("sticky_card_${note.id}")
    ) {
        // Static low-cost card surface (0 FPS idle overhead, zero offscreen shadow rasterization lag)
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(cardBg)
                .border(if (isRearranging) 2.5.dp else 1.2.dp, borderCol, shape)
        )

        // FOREGROUND CONTENT LAYER: Text, tables, tags, and realistic Pin/Paperclip badge
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(effectiveAccent)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = note.title.ifBlank { if (isBn) "শিরোনামহীন নোট" else "Untitled Note" },
                        color = textColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = if (isBoldMode) ArchivoBlackFontFamily else AppFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isRearranging) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = onMovePrev,
                            modifier = Modifier
                                .size(28.dp)
                                .background(palette.accentBorder.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = textColor, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = onMoveNext,
                            modifier = Modifier
                                .size(28.dp)
                                .background(palette.accentBorder.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = textColor, modifier = Modifier.size(18.dp))
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(34.dp))
                }
            }

            if (subHeading.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subHeading,
                    color = textColor.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (noteTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    noteTags.take(3).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = effectiveAccent.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "#$tag",
                                color = textColor.copy(alpha = 0.85f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (noteTags.size > 3) {
                        Text(
                            text = "+${noteTags.size - 3}",
                            color = textColor.copy(alpha = 0.65f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Fast-path plain String Text when note has no inline bold/italic/alignment spans
            if (parsedPreview.plainText.isNotBlank() && remainingTextLines > 0) {
                val basePreviewColor = textColor.copy(alpha = 0.90f)
                if (!parsedPreview.hasCustomFormatting) {
                    Text(
                        text = parsedPreview.plainText,
                        color = basePreviewColor,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontFamily = regularFont,
                        fontWeight = if (isBoldMode) FontWeight.Bold else FontWeight.Normal,
                        maxLines = remainingTextLines,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    val annotated = remember(parsedPreview, isBoldMode, basePreviewColor, effectiveAccent) {
                        buildAnnotatedFromParsedSticky(
                            parsed = parsedPreview,
                            isBoldMode = isBoldMode,
                            baseColor = basePreviewColor,
                            accentColor = effectiveAccent
                        )
                    }
                    Text(
                        text = annotated,
                        color = basePreviewColor,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontFamily = regularFont,
                        fontWeight = if (isBoldMode) FontWeight.Bold else FontWeight.Normal,
                        maxLines = remainingTextLines,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (tableData != null && tableRowsToDisplay > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                StickyTableCompactPreview(
                    table = tableData,
                    maxRows = tableRowsToDisplay,
                    accentColor = effectiveAccent,
                    textColor = textColor
                )
            }

            if (attachmentsCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(effectiveAccent.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = null,
                        tint = effectiveAccent,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isBn) "$attachmentsCount টি সংযুক্ত ফাইল" else "$attachmentsCount Attachment(s)",
                        color = textColor.copy(alpha = 0.85f),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (isRearranging) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isBn) "ড্র্যাগ করুন বা তীর চিহ্নে ট্যাপ করে স্থান পরিবর্তন করুন" else "Drag or tap arrows to rearrange • Tap outside when done",
                    color = AppAccentColor(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Cached 3D Push-Pin or Metallic Gold Paperclip overlapping top edge of card
        if (!isRearranging) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 10.dp)
                    .offset(y = (-5).dp)
                    .size(40.dp)
            ) {
                if (pinIcon == "📎") {
                    StickyRealisticPaperclipCanvas(modifier = Modifier.fillMaxSize())
                } else {
                    StickyRealisticPushPinCanvas(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

/**
 * Draws a realistic 3D Glossy Blue Push-Pin using drawWithCache so Paths and Brushes are allocated once per size.
 */
@Composable
private fun StickyRealisticPushPinCanvas(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier.drawWithCache {
            val w = size.width
            val h = size.height
            val puncture = Offset(w * 0.28f, h * 0.78f)
            val shadowPath = Path().apply {
                moveTo(puncture.x, puncture.y)
                lineTo(w * 0.62f, h * 0.72f)
                cubicTo(w * 0.88f, h * 0.65f, w * 0.95f, h * 0.85f, w * 0.72f, h * 0.92f)
                cubicTo(w * 0.56f, h * 0.96f, w * 0.44f, h * 0.86f, puncture.x, puncture.y)
                close()
            }
            val collarCenter = Offset(w * 0.46f, h * 0.50f)
            val needlePath = Path().apply {
                moveTo(collarCenter.x - w * 0.05f, collarCenter.y - h * 0.02f)
                lineTo(collarCenter.x + w * 0.05f, collarCenter.y + h * 0.04f)
                lineTo(puncture.x, puncture.y)
                close()
            }
            val needleBrush = Brush.linearGradient(
                colors = listOf(Color(0xFFF1F5F9), Color(0xFF94A3B8), Color(0xFF475569)),
                start = collarCenter,
                end = puncture
            )
            val collarBrush = Brush.linearGradient(
                colors = listOf(Color(0xFF0284C7), Color(0xFF0369A1), Color(0xFF1E3A8A)),
                start = Offset(collarCenter.x - w * 0.24f, collarCenter.y),
                end = Offset(collarCenter.x + w * 0.24f, collarCenter.y)
            )
            val topCenter = Offset(collarCenter.x, collarCenter.y - h * 0.26f)
            val bodyPath = Path().apply {
                moveTo(collarCenter.x - w * 0.13f, collarCenter.y - h * 0.01f)
                lineTo(topCenter.x - w * 0.17f, topCenter.y)
                lineTo(topCenter.x + w * 0.17f, topCenter.y)
                lineTo(collarCenter.x + w * 0.13f, collarCenter.y - h * 0.01f)
                close()
            }
            val bodyBrush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF0369A1), Color(0xFF0EA5E9), Color(0xFF1D4ED8)),
                startX = collarCenter.x - w * 0.17f,
                endX = collarCenter.x + w * 0.17f
            )
            val capBrush = Brush.radialGradient(
                colors = listOf(Color(0xFF7DD3FC), Color(0xFF0284C7), Color(0xFF1E40AF)),
                center = Offset(topCenter.x - w * 0.04f, topCenter.y - h * 0.02f),
                radius = w * 0.24f
            )
            val strokeStyle = Stroke(width = w * 0.025f)

            onDrawBehind {
                drawPath(shadowPath, color = Color.Black.copy(alpha = 0.22f))
                drawPath(path = needlePath, brush = needleBrush)
                drawCircle(
                    color = Color.Black.copy(alpha = 0.35f),
                    radius = w * 0.03f,
                    center = puncture
                )
                rotate(degrees = 32f, pivot = collarCenter) {
                    drawOval(
                        brush = collarBrush,
                        topLeft = Offset(collarCenter.x - w * 0.23f, collarCenter.y - h * 0.11f),
                        size = Size(w * 0.46f, h * 0.22f)
                    )
                    drawOval(
                        color = Color(0xFF38BDF8).copy(alpha = 0.55f),
                        topLeft = Offset(collarCenter.x - w * 0.21f, collarCenter.y - h * 0.10f),
                        size = Size(w * 0.42f, h * 0.18f),
                        style = strokeStyle
                    )
                    drawPath(path = bodyPath, brush = bodyBrush)
                    drawOval(
                        brush = capBrush,
                        topLeft = Offset(topCenter.x - w * 0.20f, topCenter.y - h * 0.10f),
                        size = Size(w * 0.40f, h * 0.20f)
                    )
                    drawOval(
                        color = Color(0xFFE0F2FE).copy(alpha = 0.78f),
                        topLeft = Offset(topCenter.x - w * 0.11f, topCenter.y - h * 0.06f),
                        size = Size(w * 0.18f, h * 0.09f)
                    )
                }
            }
        }
    )
}

/**
 * Draws a realistic 3D Metallic Gold/Copper Paperclip using drawWithCache so Brushes/Strokes are allocated once per size.
 */
@Composable
private fun StickyRealisticPaperclipCanvas(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier.drawWithCache {
            val w = size.width
            val h = size.height
            val center = Offset(w * 0.5f, h * 0.5f)
            val strokeW = w * 0.075f
            val goldBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFDE68A),
                    Color(0xFFD97706),
                    Color(0xFFB45309),
                    Color(0xFFF59E0B),
                    Color(0xFFFEF08A)
                ),
                start = Offset(w * 0.25f, h * 0.05f),
                end = Offset(w * 0.75f, h * 0.95f)
            )
            val outerStroke = Stroke(width = strokeW, cap = StrokeCap.Round)
            val innerStroke = Stroke(width = strokeW * 0.88f, cap = StrokeCap.Round)
            val highlightStroke = Stroke(width = strokeW * 0.25f, cap = StrokeCap.Round)

            onDrawBehind {
                rotate(degrees = 16f, pivot = center) {
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.20f),
                        topLeft = Offset(w * 0.30f + 2f, h * 0.08f + 3f),
                        size = Size(w * 0.40f, h * 0.82f),
                        cornerRadius = CornerRadius(w * 0.20f, w * 0.20f),
                        style = outerStroke
                    )
                    drawRoundRect(
                        brush = goldBrush,
                        topLeft = Offset(w * 0.29f, h * 0.08f),
                        size = Size(w * 0.42f, h * 0.82f),
                        cornerRadius = CornerRadius(w * 0.21f, w * 0.21f),
                        style = outerStroke
                    )
                    drawRoundRect(
                        brush = goldBrush,
                        topLeft = Offset(w * 0.38f, h * 0.22f),
                        size = Size(w * 0.24f, h * 0.56f),
                        cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
                        style = innerStroke
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.55f),
                        topLeft = Offset(w * 0.29f, h * 0.08f),
                        size = Size(w * 0.42f, h * 0.82f),
                        cornerRadius = CornerRadius(w * 0.21f, w * 0.21f),
                        style = highlightStroke
                    )
                }
            }
        }
    )
}

@Composable
private fun StickyTableCompactPreview(
    table: StickyTableData,
    maxRows: Int,
    accentColor: Color,
    textColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(textColor.copy(alpha = 0.04f))
            .padding(vertical = 2.dp)
    ) {
        val count = minOf(table.rows, maxRows)
        for (r in 0 until count) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (r == 0) accentColor.copy(alpha = 0.14f) else if (r % 2 == 1) textColor.copy(alpha = 0.03f) else Color.Transparent)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                for (c in 0 until table.cols) {
                    val cellText = table.cells.getOrNull(r)?.getOrNull(c) ?: ""
                    Text(
                        text = cellText.ifBlank { "-" },
                        color = textColor,
                        fontSize = 11.sp,
                        fontWeight = if (r == 0) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 3.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StickyNoteFullScreenDialog(
    initialNote: StickyNoteEntity?,
    startInEditMode: Boolean,
    viewModel: FinancialViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isBn = userProfile.language == "bn"
    val isLight = isAppLightMode()
    val isBoldMode = LocalBoldTextEnabled.current

    var currentNoteId by remember(initialNote?.id) { mutableIntStateOf(initialNote?.id ?: 0) }
    var isEditing by remember(initialNote?.id, startInEditMode) { mutableStateOf(startInEditMode) }

    val (migratedBody, initialEnvelope) = remember(initialNote?.id) {
        parseNoteBodyAndEnvelope(
            initialNote?.bodyText ?: "",
            initialNote?.subtitle ?: ""
        )
    }

    var title by remember(initialNote?.id) { mutableStateOf(initialNote?.title ?: "") }
    var subHeading by remember(initialNote?.id) { mutableStateOf(initialEnvelope.subHeading) }
    var pinIcon by remember(initialNote?.id) { mutableStateOf(initialEnvelope.pinIcon) }
    var bodyField by remember(initialNote?.id) {
        mutableStateOf(TextFieldValue(text = migratedBody))
    }
    var colorIndex by remember(initialNote?.id) { mutableIntStateOf(initialNote?.colorIndex ?: 0) }
    var lineAlignments by remember(initialNote?.id) {
        mutableStateOf(parseLineAlignments(initialNote?.textAlignment ?: "START"))
    }
    val undoRedo = remember(initialNote?.id) { StickyUndoRedoController() }
    var committedSnapshot by remember(initialNote?.id) {
        mutableStateOf(StickyDraftSnapshot(TextFieldValue(text = migratedBody), parseLineAlignments(initialNote?.textAlignment ?: "START")))
    }
    var isApplyingHistory by remember(initialNote?.id) { mutableStateOf(false) }
    val editorScope = rememberCoroutineScope()
    val subHeadingFocusRequester = remember { FocusRequester() }
    val bodyFocusRequester = remember { FocusRequester() }
    val subHeadingBringIntoViewRequester = remember { BringIntoViewRequester() }
    val bodyBringIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(bodyField.text, lineAlignments) {
        if (isApplyingHistory) {
            isApplyingHistory = false
            return@LaunchedEffect
        }
        delay(550)
        val current = StickyDraftSnapshot(bodyField, lineAlignments)
        if (current.textFieldValue.text != committedSnapshot.textFieldValue.text ||
            current.lineAlignments != committedSnapshot.lineAlignments
        ) {
            undoRedo.pushSnapshot(committedSnapshot)
            committedSnapshot = current
        }
    }

    var tags by remember(initialNote?.id) {
        mutableStateOf(
            initialNote?.tags?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        )
    }
    var newTagInput by remember { mutableStateOf("") }
    var showAddTagField by remember { mutableStateOf(false) }

    var tableData by remember(initialNote?.id) {
        mutableStateOf(initialEnvelope.table)
    }
    var attachments by remember(initialNote?.id) {
        mutableStateOf(StickyAttachmentItem.listFromJson(initialNote?.attachmentsJson ?: "[]"))
    }

    var showMoreMenu by remember { mutableStateOf(false) }
    var showPinChoiceDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showUnsavedWarning by remember { mutableStateOf(false) }
    var previewImageItem by remember { mutableStateOf<StickyAttachmentItem?>(null) }
    var longPressedAttachmentIndex by remember { mutableStateOf<Int?>(null) }

    val palette = getPaletteForIndex(colorIndex)
    val surfaceBg = if (isLight) palette.cardBgLight else palette.cardBgDark
    val textColor = if (isLight) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val effectiveAccent = if (colorIndex == 0) AppAccentColor() else palette.accentBorder

    val initialTableJson = remember(initialNote?.id) { initialEnvelope.table?.toJson() }
    val serializedAlignments = remember(lineAlignments) { serializeLineAlignments(lineAlignments) }
    val serializedTags = remember(tags) { tags.joinToString(",") }
    val serializedTable = remember(tableData) { tableData?.toJson() }
    val serializedAttachments = remember(attachments) { StickyAttachmentItem.listToJson(attachments) }

    val hasUnsavedChanges = remember(
        title, subHeading, pinIcon, bodyField.text, colorIndex,
        serializedAlignments, serializedTags, serializedTable, serializedAttachments, initialNote
    ) {
        if (initialNote == null) {
            title.isNotBlank() || subHeading.isNotBlank() || bodyField.text.isNotBlank() || tableData != null || attachments.isNotEmpty()
        } else {
            title != initialNote.title ||
                subHeading != initialEnvelope.subHeading ||
                pinIcon != initialEnvelope.pinIcon ||
                bodyField.text != migratedBody ||
                colorIndex != initialNote.colorIndex ||
                serializedAlignments != initialNote.textAlignment ||
                serializedTags != initialNote.tags ||
                serializedTable != initialTableJson ||
                serializedAttachments != initialNote.attachmentsJson
        }
    }

    fun persistCurrentDraft(
        overrideAttachments: List<StickyAttachmentItem> = attachments,
        overrideTable: StickyTableData? = tableData,
        overridePinIcon: String = pinIcon,
        overrideBodyText: String = bodyField.text,
        exitEditAfterSave: Boolean = false,
        closeDialogAfterSave: Boolean = false
    ) {
        val hasContent = title.isNotBlank() || subHeading.isNotBlank() || overrideBodyText.isNotBlank() || overrideTable != null || overrideAttachments.isNotEmpty()
        if (!hasContent) {
            if (closeDialogAfterSave) onDismiss()
            else if (exitEditAfterSave) isEditing = false
            return
        }
        val serializedSubtitle = StickySubtitleEnvelope(
            subHeading = subHeading.trim(),
            pinIcon = overridePinIcon,
            table = overrideTable
        ).toSerializedString()

        val entity = StickyNoteEntity(
            id = currentNoteId,
            title = title.trim(),
            subtitle = serializedSubtitle,
            bodyText = overrideBodyText,
            textAlignment = serializeLineAlignments(lineAlignments),
            colorIndex = colorIndex,
            tags = tags.joinToString(","),
            attachmentsJson = StickyAttachmentItem.listToJson(overrideAttachments),
            sortOrder = initialNote?.sortOrder ?: 0,
            createdAt = initialNote?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        viewModel.saveStickyNote(entity) { savedId ->
            currentNoteId = savedId
            if (closeDialogAfterSave) {
                onDismiss()
            } else if (exitEditAfterSave) {
                isEditing = false
            }
        }
    }

    fun handleBackRequest() {
        if (hasUnsavedChanges) {
            showUnsavedWarning = true
        } else if (isEditing && initialNote != null) {
            isEditing = false
        } else {
            onDismiss()
        }
    }

    if (showUnsavedWarning) {
        UnsavedChangesWarningDialog(
            isBengali = isBn,
            onKeepEditing = { showUnsavedWarning = false },
            onDiscard = {
                showUnsavedWarning = false
                if (initialNote != null) {
                    // Revert to initial state and switch to preview
                    title = initialNote.title
                    subHeading = initialEnvelope.subHeading
                    pinIcon = initialEnvelope.pinIcon
                    bodyField = TextFieldValue(text = migratedBody)
                    colorIndex = initialNote.colorIndex
                    lineAlignments = parseLineAlignments(initialNote.textAlignment)
                    tags = initialNote.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    tableData = initialEnvelope.table
                    attachments = StickyAttachmentItem.listFromJson(initialNote.attachmentsJson)
                    isEditing = false
                } else {
                    onDismiss()
                }
            }
        )
    }

    if (showDeleteConfirm && initialNote != null) {
        ConfirmDeleteDialog(
            itemName = title.ifBlank { if (isBn) "স্টিকি নোট" else "Sticky Note" },
            requireTypedConfirmation = false,
            onConfirm = {
                showDeleteConfirm = false
                viewModel.softDeleteStickyNote(initialNote)
                onDismiss()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    if (showPinChoiceDialog) {
        AlertDialog(
            onDismissRequest = { showPinChoiceDialog = false },
            containerColor = getDialogBackground(),
            shape = RoundedCornerShape(22.dp),
            title = {
                Text(
                    text = if (isBn) "কার্ডের পিন / ক্লিপ আইকন বেছে নিন" else "Choose Attached Card Icon",
                    color = AppTextColor(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isBn) "স্টিকি ল্যান্ডিং পেজে কার্ডের ওপরে কোন আইকনটি দেখাবে তা নির্বাচন করুন:"
                        else "Select the icon displayed at the top of this note card on the Sticky wall:",
                        color = AppSecondaryTextColor(),
                        fontSize = 13.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf("📌" to (if (isBn) "পিন (Pin)" else "Wall Pin (📌)"), "📎" to (if (isBn) "ক্লিপ (Clip)" else "Paperclip (📎)")).forEach { (iconStr, labelStr) ->
                            val selected = pinIcon == iconStr
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) effectiveAccent.copy(alpha = 0.18f) else getSettingsItemBg(),
                                border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) effectiveAccent else AppTextColor().copy(alpha = 0.15f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        pinIcon = iconStr
                                        showPinChoiceDialog = false
                                        if (currentNoteId != 0 || initialNote != null) {
                                            persistCurrentDraft(overridePinIcon = iconStr)
                                        }
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 14.dp, horizontal = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = iconStr, fontSize = 26.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = labelStr,
                                        color = AppTextColor(),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPinChoiceDialog = false }) {
                    Text(if (isBn) "বন্ধ করুন" else "Close", color = effectiveAccent, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Full-screen in-app image preview when clicking an image attachment
    previewImageItem?.let { imgItem ->
        FullScreenImagePreviewDialog(
            imageUri = imgItem.uri,
            title = imgItem.name,
            onDismiss = { previewImageItem = null }
        )
    }

    // Long-press Attachment Options Dialog (View, Download, Share, Remove)
    longPressedAttachmentIndex?.let { attIdx ->
        val item = attachments.getOrNull(attIdx)
        if (item != null) {
            val isImage = item.mimeType.startsWith("image/") ||
                item.uri.endsWith(".png", true) ||
                item.uri.endsWith(".jpg", true) ||
                item.uri.endsWith(".jpeg", true) ||
                item.uri.endsWith(".webp", true)

            AlertDialog(
                onDismissRequest = { longPressedAttachmentIndex = null },
                containerColor = getDialogBackground(),
                shape = RoundedCornerShape(22.dp),
                title = {
                    Text(
                        text = item.name,
                        color = AppTextColor(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        // 1. View
                        TextButton(
                            onClick = {
                                longPressedAttachmentIndex = null
                                if (isImage) {
                                    previewImageItem = item
                                } else {
                                    openStickyAttachmentExternally(context, item)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Visibility, contentDescription = null, tint = effectiveAccent)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(if (isBn) "দেখুন (View)" else "View", color = AppTextColor(), fontWeight = FontWeight.SemiBold)
                            }
                        }
                        // 2. Download
                        TextButton(
                            onClick = {
                                longPressedAttachmentIndex = null
                                downloadStickyAttachmentToDevice(context, item, isBn)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF10B981))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(if (isBn) "ডাউনলোড করুন (Download)" else "Download", color = AppTextColor(), fontWeight = FontWeight.SemiBold)
                            }
                        }
                        // 3. Share
                        TextButton(
                            onClick = {
                                longPressedAttachmentIndex = null
                                shareStickyAttachmentExternally(context, item)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF3B82F6))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(if (isBn) "শেয়ার করুন (Share)" else "Share", color = AppTextColor(), fontWeight = FontWeight.SemiBold)
                            }
                        }
                        // 4. Remove
                        TextButton(
                            onClick = {
                                longPressedAttachmentIndex = null
                                val updated = attachments.toMutableList().apply { removeAt(attIdx) }
                                attachments = updated
                                persistCurrentDraft(overrideAttachments = updated)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = AccentPink)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(if (isBn) "মুছে ফেলুন (Remove)" else "Remove", color = AccentPink, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { longPressedAttachmentIndex = null }) {
                        Text(if (isBn) "বাতিল" else "Cancel", color = AppSecondaryTextColor())
                    }
                }
            )
        }
    }

    // File / Photo Picker for Attachments (automatically enforces <= 2MB JPEG compression for images)
    val attachmentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = copyUriToPersistentFile(context, uri, "STICKY")
            var displayName = "Attached_File"
            var fileSize = 0L
            runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIdx >= 0) displayName = cursor.getString(nameIdx) ?: displayName
                    }
                }
            }
            if (savedPath.startsWith("file://")) {
                runCatching {
                    val f = File(Uri.parse(savedPath).path ?: "")
                    if (f.exists()) fileSize = f.length()
                }
            }
            if (fileSize <= 0L) {
                runCatching {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        fileSize = pfd.statSize.coerceAtLeast(0L)
                    }
                }
            }
            val mime = context.contentResolver.getType(uri) ?: if (savedPath.endsWith(".jpg", true)) "image/jpeg" else "*/*"
            val updatedAttachments = attachments + StickyAttachmentItem(
                uri = savedPath,
                name = displayName,
                mimeType = mime,
                sizeBytes = fileSize
            )
            attachments = updatedAttachments
            persistCurrentDraft(overrideAttachments = updatedAttachments)
            Toast.makeText(
                context,
                if (isBn) "ফাইল সংযুক্ত ও সেভ হয়েছে" else "Attachment added & saved",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun pushUndoCheckpointBeforeImmediateAction() {
        val current = StickyDraftSnapshot(bodyField, lineAlignments)
        if (current.textFieldValue.text != committedSnapshot.textFieldValue.text ||
            current.lineAlignments != committedSnapshot.lineAlignments
        ) {
            undoRedo.pushSnapshot(committedSnapshot)
        }
        undoRedo.pushSnapshot(current)
    }

    // Helper to wrap OR cleanly unwrap selected text in *...* or _..._
    fun applyInlineFormatting(marker: String) {
        pushUndoCheckpointBeforeImmediateAction()
        val text = bodyField.text
        val sel = bodyField.selection
        val start = sel.min.coerceIn(0, text.length)
        val end = sel.max.coerceIn(0, text.length)
        val updatedField = if (start < end) {
            val selectedSub = text.substring(start, end)
            if (selectedSub.startsWith(marker) && selectedSub.endsWith(marker) && selectedSub.length >= 2 * marker.length) {
                val unwrapped = selectedSub.substring(marker.length, selectedSub.length - marker.length)
                val newText = text.replaceRange(start, end, unwrapped)
                TextFieldValue(
                    text = newText,
                    selection = TextRange(start, start + unwrapped.length)
                )
            } else if (
                start >= marker.length &&
                end + marker.length <= text.length &&
                text.substring(start - marker.length, start) == marker &&
                text.substring(end, end + marker.length) == marker
            ) {
                val newText = text.removeRange(end, end + marker.length)
                    .removeRange(start - marker.length, start)
                TextFieldValue(
                    text = newText,
                    selection = TextRange(start - marker.length, end - marker.length)
                )
            } else {
                val replacement = "$marker$selectedSub$marker"
                val newText = text.replaceRange(start, end, replacement)
                TextFieldValue(
                    text = newText,
                    selection = TextRange(start + marker.length, end + marker.length)
                )
            }
        } else {
            val insertion = "$marker$marker"
            val newText = text.substring(0, start) + insertion + text.substring(start)
            TextFieldValue(
                text = newText,
                selection = TextRange(start + marker.length)
            )
        }
        bodyField = updatedField
        committedSnapshot = StickyDraftSnapshot(updatedField, lineAlignments)
    }

    fun applyLinePrefixToggle(prefix: String) {
        pushUndoCheckpointBeforeImmediateAction()
        val updatedField = toggleLinePrefixAtSelection(bodyField, prefix)
        bodyField = updatedField
        committedSnapshot = StickyDraftSnapshot(updatedField, lineAlignments)
    }

    // Helper to apply alignment ONLY to the line(s) containing the current cursor/selection
    fun applyAlignmentToSelectedLines(alignMode: String) {
        pushUndoCheckpointBeforeImmediateAction()
        val text = bodyField.text
        val selStart = bodyField.selection.min.coerceIn(0, text.length)
        val selEnd = bodyField.selection.max.coerceIn(0, text.length)

        var currentPos = 0
        var lineIdx = 0
        val updated = lineAlignments.toMutableMap()
        val lines = text.split('\n')
        for (line in lines) {
            val lineStart = currentPos
            val lineEnd = currentPos + line.length
            val overlaps = if (selStart == selEnd) {
                selStart in lineStart..lineEnd
            } else {
                maxOf(selStart, lineStart) <= minOf(selEnd, lineEnd)
            }
            if (overlaps) {
                if (alignMode == "START") updated.remove(lineIdx)
                else updated[lineIdx] = alignMode
            }
            currentPos = lineEnd + 1
            lineIdx++
        }
        lineAlignments = updated
        committedSnapshot = StickyDraftSnapshot(bodyField, updated)
    }

    val currentCursorLineAlignment = remember(bodyField.text, bodyField.selection, lineAlignments) {
        val text = bodyField.text
        val cursor = bodyField.selection.min.coerceIn(0, text.length)
        val lineIdx = text.substring(0, cursor).count { it == '\n' }
        lineAlignments[lineIdx] ?: "START"
    }
    val isCursorBold = remember(bodyField) { isCursorInsideInlineMarker(bodyField, '*') }
    val isCursorItalic = remember(bodyField) { isCursorInsideInlineMarker(bodyField, '_') }
    val currentLineStartsWithChecklist = remember(bodyField) {
        val text = bodyField.text
        val cursor = bodyField.selection.min.coerceIn(0, text.length)
        val lineStart = if (cursor == 0) 0 else (text.lastIndexOf('\n', cursor - 1) + 1)
        val lineEndIdx = text.indexOf('\n', lineStart)
        val line = text.substring(lineStart, if (lineEndIdx == -1) text.length else lineEndIdx)
        line.startsWith(STICKY_CHECKBOX_UNCHECKED) || line.startsWith(STICKY_CHECKBOX_CHECKED)
    }
    val currentLineStartsWithBullet = remember(bodyField) {
        val text = bodyField.text
        val cursor = bodyField.selection.min.coerceIn(0, text.length)
        val lineStart = if (cursor == 0) 0 else (text.lastIndexOf('\n', cursor - 1) + 1)
        val lineEndIdx = text.indexOf('\n', lineStart)
        val line = text.substring(lineStart, if (lineEndIdx == -1) text.length else lineEndIdx)
        line.startsWith(STICKY_BULLET_PREFIX)
    }

    BackHandler {
        handleBackRequest()
    }

    Dialog(
        onDismissRequest = {
            handleBackRequest()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        DialogEdgeToEdge()
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = surfaceBg
        ) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0.dp),
                topBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .dialogSafeTopPadding(extraTop = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Back Button
                            IconButton(
                                onClick = { handleBackRequest() },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(textColor.copy(alpha = 0.08f), CircleShape)
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = textColor)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Center: Note Title Centered in Top Bar WITHOUT pin/paperclip icon
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isEditing) {
                                    BasicTextField(
                                        value = title,
                                        onValueChange = { title = it },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = textColor,
                                            fontSize = 19.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = if (isBoldMode) ArchivoBlackFontFamily else AppFontFamily,
                                            textAlign = TextAlign.Center
                                        ),
                                        cursorBrush = SolidColor(effectiveAccent),
                                        keyboardOptions = KeyboardOptions(
                                            capitalization = KeyboardCapitalization.Sentences,
                                            imeAction = ImeAction.Next
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onNext = { subHeadingFocusRequester.requestFocus() }
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        decorationBox = { inner ->
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (title.isEmpty()) {
                                                    Text(
                                                        text = if (isBn) "শিরোনাম লিখুন..." else "Title...",
                                                        color = textColor.copy(alpha = 0.42f),
                                                        fontSize = 19.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                                inner()
                                            }
                                        }
                                    )
                                } else {
                                    Text(
                                        text = title.ifBlank { if (isBn) "শিরোনামহীন নোট" else "Untitled Note" },
                                        color = textColor,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = if (isBoldMode) ArchivoBlackFontFamily else AppFontFamily,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Right: 1-tap Preview/Edit toggle (preserves unsaved draft) + Save button + More Menu
                            IconButton(
                                onClick = { isEditing = !isEditing },
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(textColor.copy(alpha = 0.08f), CircleShape)
                                    .testTag("sticky_note_mode_toggle_btn")
                            ) {
                                Icon(
                                    imageVector = if (isEditing) Icons.Default.Visibility else Icons.Default.Edit,
                                    contentDescription = if (isEditing) "Preview Note" else "Edit Note",
                                    tint = textColor,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))

                            if (isEditing) {
                                FilledIconButton(
                                    onClick = {
                                        persistCurrentDraft(
                                            exitEditAfterSave = true,
                                            closeDialogAfterSave = false
                                        )
                                    },
                                    modifier = Modifier.size(38.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = effectiveAccent,
                                        contentColor = AppOnAccentColor(effectiveAccent)
                                    )
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Save Note", modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            Box {
                                IconButton(
                                    onClick = { showMoreMenu = true },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(textColor.copy(alpha = 0.08f), CircleShape)
                                        .testTag("sticky_note_more_menu_btn")
                                ) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = textColor)
                                }

                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false },
                                    offset = DpOffset(x = (-8).dp, y = 4.dp),
                                    modifier = Modifier
                                        .background(getDialogBackground())
                                        .heightIn(max = 320.dp)
                                ) {
                                    // 1. Edit
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (isBn) "এডিট (Edit)" else "Edit",
                                                color = AppTextColor(),
                                                fontWeight = FontWeight.Medium
                                            )
                                        },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = effectiveAccent) },
                                        onClick = {
                                            showMoreMenu = false
                                            isEditing = true
                                        }
                                    )
                                    // 2. Remove
                                    if (initialNote != null || currentNoteId != 0) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = if (isBn) "মুছে ফেলুন (Remove)" else "Remove",
                                                    color = AccentPink,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            },
                                            leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = AccentPink) },
                                            onClick = {
                                                showMoreMenu = false
                                                if (initialNote != null) {
                                                    showDeleteConfirm = true
                                                } else {
                                                    onDismiss()
                                                }
                                            }
                                        )
                                    }
                                    // 3. Show Preview (preserves draft state without discarding)
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (isBn) "প্রিভিউ দেখুন (Show Preview)" else "Show Preview",
                                                color = AppTextColor(),
                                                fontWeight = FontWeight.Medium
                                            )
                                        },
                                        leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF10B981)) },
                                        onClick = {
                                            showMoreMenu = false
                                            isEditing = false
                                        }
                                    )
                                    // 4. Attachment
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (isBn) "ফাইল সংযুক্ত করুন (Attachment)" else "Attachment",
                                                color = AppTextColor(),
                                                fontWeight = FontWeight.Medium
                                            )
                                        },
                                        leadingIcon = { Icon(Icons.Default.AttachFile, contentDescription = null, tint = Color(0xFF3B82F6)) },
                                        onClick = {
                                            showMoreMenu = false
                                            attachmentLauncher.launch("*/*")
                                        }
                                    )
                                    // 5. Share
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (isBn) "শেয়ার করুন (Share)" else "Share",
                                                color = AppTextColor(),
                                                fontWeight = FontWeight.Medium
                                            )
                                        },
                                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFFF59E0B)) },
                                        onClick = {
                                            showMoreMenu = false
                                            shareStickyNoteContent(context, title, subHeading, bodyField.text, tableData)
                                        }
                                    )
                                    // 6. Attached Icon (📌 / 📎)
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (isBn) "কার্ড আইকন ($pinIcon)" else "Attached Icon ($pinIcon)",
                                                color = AppTextColor(),
                                                fontWeight = FontWeight.Medium
                                            )
                                        },
                                        leadingIcon = {
                                            Text(text = pinIcon, fontSize = 17.sp)
                                        },
                                        onClick = {
                                            showMoreMenu = false
                                            showPinChoiceDialog = true
                                        }
                                    )
                                }
                            }
                        }

                        // Color palette selector has been moved to the bottom end line of the page content
                    }
                },
                bottomBar = {
                    // Show rich notes formatting toolbar ONLY in Editing Mode (never in Preview Mode)
                    if (isEditing) {
                        Surface(
                            color = if (isLight) Color.White.copy(alpha = 0.96f) else Color(0xFF1E293B).copy(alpha = 0.96f),
                            tonalElevation = 6.dp,
                            shadowElevation = 8.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .dialogSafeBottomPadding(extraBottom = 4.dp)
                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier
                                        .widthIn(max = 640.dp)
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 1. Undo
                                    IconButton(
                                        onClick = {
                                            undoRedo.undo(StickyDraftSnapshot(bodyField, lineAlignments))?.let { prev ->
                                                isApplyingHistory = true
                                                bodyField = prev.textFieldValue
                                                lineAlignments = prev.lineAlignments
                                                committedSnapshot = prev
                                            }
                                        },
                                        enabled = undoRedo.canUndo,
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Undo,
                                            contentDescription = "Undo",
                                            tint = if (undoRedo.canUndo) textColor else textColor.copy(alpha = 0.3f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    // 2. Redo
                                    IconButton(
                                        onClick = {
                                            undoRedo.redo(StickyDraftSnapshot(bodyField, lineAlignments))?.let { next ->
                                                isApplyingHistory = true
                                                bodyField = next.textFieldValue
                                                lineAlignments = next.lineAlignments
                                                committedSnapshot = next
                                            }
                                        },
                                        enabled = undoRedo.canRedo,
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Redo,
                                            contentDescription = "Redo",
                                            tint = if (undoRedo.canRedo) textColor else textColor.copy(alpha = 0.3f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    // 3. Bold
                                    IconButton(
                                        onClick = { applyInlineFormatting("*") },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isCursorBold) effectiveAccent.copy(alpha = 0.25f) else Color.Transparent)
                                    ) {
                                        Icon(Icons.Default.FormatBold, contentDescription = "Bold", tint = textColor, modifier = Modifier.size(20.dp))
                                    }
                                    // 4. Italic
                                    IconButton(
                                        onClick = { applyInlineFormatting("_") },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isCursorItalic) effectiveAccent.copy(alpha = 0.25f) else Color.Transparent)
                                    ) {
                                        Text(
                                            text = "I",
                                            color = textColor,
                                            fontSize = 17.sp,
                                            fontStyle = FontStyle.Italic,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    // 5. Checklist
                                    IconButton(
                                        onClick = { applyLinePrefixToggle(STICKY_CHECKBOX_UNCHECKED) },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (currentLineStartsWithChecklist) effectiveAccent.copy(alpha = 0.25f) else Color.Transparent)
                                    ) {
                                        Icon(Icons.Default.CheckBox, contentDescription = "Checklist", tint = textColor, modifier = Modifier.size(20.dp))
                                    }
                                    // 6. Bullet List
                                    IconButton(
                                        onClick = { applyLinePrefixToggle(STICKY_BULLET_PREFIX) },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (currentLineStartsWithBullet) effectiveAccent.copy(alpha = 0.25f) else Color.Transparent)
                                    ) {
                                        Icon(Icons.Default.FormatListBulleted, contentDescription = "Bullet List", tint = textColor, modifier = Modifier.size(20.dp))
                                    }
                                    // 7. Align Left
                                    IconButton(
                                        onClick = { applyAlignmentToSelectedLines("START") },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (currentCursorLineAlignment == "START") effectiveAccent.copy(alpha = 0.25f) else Color.Transparent)
                                    ) {
                                        Icon(Icons.Default.FormatAlignLeft, contentDescription = "Align Line Left", tint = textColor, modifier = Modifier.size(20.dp))
                                    }
                                    // 8. Align Center
                                    IconButton(
                                        onClick = { applyAlignmentToSelectedLines("CENTER") },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (currentCursorLineAlignment == "CENTER") effectiveAccent.copy(alpha = 0.25f) else Color.Transparent)
                                    ) {
                                        Icon(Icons.Default.FormatAlignCenter, contentDescription = "Align Line Center", tint = textColor, modifier = Modifier.size(20.dp))
                                    }
                                    // 9. Align Right
                                    IconButton(
                                        onClick = { applyAlignmentToSelectedLines("END") },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (currentCursorLineAlignment == "END") effectiveAccent.copy(alpha = 0.25f) else Color.Transparent)
                                    ) {
                                        Icon(Icons.Default.FormatAlignRight, contentDescription = "Align Line Right", tint = textColor, modifier = Modifier.size(20.dp))
                                    }
                                    // 10. Justify
                                    IconButton(
                                        onClick = { applyAlignmentToSelectedLines("JUSTIFY") },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (currentCursorLineAlignment == "JUSTIFY") effectiveAccent.copy(alpha = 0.25f) else Color.Transparent)
                                    ) {
                                        Icon(Icons.Default.FormatAlignJustify, contentDescription = "Justify Line", tint = textColor, modifier = Modifier.size(20.dp))
                                    }
                                    // 11. Insert Borderless Visual Grid Table
                                    IconButton(
                                        onClick = {
                                            if (tableData == null) {
                                                tableData = StickyTableData(
                                                    rows = 3,
                                                    cols = 3,
                                                    cells = List(3) { r ->
                                                        List(3) { c -> if (r == 0) "Header ${c + 1}" else "" }
                                                    }
                                                )
                                            } else {
                                                val t = tableData!!
                                                tableData = t.copy(
                                                    rows = t.rows + 1,
                                                    cells = t.cells + listOf(List(t.cols) { "" })
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (tableData != null) effectiveAccent.copy(alpha = 0.22f) else Color.Transparent)
                                    ) {
                                        Icon(Icons.Default.TableChart, contentDescription = "Insert Visual Grid Table", tint = textColor, modifier = Modifier.size(20.dp))
                                    }
                                    // 12. Attach File
                                    IconButton(
                                        onClick = { attachmentLauncher.launch("*/*") },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(Icons.Default.AttachFile, contentDescription = "Attach File", tint = textColor, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                val pageScrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(pageScrollState)
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tags Row
                    if (tags.isNotEmpty() || isEditing) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            tags.forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = effectiveAccent.copy(alpha = 0.16f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "#$tag",
                                            color = textColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (isEditing) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove Tag",
                                                tint = textColor.copy(alpha = 0.7f),
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable { tags = tags - tag }
                                            )
                                        }
                                    }
                                }
                            }

                            if (isEditing) {
                                if (showAddTagField) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(textColor.copy(alpha = 0.08f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        BasicTextField(
                                            value = newTagInput,
                                            onValueChange = { newTagInput = it.replace(",", "") },
                                            singleLine = true,
                                            textStyle = TextStyle(
                                                color = textColor,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            cursorBrush = SolidColor(effectiveAccent),
                                            modifier = Modifier.widthIn(min = 60.dp, max = 140.dp),
                                            decorationBox = { inner ->
                                                if (newTagInput.isEmpty()) {
                                                    Text(
                                                        text = if (isBn) "ট্যাগ নাম..." else "Tag name...",
                                                        color = textColor.copy(alpha = 0.45f),
                                                        fontSize = 12.sp
                                                    )
                                                }
                                                inner()
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Confirm Tag",
                                            tint = effectiveAccent,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clickable {
                                                    val clean = newTagInput.trim().removePrefix("#")
                                                    if (clean.isNotBlank() && !tags.any { it.equals(clean, ignoreCase = true) }) {
                                                        tags = tags + clean
                                                    }
                                                    newTagInput = ""
                                                    showAddTagField = false
                                                }
                                        )
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = textColor.copy(alpha = 0.07f),
                                        modifier = Modifier.clickable { showAddTagField = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Add Tag",
                                                tint = textColor.copy(alpha = 0.75f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isBn) "ট্যাগ যোগ করুন" else "Add Tag",
                                                color = textColor.copy(alpha = 0.75f),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Sub-heading displayed right before the main body text (both in Edit Mode and Preview Mode)
                    if (isEditing) {
                        BasicTextField(
                            value = subHeading,
                            onValueChange = { subHeading = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = textColor.copy(alpha = 0.78f),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = if (isBoldMode) StickyBoldBaseFontFamily else AppFontFamily
                            ),
                            cursorBrush = SolidColor(effectiveAccent),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { bodyFocusRequester.requestFocus() }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(subHeadingFocusRequester)
                                .bringIntoViewRequester(subHeadingBringIntoViewRequester)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        editorScope.launch { subHeadingBringIntoViewRequester.bringIntoView() }
                                    }
                                },
                            decorationBox = { inner ->
                                if (subHeading.isEmpty()) {
                                    Text(
                                        text = if (isBn) "উপ-শিরোনাম (Sub-heading)..." else "Sub-heading...",
                                        color = textColor.copy(alpha = 0.38f),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                inner()
                            }
                        )
                    } else if (subHeading.isNotBlank()) {
                        Text(
                            text = subHeading,
                            color = textColor.copy(alpha = 0.75f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = if (isBoldMode) StickyBoldBaseFontFamily else AppFontFamily,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Main Body Text (Plain background, no card container)
                    val visualTransformation = remember(isLight, isBoldMode, textColor, effectiveAccent, lineAlignments) {
                        StickyFormattingVisualTransformation(
                            isLight = isLight,
                            isBoldMode = isBoldMode,
                            baseColor = textColor,
                            accentColor = effectiveAccent,
                            lineAlignments = lineAlignments
                        )
                    }

                    if (isEditing) {
                        BasicTextField(
                            value = bodyField,
                            onValueChange = { rawNewVal ->
                                val continuedVal = handleChecklistOrBulletNewline(bodyField, rawNewVal) ?: rawNewVal
                                if (continuedVal.text != bodyField.text && lineAlignments.isNotEmpty()) {
                                    lineAlignments = remapLineAlignmentsOnEdit(
                                        oldText = bodyField.text,
                                        newText = continuedVal.text,
                                        alignments = lineAlignments
                                    )
                                }
                                bodyField = continuedVal
                            },
                            textStyle = TextStyle(
                                color = textColor,
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                fontFamily = if (isBoldMode) StickyBoldBaseFontFamily else AppFontFamily,
                                fontWeight = if (isBoldMode) FontWeight.Bold else FontWeight.Normal
                            ),
                            visualTransformation = visualTransformation,
                            cursorBrush = SolidColor(effectiveAccent),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 140.dp)
                                .focusRequester(bodyFocusRequester)
                                .bringIntoViewRequester(bodyBringIntoViewRequester)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        editorScope.launch { bodyBringIntoViewRequester.bringIntoView() }
                                    }
                                },
                            decorationBox = { inner ->
                                if (bodyField.text.isEmpty()) {
                                    Text(
                                        text = if (isBn) "নোট লিখুন..." else "Note...",
                                        color = textColor.copy(alpha = 0.42f),
                                        fontSize = 16.sp
                                    )
                                }
                                inner()
                            }
                        )
                    } else {
                        val rawDisplayBody = bodyField.text.ifBlank {
                            if (tableData == null && attachments.isEmpty() && subHeading.isBlank()) {
                                if (isBn) "কোনো লেখা নেই" else "Empty note"
                            } else {
                                ""
                            }
                        }
                        if (rawDisplayBody.isNotBlank()) {
                            val baseNoteFont = if (isBoldMode) StickyBoldBaseFontFamily else AppFontFamily
                            val baseNoteWeight = if (isBoldMode) FontWeight.Bold else FontWeight.Normal
                            val hasChecklistLines = remember(rawDisplayBody) {
                                rawDisplayBody.lineSequence().any {
                                    it.startsWith(STICKY_CHECKBOX_UNCHECKED) || it.startsWith(STICKY_CHECKBOX_CHECKED)
                                }
                            }
                            if (hasChecklistLines) {
                                val lines = remember(rawDisplayBody) { rawDisplayBody.split('\n') }
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    lines.forEachIndexed { lineIdx, line ->
                                        val isUnchecked = line.startsWith(STICKY_CHECKBOX_UNCHECKED)
                                        val isChecked = line.startsWith(STICKY_CHECKBOX_CHECKED)
                                        val lineAlign = mapStringToTextAlign(lineAlignments[lineIdx] ?: "START")
                                        if (isUnchecked || isChecked) {
                                            val itemText = line.removePrefix(
                                                if (isChecked) STICKY_CHECKBOX_CHECKED else STICKY_CHECKBOX_UNCHECKED
                                            )
                                            val lineColor = if (isChecked) textColor.copy(alpha = 0.52f) else textColor
                                            val formattedItem = buildFormattedStickyAnnotatedString(
                                                rawText = itemText,
                                                isBoldMode = isBoldMode,
                                                baseColor = lineColor,
                                                accentColor = effectiveAccent
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        val updatedText = toggleChecklistLineAtIndex(bodyField.text, lineIdx)
                                                        bodyField = bodyField.copy(text = updatedText)
                                                        persistCurrentDraft(overrideBodyText = updatedText)
                                                    }
                                                    .padding(vertical = 4.dp, horizontal = 2.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Icon(
                                                    imageVector = if (isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                                    contentDescription = if (isChecked) "Checked" else "Unchecked",
                                                    tint = if (isChecked) effectiveAccent else textColor.copy(alpha = 0.65f),
                                                    modifier = Modifier
                                                        .padding(top = 2.dp)
                                                        .size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = formattedItem,
                                                    color = lineColor,
                                                    fontSize = 16.sp,
                                                    lineHeight = 24.sp,
                                                    fontFamily = baseNoteFont,
                                                    fontWeight = baseNoteWeight,
                                                    textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                                    textAlign = lineAlign,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        } else if (line.isNotEmpty()) {
                                            val formattedLine = buildFormattedStickyAnnotatedString(
                                                rawText = line,
                                                isBoldMode = isBoldMode,
                                                baseColor = textColor,
                                                accentColor = effectiveAccent
                                            )
                                            Text(
                                                text = formattedLine,
                                                color = textColor,
                                                fontSize = 16.sp,
                                                lineHeight = 24.sp,
                                                fontFamily = baseNoteFont,
                                                fontWeight = baseNoteWeight,
                                                textAlign = lineAlign,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.height(6.dp))
                                        }
                                    }
                                }
                            } else {
                                val parsedFullBody = remember(rawDisplayBody, lineAlignments) {
                                    parseStickyFormattedText(rawDisplayBody, lineAlignments)
                                }
                                if (!parsedFullBody.hasCustomFormatting) {
                                    Text(
                                        text = parsedFullBody.plainText,
                                        color = textColor,
                                        fontSize = 16.sp,
                                        lineHeight = 24.sp,
                                        fontFamily = baseNoteFont,
                                        fontWeight = baseNoteWeight,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    val annotated = remember(parsedFullBody, isBoldMode, textColor, effectiveAccent) {
                                        buildAnnotatedFromParsedSticky(
                                            parsed = parsedFullBody,
                                            isBoldMode = isBoldMode,
                                            baseColor = textColor,
                                            accentColor = effectiveAccent
                                        )
                                    }
                                    Text(
                                        text = annotated,
                                        color = textColor,
                                        fontSize = 16.sp,
                                        lineHeight = 24.sp,
                                        fontFamily = baseNoteFont,
                                        fontWeight = baseNoteWeight,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // Borderless Visual Grid Table Section
                    tableData?.let { table ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(textColor.copy(alpha = 0.04f))
                                .padding(12.dp)
                        ) {
                            if (isEditing) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isBn) "টেবিল গ্রিড (${table.rows}×${table.cols})" else "Table Grid (${table.rows}×${table.cols})",
                                        color = textColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { tableData = null },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Remove Table", tint = AccentPink, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = effectiveAccent.copy(alpha = 0.14f),
                                        modifier = Modifier.clickable {
                                            val newRows = table.rows + 1
                                            val newCells = table.cells + listOf(List(table.cols) { "" })
                                            tableData = StickyTableData(newRows, table.cols, newCells)
                                        }
                                    ) {
                                        Text(
                                            text = "+Row",
                                            color = effectiveAccent,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                    if (table.rows > 1) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = AccentPink.copy(alpha = 0.14f),
                                            modifier = Modifier.clickable {
                                                val newRows = table.rows - 1
                                                tableData = StickyTableData(newRows, table.cols, table.cells.take(newRows))
                                            }
                                        ) {
                                            Text(
                                                text = "-Row",
                                                color = AccentPink,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = effectiveAccent.copy(alpha = 0.14f),
                                        modifier = Modifier.clickable {
                                            val newCols = table.cols + 1
                                            val newCells = table.cells.map { it + "" }
                                            tableData = StickyTableData(table.rows, newCols, newCells)
                                        }
                                    ) {
                                        Text(
                                            text = "+Col",
                                            color = effectiveAccent,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                    if (table.cols > 1) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = AccentPink.copy(alpha = 0.14f),
                                            modifier = Modifier.clickable {
                                                val newCols = table.cols - 1
                                                tableData = StickyTableData(table.rows, newCols, table.cells.map { it.take(newCols) })
                                            }
                                        ) {
                                            Text(
                                                text = "-Col",
                                                color = AccentPink,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                EditableStickyTableGrid(
                                    table = table,
                                    accentColor = effectiveAccent,
                                    textColor = textColor,
                                    onCellChange = { r, c, value ->
                                        val mutable = table.cells.map { it.toMutableList() }.toMutableList()
                                        if (r in mutable.indices && c in mutable[r].indices) {
                                            mutable[r][c] = value
                                            tableData = table.copy(cells = mutable)
                                        }
                                    }
                                )
                            } else {
                                StickyTableRenderedView(
                                    table = table,
                                    accentColor = effectiveAccent,
                                    textColor = textColor
                                )
                            }
                        }
                    }

                    // Attachments Section (Single tap = Image Preview or App Chooser; Long press = View, Download, Share, Remove)
                    if (attachments.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(textColor.copy(alpha = 0.04f))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (isBn) "সংযুক্ত ফাইল (${attachments.size})" else "Attachments (${attachments.size})",
                                color = textColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )

                            attachments.forEachIndexed { idx, item ->
                                val isImage = item.mimeType.startsWith("image/") ||
                                    item.uri.endsWith(".png", true) ||
                                    item.uri.endsWith(".jpg", true) ||
                                    item.uri.endsWith(".jpeg", true) ||
                                    item.uri.endsWith(".webp", true)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(textColor.copy(alpha = 0.06f))
                                        .combinedClickable(
                                            onClick = {
                                                if (isImage) {
                                                    previewImageItem = item
                                                } else {
                                                    openStickyAttachmentExternally(context, item)
                                                }
                                            },
                                            onLongClick = {
                                                longPressedAttachmentIndex = idx
                                            }
                                        )
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isImage) {
                                        AsyncImage(
                                            model = item.uri,
                                            contentDescription = item.name,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(effectiveAccent.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AttachFile,
                                                contentDescription = null,
                                                tint = effectiveAccent
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            color = textColor,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${formatFileSize(item.sizeBytes)} • ${if (isBn) "ট্যাপ: খুলুন • লং-প্রেস: অপশন" else "Tap to view • Hold for options"}",
                                            color = textColor.copy(alpha = 0.65f),
                                            fontSize = 11.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = { longPressedAttachmentIndex = idx },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Attachment Options",
                                            tint = textColor.copy(alpha = 0.65f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Page Color Options placed at the bottom end line of the page content (centered horizontally, NOT inside the 8-icon bottom bar)
                    if (isEditing) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                STICKY_COLOR_PALETTES.forEach { p ->
                                    val isSelected = p.index == colorIndex
                                    val dotFill = if (p.index == 0) {
                                        if (isLight) Color.White else Color(0xFF1E293B)
                                    } else {
                                        p.accentBorder
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(if (isSelected) 30.dp else 24.dp)
                                            .clip(CircleShape)
                                            .background(dotFill)
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.2.dp,
                                                color = if (isSelected) textColor else p.accentBorder.copy(alpha = 0.7f),
                                                shape = CircleShape
                                            )
                                            .clickable { colorIndex = p.index }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(64.dp + rememberSplitScreenSafeBottomPadding()))
                }
            }
        }
    }
}

@Composable
private fun EditableStickyTableGrid(
    table: StickyTableData,
    accentColor: Color,
    textColor: Color,
    onCellChange: (Int, Int, String) -> Unit
) {
    // Borderless visual grid table
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .clip(RoundedCornerShape(10.dp)),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (r in 0 until table.rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (c in 0 until table.cols) {
                    val value = table.cells.getOrNull(r)?.getOrNull(c) ?: ""
                    Box(
                        modifier = Modifier
                            .width(115.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (r == 0) accentColor.copy(alpha = 0.16f)
                                else textColor.copy(alpha = 0.05f)
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        BasicTextField(
                            value = value,
                            onValueChange = { onCellChange(r, c, it) },
                            textStyle = TextStyle(
                                color = textColor,
                                fontSize = 13.sp,
                                fontWeight = if (r == 0) FontWeight.Bold else FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(accentColor),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner ->
                                if (value.isEmpty()) {
                                    Text(
                                        text = if (r == 0) "Header ${c + 1}" else "Cell",
                                        color = textColor.copy(alpha = 0.35f),
                                        fontSize = 12.sp
                                    )
                                }
                                inner()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StickyTableRenderedView(
    table: StickyTableData,
    accentColor: Color,
    textColor: Color
) {
    // Borderless visual grid table
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .clip(RoundedCornerShape(10.dp)),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (r in 0 until table.rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (c in 0 until table.cols) {
                    val value = table.cells.getOrNull(r)?.getOrNull(c) ?: ""
                    Box(
                        modifier = Modifier
                            .width(115.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (r == 0) accentColor.copy(alpha = 0.16f)
                                else textColor.copy(alpha = 0.04f)
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = value.ifBlank { "-" },
                            color = textColor,
                            fontSize = 13.sp,
                            fontWeight = if (r == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

private fun resolveShareableUri(context: android.content.Context, rawUri: String): Uri? {
    return try {
        val parsedUri = Uri.parse(rawUri)
        if (parsedUri.scheme == "file") {
            val file = File(parsedUri.path ?: return null)
            if (!file.exists()) return null
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } else {
            parsedUri
        }
    } catch (_: Exception) {
        null
    }
}

private fun openStickyAttachmentExternally(context: android.content.Context, item: StickyAttachmentItem) {
    try {
        val shareUri = resolveShareableUri(context, item.uri) ?: return
        val ext = MimeTypeMap.getFileExtensionFromUrl(item.name).lowercase(Locale.ROOT)
        val resolvedMime = if (item.mimeType != "*/*" && item.mimeType.isNotBlank()) {
            item.mimeType
        } else {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(shareUri, resolvedMime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, item.name))
    } catch (e: Exception) {
        Toast.makeText(context, "No external app found to open ${item.name}", Toast.LENGTH_SHORT).show()
    }
}

private fun shareStickyAttachmentExternally(context: android.content.Context, item: StickyAttachmentItem) {
    try {
        val shareUri = resolveShareableUri(context, item.uri) ?: return
        val ext = MimeTypeMap.getFileExtensionFromUrl(item.name).lowercase(Locale.ROOT)
        val resolvedMime = if (item.mimeType != "*/*" && item.mimeType.isNotBlank()) {
            item.mimeType
        } else {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = resolvedMime
            putExtra(Intent.EXTRA_STREAM, shareUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share ${item.name}"))
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to share ${item.name}", Toast.LENGTH_SHORT).show()
    }
}

private fun downloadStickyAttachmentToDevice(context: android.content.Context, item: StickyAttachmentItem, isBn: Boolean) {
    try {
        val parsedUri = Uri.parse(item.uri)
        val inputStream = if (parsedUri.scheme == "file") {
            val f = File(parsedUri.path ?: return)
            if (!f.exists()) return
            f.inputStream()
        } else {
            context.contentResolver.openInputStream(parsedUri) ?: return
        }

        val cleanName = item.name.ifBlank { "Sticky_Attachment_${System.currentTimeMillis()}" }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, cleanName)
                put(MediaStore.Downloads.MIME_TYPE, item.mimeType.ifBlank { "application/octet-stream" })
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val destUri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (destUri != null) {
                context.contentResolver.openOutputStream(destUri)?.use { out ->
                    inputStream.use { inp -> inp.copyTo(out) }
                }
                Toast.makeText(
                    context,
                    if (isBn) "Downloads ফোল্ডারে সেভ হয়েছে: $cleanName" else "Saved to Downloads: $cleanName",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        }

        val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        val outFile = File(downloadsDir, cleanName)
        FileOutputStream(outFile).use { out ->
            inputStream.use { inp -> inp.copyTo(out) }
        }
        Toast.makeText(
            context,
            if (isBn) "ডাউনলোড সম্পন্ন: ${outFile.name}" else "Downloaded: ${outFile.name}",
            Toast.LENGTH_SHORT
        ).show()
    } catch (e: Exception) {
        Toast.makeText(
            context,
            if (isBn) "ডাউনলোড ব্যর্থ হয়েছে" else "Download failed: ${e.localizedMessage}",
            Toast.LENGTH_SHORT
        ).show()
    }
}

private fun shareStickyNoteContent(
    context: android.content.Context,
    title: String,
    subHeading: String,
    bodyText: String,
    table: StickyTableData?
) {
    val sb = StringBuilder()
    if (title.isNotBlank()) sb.append(title).append("\n")
    if (subHeading.isNotBlank()) sb.append(subHeading).append("\n")
    if (bodyText.isNotBlank()) {
        if (sb.isNotEmpty()) sb.append("\n")
        sb.append(bodyText).append("\n")
    }
    if (table != null) {
        sb.append("\n")
        for (r in 0 until table.rows) {
            sb.append(table.cells.getOrNull(r)?.joinToString(" | ") ?: "").append("\n")
        }
    }
    val content = sb.toString().trim().ifBlank { "Sticky Note" }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title.ifBlank { "Sticky Note" })
        putExtra(Intent.EXTRA_TEXT, content)
    }
    context.startActivity(Intent.createChooser(intent, "Share Sticky Note"))
}
