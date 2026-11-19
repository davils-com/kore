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
private const val LONGEST_BREAK: Int = 2

/**
 * A line break: a line feed, a carriage return followed by a line feed, or a carriage return alone.
 *
 * These are the three breaks that end a line in a [LineMap], in YAML and in most text formats. The next line, line
 * separator and paragraph separator characters of Unicode are no line breaks here, as in YAML 1.2.
 *
 * @property text the characters of the break.
 * @since 2.1.0
 */
public enum class LineBreak(public val text: String) {
    /**
     * A line feed, `\n`, the break of Unix and of most formats.
     *
     * @since 2.1.0
     */
    Lf("\n"),

    /**
     * A carriage return followed by a line feed, `\r\n`, the break of Windows and of many network protocols.
     *
     * @since 2.1.0
     */
    CrLf("\r\n"),

    /**
     * A carriage return alone, `\r`, the break of classic Mac OS.
     *
     * @since 2.1.0
     */
    Cr("\r"),
    ;

    /**
     * Finds line breaks in text.
     *
     * @since 2.1.0
     */
    public companion object {
        /**
         * Returns the first line break in [text], such as the break a file uses, or `null` if it has none.
         *
         * A carriage return directly followed by a line feed is one [CrLf] break. The text is read up to its first
         * break only.
         *
         * @param text the text to look at.
         * @return the first break, or `null` if [text] holds no line feed and no carriage return.
         * @since 2.1.0
         */
        public fun firstIn(text: CharSequence): LineBreak? {
            val index = text.indexOfFirst { character -> character == LINE_FEED || character == CARRIAGE_RETURN }
            if (index < 0) return null
            val end = minOf(a = index + LONGEST_BREAK, b = text.length)
            val start = text.subSequence(startIndex = index, endIndex = end)
            return listOf(CrLf, Lf, Cr).first { candidate -> start.startsWith(candidate.text) }
        }
    }
}

/**
 * Returns this text with every line break replaced by [target], such as a text with mixed breaks that is written with
 * the breaks of one platform.
 *
 * A carriage return directly followed by a line feed is one break. Every other character is kept as it is, so the
 * result is the same text when [target] is the only break it has. Applying the function twice with the same [target]
 * gives the same text as applying it once.
 *
 * @param target the break to write; a line feed by default.
 * @return the text with every line break replaced.
 * @throws IllegalArgumentException if this text is longer than [Limits.MAX_LINE_MAP_LENGTH] UTF-16 code units; it is
 * refused before it is read.
 * @since 2.1.0
 */
public fun CharSequence.withLineBreaks(target: LineBreak = LineBreak.Lf): String {
    require(length <= Limits.MAX_LINE_MAP_LENGTH) {
        "Line breaks are replaced in at most ${Limits.MAX_LINE_MAP_LENGTH} characters, but the text has $length"
    }
    val source = this
    return buildString(source.length) {
        var index = 0
        while (index < source.length) {
            val width = breakWidthAt(text = source, index = index)
            if (width == 0) append(source[index])
            if (width > 0) append(target.text)
            index += maxOf(a = width, b = 1)
        }
    }
}

private fun breakWidthAt(text: CharSequence, index: Int): Int {
    val character = text[index]
    var width = 0
    if (character == LINE_FEED || character == CARRIAGE_RETURN) width = 1
    if (character == CARRIAGE_RETURN && text.getOrNull(index + 1) == LINE_FEED) width = LONGEST_BREAK
    return width
}
