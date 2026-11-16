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

private const val HASH_FACTOR: Int = 31

private val rangeOrder: Comparator<KoreTextRange> = compareBy({ it.start }, { it.end })

/**
 * A half-open range of UTF-16 offsets in a text: it covers the characters from [start] up to, but not including,
 * [end].
 *
 * Offsets count UTF-16 code units, the unit of [String] indexes, so a character outside the Basic Multilingual Plane
 * takes two of them and a range may start or end between the two halves of a surrogate pair. A range is not bound to
 * a text: the constructor only checks that the offsets are ordered, and the functions that apply a range to a text,
 * such as [textIn], [KoreLineMap.snippet] and [edited], check that it fits that text.
 *
 * A range is an immutable value and safe to share between threads. Two ranges are equal when their offsets are equal,
 * and [compareTo] orders them by [start] and then by [end], which is consistent with [equals]. The extension functions
 * [contains], [touches], [intersects], [covering] and [shiftedBy] compare and derive ranges.
 *
 * @property start the offset of the first covered character, counted from zero.
 * @property end the offset just after the last covered character; equal to [start] for an empty range.
 * @throws IllegalArgumentException if [start] is negative or [end] lies before [start].
 * @since 1.0.0
 */
public class KoreTextRange(public val start: Int, public val end: Int) : Comparable<KoreTextRange> {
    init {
        require(start >= 0) { "A text range cannot start before the text, but started at $start" }
        require(end >= start) { "A text range cannot end before it starts, but was $start..$end" }
    }

    /**
     * The number of UTF-16 code units the range covers, from zero up to [Int.MAX_VALUE].
     *
     * @since 1.0.0
     */
    public val length: Int get() = end - start

    /**
     * Whether the range covers no character, which makes it a position between two characters, such as the place of
     * an insertion.
     *
     * @since 1.0.0
     */
    public val isEmpty: Boolean get() = start == end

    /**
     * Compares this range with [other] by [start] first and by [end] second, so a range sorts before every longer
     * range that starts at the same offset.
     *
     * @param other the range to compare with.
     * @return a negative number, zero or a positive number as this range sorts before, together with or after [other].
     * @since 1.0.0
     */
    override fun compareTo(other: KoreTextRange): Int = rangeOrder.compare(this, other)

    /**
     * Returns whether [other] is a range with the same [start] and the same [end].
     *
     * @param other the object to compare with, which may be `null`.
     * @return `true` if [other] is an equal range, `false` otherwise.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is KoreTextRange && start == other.start && end == other.end

    /**
     * Returns a hash code derived from [start] and [end].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = HASH_FACTOR * start + end

    /**
     * Returns the range as `start..end`, such as `2..6`.
     *
     * The text shows the two offsets as they are and never any text the range points into. Despite the notation, [end]
     * is exclusive, as everywhere else.
     *
     * @return the range in the form `start..end`.
     * @since 1.0.0
     */
    override fun toString(): String = "$start..$end"

    /**
     * Creates text ranges from an offset or from a start and a length.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Returns the empty range at [offset], the position just before the character at that offset.
         *
         * @param offset the offset of the position, counted from zero.
         * @return the empty range from [offset] to [offset].
         * @throws IllegalArgumentException if [offset] is negative.
         * @since 1.0.0
         */
        public fun at(offset: Int): KoreTextRange = KoreTextRange(start = offset, end = offset)

        /**
         * Returns the range of [length] UTF-16 code units that begins at [start].
         *
         * The end is computed without overflow: a range that would end after [Int.MAX_VALUE] is refused instead of
         * wrapping around to a negative offset.
         *
         * @param start the offset of the first covered character, counted from zero.
         * @param length the number of covered UTF-16 code units.
         * @return the range from [start] to `start + length`.
         * @throws IllegalArgumentException if [start] or [length] is negative, or if the range would end after
         * [Int.MAX_VALUE].
         * @since 1.0.0
         */
        public fun of(start: Int, length: Int): KoreTextRange {
            require(length >= 0) { "A text range cannot have a negative length, but had $length" }
            val end = start.toLong() + length
            require(end <= Int.MAX_VALUE) {
                "A text range of length $length from $start would end after the last possible offset ${Int.MAX_VALUE}"
            }
            return KoreTextRange(start = start, end = end.toInt())
        }
    }
}
