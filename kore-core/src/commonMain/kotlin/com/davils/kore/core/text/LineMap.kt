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

import com.davils.kore.core.Limits

private const val LINE_FEED: Char = '\n'
private const val CARRIAGE_RETURN: Char = '\r'
private const val DEFAULT_SNIPPET_WIDTH: Int = 120
private const val PAIR_BLOCK: Int = 64

/**
 * An index of the lines of a text that turns offsets into lines and columns and cuts snippets for messages.
 *
 * A line ends at a line feed (`\n`), at a carriage return followed by a line feed (`\r\n`) or at a carriage return
 * on its own (`\r`); a break belongs to the line it ends. Other separators, such as U+2028 or U+0085, do not end a
 * line. Every text has at least one line: an empty text has one empty line, and a text that ends with a break has an
 * empty last line after it. Offsets and columns count UTF-16 code units unless a function says otherwise; lines and
 * columns are counted from zero.
 *
 * [LineMap.of] indexes a snapshot of the text and refuses a text longer than [Limits.MAX_LINE_MAP_LENGTH],
 * both before it reads the text and again, before it scans it, once it has taken the snapshot. The map keeps the
 * snapshot and one number for every line, so its memory grows with the length of the text and the number of its
 * lines; a text at the limit that consists only of line breaks takes several hundred megabytes. A caller that indexes
 * untrusted text and needs a tighter bound checks the length first.
 *
 * A map is immutable and safe to share between threads; later changes to the indexed [CharSequence] do not affect it.
 * [snippet] returns parts of the text unescaped, so snippets of untrusted text are shown through [render] or
 * [TextSnippet.toString]. The messages of the refusals name only numbers such as offsets, lines, widths and
 * lengths, never the text.
 *
 * @since 1.0.0
 */
public class LineMap private constructor(private val text: String) {
    init {
        requireIndexable(text.length)
    }

    private val starts: IntArray = lineStartsOf(text)

    private val pairCounts: IntArray = pairCountsOf(text)

    /**
     * The number of lines of the text, at least one.
     *
     * @since 1.0.0
     */
    public val lineCount: Int get() = starts.size

    /**
     * Returns the line that [offset] lies on.
     *
     * The offset just after the last character, the length of the text, is valid and lies on the last line. An offset
     * that points at a line break lies on the line the break ends, both halves of `\r\n` included.
     *
     * @param offset an offset from zero to the length of the text, both included.
     * @return the line, counted from zero.
     * @throws IllegalArgumentException if [offset] is negative or larger than the length of the text.
     * @since 1.0.0
     */
    public fun lineOf(offset: Int): Int {
        requireOffset(offset)
        val found = starts.asList().binarySearch(offset)
        if (found >= 0) return found
        return -found - 2
    }

    /**
     * Returns the line and the column of [offset].
     *
     * The column counts UTF-16 code units from the start of the line, so a character outside the Basic Multilingual
     * Plane counts twice; use [codePointColumnOf] to count code points. An offset that points at a line break gets a
     * column at or after the end of the content of its line.
     *
     * @param offset an offset from zero to the length of the text, both included.
     * @return the position of [offset], counted from zero.
     * @throws IllegalArgumentException if [offset] is negative or larger than the length of the text.
     * @since 1.0.0
     */
    public fun positionOf(offset: Int): TextPosition {
        val line = lineOf(offset)
        return TextPosition(line = line, column = offset - starts[line])
    }

    /**
     * Returns the offset of [position], the reverse of [positionOf].
     *
     * The column counts UTF-16 code units. A column at or after the end of the content of the line, however large,
     * gives the end of that content, before its line break, so a position taken from another text or from user input
     * never leads to an offset on another line or to an overflow.
     *
     * @param position the position, whose line must exist in the text.
     * @return the offset of [position] in the text.
     * @throws IllegalArgumentException if the line of [position] is not a line of the text.
     * @since 1.0.0
     */
    public fun offsetOf(position: TextPosition): Int {
        val content = lineRange(position.line)
        if (position.column >= content.length) return content.end
        return content.start + position.column
    }

    /**
     * Returns the range of the content of [line], without its line break.
     *
     * @param line the line, counted from zero.
     * @return the range from the first character of the line to the start of its break, or to the end of the text on
     * the last line.
     * @throws IllegalArgumentException if [line] is negative or not less than [lineCount].
     * @since 1.0.0
     */
    public fun lineRange(line: Int): TextRange {
        require(line in starts.indices) { "The text has $lineCount lines, so there is no line $line" }
        val start = starts[line]
        val next = starts.getOrNull(line + 1) ?: return TextRange(start = start, end = text.length)
        return TextRange(start = start, end = next - breakLengthBefore(next, start))
    }

    /**
     * Returns the column of [offset] counted in Unicode code points instead of UTF-16 code units.
     *
     * A surrogate pair counts as one code point and a lone surrogate as one of its own, so the result equals
     * [String.codePointCount] from the start of the line to [offset], also for an offset between the two halves of a
     * pair. The column counts neither grapheme clusters nor display width: a combining mark counts as a code point of
     * its own, and a wide character counts once although a terminal shows it in two columns. The cost of a call does
     * not grow with the length of the line.
     *
     * @param offset an offset from zero to the length of the text, both included.
     * @return the number of code points between the start of the line and [offset].
     * @throws IllegalArgumentException if [offset] is negative or larger than the length of the text.
     * @since 1.0.0
     */
    public fun codePointColumnOf(offset: Int): Int {
        val start = starts[lineOf(offset)]
        val pairs = pairsBefore(offset) - pairsBefore(start)
        return offset - start - pairs
    }

    /**
     * Returns the part of the line around [range], cut to at most [width] UTF-16 code units, with the range
     * highlighted.
     *
     * The snippet shows the line on which the range starts. The window starts half the width before the start of the
     * range, but not before the start of the line, and ends after the width or at the end of the line. It does not
     * move back to fill the width near the end of the line, so the part before the range depends neither on the length
     * of the range nor on the rest of the line, which keeps [masked] snippets independent of the length of a secret. A
     * window start that would split a surrogate pair moves one code unit inward, but never past the start of the
     * range: at a width of one, a range that itself starts between the two halves of a pair is shown from its start.
     * The start of the window therefore depends only on the text before the range. A window end that would split a
     * pair moves inward, or outward when moving inward would pass the start of the range, which can make the line one
     * code unit longer than a width of one or two. A width larger than [Limits.MAX_SNIPPET_WIDTH] is reduced to
     * that limit.
     *
     * The highlight starts at the start of the range and ends at its end, at the end of the window or at the end of
     * the line, whichever comes first, so a range over several lines is highlighted only on its first line. A range
     * that starts in a line break is shown as an empty highlight at the end of its line; the position of the snippet is
     * still that of the start of the range.
     *
     * The line of the snippet is a copy of the indexed text, neither escaped nor masked: show it with [render] or
     * [TextSnippet.toString], and call [masked] first when the range covers a secret.
     *
     * @param range the range to highlight, which must end within the text.
     * @param width the most UTF-16 code units to show, 120 unless given.
     * @return the snippet of the line around [range].
     * @throws IllegalArgumentException if [width] is less than one or [range] ends after the end of the text.
     * @since 1.0.0
     */
    public fun snippet(range: TextRange, width: Int = DEFAULT_SNIPPET_WIDTH): TextSnippet {
        require(width > 0) { "A snippet needs a width of at least one character, but was $width" }
        requireOffset(range.end)
        val window = minOf(a = width, b = Limits.MAX_SNIPPET_WIDTH)
        val position = positionOf(range.start)
        val content = lineRange(position.line)
        val anchor = minOf(a = range.start, b = content.end)
        val from = windowStart(anchor, content, window)
        val to = windowEnd(from = from, anchor = anchor, content = content, window = window)
        val highlightEnd = range.end.coerceIn(minimumValue = anchor, maximumValue = to)
        return TextSnippet(
            position = position,
            line = text.substring(startIndex = from, endIndex = to),
            highlight = TextRange(start = anchor - from, end = highlightEnd - from),
            isCutBefore = from > content.start,
            isCutAfter = to < content.end,
        )
    }

    private fun windowStart(anchor: Int, content: TextRange, window: Int): Int {
        val start = maxOf(a = content.start, b = anchor - window / 2)
        val isInsidePair = start < anchor && splitsPair(start)
        if (isInsidePair) return start + 1
        return start
    }

    private fun windowEnd(from: Int, anchor: Int, content: TextRange, window: Int): Int {
        val end = from + minOf(a = window, b = content.end - from)
        if (!splitsPair(end)) return end
        return (end - 1).takeIf { inward -> inward > anchor } ?: end + 1
    }

    private fun splitsPair(index: Int): Boolean = index < text.length && isPairEnd(text, index)

    private fun pairsBefore(offset: Int): Int {
        val block = offset / PAIR_BLOCK
        val blockStart = block * PAIR_BLOCK
        return pairCounts[block] + (blockStart until offset).count { index -> isPairEnd(text, index) }
    }

    private fun breakLengthBefore(next: Int, start: Int): Int {
        val isPair = next - 2 >= start && text[next - 2] == CARRIAGE_RETURN && text[next - 1] == LINE_FEED
        if (isPair) return 2
        return 1
    }

    private fun requireOffset(offset: Int) {
        require(offset in 0..text.length) { "The offset $offset lies outside the text of length ${text.length}" }
    }

    /**
     * Creates line maps.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Returns the line map of [text].
         *
         * The length is checked before anything else, so a text above the limit is refused before it is copied or
         * scanned. The map then indexes a snapshot of [text] taken during the call and checks the length of that
         * snapshot again before it scans it, so the limit holds even for a [CharSequence] whose length reports less
         * than its content. A [StringBuilder] or another mutable [CharSequence] may be changed afterwards without
         * affecting the map, but must not change while the call runs.
         *
         * @param text the text to index, at most [Limits.MAX_LINE_MAP_LENGTH] UTF-16 code units long.
         * @return the line map of the snapshot.
         * @throws IllegalArgumentException if [text] or its snapshot is longer than [Limits.MAX_LINE_MAP_LENGTH].
         * The message names only the limit and the length.
         * @since 1.0.0
         */
        public fun of(text: CharSequence): LineMap {
            requireIndexable(text.length)
            return LineMap(text.toString())
        }
    }
}

private fun requireIndexable(length: Int) {
    require(length <= Limits.MAX_LINE_MAP_LENGTH) {
        "A line map indexes at most ${Limits.MAX_LINE_MAP_LENGTH} characters, but the text has $length"
    }
}

private fun lineStartsOf(text: String): IntArray {
    val starts = IntArray(lineCountOf(text))
    var line = 1
    for (index in text.indices) {
        if (endsLine(text, index)) {
            starts[line] = index + 1
            line++
        }
    }
    return starts
}

private fun lineCountOf(text: String): Int {
    var count = 1
    for (index in text.indices) {
        if (endsLine(text, index)) count++
    }
    return count
}

private fun pairCountsOf(text: String): IntArray {
    val counts = IntArray(text.length / PAIR_BLOCK + 1)
    var pairs = 0
    for (block in 1 until counts.size) {
        val blockStart = block * PAIR_BLOCK
        for (index in blockStart - PAIR_BLOCK until blockStart) {
            if (isPairEnd(text, index)) pairs++
        }
        counts[block] = pairs
    }
    return counts
}

private fun endsLine(text: String, index: Int): Boolean {
    val character = text[index]
    if (character == LINE_FEED) return true
    return character == CARRIAGE_RETURN && text.getOrNull(index + 1) != LINE_FEED
}

private fun isPairEnd(text: String, index: Int): Boolean =
    index > 0 && text[index - 1].isHighSurrogate() && text[index].isLowSurrogate()
