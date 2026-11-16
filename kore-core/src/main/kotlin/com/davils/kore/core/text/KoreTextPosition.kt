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

private val positionOrder: Comparator<KoreTextPosition> = compareBy({ it.line }, { it.column })

/**
 * A place in a text given as a line and a column, both counted from zero.
 *
 * A position is not bound to a text, so the constructor checks only that both numbers are not negative. A position
 * made by [KoreLineMap.positionOf] counts its [column] in UTF-16 code units from the start of its line; use
 * [KoreLineMap.codePointColumnOf] for a column in code points. Only the text form counts from one, the way editors
 * and compilers show a position.
 *
 * A position is an immutable value and safe to share between threads. Two positions are equal when their line and
 * column are equal, and [compareTo] orders them by [line] and then by [column], which is consistent with [equals].
 *
 * @property line the line, counted from zero.
 * @property column the column within the line, counted from zero.
 * @throws IllegalArgumentException if [line] or [column] is negative.
 * @since 1.0.0
 */
public class KoreTextPosition(public val line: Int, public val column: Int) : Comparable<KoreTextPosition> {
    init {
        require(line >= 0) { "A line is counted from zero, but was $line" }
        require(column >= 0) { "A column is counted from zero, but was $column" }
    }

    /**
     * Compares this position with [other] by [line] first and by [column] second, which is the order of the places
     * in the text.
     *
     * @param other the position to compare with.
     * @return a negative number, zero or a positive number as this position comes before, at or after [other].
     * @since 1.0.0
     */
    override fun compareTo(other: KoreTextPosition): Int = positionOrder.compare(this, other)

    /**
     * Returns whether [other] is a position with the same [line] and the same [column].
     *
     * @param other the object to compare with, which may be `null`.
     * @return `true` if [other] is an equal position, `false` otherwise.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean =
        other is KoreTextPosition && line == other.line && column == other.column

    /**
     * Returns a hash code derived from [line] and [column].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = HASH_FACTOR * line + column

    /**
     * Returns the position as `line:column`, both counted from one, such as `4:8` for line 3 and column 7.
     *
     * The numbers are computed without overflow, so the largest line, [Int.MAX_VALUE], is shown as `2147483648`.
     *
     * @return the position in the form `line:column`, counted from one.
     * @since 1.0.0
     */
    override fun toString(): String = "${line.toLong() + 1}:${column.toLong() + 1}"
}
