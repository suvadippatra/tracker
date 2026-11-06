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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
class StickyFormattingVisualTransformation(
    private val isLight: Boolean,
    private val isBoldMode: Boolean,
    private val baseColor: Color,
    private val accentColor: Color,
    private val lineAlignments: Map<Int, String>
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val markerColor = baseColor.copy(alpha = 0.28f)
        val boldSpanFont = if (isBoldMode) ArchivoBlackFontFamily else AppBoldFontFamily

        val styled = buildAnnotatedString {
            append(raw)

            // Apply per-line ParagraphStyle alignment
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

            // Style *bold* spans
            val boldRegex = Regex("""\*([^\*\n]+)\*""")
            boldRegex.findAll(raw).forEach { match ->
                val fullRange = match.range
                addStyle(
                    SpanStyle(color = markerColor, fontSize = 10.sp),
                    fullRange.first,
                    fullRange.first + 1
                )
                addStyle(
                    SpanStyle(
                        fontFamily = boldSpanFont,
                        fontWeight = FontWeight.Black,
                        fontSize = if (isBoldMode) 1.12.em else 1.05.em,
                        color = if (isBoldMode) accentColor else baseColor,
                        textDecoration = if (isBoldMode) TextDecoration.Underline else TextDecoration.None
                    ),
                    fullRange.first + 1,
                    fullRange.last
                )
                addStyle(
                    SpanStyle(color = markerColor, fontSize = 10.sp),
                    fullRange.last,
                    fullRange.last + 1
                )
            }

            // Style _italic_ spans
            val italicRegex = Regex("""_([^_\n]+)_""")
            italicRegex.findAll(raw).forEach { match ->
                val fullRange = match.range
                addStyle(
                    SpanStyle(color = markerColor, fontSize = 10.sp),
                    fullRange.first,
                    fullRange.first + 1
                )
                addStyle(
                    SpanStyle(
                        fontStyle = FontStyle.Italic,
                        color = baseColor.copy(alpha = 0.92f)
                    ),
                    fullRange.first + 1,
                    fullRange.last
                )
                addStyle(
                    SpanStyle(color = markerColor, fontSize = 10.sp),
                    fullRange.last,
                    fullRange.last + 1
                )
            }
        }

        return TransformedText(styled, OffsetMapping.Identity)
    }
}

/**
 * Builds an AnnotatedString for viewer/card display that strips `*` and `_` markers
 * and applies per-line ParagraphStyle alignments.
 */
@Composable
fun buildFormattedStickyAnnotatedString(
    rawText: String,
    isBoldMode: Boolean,
    baseColor: Color,
    accentColor: Color,
    lineAlignments: Map<Int, String> = emptyMap()
): AnnotatedString {
    val boldSpanFont = if (isBoldMode) ArchivoBlackFontFamily else AppBoldFontFamily
    val regularFont = if (isBoldMode) StickyBoldBaseFontFamily else AppFontFamily

    return remember(rawText, isBoldMode, baseColor, accentColor, lineAlignments) {
        buildAnnotatedString {
            val lines = rawText.split('\n')
            lines.forEachIndexed { lineIdx, lineText ->
                val paraStart = this.length
                var i = 0
                val n = lineText.length
                while (i < n) {
                    if (lineText[i] == '*') {
                        val end = lineText.indexOf('*', i + 1)
                        if (end > i + 1) {
                            val content = lineText.substring(i + 1, end)
                            pushStyle(
                                SpanStyle(
                                    fontFamily = boldSpanFont,
                                    fontWeight = FontWeight.Black,
                                    fontSize = if (isBoldMode) 1.12.em else 1.05.em,
                                    color = if (isBoldMode) accentColor else baseColor,
                                    textDecoration = if (isBoldMode) TextDecoration.Underline else TextDecoration.None
                                )
                            )
                            append(content)
                            pop()
                            i = end + 1
                            continue
                        }
                    }
                    if (lineText[i] == '_') {
                        val end = lineText.indexOf('_', i + 1)
                        if (end > i + 1) {
                            val content = lineText.substring(i + 1, end)
                            pushStyle(
                                SpanStyle(
                                    fontFamily = regularFont,
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = if (isBoldMode) FontWeight.Bold else FontWeight.Normal,
                                    color = baseColor.copy(alpha = 0.92f)
                                )
                            )
                            append(content)
                            pop()
                            i = end + 1
                            continue
                        }
                    }
                    pushStyle(
                        SpanStyle(
                            fontFamily = regularFont,
                            fontWeight = if (isBoldMode) FontWeight.Bold else FontWeight.Normal,
                            color = baseColor
                        )
                    )
                    append(lineText[i])
                    pop()
                    i++
                }
                if (lineIdx < lines.lastIndex) {
                    append('\n')
                }
                val paraEnd = this.length
                val alignStr = lineAlignments[lineIdx] ?: "START"
                if (alignStr != "START" && paraStart < paraEnd) {
                    addStyle(
                        style = ParagraphStyle(textAlign = mapStringToTextAlign(alignStr)),
                        start = paraStart,
                        end = paraEnd
                    )
                }
            }
        }
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

    val notes = remember(allNotes, selectedTag) {
        if (selectedTag == "ALL" || selectedTag.isBlank()) {
            allNotes
        } else {
            allNotes.filter { note ->
                note.tags.split(",").map { it.trim() }.any { it.equals(selectedTag, ignoreCase = true) }
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

    // Fast Simple Harmonic Oscillator (SHO) phase for floating cards (paused when heavy effects are disabled)
    val disableHeavy = isHeavyEffectsDisabled()
    val shoPhaseState: State<Float> = if (disableHeavy) {
        remember { mutableFloatStateOf(0f) }
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "sticky_sho")
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "sho_phase"
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(rearrangingNoteId) {
                if (rearrangingNoteId != null) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        rearrangingNoteId = null
                    }
                }
            }
    ) {
        val containerWidth = maxWidth
        val containerHeight = maxHeight
        val isWallMode = containerWidth >= 600.dp || containerWidth > containerHeight

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
            // Tablet / Landscape Wall Mode: strictly fit all columns inside containerWidth
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

            val columns = remember(localNotesOrder, columnCount) {
                val cols = List(columnCount) { mutableListOf<Pair<Int, StickyNoteEntity>>() }
                localNotesOrder.forEachIndexed { idx, note ->
                    cols[idx % columnCount].add(idx to note)
                }
                cols
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = horizontalPad,
                        end = horizontalPad,
                        top = 12.dp,
                        bottom = 120.dp
                    ),
                horizontalArrangement = Arrangement.spacedBy(colSpacing)
            ) {
                columns.forEachIndexed { colIndex, colItems ->
                    val columnTopOffset = remember(colIndex) {
                        when (colIndex % 4) {
                            0 -> 4.dp
                            1 -> 28.dp
                            2 -> 14.dp
                            else -> 36.dp
                        }
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = columnTopOffset),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        colItems.forEach { (globalIdx, note) ->
                            val colAlign = if (globalIdx % 2 == 0) Alignment.CenterStart else Alignment.CenterEnd
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = colAlign
                            ) {
                                StickyFloatingCard(
                                    note = note,
                                    index = globalIdx,
                                    cardWidth = cardTargetWidth,
                                    maxPreviewLines = maxPreviewLines,
                                    shoPhaseProvider = { shoPhaseState.value },
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
        } else {
            // Mobile Portrait Mode: Alternating Leftward & Rightward arrangement controlled by cardWidthPercent
            val usableWidth = (containerWidth - 28.dp).coerceAtLeast(220.dp)
            val cardTargetWidth = usableWidth * (cardWidthPercent / 100f)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 14.dp,
                    end = 14.dp,
                    top = 12.dp,
                    bottom = 120.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(localNotesOrder, key = { _, n -> n.id }) { idx, note ->
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
                            shoPhaseProvider = { shoPhaseState.value },
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
                .padding(end = 20.dp, bottom = 104.dp)
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

@Composable
private fun StickyFloatingCard(
    note: StickyNoteEntity,
    index: Int,
    cardWidth: Dp,
    maxPreviewLines: Int,
    shoPhaseProvider: () -> Float,
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
    val borderCol = if (isRearranging) AppAccentColor() else palette.accentBorder.copy(alpha = if (note.colorIndex == 0) 0.32f else 0.65f)
    val textColor = if (isLight) Color(0xFF1E293B) else Color(0xFFF8FAFC)
    val fastMode = isHeavyEffectsDisabled() || Build.VERSION.SDK_INT < Build.VERSION_CODES.P

    val phaseSeed = remember(note.id, index) { (note.id * 1.15f + index * 0.85f) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    val shape = RoundedCornerShape(20.dp)
    val (cleanedBody, envelope) = remember(note.bodyText, note.subtitle) {
        parseNoteBodyAndEnvelope(note.bodyText, note.subtitle)
    }
    val subHeading = envelope.subHeading
    val pinIcon = envelope.pinIcon
    val tableData = envelope.table

    val lineAlignments = remember(note.textAlignment) { parseLineAlignments(note.textAlignment) }
    val attachmentsCount = remember(note.attachmentsJson) {
        StickyAttachmentItem.listFromJson(note.attachmentsJson).size
    }

    val tableRowsToDisplay = remember(tableData, maxPreviewLines) {
        if (tableData == null) 0 else minOf(tableData.rows, maxPreviewLines)
    }
    val remainingTextLines = (maxPreviewLines - tableRowsToDisplay).coerceAtLeast(1)

    // Outer container: stationary for text, handles single-tap (open view) and 3-sec hold (rearrange)
    Box(
        modifier = Modifier
            .width(cardWidth)
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .graphicsLayer {
                if (isRearranging) {
                    translationX = dragOffset.x
                    translationY = dragOffset.y
                    scaleX = 1.03f
                    scaleY = 1.03f
                }
            }
            .pointerInput(note.id, isRearranging) {
                if (!isRearranging) {
                    val touchSlopPx = viewConfiguration.touchSlop
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downPos = down.position
                        var longPressTriggered = false
                        var movedOutOfBounds = false

                        val holdResult = withTimeoutOrNull(3000L) {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    return@withTimeoutOrNull false
                                }
                                val dist = (change.position - downPos).getDistance()
                                if (dist > touchSlopPx) {
                                    movedOutOfBounds = true
                                    return@withTimeoutOrNull false
                                }
                            }
                            false
                        }

                        if (holdResult == null && !movedOutOfBounds) {
                            longPressTriggered = true
                            onLongPressRearrange()
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.none { it.pressed }) break
                                event.changes.forEach { it.consume() }
                            }
                        } else if (!movedOutOfBounds && !longPressTriggered) {
                            onTap()
                        }
                    }
                } else {
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
            }
            .testTag("sticky_card_${note.id}")
    ) {
        // BACKGROUND-ONLY WOBBLE LAYER: Only the background card surface moves/rotates in draw phase (0 recompositions!), keeping foreground text 100% still!
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    if (!isRearranging) {
                        val phase = shoPhaseProvider() + phaseSeed
                        translationX = (sin(phase.toDouble()) * 4.5f).toFloat()
                        translationY = (cos((phase * 1.15f).toDouble()) * 3.0f).toFloat()
                        rotationZ = (sin((phase * 0.85f).toDouble()) * 1.15f).toFloat()
                    }
                }
                .then(
                    if (fastMode) {
                        Modifier
                            .clip(shape)
                            .background(cardBg)
                            .border(if (isRearranging) 2.5.dp else 1.2.dp, borderCol, shape)
                    } else {
                        Modifier
                            .shadow(
                                elevation = if (isRearranging) 12.dp else 4.dp,
                                shape = shape,
                                clip = true
                            )
                            .background(cardBg)
                            .border(if (isRearranging) 2.5.dp else 1.2.dp, borderCol, shape)
                    }
                )
        )

        // FOREGROUND STATIONARY CONTENT LAYER: Text, tables, tags, and realistic Pin/Paperclip badge
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top bar: Title + Synced Pin/Paperclip Badge (no Edit or Delete buttons on landing page)
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
                            .background(if (note.colorIndex == 0) AppAccentColor() else palette.accentBorder)
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
                    // Synced Pin (📌) or Paperclip (📎) badge so card looks pinned to wall or clipped to hard paper
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (isLight) Color.Black.copy(alpha = 0.05f)
                                else Color.White.copy(alpha = 0.08f)
                            )
                            .border(
                                0.8.dp,
                                (if (note.colorIndex == 0) AppAccentColor() else palette.accentBorder).copy(alpha = 0.35f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pinIcon,
                            fontSize = 14.sp,
                            modifier = Modifier.rotate(if (pinIcon == "📌") -12f else 8f)
                        )
                    }
                }
            }

            // Sub-heading right below title and before body text
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

            // Tags chips if present
            val noteTags = remember(note.tags) {
                note.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
            }
            if (noteTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    noteTags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = (if (note.colorIndex == 0) AppAccentColor() else palette.accentBorder).copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "#$tag",
                                color = textColor.copy(alpha = 0.85f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Body Preview with per-line alignment and Bold-in-Bold support
            if (cleanedBody.isNotBlank() && remainingTextLines > 0) {
                val annotated = buildFormattedStickyAnnotatedString(
                    rawText = cleanedBody,
                    isBoldMode = isBoldMode,
                    baseColor = textColor.copy(alpha = 0.90f),
                    accentColor = if (note.colorIndex == 0) AppAccentColor() else palette.accentBorder,
                    lineAlignments = lineAlignments
                )
                Text(
                    text = annotated,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = remainingTextLines,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Borderless Visual Table Preview
            if (tableData != null && tableRowsToDisplay > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                StickyTableCompactPreview(
                    table = tableData,
                    maxRows = tableRowsToDisplay,
                    accentColor = if (note.colorIndex == 0) AppAccentColor() else palette.accentBorder,
                    textColor = textColor
                )
            }

            // Attachment indicator chip on card
            if (attachmentsCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background((if (note.colorIndex == 0) AppAccentColor() else palette.accentBorder).copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = null,
                        tint = if (note.colorIndex == 0) AppAccentColor() else palette.accentBorder,
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
    }
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

    val hasUnsavedChanges = remember(
        title, subHeading, pinIcon, bodyField.text, colorIndex, lineAlignments, tags, tableData, attachments, initialNote
    ) {
        if (initialNote == null) {
            title.isNotBlank() || subHeading.isNotBlank() || bodyField.text.isNotBlank() || tableData != null || attachments.isNotEmpty()
        } else {
            title != initialNote.title ||
                subHeading != initialEnvelope.subHeading ||
                pinIcon != initialEnvelope.pinIcon ||
                bodyField.text != migratedBody ||
                colorIndex != initialNote.colorIndex ||
                serializeLineAlignments(lineAlignments) != initialNote.textAlignment ||
                tags.joinToString(",") != initialNote.tags ||
                tableData?.toJson() != initialEnvelope.table?.toJson() ||
                StickyAttachmentItem.listToJson(attachments) != initialNote.attachmentsJson
        }
    }

    fun persistCurrentDraft(
        overrideAttachments: List<StickyAttachmentItem> = attachments,
        overrideTable: StickyTableData? = tableData,
        overridePinIcon: String = pinIcon,
        exitEditAfterSave: Boolean = false,
        closeDialogAfterSave: Boolean = false
    ) {
        val hasContent = title.isNotBlank() || subHeading.isNotBlank() || bodyField.text.isNotBlank() || overrideTable != null || overrideAttachments.isNotEmpty()
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
            bodyText = bodyField.text,
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
        if (isEditing && hasUnsavedChanges) {
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

    // Helper to wrap selected text in *...* or _..._
    fun applyInlineFormatting(marker: String) {
        val text = bodyField.text
        val sel = bodyField.selection
        val start = sel.min.coerceIn(0, text.length)
        val end = sel.max.coerceIn(0, text.length)
        if (start < end) {
            val selectedSub = text.substring(start, end)
            val replacement = "$marker$selectedSub$marker"
            val newText = text.replaceRange(start, end, replacement)
            bodyField = TextFieldValue(
                text = newText,
                selection = TextRange(start + marker.length, end + marker.length)
            )
        } else {
            val insertion = "$marker$marker"
            val newText = text.substring(0, start) + insertion + text.substring(start)
            bodyField = TextFieldValue(
                text = newText,
                selection = TextRange(start + marker.length)
            )
        }
    }

    // Helper to apply alignment ONLY to the line(s) containing the current cursor/selection
    fun applyAlignmentToSelectedLines(alignMode: String) {
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
    }

    val currentCursorLineAlignment = remember(bodyField.text, bodyField.selection, lineAlignments) {
        val text = bodyField.text
        val cursor = bodyField.selection.min.coerceIn(0, text.length)
        val lineIdx = text.substring(0, cursor).count { it == '\n' }
        lineAlignments[lineIdx] ?: "START"
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
                    .dialogSystemBarsPadding(extraTop = 8.dp)
                    .imePadding(),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0.dp),
                topBar = {
                    Column(modifier = Modifier.fillMaxWidth()) {
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

                            // Center: Note Title in Top Bar (between Back button and Triple-Dot menu on both mobile & tablet!)
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
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
                                            fontFamily = if (isBoldMode) ArchivoBlackFontFamily else AppFontFamily
                                        ),
                                        cursorBrush = SolidColor(effectiveAccent),
                                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                        modifier = Modifier.fillMaxWidth(),
                                        decorationBox = { inner ->
                                            if (title.isEmpty()) {
                                                Text(
                                                    text = if (isBn) "শিরোনাম লিখুন..." else "Title...",
                                                    color = textColor.copy(alpha = 0.42f),
                                                    fontSize = 19.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            inner()
                                        }
                                    )
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = pinIcon,
                                            fontSize = 16.sp,
                                            modifier = Modifier.padding(end = 6.dp)
                                        )
                                        Text(
                                            text = title.ifBlank { if (isBn) "শিরোনামহীন নোট" else "Untitled Note" },
                                            color = textColor,
                                            fontSize = 19.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = if (isBoldMode) ArchivoBlackFontFamily else AppFontFamily,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Right: Save button (when editing) + Triple-Dot (⋮) Menu Button
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
                                    // 3. Show Preview
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
                                            if (isEditing) {
                                                persistCurrentDraft(exitEditAfterSave = true)
                                            }
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

                        // Color Palette Picker Row shown cleanly under top bar ONLY in Edit Mode
                        if (isEditing) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 20.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                                            .size(if (isSelected) 28.dp else 22.dp)
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
                },
                bottomBar = {
                    // Show 8-icon formatting toolbar ONLY in Editing Mode (never in Preview Mode)
                    if (isEditing) {
                        Surface(
                            color = if (isLight) Color.White.copy(alpha = 0.94f) else Color(0xFF1E293B).copy(alpha = 0.96f),
                            tonalElevation = 6.dp,
                            shadowElevation = 8.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier
                                        .widthIn(max = 560.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 1. Bold
                                    IconButton(
                                        onClick = { applyInlineFormatting("*") },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Default.FormatBold, contentDescription = "Bold", tint = textColor)
                                    }
                                    // 2. Italic
                                    IconButton(
                                        onClick = { applyInlineFormatting("_") },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Text(
                                            text = "I",
                                            color = textColor,
                                            fontSize = 18.sp,
                                            fontStyle = FontStyle.Italic,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    // 3. Align Left
                                    IconButton(
                                        onClick = { applyAlignmentToSelectedLines("START") },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (currentCursorLineAlignment == "START") effectiveAccent.copy(alpha = 0.25f) else Color.Transparent)
                                    ) {
                                        Icon(Icons.Default.FormatAlignLeft, contentDescription = "Align Line Left", tint = textColor)
                                    }
                                    // 4. Align Center
                                    IconButton(
                                        onClick = { applyAlignmentToSelectedLines("CENTER") },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (currentCursorLineAlignment == "CENTER") effectiveAccent.copy(alpha = 0.25f) else Color.Transparent)
                                    ) {
                                        Icon(Icons.Default.FormatAlignCenter, contentDescription = "Align Line Center", tint = textColor)
                                    }
                                    // 5. Align Right
                                    IconButton(
                                        onClick = { applyAlignmentToSelectedLines("END") },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (currentCursorLineAlignment == "END") effectiveAccent.copy(alpha = 0.25f) else Color.Transparent)
                                    ) {
                                        Icon(Icons.Default.FormatAlignRight, contentDescription = "Align Line Right", tint = textColor)
                                    }
                                    // 6. Justify
                                    IconButton(
                                        onClick = { applyAlignmentToSelectedLines("JUSTIFY") },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (currentCursorLineAlignment == "JUSTIFY") effectiveAccent.copy(alpha = 0.25f) else Color.Transparent)
                                    ) {
                                        Icon(Icons.Default.FormatAlignJustify, contentDescription = "Justify Line", tint = textColor)
                                    }
                                    // 7. Insert Borderless Visual Grid Table
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
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (tableData != null) effectiveAccent.copy(alpha = 0.22f) else Color.Transparent)
                                    ) {
                                        Icon(Icons.Default.TableChart, contentDescription = "Insert Visual Grid Table", tint = textColor)
                                    }
                                    // 8. Attach File
                                    IconButton(
                                        onClick = { attachmentLauncher.launch("*/*") },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Default.AttachFile, contentDescription = "Attach File", tint = textColor)
                                    }
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
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
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier.fillMaxWidth(),
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
                            onValueChange = { newVal ->
                                bodyField = newVal
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
                                .defaultMinSize(minHeight = if (tableData != null) 140.dp else 260.dp),
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
                        val annotated = buildFormattedStickyAnnotatedString(
                            rawText = bodyField.text.ifBlank { if (tableData == null && attachments.isEmpty() && subHeading.isBlank()) (if (isBn) "কোনো লেখা নেই" else "Empty note") else "" },
                            isBoldMode = isBoldMode,
                            baseColor = textColor,
                            accentColor = effectiveAccent,
                            lineAlignments = lineAlignments
                        )
                        if (annotated.text.isNotBlank()) {
                            Text(
                                text = annotated,
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
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
                                        fontSize = 13.sp
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(
                                            onClick = {
                                                val newRows = table.rows + 1
                                                val newCells = table.cells + listOf(List(table.cols) { "" })
                                                tableData = StickyTableData(newRows, table.cols, newCells)
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("+Row", color = effectiveAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        if (table.rows > 1) {
                                            TextButton(
                                                onClick = {
                                                    val newRows = table.rows - 1
                                                    tableData = StickyTableData(newRows, table.cols, table.cells.take(newRows))
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text("-Row", color = AccentPink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        TextButton(
                                            onClick = {
                                                val newCols = table.cols + 1
                                                val newCells = table.cells.map { it + "" }
                                                tableData = StickyTableData(table.rows, newCols, newCells)
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("+Col", color = effectiveAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        if (table.cols > 1) {
                                            TextButton(
                                                onClick = {
                                                    val newCols = table.cols - 1
                                                    tableData = StickyTableData(table.rows, newCols, table.cells.map { it.take(newCols) })
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text("-Col", color = AccentPink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        IconButton(
                                            onClick = { tableData = null },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Remove Table", tint = AccentPink, modifier = Modifier.size(16.dp))
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

                    Spacer(modifier = Modifier.height(64.dp))
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
