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

@file:OptIn(DavilsInternal::class)

package com.davils.kore.testing

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import com.davils.kore.core.text.TextRange

/**
 * A text for tests together with the carets and ranges that were marked in it.
 *
 * [MarkedText.of] reads a text in which places are marked inline, such as `port: <caret>80` or
 * `port: <range>80</range>`, removes the markers and keeps where they stood. This lets a test write the input it feeds
 * to code built on Kore and the offsets it expects back in one literal. Offsets count UTF-16 code units of [text], the
 * text without its markers, and fit [TextRange] and the other types of `com.davils.kore.core.text`.
 *
 * The constructor builds a marked text directly and checks nothing: its carets and ranges may lie outside [text] and
 * come in any order. It keeps unmodifiable copies of the lists it is given, so later changes to those lists do not
 * affect it, and the lists it returns cannot be changed, not even after a cast. A marked text is immutable and safe to
 * share between threads. Two marked texts are equal when their texts, carets and ranges are equal.
 *
 * This class is test support, like everything in `kore-testing`, and does not belong on a production class path.
 *
 * @property text the text without markers.
 * @param carets the offsets of the carets in [text], kept in the given order.
 * @param ranges the marked ranges of [text], kept in the given order.
 * @throws NullPointerException if [carets] or [ranges] contains `null`, which only a caller in Java can pass.
 * @since 1.0.0
 */
public class MarkedText(
    public val text: String,
    carets: List<Int>,
    ranges: List<TextRange>,
) {
    /**
     * The offsets in [text] at which carets were marked; [MarkedText.of] lists them in the order they appear in.
     *
     * The list is an unmodifiable copy: a cast to [MutableList] does not make it changeable. On the JVM every attempt
     * to change it throws [UnsupportedOperationException]; on Wasm the cast itself fails.
     *
     * @since 1.0.0
     */
    public val carets: List<Int> = carets.immutableList()

    /**
     * The marked ranges of [text]; [MarkedText.of] sorts them by start and then by end.
     *
     * The list is an unmodifiable copy: a cast to [MutableList] does not make it changeable. On the JVM every attempt
     * to change it throws [UnsupportedOperationException]; on Wasm the cast itself fails.
     *
     * @since 1.0.0
     */
    public val ranges: List<TextRange> = ranges.immutableList()

    /**
     * The offset of the only caret, for the common test with exactly one.
     *
     * @throws NoSuchElementException if no caret was marked.
     * @throws IllegalArgumentException if more than one caret was marked.
     * @since 1.0.0
     */
    public val caret: Int get() = carets.single()

    /**
     * The only marked range, for the common test with exactly one.
     *
     * @throws NoSuchElementException if no range was marked.
     * @throws IllegalArgumentException if more than one range was marked.
     * @since 1.0.0
     */
    public val range: TextRange get() = ranges.single()

    private val identity: List<Any> get() = listOf(text, carets, ranges)

    /**
     * Returns whether [other] is a marked text with the same text, the same carets and the same ranges, each in the
     * same order.
     *
     * @param other the object to compare with, which may be `null`.
     * @return `true` if [other] is an equal marked text, `false` otherwise.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is MarkedText && identity == other.identity

    /**
     * Returns a hash code derived from the text, the carets and the ranges.
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = identity.hashCode()

    /**
     * Returns the marked text as one line, such as `MarkedText(length=8, carets=[6], ranges=[6..8])`.
     *
     * The line names the length of the text, the carets and the ranges, but never the text itself, so a test text full
     * of control characters or secrets cannot garble or leak into a test report. Compare [text] directly to see it.
     *
     * @return the length of the text, the carets and the ranges.
     * @since 1.0.0
     */
    override fun toString(): String = "MarkedText(length=${text.length}, carets=$carets, ranges=$ranges)"

    /**
     * Reads marked texts and names the markers they use.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The marker of a caret, `<caret>`; [of] adds the offset at which it stands to [MarkedText.carets].
         *
         * @since 1.0.0
         */
        public const val CARET: String = "<caret>"

        /**
         * The marker that opens a range, `<range>`; ranges may nest.
         *
         * @since 1.0.0
         */
        public const val RANGE_START: String = "<range>"

        /**
         * The marker that closes the range opened last, `</range>`.
         *
         * @since 1.0.0
         */
        public const val RANGE_END: String = "</range>"

        /**
         * Reads [marked], removes every marker and returns the remaining text with the carets and ranges the markers
         * stood for.
         *
         * Markers are read from left to right, and every occurrence of [CARET], [RANGE_START] or [RANGE_END] counts
         * as a marker. There is no way to escape one, and a text whose characters around a removed marker would join
         * into another marker, such as `<car<caret>et>`, is refused instead of being returned with that marker in it,
         * so the text of the result never contains a marker: a text that must contain one literally is built with the
         * constructor instead. Each [RANGE_END] closes the range opened last, so ranges nest. The carets are listed in
         * the order they appear in, and the ranges sorted by start and then by end. Every offset refers to the text
         * without markers.
         *
         * The input is meant to be written by the author of a test, so no limit applies to its length. The messages of
         * the refusals name only the markers and how many ranges stayed open, never the text.
         *
         * @param marked the text with its markers.
         * @return the text without markers, with the carets and ranges they marked.
         * @throws IllegalStateException if a [RANGE_END] has no open range before it, if a range is never closed, or if
         * the text without markers would contain a marker.
         * @since 1.0.0
         */
        public fun of(marked: String): MarkedText = TextMarkers(marked).read()
    }
}
