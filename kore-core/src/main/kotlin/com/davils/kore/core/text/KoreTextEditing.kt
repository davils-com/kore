/*
 * Copyright 2026 Davils
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.davils.kore.core.text

import com.davils.kore.core.limit.KoreLimits

private const val LONGEST_EDITED_TEXT: Int = (Int.MAX_VALUE - 8) / 2

private val editOrder: Comparator<KoreTextEdit> = compareBy({ it.range.start }, { it.range.end })

/**
 * Returns this text with all [edits] applied at once.
 *
 * Every range refers to this text as it is before any edit, so the edits do not shift each other. They are applied
 * in the order of their ranges, by start and then by end, whatever order they are given in: an insertion at the
 * offset where a replacement starts always comes before the replacement, and edits with the same range, such as two
 * insertions at one offset, keep the order in which they are given. Edits must not overlap; edits that only meet at an
 * offset, and an insertion at either end of another edit, are allowed.
 *
 * No limit of [KoreLimits] applies: the work and the memory grow with the length of this text, the number of edits
 * and the length of their replacements. The length of the result is computed without overflow before anything is
 * copied, and a result longer than 1,073,741,819 UTF-16 code units is refused. That bound is half of
 * `Int.MAX_VALUE - 8`, the largest array length the JDK grows its own arrays to, so a string can hold every result
 * that is not refused, whatever characters it contains, and building it never asks the virtual machine for an array
 * beyond its limit. Below that bound the result can still exhaust the heap, and several edits may share one long
 * replacement: a caller that takes edits from untrusted input bounds their number and the total length of their
 * replacements first. The messages of the refusals name only ranges and lengths, never this text or a replacement.
 *
 * This text is left unchanged and must not change while the call runs. The function keeps no state and may be
 * called from any thread.
 *
 * @param edits the edits to apply, in any order; empty to get an unchanged copy of the text.
 * @return a new string with every edit applied.
 * @throws IllegalArgumentException if two edits overlap, if an edit reaches beyond the end of this text, or if the
 * result would be longer than 1,073,741,819 UTF-16 code units.
 * @since 1.0.0
 */
public fun CharSequence.edited(edits: Iterable<KoreTextEdit>): String {
    val ordered = orderedDisjoint(edits)
    requireInside(ordered, length)
    val result = StringBuilder(editedLengthOf(length, ordered))
    var copied = 0
    ordered.forEach { edit ->
        result.append(this, copied, edit.range.start)
        result.append(edit.replacement)
        copied = edit.range.end
    }
    result.append(this, copied, length)
    return result.toString()
}

/**
 * Returns where [offset] of the original text lies once these edits are applied, as [edited] applies them.
 *
 * An offset before an edit, or at the start of an edit, keeps its place in front of it, so it stays before the text an
 * insertion adds. An offset inside the range an edit replaces moves to the end of the replacement. An offset at or
 * after the end of an edit, other than its start, moves by the [delta][KoreTextEdit.delta] of that edit. As a
 * result, every offset outside the replaced ranges that is not the offset of an insertion still points at the same
 * character after the edits. An offset at which an insertion adds text stays in front of that text and so points at
 * its first character; the character it pointed at before follows the inserted text.
 *
 * The edits are ordered and checked for overlaps exactly like [edited] does. Because no text is given, the function
 * cannot check that [offset] or the edits lie inside a text: an offset past the end of the text is moved by the delta
 * of every edit, as if the text were long enough. The messages of the refusals name only ranges and offsets.
 *
 * No limit of [KoreLimits] applies; the work grows with the number of edits. The function keeps no state and may be
 * called from any thread.
 *
 * @param offset the offset in the original text, counted from zero.
 * @return the corresponding offset in the edited text.
 * @throws IllegalArgumentException if [offset] is negative, if two edits overlap, or if the edits would move the
 * offset past [Int.MAX_VALUE].
 * @since 1.0.0
 */
public fun Iterable<KoreTextEdit>.mapOffset(offset: Int): Int {
    require(offset >= 0) { "An offset cannot lie before the text, but was $offset" }
    val ordered = orderedDisjoint(this)
    val mapped = ordered.fold(offset.toLong()) { moved, edit -> mappedThrough(edit, offset, moved) }
    require(mapped <= Int.MAX_VALUE) {
        "The edits would move the offset $offset after the last possible offset ${Int.MAX_VALUE}"
    }
    return mapped.toInt()
}

private fun mappedThrough(edit: KoreTextEdit, original: Int, mapped: Long): Long {
    if (original < edit.range.end || original == edit.range.start) return mappedBefore(edit, original, mapped)
    return mapped + edit.delta
}

private fun mappedBefore(edit: KoreTextEdit, original: Int, mapped: Long): Long {
    if (original <= edit.range.start) return mapped
    return mapped - (original - edit.range.start) + edit.replacement.length
}

private fun orderedDisjoint(edits: Iterable<KoreTextEdit>): List<KoreTextEdit> {
    val ordered = edits.sortedWith(editOrder)
    ordered.zipWithNext().forEach { (previous, next) ->
        require(previous.range.end <= next.range.start) {
            "Edits must not overlap, but the edit of ${previous.range} overlaps the edit of ${next.range}"
        }
    }
    return ordered
}

private fun requireInside(ordered: List<KoreTextEdit>, textLength: Int) {
    val last = ordered.lastOrNull() ?: return
    require(last.range.end <= textLength) {
        "The edit of ${last.range} reaches beyond the text of length $textLength"
    }
}

private fun editedLengthOf(textLength: Int, ordered: List<KoreTextEdit>): Int {
    val editedLength = textLength.toLong() + ordered.sumOf { edit -> edit.delta.toLong() }
    require(editedLength <= LONGEST_EDITED_TEXT) {
        "The edited text would have $editedLength characters, more than the $LONGEST_EDITED_TEXT it may have"
    }
    return editedLength.toInt()
}
