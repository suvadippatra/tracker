package com.example.ui.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.StickyAttachment
import com.example.data.model.StickyAttachmentSerializer
import com.example.data.model.StickyNoteEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinancialViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.cos
import kotlin.math.sin

// Parsed representation of Sticky body lines (text line vs table row)
sealed class StickyBodyElement {
    data class TextLine(val text: String) : StickyBodyElement()
    data class TableBlock(val rows: List<List<String>>) : StickyBodyElement()
}

fun parseStickyBodyElements(bodyText: String): List<StickyBodyElement> {
    if (bodyText.isBlank()) return emptyList()
    val rawLines = bodyText.lines()
    val result = mutableListOf<StickyBodyElement>()
    val currentTableRows = mutableListOf<List<String>>()

    fun flushTable() {
        if (currentTableRows.isNotEmpty()) {
            result.add(StickyBodyElement.TableBlock(currentTableRows.toList()))
            currentTableRows.clear()
        }
    }

    for (line in rawLines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.length >= 3) {
            val cells = trimmed.removePrefix("|").removeSuffix("|").split("|").map { it.trim() }
            // Skip pure markdown separator rows like |---|---|
            val isSeparator = cells.all { cell -> cell.isNotEmpty() && cell.all { it == '-' || it == ':' } }
            if (!isSeparator) {
                currentTableRows.add(cells)
            }
        } else {
            flushTable()
            result.add(StickyBodyElement.TextLine(line))
        }
    }
    flushTable()
    return result
}

/**
 * Formats inline *bold* and -italic- markers into an AnnotatedString.
 */
fun parseStickyRichText(input: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val n = input.length
        while (i < n) {
            val ch = input[i]
            if (ch == '*') {
                val closeIdx = input.indexOf('*', i + 1)
                if (closeIdx > i + 1) {
                    val inner = input.substring(i + 1, closeIdx)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(inner)
                    pop()
                    i = closeIdx + 1
                    continue
                }
            } else if (ch == '-') {
                val prevIsSpaceOrStart = (i == 0 || input[i - 1].isWhitespace())
                val closeIdx = input.indexOf('-', i + 1)
                val nextIsSpaceOrEnd = (closeIdx != -1 && (closeIdx + 1 == n || input[closeIdx + 1].isWhitespace() || input[closeIdx + 1] in ".,!?;:)"))
                if (prevIsSpaceOrStart && closeIdx > i + 1 && nextIsSpaceOrEnd && !input.substring(i + 1, closeIdx).all { it == '-' }) {
                    val inner = input.substring(i + 1, closeIdx)
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(inner)
                    pop()
                    i = closeIdx + 1
                    continue
                }
            }
            append(ch)
            i++
        }
    }
}

fun resolveTextAlign(alignmentKey: String): TextAlign {
    return when (alignmentKey.uppercase()) {
        "CENTER" -> TextAlign.Center
        "END", "RIGHT" -> TextAlign.End
        "JUSTIFY" -> TextAlign.Justify
        else -> TextAlign.Start
    }
}

@Composable
fun StickyNotesTab(viewModel: FinancialViewModel) {
    val stickyNotes by viewModel.allStickyNotes.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isBn = userProfile.language == "bn"
    val widthPercent = userProfile.stickyCardWidthPercent.coerceIn(50, 80)
    val maxPreviewLines = userProfile.stickyPreviewLines.coerceIn(4, 10)
    val heavyEffectsDisabled = isHeavyEffectsDisabled()
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current

    var selectedNoteForViewer by remember { mutableStateOf<StickyNoteEntity?>(null) }
    var openInEditMode by remember { mutableStateOf(false) }
    var isCreatingNewNote by remember { mutableStateOf(false) }
    var reorderingNoteId by remember { mutableStateOf<Int?>(null) }

    // Keep selectedNoteForViewer synced with latest DB updates for existing saved notes only
    LaunchedEffect(stickyNotes, selectedNoteForViewer?.id) {
        val currentId = selectedNoteForViewer?.id ?: return@LaunchedEffect
        // If it's a new draft being written (id == 0 or isCreatingNewNote), never close it
        if (currentId <= 0 || isCreatingNewNote) return@LaunchedEffect
        val updated = stickyNotes.find { it.id == currentId }
        if (updated != null) {
            selectedNoteForViewer = updated
        }
    }

    // Simple Harmonic Oscillator phase driver (single lightweight float for all cards)
    val infiniteTransition = rememberInfiniteTransition(label = "sticky_sho")
    val shoPhase by if (heavyEffectsDisabled) {
        remember { mutableFloatStateOf(0f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "sho_phase"
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("sticky_notes_tab")
    ) {
        val containerWidth = maxWidth
        val containerHeight = maxHeight
        // On tablet, foldable, or mobile landscape where horizontal width >= vertical height of cards (or width >= 540.dp),
        // arrange cards in the whole space like sticky notes on a wall (horizontally & vertically scrollable).
        val estimatedCardHeight = 220.dp
        val isWallMode = containerWidth >= containerHeight || containerWidth >= 560.dp || containerWidth >= (estimatedCardHeight * 2.1f)

        Column(modifier = Modifier.fillMaxSize()) {
            // Reorder mode banner when activated via 3-second long press
            AnimatedVisibility(
                visible = reorderingNoteId != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = AppAccentColor().copy(alpha = 0.16f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.DragIndicator, contentDescription = null, tint = AppAccentColor(), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBn) "পুনর্বিন্যাস মোড সক্রিয়: স্থানান্তর করতে তীর বা অন্য কার্ডে ট্যাপ করুন" else "Rearrange Mode Active: Tap another card to swap or use arrows",
                                color = AppTextColor(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        TextButton(onClick = { reorderingNoteId = null }) {
                            Text(if (isBn) "সম্পন্ন" else "Done", color = AppAccentColor(), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (stickyNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(76.dp),
                            shape = RoundedCornerShape(22.dp),
                            color = AppAccentColor().copy(alpha = 0.12f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.StickyNote2,
                                    contentDescription = null,
                                    tint = AppAccentColor(),
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isBn) "কোনো স্টিকি নোট নেই" else "No Sticky Notes Yet",
                            color = AppTextColor(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBn) "নতুন ফ্লোটিং নোট, টেবিল বা ফাইল যোগ করতে নিচের + বাটনে ট্যাপ করুন।" else "Tap the + button below to create floating notes, formatted tables, and attachments.",
                            color = AppSecondaryTextColor(),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(max = 320.dp)
                        )
                    }
                }
            } else if (isWallMode) {
                // Wall Layout for Tablet / Foldable / Landscape:
                // Horizontally and vertically scrollable wall where cards are arranged across rows and columns like sticky notes on a wall
                val wallCardWidth = (containerWidth * (widthPercent / 100f) * 0.46f).coerceIn(220.dp, 340.dp)
                val columnsPerRow = ((containerWidth.value / 240f).toInt().coerceAtLeast(3)).coerceAtMost(5)
                val rows = remember(stickyNotes, columnsPerRow) {
                    stickyNotes.chunked(columnsPerRow)
                }
                val hScroll = rememberScrollState()
                val vScroll = rememberScrollState()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(vScroll)
                        .horizontalScroll(hScroll)
                        .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 110.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        rows.forEachIndexed { rowIndex, rowNotes ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                rowNotes.forEachIndexed { colIndex, note ->
                                    val flatIndex = rowIndex * columnsPerRow + colIndex
                                    StickyFloatingCard(
                                        note = note,
                                        index = flatIndex,
                                        totalCount = stickyNotes.size,
                                        shoPhase = shoPhase,
                                        heavyEffectsDisabled = heavyEffectsDisabled,
                                        maxPreviewLines = maxPreviewLines,
                                        isReorderingThis = reorderingNoteId == note.id,
                                        isAnyReordering = reorderingNoteId != null,
                                        modifier = Modifier.width(wallCardWidth),
                                        onClick = {
                                            val activeReorderId = reorderingNoteId
                                            if (activeReorderId != null) {
                                                if (activeReorderId == note.id) {
                                                    reorderingNoteId = null
                                                } else {
                                                    val mutable = stickyNotes.toMutableList()
                                                    val fromIdx = mutable.indexOfFirst { it.id == activeReorderId }
                                                    val toIdx = mutable.indexOfFirst { it.id == note.id }
                                                    if (fromIdx != -1 && toIdx != -1) {
                                                        val moved = mutable.removeAt(fromIdx)
                                                        mutable.add(toIdx, moved)
                                                        viewModel.reorderStickyNotes(mutable)
                                                    }
                                                    reorderingNoteId = null
                                                }
                                            } else {
                                                openInEditMode = false
                                                selectedNoteForViewer = note
                                            }
                                        },
                                        onThreeSecondLongPress = {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            reorderingNoteId = if (reorderingNoteId == note.id) null else note.id
                                            Toast.makeText(
                                                context,
                                                if (isBn) "পুনর্বিন্যাস মোড সক্রিয়" else "Rearrange mode enabled",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        onMovePrevious = {
                                            if (flatIndex > 0) {
                                                val mutable = stickyNotes.toMutableList()
                                                val item = mutable.removeAt(flatIndex)
                                                mutable.add(flatIndex - 1, item)
                                                viewModel.reorderStickyNotes(mutable)
                                            }
                                        },
                                        onMoveNext = {
                                            if (flatIndex < stickyNotes.size - 1) {
                                                val mutable = stickyNotes.toMutableList()
                                                val item = mutable.removeAt(flatIndex)
                                                mutable.add(flatIndex + 1, item)
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
                // Portrait Mobile Layout:
                // Vertically scrollable column where each card has fixed width = widthPercent (50%..80%) of screen width
                val cardWidthFraction = (widthPercent / 100f).coerceIn(0.5f, 0.8f)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 110.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    itemsIndexed(stickyNotes, key = { _, item -> item.id }) { index, note ->
                        StickyFloatingCard(
                            note = note,
                            index = index,
                            totalCount = stickyNotes.size,
                            shoPhase = shoPhase,
                            heavyEffectsDisabled = heavyEffectsDisabled,
                            maxPreviewLines = maxPreviewLines,
                            isReorderingThis = reorderingNoteId == note.id,
                            isAnyReordering = reorderingNoteId != null,
                            modifier = Modifier.fillMaxWidth(cardWidthFraction),
                            onClick = {
                                val activeReorderId = reorderingNoteId
                                if (activeReorderId != null) {
                                    if (activeReorderId == note.id) {
                                        reorderingNoteId = null
                                    } else {
                                        val mutable = stickyNotes.toMutableList()
                                        val fromIdx = mutable.indexOfFirst { it.id == activeReorderId }
                                        val toIdx = mutable.indexOfFirst { it.id == note.id }
                                        if (fromIdx != -1 && toIdx != -1) {
                                            val moved = mutable.removeAt(fromIdx)
                                            mutable.add(toIdx, moved)
                                            viewModel.reorderStickyNotes(mutable)
                                        }
                                        reorderingNoteId = null
                                    }
                                } else {
                                    openInEditMode = false
                                    selectedNoteForViewer = note
                                }
                            },
                            onThreeSecondLongPress = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                reorderingNoteId = if (reorderingNoteId == note.id) null else note.id
                                Toast.makeText(
                                    context,
                                    if (isBn) "পুনর্বিন্যাস মোড সক্রিয়" else "Rearrange mode enabled",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onMovePrevious = {
                                if (index > 0) {
                                    val mutable = stickyNotes.toMutableList()
                                    val item = mutable.removeAt(index)
                                    mutable.add(index - 1, item)
                                    viewModel.reorderStickyNotes(mutable)
                                }
                            },
                            onMoveNext = {
                                if (index < stickyNotes.size - 1) {
                                    val mutable = stickyNotes.toMutableList()
                                    val item = mutable.removeAt(index)
                                    mutable.add(index + 1, item)
                                    viewModel.reorderStickyNotes(mutable)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Button to create a new Sticky Note
        FloatingActionButton(
            onClick = {
                isCreatingNewNote = true
                openInEditMode = true
                selectedNoteForViewer = StickyNoteEntity()
            },
            containerColor = AppAccentColor(),
            contentColor = AppOnAccentColor(),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 88.dp)
                .testTag("add_sticky_note_fab")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Sticky Note", tint = AppOnAccentColor())
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isBn) "নতুন স্টিকি" else "New Sticky",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = AppOnAccentColor()
                )
            }
        }
    }

    // Full-Screen Sticky Note Viewer / Editor Dialog
    if (selectedNoteForViewer != null) {
        StickyNoteFullScreenDialog(
            note = selectedNoteForViewer!!,
            initialEditMode = openInEditMode || isCreatingNewNote,
            isNewNote = isCreatingNewNote,
            isBn = isBn,
            onDismiss = {
                selectedNoteForViewer = null
                isCreatingNewNote = false
                openInEditMode = false
            },
            onSave = { updatedNote ->
                viewModel.saveStickyNote(updatedNote) { savedId ->
                    selectedNoteForViewer = updatedNote.copy(id = savedId)
                    isCreatingNewNote = false
                    openInEditMode = false
                }
            },
            onMoveToTrash = { noteToDelete ->
                if (noteToDelete.id != 0) {
                    viewModel.softDeleteStickyNote(noteToDelete)
                    Toast.makeText(
                        context,
                        if (isBn) "স্টিকি নোট রিসাইকেল বিনে সরানো হয়েছে" else "Sticky note moved to Recycle Bin",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                selectedNoteForViewer = null
                isCreatingNewNote = false
                openInEditMode = false
            }
        )
    }
}

@Composable
private fun StickyFloatingCard(
    note: StickyNoteEntity,
    index: Int,
    totalCount: Int,
    shoPhase: Float,
    heavyEffectsDisabled: Boolean,
    maxPreviewLines: Int,
    isReorderingThis: Boolean,
    isAnyReordering: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onThreeSecondLongPress: () -> Unit,
    onMovePrevious: () -> Unit,
    onMoveNext: () -> Unit
) {
    val isLight = isAppLightMode()
    val cardBg = if (isLight) M3SurfaceContainerLight else M3SurfaceContainerDark
    val accent = AppAccentColor()
    val shape = RoundedCornerShape(22.dp)
    val phaseOffset = remember(note.id, index) { (index * 1.15f) + (note.id * 0.45f) }

    val hasTitle = note.title.isNotBlank()
    val hasSubtitle = note.subtitle.isNotBlank()
    val showHeaderDivider = hasTitle || hasSubtitle
    val textAlign = resolveTextAlign(note.textAlignment)

    val parsedElements = remember(note.bodyText) { parseStickyBodyElements(note.bodyText) }

    Box(
        modifier = modifier
            .graphicsLayer {
                if (!heavyEffectsDisabled) {
                    val angle = shoPhase + phaseOffset
                    translationY = sin(angle) * 5.5f
                    translationX = cos(angle * 0.5f) * 2.5f
                }
                if (isReorderingThis) {
                    scaleX = 1.03f
                    scaleY = 1.03f
                }
            }
            .shadow(
                elevation = if (isReorderingThis) 12.dp else if (isLight) 5.dp else 8.dp,
                shape = shape,
                clip = true,
                ambientColor = if (isLight) Color.Black.copy(alpha = 0.25f) else Color.Black,
                spotColor = if (isLight) Color.Black.copy(alpha = 0.30f) else Color.Black
            )
            .background(cardBg, shape)
            .then(
                if (isReorderingThis) {
                    Modifier.border(2.dp, accent, shape)
                } else if (isAnyReordering) {
                    Modifier.border(1.dp, accent.copy(alpha = 0.45f), shape)
                } else {
                    Modifier
                }
            )
            .pointerInput(note.id, isAnyReordering) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var longPressTriggered = false
                    val upOrCancel = withTimeoutOrNull(3000L) {
                        waitForUpOrCancellation()
                    }
                    if (upOrCancel == null) {
                        // Held for 3 full seconds!
                        longPressTriggered = true
                        onThreeSecondLongPress()
                        waitForUpOrCancellation()
                    } else if (!longPressTriggered) {
                        val upPos = upOrCancel.position
                        if ((upPos - down.position).getDistance() < viewConfiguration.touchSlop * 1.5f) {
                            onClick()
                        }
                    }
                }
            }
            .padding(16.dp)
            .testTag("sticky_card_${note.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (isReorderingThis) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#${index + 1}",
                        color = accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalIconButton(
                            onClick = onMovePrevious,
                            enabled = index > 0,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Earlier", modifier = Modifier.size(18.dp))
                        }
                        FilledTonalIconButton(
                            onClick = onMoveNext,
                            enabled = index < totalCount - 1,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Later", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            if (hasTitle) {
                Text(
                    text = parseStickyRichText(note.title),
                    color = AppTextColor(),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (hasSubtitle) {
                if (hasTitle) Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = parseStickyRichText(note.subtitle),
                    color = AppSecondaryTextColor(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (showHeaderDivider) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = getGlassBorder().copy(alpha = 0.65f)
                )
            }

            // Render body lines & table rows up to maxPreviewLines (counting both normal lines and table rows)
            var remainingLines = maxPreviewLines
            for (element in parsedElements) {
                if (remainingLines <= 0) break
                when (element) {
                    is StickyBodyElement.TextLine -> {
                        val lineText = element.text
                        Text(
                            text = parseStickyRichText(lineText.ifEmpty { " " }),
                            color = AppTextColor().copy(alpha = 0.9f),
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp,
                            textAlign = textAlign,
                            maxLines = remainingLines,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                        // Estimate consumed lines (at least 1 line per paragraph)
                        val estimatedUsed = ((lineText.length / 38) + 1).coerceAtLeast(1)
                        remainingLines -= estimatedUsed
                    }
                    is StickyBodyElement.TableBlock -> {
                        val visibleRows = element.rows.take(remainingLines)
                        if (visibleRows.isNotEmpty()) {
                            StickyTableRenderedView(
                                rows = visibleRows,
                                isCompactPreview = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )
                            remainingLines -= visibleRows.size
                        }
                    }
                }
            }

            if (note.bodyText.isBlank() && !hasTitle && !hasSubtitle) {
                Text(
                    text = "Empty sticky note",
                    color = AppSecondaryTextColor().copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

@Composable
fun StickyTableRenderedView(
    rows: List<List<String>>,
    isCompactPreview: Boolean,
    modifier: Modifier = Modifier
) {
    if (rows.isEmpty()) return
    val colCount = rows.maxOfOrNull { it.size } ?: 1
    val borderColor = getGlassBorder()
    val headerBg = AppAccentColor().copy(alpha = 0.12f)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
    ) {
        rows.forEachIndexed { rowIdx, row ->
            val isHeader = rowIdx == 0 && rows.size > 1
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isHeader) headerBg else Color.Transparent)
                    .padding(horizontal = 8.dp, vertical = if (isCompactPreview) 5.dp else 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (c in 0 until colCount) {
                    val cellText = row.getOrElse(c) { "" }
                    Text(
                        text = parseStickyRichText(cellText),
                        color = AppTextColor(),
                        fontSize = if (isCompactPreview) 12.sp else 13.5.sp,
                        fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                        maxLines = if (isCompactPreview) 1 else 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    )
                }
            }
            if (rowIdx < rows.size - 1) {
                HorizontalDivider(color = borderColor.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun StickyNoteFullScreenDialog(
    note: StickyNoteEntity,
    initialEditMode: Boolean,
    isNewNote: Boolean,
    isBn: Boolean,
    onDismiss: () -> Unit,
    onSave: (StickyNoteEntity) -> Unit,
    onMoveToTrash: (StickyNoteEntity) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600 || configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    var isEditing by remember { mutableStateOf(initialEditMode) }
    var title by remember(note.id) { mutableStateOf(note.title) }
    var subtitle by remember(note.id) { mutableStateOf(note.subtitle) }
    var bodyText by remember(note.id) { mutableStateOf(note.bodyText) }
    var textAlignment by remember(note.id) { mutableStateOf(note.textAlignment) }
    var attachments by remember(note.id) {
        mutableStateOf(StickyAttachmentSerializer.deserialize(note.attachmentsJson))
    }

    var showMoreMenu by remember { mutableStateOf(false) }
    var showAttachTypeSheet by remember { mutableStateOf(false) }
    var showTableBuilder by remember { mutableStateOf(false) }
    var activeAttachmentMenu by remember { mutableStateOf<StickyAttachment?>(null) }
    var renamingAttachment by remember { mutableStateOf<StickyAttachment?>(null) }
    var viewingImageAttachment by remember { mutableStateOf<StickyAttachment?>(null) }
    var viewingPdfAttachment by remember { mutableStateOf<StickyAttachment?>(null) }
    var isProcessingAttachment by remember { mutableStateOf(false) }

    fun persistAttachments(updatedList: List<StickyAttachment>) {
        attachments = updatedList
        val updatedEntity = note.copy(
            title = title.trim(),
            subtitle = subtitle.trim(),
            bodyText = bodyText,
            textAlignment = textAlignment,
            attachmentsJson = StickyAttachmentSerializer.serialize(updatedList)
        )
        onSave(updatedEntity)
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isProcessingAttachment = true
                val result = processStickyImageAttachment(context, uri)
                isProcessingAttachment = false
                result.onSuccess { attachment ->
                    persistAttachments(attachments + attachment)
                    Toast.makeText(
                        context,
                        if (isBn) "ছবি সংযুক্ত করা হয়েছে" else "Image attached",
                        Toast.LENGTH_SHORT
                    ).show()
                }.onFailure { err ->
                    Toast.makeText(context, err.message ?: "Failed to attach image", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isProcessingAttachment = true
                val result = processStickyPdfAttachment(context, uri)
                isProcessingAttachment = false
                result.onSuccess { attachment ->
                    persistAttachments(attachments + attachment)
                    Toast.makeText(
                        context,
                        if (isBn) "পিডিএফ সংযুক্ত করা হয়েছে" else "PDF attached",
                        Toast.LENGTH_SHORT
                    ).show()
                }.onFailure { err ->
                    Toast.makeText(context, err.message ?: "Failed to attach PDF", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (isEditing && (title.isNotBlank() || subtitle.isNotBlank() || bodyText.isNotBlank() || attachments.isNotEmpty())) {
                onSave(
                    note.copy(
                        title = title.trim(),
                        subtitle = subtitle.trim(),
                        bodyText = bodyText,
                        textAlignment = textAlignment,
                        attachmentsJson = StickyAttachmentSerializer.serialize(attachments)
                    )
                )
            }
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        DialogEdgeToEdge()
        BackHandler {
            if (isEditing && !isNewNote) {
                onSave(
                    note.copy(
                        title = title.trim(),
                        subtitle = subtitle.trim(),
                        bodyText = bodyText,
                        textAlignment = textAlignment,
                        attachmentsJson = StickyAttachmentSerializer.serialize(attachments)
                    )
                )
                isEditing = false
            } else {
                onDismiss()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(getAppBackgroundGradient(LocalAppTheme.current))
                .dialogSystemBarsPadding(extraTop = 8.dp)
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 860.dp)
                    .fillMaxWidth()
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (isEditing && !isNewNote) {
                                onSave(
                                    note.copy(
                                        title = title.trim(),
                                        subtitle = subtitle.trim(),
                                        bodyText = bodyText,
                                        textAlignment = textAlignment,
                                        attachmentsJson = StickyAttachmentSerializer.serialize(attachments)
                                    )
                                )
                                isEditing = false
                            } else {
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .background(getGlassIconButtonBg(), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Back",
                            tint = AppTextColor(),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Text(
                        text = if (isEditing) {
                            if (isNewNote) (if (isBn) "নতুন স্টিকি নোট" else "New Sticky Note")
                            else (if (isBn) "স্টিকি নোট সম্পাদনা" else "Edit Sticky Note")
                        } else {
                            if (isBn) "স্টিকি নোট" else "Sticky Note"
                        },
                        color = AppTextColor(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isEditing) {
                            Button(
                                onClick = {
                                    onSave(
                                        note.copy(
                                            title = title.trim(),
                                            subtitle = subtitle.trim(),
                                            bodyText = bodyText,
                                            textAlignment = textAlignment,
                                            attachmentsJson = StickyAttachmentSerializer.serialize(attachments)
                                        )
                                    )
                                    isEditing = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppAccentColor(),
                                    contentColor = AppOnAccentColor()
                                ),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = AppOnAccentColor(), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isBn) "সেভ" else "Save", color = AppOnAccentColor(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Box {
                            IconButton(
                                onClick = { showMoreMenu = true },
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(getGlassIconButtonBg(), CircleShape)
                                    .testTag("sticky_more_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More Options",
                                    tint = AppTextColor()
                                )
                            }

                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false },
                                offset = DpOffset(x = (-8).dp, y = 4.dp),
                                containerColor = getDialogBackground()
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (isEditing) (if (isBn) "প্রিভিউ মোড" else "Preview Mode")
                                            else (if (isBn) "সম্পাদনা করুন (Edit)" else "Edit"),
                                            color = AppTextColor(),
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        isEditing = !isEditing
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (isBn) "ফাইল সংযুক্ত করুন (Attach File)" else "Attach File",
                                            color = AppTextColor(),
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        showAttachTypeSheet = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (isBn) "রিসাইকেল বিনে সরান (Remove)" else "Remove",
                                            color = AccentPink,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        onMoveToTrash(note)
                                    }
                                )
                            }
                        }
                    }
                }

                if (isProcessingAttachment) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        color = AppAccentColor()
                    )
                }

                // Scrollable Content Area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (isEditing) {
                        // Formatting & Alignment Toolbar
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Bold syntax helper
                                    AssistChip(
                                        onClick = { bodyText = if (bodyText.isEmpty()) "*bold*" else "$bodyText *bold*" },
                                        label = { Text("*Bold*", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                                    )
                                    // Italic syntax helper
                                    AssistChip(
                                        onClick = { bodyText = if (bodyText.isEmpty()) "-italic-" else "$bodyText -italic-" },
                                        label = { Text("-Italic-", fontStyle = FontStyle.Italic, fontSize = 12.sp) }
                                    )
                                    // Interactive Table Builder
                                    AssistChip(
                                        onClick = { showTableBuilder = true },
                                        leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        label = { Text(if (isBn) "+ টেবিল" else "+ Table", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                                    )
                                    // Attach File helper
                                    AssistChip(
                                        onClick = { showAttachTypeSheet = true },
                                        leadingIcon = { Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        label = { Text(if (isBn) "ফাইল" else "Attach", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                                    )
                                }

                                // Paragraph Alignment Selector: Left, Center, Right, Justify
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        Triple("START", if (isBn) "বাম" else "Left", Icons.Default.FormatAlignLeft),
                                        Triple("CENTER", if (isBn) "মাঝে" else "Center", Icons.Default.FormatAlignCenter),
                                        Triple("END", if (isBn) "ডান" else "Right", Icons.Default.FormatAlignRight),
                                        Triple("JUSTIFY", if (isBn) "জাস্টিফাই" else "Justify", Icons.Default.FormatAlignJustify)
                                    ).forEach { (alignKey, label, icon) ->
                                        val selected = textAlignment.equals(alignKey, ignoreCase = true)
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { textAlignment = alignKey },
                                            color = if (selected) AppAccentColor().copy(alpha = 0.18f) else getGlassIconButtonBg(),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxSize(),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = label,
                                                    tint = if (selected) AppAccentColor() else AppTextColor().copy(alpha = 0.7f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = label,
                                                    color = if (selected) AppAccentColor() else AppTextColor().copy(alpha = 0.8f),
                                                    fontSize = 11.5.sp,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Always show Title, Subtitle, and Body fields in Edit mode
                        GlassTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text(if (isBn) "শিরোনাম (Title - ঐচ্ছিক)" else "Title (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        GlassTextField(
                            value = subtitle,
                            onValueChange = { subtitle = it },
                            label = { Text(if (isBn) "উপশিরোনাম (Subtitle - ঐচ্ছিক)" else "Subtitle (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        GlassTextField(
                            value = bodyText,
                            onValueChange = { bodyText = it },
                            label = {
                                Text(
                                    if (isBn) "নোটের বিস্তারিত (*bold*, -italic-, অথবা | A | B | টেবিল)"
                                    else "Note Body (*bold*, -italic-, or | Col 1 | Col 2 | table)"
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 220.dp),
                            singleLine = false
                        )
                    } else {
                        // Viewer Mode
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                val hasTitle = title.isNotBlank()
                                val hasSubtitle = subtitle.isNotBlank()
                                val showDivider = hasTitle || hasSubtitle
                                val align = resolveTextAlign(textAlignment)

                                if (hasTitle) {
                                    Text(
                                        text = parseStickyRichText(title),
                                        color = AppTextColor(),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (hasSubtitle) {
                                    if (hasTitle) Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = parseStickyRichText(subtitle),
                                        color = AppSecondaryTextColor(),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                if (showDivider) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 14.dp),
                                        color = getGlassBorder()
                                    )
                                }

                                val elements = remember(bodyText) { parseStickyBodyElements(bodyText) }
                                if (elements.isEmpty()) {
                                    Text(
                                        text = if (isBn) "কোনো লেখা নেই। সম্পাদনা করতে উপরে ⋮ মেনু থেকে Edit নির্বাচন করুন।" else "No text content. Tap Edit from the top-right menu to write.",
                                        color = AppSecondaryTextColor(),
                                        fontSize = 14.sp,
                                        fontStyle = FontStyle.Italic
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        elements.forEach { el ->
                                            when (el) {
                                                is StickyBodyElement.TextLine -> {
                                                    Text(
                                                        text = parseStickyRichText(el.text.ifEmpty { " " }),
                                                        color = AppTextColor(),
                                                        fontSize = 15.sp,
                                                        lineHeight = 22.sp,
                                                        textAlign = align,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                                is StickyBodyElement.TableBlock -> {
                                                    StickyTableRenderedView(
                                                        rows = el.rows,
                                                        isCompactPreview = false,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Attachments Section at the bottom of the full-screen Sticky page
                    if (attachments.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (isBn) "সংযুক্ত ফাইলসমূহ (${attachments.size})" else "Attachments (${attachments.size})",
                                color = AppTextColor(),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val cols = if (isTablet) 3 else 2
                            attachments.chunked(cols).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowItems.forEach { att ->
                                        StickyAttachmentBox(
                                            attachment = att,
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                if (att.type == "IMAGE") {
                                                    viewingImageAttachment = att
                                                } else {
                                                    viewingPdfAttachment = att
                                                }
                                            },
                                            onLongPress = {
                                                activeAttachmentMenu = att
                                            }
                                        )
                                    }
                                    repeat(cols - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Attach File Type Picker Dialog (Image <=1MB auto-compressed up to 10MB, PDF <=1MB)
    if (showAttachTypeSheet) {
        AlertDialog(
            onDismissRequest = { showAttachTypeSheet = false },
            containerColor = getDialogBackground(),
            title = {
                Text(
                    if (isBn) "ফাইল সংযুক্ত করুন" else "Attach File to Sticky Note",
                    color = AppTextColor(),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isBn) {
                            "• ছবি: ১ MB-এর কম হলে সরাসরি সেভ হবে; ১–১০ MB হলে স্বয়ংক্রিয়ভাবে ১ MB-এর নিচে কমপ্রেস হবে।\n• পিডিএফ (PDF): সর্বোচ্চ ১ MB।"
                        } else {
                            "• Images: Stored directly if ≤ 1 MB; auto-compressed below 1 MB if between 1 MB and 10 MB (max 10 MB).\n• PDF Documents: Maximum 1 MB."
                        },
                        color = AppSecondaryTextColor(),
                        fontSize = 12.5.sp
                    )
                    Button(
                        onClick = {
                            showAttachTypeSheet = false
                            imagePickerLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AppAccentColor()),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isBn) "ছবি নির্বাচন করুন (Image)" else "Attach Image (Up to 10 MB)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            showAttachTypeSheet = false
                            pdfPickerLauncher.launch("application/pdf")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = AppAccentColor())
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isBn) "পিডিএফ নির্বাচন করুন (Max 1 MB)" else "Attach PDF (Max 1 MB)", color = AppTextColor(), fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAttachTypeSheet = false }) {
                    Text(if (isBn) "বাতিল" else "Cancel", color = AppSecondaryTextColor())
                }
            }
        )
    }

    // Interactive Table Grid Builder Dialog
    if (showTableBuilder) {
        StickyTableBuilderDialog(
            isBn = isBn,
            onDismiss = { showTableBuilder = false },
            onInsertTable = { tableMarkdown ->
                bodyText = if (bodyText.isBlank()) tableMarkdown else "$bodyText\n$tableMarkdown"
                showTableBuilder = false
            }
        )
    }

    // Long-press Attachment Action Dialog (Rename, Remove, Download, Share)
    if (activeAttachmentMenu != null) {
        val att = activeAttachmentMenu!!
        AlertDialog(
            onDismissRequest = { activeAttachmentMenu = null },
            containerColor = getDialogBackground(),
            title = {
                Text(
                    text = att.name,
                    color = AppTextColor(),
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            activeAttachmentMenu = null
                            renamingAttachment = att
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = AppAccentColor())
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(if (isBn) "নাম পরিবর্তন করুন (Rename)" else "Rename", color = AppTextColor(), modifier = Modifier.weight(1f))
                    }
                    TextButton(
                        onClick = {
                            activeAttachmentMenu = null
                            downloadStickyAttachmentToDevice(context, att)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = AppAccentColor())
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(if (isBn) "ডাউনলোড করুন (Download)" else "Download", color = AppTextColor(), modifier = Modifier.weight(1f))
                    }
                    TextButton(
                        onClick = {
                            activeAttachmentMenu = null
                            shareStickyAttachment(context, att)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = AppAccentColor())
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(if (isBn) "শেয়ার করুন (Share)" else "Share", color = AppTextColor(), modifier = Modifier.weight(1f))
                    }
                    TextButton(
                        onClick = {
                            activeAttachmentMenu = null
                            deleteInternalAttachmentFile(att.uri)
                            persistAttachments(attachments.filterNot { it.id == att.id })
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = AccentPink)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(if (isBn) "মুছে ফেলুন (Remove)" else "Remove", color = AccentPink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeAttachmentMenu = null }) {
                    Text(if (isBn) "বন্ধ করুন" else "Close", color = AppSecondaryTextColor())
                }
            }
        )
    }

    // Rename Attachment Dialog
    if (renamingAttachment != null) {
        val target = renamingAttachment!!
        var newName by remember(target.id) { mutableStateOf(target.name) }
        AlertDialog(
            onDismissRequest = { renamingAttachment = null },
            containerColor = getDialogBackground(),
            title = { Text(if (isBn) "ফাইলের নাম পরিবর্তন" else "Rename Attachment", color = AppTextColor(), fontWeight = FontWeight.Bold) },
            text = {
                GlassTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(if (isBn) "নতুন নাম" else "File Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newName.trim().ifBlank { target.name }
                        val updated = attachments.map {
                            if (it.id == target.id) it.copy(name = trimmed) else it
                        }
                        persistAttachments(updated)
                        renamingAttachment = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppAccentColor())
                ) {
                    Text(if (isBn) "সেভ" else "Save", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingAttachment = null }) {
                    Text(if (isBn) "বাতিল" else "Cancel", color = AppSecondaryTextColor())
                }
            }
        )
    }

    // In-App Image Attachment Viewer
    if (viewingImageAttachment != null) {
        val imgAtt = viewingImageAttachment!!
        Dialog(
            onDismissRequest = { viewingImageAttachment = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            DialogEdgeToEdge()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
                    .dialogSystemBarsPadding()
            ) {
                AsyncImage(
                    model = Uri.parse(imgAtt.uri),
                    contentDescription = imgAtt.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewingImageAttachment = null },
                        modifier = Modifier.background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Close", tint = Color.White)
                    }
                    Text(
                        text = imgAtt.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    )
                    IconButton(
                        onClick = { shareStickyAttachment(context, imgAtt) },
                        modifier = Modifier.background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                    }
                }
            }
        }
    }

    // Built-in PDF Viewer Dialog (with external open option)
    if (viewingPdfAttachment != null) {
        StickyPdfViewerDialog(
            attachment = viewingPdfAttachment!!,
            onDismiss = { viewingPdfAttachment = null },
            onShare = { shareStickyAttachment(context, viewingPdfAttachment!!) }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StickyAttachmentBox(
    attachment: StickyAttachment,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val isLight = isAppLightMode()
    val boxBg = if (isLight) M3SurfaceContainerHighLight else M3SurfaceContainerHighDark
    val shape = RoundedCornerShape(14.dp)
    val sizeKb = (attachment.sizeBytes / 1024L).coerceAtLeast(1L)

    Row(
        modifier = modifier
            .height(66.dp)
            .shadow(elevation = if (isLight) 2.dp else 4.dp, shape = shape, clip = true)
            .background(boxBg, shape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (attachment.type == "IMAGE") {
            AsyncImage(
                model = Uri.parse(attachment.uri),
                contentDescription = attachment.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppAccentColor().copy(alpha = 0.12f))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AccentPink.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = "PDF",
                    tint = AccentPink,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = attachment.name,
                color = AppTextColor(),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                lineHeight = 15.sp,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${sizeKb} KB",
                color = AppSecondaryTextColor(),
                fontSize = 10.5.sp
            )
        }
    }
}

@Composable
private fun StickyTableBuilderDialog(
    isBn: Boolean,
    onDismiss: () -> Unit,
    onInsertTable: (String) -> Unit
) {
    var rowsCount by remember { mutableIntStateOf(3) }
    var colsCount by remember { mutableIntStateOf(2) }
    val cellValues = remember { mutableStateMapOf<Pair<Int, Int>, String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = getDialogBackground(),
        title = {
            Text(
                if (isBn) "টেবিল তৈরি করুন" else "Interactive Table Builder",
                color = AppTextColor(),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isBn) "সারি (Rows): $rowsCount" else "Rows: $rowsCount", color = AppTextColor(), fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalIconButton(onClick = { if (rowsCount > 1) rowsCount-- }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease Rows", modifier = Modifier.size(16.dp))
                        }
                        FilledTonalIconButton(onClick = { if (rowsCount < 10) rowsCount++ }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Add, contentDescription = "Increase Rows", modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isBn) "কলাম (Columns): $colsCount" else "Columns: $colsCount", color = AppTextColor(), fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalIconButton(onClick = { if (colsCount > 1) colsCount-- }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease Columns", modifier = Modifier.size(16.dp))
                        }
                        FilledTonalIconButton(onClick = { if (colsCount < 5) colsCount++ }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Add, contentDescription = "Increase Columns", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                HorizontalDivider(color = getGlassBorder())

                for (r in 0 until rowsCount) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (c in 0 until colsCount) {
                            val key = r to c
                            OutlinedTextField(
                                value = cellValues[key] ?: "",
                                onValueChange = { cellValues[key] = it },
                                placeholder = {
                                    Text(
                                        if (r == 0) "H${c + 1}" else "R${r}C${c + 1}",
                                        fontSize = 11.sp
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.5.sp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sb = StringBuilder()
                    for (r in 0 until rowsCount) {
                        sb.append("| ")
                        for (c in 0 until colsCount) {
                            val v = (cellValues[r to c] ?: "").replace("|", "/").trim().ifEmpty { "-" }
                            sb.append(v)
                            sb.append(" | ")
                        }
                        if (r < rowsCount - 1) sb.append("\n")
                    }
                    onInsertTable(sb.toString())
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppAccentColor())
            ) {
                Text(if (isBn) "টেবিল যুক্ত করুন" else "Insert Table", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isBn) "বাতিল" else "Cancel", color = AppSecondaryTextColor())
            }
        }
    )
}

@Composable
private fun StickyPdfViewerDialog(
    attachment: StickyAttachment,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    var pageBitmaps by remember(attachment.uri) { mutableStateOf<List<Bitmap>>(emptyList()) }
    var loadError by remember(attachment.uri) { mutableStateOf<String?>(null) }

    LaunchedEffect(attachment.uri) {
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(attachment.uri)
                val file = File(uri.path ?: "")
                if (file.exists()) {
                    val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(pfd)
                    val rendered = mutableListOf<Bitmap>()
                    val maxPages = renderer.pageCount.coerceAtMost(15)
                    for (i in 0 until maxPages) {
                        val page = renderer.openPage(i)
                        val bmp = Bitmap.createBitmap(
                            (page.width * 1.5f).toInt().coerceAtLeast(1),
                            (page.height * 1.5f).toInt().coerceAtLeast(1),
                            Bitmap.Config.ARGB_8888
                        )
                        bmp.eraseColor(android.graphics.Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        rendered.add(bmp)
                    }
                    renderer.close()
                    pfd.close()
                    pageBitmaps = rendered
                } else {
                    loadError = "PDF file not found"
                }
            } catch (e: Exception) {
                loadError = e.message ?: "Unable to render PDF"
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        DialogEdgeToEdge()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(getAppBackgroundGradient(LocalAppTheme.current))
                .dialogSystemBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .background(getGlassIconButtonBg(), CircleShape)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = AppTextColor())
                    }
                    Text(
                        text = attachment.name,
                        color = AppTextColor(),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    )
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier
                            .size(40.dp)
                            .background(getGlassIconButtonBg(), CircleShape)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = AppTextColor())
                    }
                }

                if (loadError != null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(loadError!!, color = AccentPink)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(pageBitmaps) { idx, bmp ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Page ${idx + 1}",
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Page ${idx + 1}", color = AppSecondaryTextColor(), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// FILE SIZE, COMPRESSION & STORAGE HELPERS
// ==========================================

private fun queryDisplayNameAndSize(context: Context, uri: Uri): Pair<String, Long> {
    var name = "attachment_${System.currentTimeMillis()}"
    var size = -1L
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
            if (nameIdx >= 0) {
                val n = cursor.getString(nameIdx)
                if (!n.isNullOrBlank()) name = n
            }
            if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                size = cursor.getLong(sizeIdx)
            }
        }
    }
    if (size < 0) {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            size = pfd.statSize
        }
    }
    return name to size
}

suspend fun processStickyImageAttachment(context: Context, uri: Uri): Result<StickyAttachment> = withContext(Dispatchers.IO) {
    try {
        val (origName, rawSize) = queryDisplayNameAndSize(context, uri)
        val tenMb = 10L * 1024L * 1024L
        val oneMb = 1L * 1024L * 1024L

        if (rawSize > tenMb) {
            return@withContext Result.failure(IllegalArgumentException("Image exceeds 10 MB limit (${rawSize / (1024 * 1024)} MB)."))
        }

        val dir = File(context.filesDir, "sticky_attachments").apply { mkdirs() }
        val cleanName = if (origName.contains(".")) origName else "$origName.jpg"
        val destFile = File(dir, "sticky_img_${System.currentTimeMillis()}_${(100..999).random()}.jpg")

        if (rawSize in 1..oneMb) {
            // Directly copy without re-encoding
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
        } else {
            // Auto-compress below 1 MB
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext Result.failure(IllegalArgumentException("Unable to read image"))

            if (bytes.size > tenMb) {
                return@withContext Result.failure(IllegalArgumentException("Image exceeds 10 MB limit."))
            }

            if (bytes.size <= oneMb) {
                destFile.writeBytes(bytes)
            } else {
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOpts)
                var sampleSize = 1
                val maxDim = 1920
                while (boundsOpts.outWidth / sampleSize > maxDim || boundsOpts.outHeight / sampleSize > maxDim) {
                    sampleSize *= 2
                }
                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
                    ?: return@withContext Result.failure(IllegalArgumentException("Failed to decode image"))

                var quality = 88
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                while (baos.size() > oneMb && quality > 25) {
                    baos.reset()
                    quality -= 12
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                }
                destFile.writeBytes(baos.toByteArray())
            }
        }

        Result.success(
            StickyAttachment(
                name = cleanName,
                uri = Uri.fromFile(destFile).toString(),
                type = "IMAGE",
                sizeBytes = destFile.length()
            )
        )
    } catch (e: Exception) {
        Result.failure(e)
    }
}

suspend fun processStickyPdfAttachment(context: Context, uri: Uri): Result<StickyAttachment> = withContext(Dispatchers.IO) {
    try {
        val (origName, rawSize) = queryDisplayNameAndSize(context, uri)
        val oneMb = 1L * 1024L * 1024L

        if (rawSize > oneMb) {
            return@withContext Result.failure(IllegalArgumentException("PDF exceeds 1 MB limit for Sticky Notes."))
        }

        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return@withContext Result.failure(IllegalArgumentException("Unable to read PDF"))

        if (bytes.size > oneMb) {
            return@withContext Result.failure(IllegalArgumentException("PDF exceeds 1 MB limit for Sticky Notes."))
        }

        val dir = File(context.filesDir, "sticky_attachments").apply { mkdirs() }
        val cleanName = if (origName.endsWith(".pdf", ignoreCase = true)) origName else "$origName.pdf"
        val destFile = File(dir, "sticky_pdf_${System.currentTimeMillis()}_${(100..999).random()}.pdf")
        destFile.writeBytes(bytes)

        Result.success(
            StickyAttachment(
                name = cleanName,
                uri = Uri.fromFile(destFile).toString(),
                type = "PDF",
                sizeBytes = destFile.length()
            )
        )
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private fun deleteInternalAttachmentFile(uriString: String) {
    try {
        val path = Uri.parse(uriString).path ?: return
        val file = File(path)
        if (file.exists()) file.delete()
    } catch (_: Exception) {
    }
}

private fun downloadStickyAttachmentToDevice(context: Context, attachment: StickyAttachment) {
    try {
        val sourceFile = File(Uri.parse(attachment.uri).path ?: "")
        if (!sourceFile.exists()) {
            Toast.makeText(context, "Attachment file not found", Toast.LENGTH_SHORT).show()
            return
        }
        val mime = if (attachment.type == "PDF") "application/pdf" else "image/jpeg"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, attachment.name)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val destUri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (destUri != null) {
                context.contentResolver.openOutputStream(destUri)?.use { out ->
                    sourceFile.inputStream().use { it.copyTo(out) }
                }
                Toast.makeText(context, "Downloaded to Downloads/${attachment.name}", Toast.LENGTH_SHORT).show()
            }
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            downloadsDir.mkdirs()
            val outFile = File(downloadsDir, attachment.name)
            sourceFile.copyTo(outFile, overwrite = true)
            Toast.makeText(context, "Saved to ${outFile.absolutePath}", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun shareStickyAttachment(context: Context, attachment: StickyAttachment) {
    try {
        val sourceFile = File(Uri.parse(attachment.uri).path ?: "")
        if (!sourceFile.exists()) {
            Toast.makeText(context, "Attachment file not found", Toast.LENGTH_SHORT).show()
            return
        }
        val shareUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            sourceFile
        )
        val mime = if (attachment.type == "PDF") "application/pdf" else "image/jpeg"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, shareUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share ${attachment.name}"))
    } catch (e: Exception) {
        Toast.makeText(context, "Share failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
