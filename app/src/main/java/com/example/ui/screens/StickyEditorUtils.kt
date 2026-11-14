package com.example.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

data class StickyDraftSnapshot(
    val textFieldValue: TextFieldValue,
    val lineAlignments: Map<Int, String>
)

/**
 * Bounded Undo/Redo controller for the Sticky Notes editor.
 * Keeps UI composables clean and prevents unbounded memory growth.
 */
class StickyUndoRedoController(
    private val maxHistory: Int = 50
) {
    private val undoStack = mutableStateListOf<StickyDraftSnapshot>()
    private val redoStack = mutableStateListOf<StickyDraftSnapshot>()

    var canUndo: Boolean by mutableStateOf(false)
        private set
    var canRedo: Boolean by mutableStateOf(false)
        private set

    fun pushSnapshot(snapshot: StickyDraftSnapshot) {
        val last = undoStack.lastOrNull()
        if (last != null &&
            last.textFieldValue.text == snapshot.textFieldValue.text &&
            last.lineAlignments == snapshot.lineAlignments
        ) {
            return
        }
        undoStack.add(snapshot)
        if (undoStack.size > maxHistory) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
        syncFlags()
    }

    fun undo(current: StickyDraftSnapshot): StickyDraftSnapshot? {
        if (undoStack.isEmpty()) return null
        val previous = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(current)
        if (redoStack.size > maxHistory) {
            redoStack.removeAt(0)
        }
        syncFlags()
        return previous
    }

    fun redo(current: StickyDraftSnapshot): StickyDraftSnapshot? {
        if (redoStack.isEmpty()) return null
        val next = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(current)
        if (undoStack.size > maxHistory) {
            undoStack.removeAt(0)
        }
        syncFlags()
        return next
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        syncFlags()
    }

    private fun syncFlags() {
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }
}

const val STICKY_CHECKBOX_UNCHECKED = "☐ "
const val STICKY_CHECKBOX_CHECKED = "☑ "
const val STICKY_BULLET_PREFIX = "• "

/**
 * Toggles a line-level prefix ("☐ " checklist or "• " bullet) across all lines intersecting
 * the current cursor or selection, preserving relative cursor/selection offsets.
 */
fun toggleLinePrefixAtSelection(
    value: TextFieldValue,
    prefix: String
): TextFieldValue {
    val text = value.text
    val selStart = value.selection.min.coerceIn(0, text.length)
    val selEnd = value.selection.max.coerceIn(0, text.length)

    // Find start of first affected line and end of last affected line
    val blockStart = if (selStart == 0) 0 else (text.lastIndexOf('\n', selStart - 1) + 1)
    val blockEndIdx = text.indexOf('\n', selEnd)
    val blockEnd = if (blockEndIdx == -1) text.length else blockEndIdx

    val blockText = text.substring(blockStart, blockEnd)
    val lines = blockText.split('\n')

    val isChecklistMode = prefix == STICKY_CHECKBOX_UNCHECKED
    val allAlreadyHaveTarget = lines.all { line ->
        if (isChecklistMode) {
            line.startsWith(STICKY_CHECKBOX_UNCHECKED) || line.startsWith(STICKY_CHECKBOX_CHECKED)
        } else {
            line.startsWith(prefix)
        }
    }

    var deltaBeforeSelStart = 0
    var deltaTotal = 0
    var runningOriginalOffset = blockStart

    val transformedLines = lines.map { line ->
        val lineOrigStart = runningOriginalOffset
        runningOriginalOffset += line.length + 1

        val newLine = if (allAlreadyHaveTarget) {
            when {
                line.startsWith(STICKY_CHECKBOX_UNCHECKED) -> line.removePrefix(STICKY_CHECKBOX_UNCHECKED)
                line.startsWith(STICKY_CHECKBOX_CHECKED) -> line.removePrefix(STICKY_CHECKBOX_CHECKED)
                line.startsWith(STICKY_BULLET_PREFIX) -> line.removePrefix(STICKY_BULLET_PREFIX)
                else -> line
            }
        } else {
            val stripped = when {
                line.startsWith(STICKY_CHECKBOX_UNCHECKED) -> line.removePrefix(STICKY_CHECKBOX_UNCHECKED)
                line.startsWith(STICKY_CHECKBOX_CHECKED) -> line.removePrefix(STICKY_CHECKBOX_CHECKED)
                line.startsWith(STICKY_BULLET_PREFIX) -> line.removePrefix(STICKY_BULLET_PREFIX)
                else -> line
            }
            prefix + stripped
        }

        val lineDelta = newLine.length - line.length
        if (lineOrigStart <= selStart) {
            deltaBeforeSelStart += lineDelta
        }
        deltaTotal += lineDelta
        newLine
    }

    val replacedBlock = transformedLines.joinToString("\n")
    val newText = text.substring(0, blockStart) + replacedBlock + text.substring(blockEnd)
    val newStart = (selStart + deltaBeforeSelStart).coerceIn(0, newText.length)
    val newEnd = if (selStart == selEnd) {
        newStart
    } else {
        (selEnd + deltaTotal).coerceIn(newStart, newText.length)
    }

    return TextFieldValue(
        text = newText,
        selection = TextRange(newStart, newEnd)
    )
}

/**
 * Handles Enter key press inside checklist or bullet list lines:
 * - If previous line has content after prefix -> automatically inserts prefix on the new line.
 * - If previous line was ONLY the empty prefix ("☐ ", "☑ ", or "• ") -> removes the empty prefix & stops list.
 * Returns null when no auto-continuation applies.
 */
fun handleChecklistOrBulletNewline(
    oldValue: TextFieldValue,
    newValue: TextFieldValue
): TextFieldValue? {
    val oldText = oldValue.text
    val newText = newValue.text
    if (newText.length != oldText.length + 1) return null

    val cursorOld = oldValue.selection.min.coerceIn(0, oldText.length)
    if (!oldValue.selection.collapsed) return null
    if (cursorOld >= newText.length || newText[cursorOld] != '\n') return null
    if (newValue.selection.min != cursorOld + 1) return null

    val lineStart = if (cursorOld == 0) 0 else (oldText.lastIndexOf('\n', cursorOld - 1) + 1)
    val prevLine = oldText.substring(lineStart, cursorOld)

    val matchedPrefix = when {
        prevLine.startsWith(STICKY_CHECKBOX_UNCHECKED) -> STICKY_CHECKBOX_UNCHECKED
        prevLine.startsWith(STICKY_CHECKBOX_CHECKED) -> STICKY_CHECKBOX_CHECKED
        prevLine.startsWith(STICKY_BULLET_PREFIX) -> STICKY_BULLET_PREFIX
        else -> null
    } ?: return null

    val contentAfterPrefix = prevLine.removePrefix(matchedPrefix)
    if (contentAfterPrefix.isBlank()) {
        // Exit list mode cleanly by stripping the empty prefix + inserted newline
        val cleaned = oldText.removeRange(lineStart, cursorOld)
        return TextFieldValue(
            text = cleaned,
            selection = TextRange(lineStart.coerceIn(0, cleaned.length))
        )
    }

    val nextPrefix = if (matchedPrefix == STICKY_CHECKBOX_CHECKED) STICKY_CHECKBOX_UNCHECKED else matchedPrefix
    val insertAt = cursorOld + 1
    val updated = newText.substring(0, insertAt) + nextPrefix + newText.substring(insertAt)
    val newCursor = (insertAt + nextPrefix.length).coerceIn(0, updated.length)
    return TextFieldValue(
        text = updated,
        selection = TextRange(newCursor)
    )
}

/**
 * Toggles a specific line between unchecked ("☐ ") and checked ("☑ ") in Preview mode.
 */
fun toggleChecklistLineAtIndex(rawText: String, lineIndex: Int): String {
    val lines = rawText.split('\n').toMutableList()
    if (lineIndex !in lines.indices) return rawText
    val line = lines[lineIndex]
    lines[lineIndex] = when {
        line.startsWith(STICKY_CHECKBOX_UNCHECKED) -> STICKY_CHECKBOX_CHECKED + line.removePrefix(STICKY_CHECKBOX_UNCHECKED)
        line.startsWith(STICKY_CHECKBOX_CHECKED) -> STICKY_CHECKBOX_UNCHECKED + line.removePrefix(STICKY_CHECKBOX_CHECKED)
        else -> line
    }
    return lines.joinToString("\n")
}

/**
 * Checks whether the current cursor or selection sits inside a paired inline marker (`*` or `_`)
 * on the current line so toolbar formatting buttons highlight accurately.
 */
fun isCursorInsideInlineMarker(value: TextFieldValue, marker: Char): Boolean {
    val text = value.text
    if (text.isEmpty() || !text.contains(marker)) return false
    val selStart = value.selection.min.coerceIn(0, text.length)
    val selEnd = value.selection.max.coerceIn(0, text.length)

    val lineStart = if (selStart == 0) 0 else (text.lastIndexOf('\n', selStart - 1) + 1)
    val lineEndIdx = text.indexOf('\n', lineStart)
    val lineEnd = if (lineEndIdx == -1) text.length else lineEndIdx
    val line = text.substring(lineStart, lineEnd)
    val relStart = (selStart - lineStart).coerceIn(0, line.length)
    val relEnd = (selEnd - lineStart).coerceIn(relStart, line.length)

    if (relStart < relEnd) {
        val selected = line.substring(relStart, relEnd)
        if (selected.startsWith(marker) && selected.endsWith(marker) && selected.length >= 2) {
            return true
        }
    }

    var idx = 0
    while (idx < line.length) {
        if (line[idx] == marker) {
            val close = line.indexOf(marker, idx + 1)
            if (close > idx + 1) {
                if (relStart in (idx + 1)..close && relEnd in (idx + 1)..close) {
                    return true
                }
                idx = close + 1
                continue
            }
        }
        idx++
    }
    return false
}
