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

/**
 * Returns whether this range covers the character at [offset].
 *
 * The range is half-open: it contains its start but not its end, so an empty range contains no offset at all. Use
 * [touches] to include the end.
 *
 * @param offset the offset to look for; a negative offset is never contained.
 * @return `true` if [offset] lies at or after the start and before the end of this range.
 * @since 1.0.0
 */
public operator fun KoreTextRange.contains(offset: Int): Boolean = offset >= start && offset < end

/**
 * Returns whether this range covers every character of [other].
 *
 * Every range contains itself, and an empty range at any offset from the start to the end of this range, both ends
 * included, counts as contained.
 *
 * @param other the range to look for.
 * @return `true` if [other] starts at or after the start of this range and ends at or before its end.
 * @since 1.0.0
 */
public operator fun KoreTextRange.contains(other: KoreTextRange): Boolean = other.start >= start && other.end <= end

/**
 * Returns whether [offset] lies inside this range or at one of its ends.
 *
 * Unlike [contains], this includes the end of the range, so an empty range touches its own offset. It answers whether
 * a caret at [offset] stands at or within the range.
 *
 * @param offset the offset to test; a negative offset never touches a range.
 * @return `true` if [offset] lies from the start to the end of this range, both included.
 * @since 1.0.0
 */
public fun KoreTextRange.touches(offset: Int): Boolean = offset in start..end

/**
 * Returns whether this range and [other] overlap, which means that each of them starts before the other one ends.
 *
 * Ranges that only meet at an end do not intersect, such as `2..4` and `4..6`. An empty range intersects another range
 * only when its offset lies strictly between the start and the end of that range, and two empty ranges never
 * intersect.
 *
 * @param other the range to compare with.
 * @return `true` if the two ranges overlap.
 * @since 1.0.0
 */
public fun KoreTextRange.intersects(other: KoreTextRange): Boolean = start < other.end && other.start < end

/**
 * Returns the smallest range that covers both this range and [other], including any gap between them.
 *
 * @param other the range to cover as well.
 * @return the range from the smaller start to the larger end of the two ranges.
 * @since 1.0.0
 */
public fun KoreTextRange.covering(other: KoreTextRange): KoreTextRange =
    KoreTextRange(start = minOf(a = start, b = other.start), end = maxOf(a = end, b = other.end))

/**
 * Returns this range moved by [delta] UTF-16 code units, keeping its length.
 *
 * The new offsets are computed without overflow: a shift that would move the range before the text or past
 * [Int.MAX_VALUE] is refused instead of wrapping around. The message of the refusal names only the range and the
 * delta.
 *
 * @param delta how far to move the range; negative to move it towards the start of the text.
 * @return the moved range.
 * @throws IllegalArgumentException if the moved range would start before offset zero or end after [Int.MAX_VALUE].
 * @since 1.0.0
 */
public fun KoreTextRange.shiftedBy(delta: Int): KoreTextRange {
    val shiftedStart = start.toLong() + delta
    val shiftedEnd = end.toLong() + delta
    require(shiftedStart >= 0) { "Shifting the text range $this by $delta would start it before the text" }
    require(shiftedEnd <= Int.MAX_VALUE) {
        "Shifting the text range $this by $delta would end it after the last possible offset ${Int.MAX_VALUE}"
    }
    return KoreTextRange(start = shiftedStart.toInt(), end = shiftedEnd.toInt())
}

/**
 * Returns the characters of [text] that this range covers.
 *
 * The result is a copy of the covered UTF-16 code units exactly as they are: it is neither escaped nor shortened, and
 * it may begin or end with half of a surrogate pair when the range does. When [text] comes from untrusted input, so
 * does the result, and it must be escaped before it is shown or logged. The text is read while the call runs and must
 * not change during it.
 *
 * The message of the refusal names only the range and the length of [text], never its content.
 *
 * @param text the text the range points into.
 * @return the covered characters, empty for an empty range.
 * @throws IllegalArgumentException if this range ends after the end of [text].
 * @since 1.0.0
 */
public fun KoreTextRange.textIn(text: CharSequence): String {
    require(end <= text.length) { "The text range $this reaches beyond the text of length ${text.length}" }
    return text.substring(startIndex = start, endIndex = end)
}
