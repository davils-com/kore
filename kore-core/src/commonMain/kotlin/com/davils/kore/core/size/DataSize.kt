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

package com.davils.kore.core.size

import kotlin.jvm.JvmInline

private const val BEYOND_LARGEST: String = "exceeds the largest data size of ${Long.MAX_VALUE} B"

/**
 * An amount of data in bytes, from zero up to [Long.MAX_VALUE] bytes.
 *
 * A size is never negative and arithmetic on sizes is exact: a result beyond [Long.MAX_VALUE] bytes throws an
 * [ArithmeticException], a result below zero an [IllegalArgumentException]. Nothing wraps around, nothing is clamped
 * and nothing is rounded. Sizes compare and are equal by their number of bytes. A size is an immutable value and safe
 * to share between threads.
 *
 * The units are binary: a kibibyte is 1024 bytes. Sizes are usually built with the extension properties such as
 * `64.kibibytes`, and [toString] shows them in the largest unit that divides them exactly.
 *
 * @property bytes the number of bytes, zero or more.
 * @throws IllegalArgumentException if [bytes] is negative.
 * @since 1.0.0
 */
@JvmInline
public value class DataSize(public val bytes: Long) : Comparable<DataSize> {
    init {
        require(bytes >= 0L) { "A data size cannot be negative, but was $bytes bytes" }
    }

    /**
     * Compares this size with [other] by their number of bytes.
     *
     * @param other the size to compare with.
     * @return a negative number, zero or a positive number as this size is smaller than, equal to or larger than
     * [other].
     * @since 1.0.0
     */
    override fun compareTo(other: DataSize): Int = bytes.compareTo(other.bytes)

    /**
     * Adds [other] to this size, exactly as [Math.addExact] adds two numbers.
     *
     * @param other the size to add.
     * @return the sum of both sizes.
     * @throws ArithmeticException if the sum exceeds [Long.MAX_VALUE] bytes. The message names both sizes and the
     * largest data size.
     * @since 1.0.0
     */
    public operator fun plus(other: DataSize): DataSize {
        if (other.bytes > Long.MAX_VALUE - bytes) throw ArithmeticException("$this plus $other $BEYOND_LARGEST")
        return DataSize(bytes + other.bytes)
    }

    /**
     * Subtracts [other] from this size.
     *
     * @param other the size to subtract.
     * @return the difference of both sizes, zero or more.
     * @throws IllegalArgumentException if [other] is larger than this size, because a size cannot be negative. The
     * message names both sizes.
     * @since 1.0.0
     */
    public operator fun minus(other: DataSize): DataSize {
        require(other.bytes <= bytes) { "$this minus $other is below zero, and a data size cannot be negative" }
        return DataSize(bytes - other.bytes)
    }

    /**
     * Multiplies this size by [factor], exactly as [Math.multiplyExact] multiplies two numbers.
     *
     * @param factor how many times this size is taken, zero or more.
     * @return the product, [ZERO] if [factor] is zero.
     * @throws IllegalArgumentException if [factor] is negative, because a size cannot be negative.
     * @throws ArithmeticException if the product exceeds [Long.MAX_VALUE] bytes. The message names the size, the
     * factor and the largest data size.
     * @since 1.0.0
     */
    public operator fun times(factor: Long): DataSize {
        require(factor >= 0L) { "A data size cannot be negative, so it cannot be multiplied by $factor" }
        val isRepresentable = factor == 0L || bytes <= Long.MAX_VALUE / factor
        if (!isRepresentable) throw ArithmeticException("$this times $factor $BEYOND_LARGEST")
        return DataSize(bytes * factor)
    }

    /**
     * Multiplies this size by [factor], exactly as [Math.multiplyExact] multiplies two numbers.
     *
     * @param factor how many times this size is taken, zero or more.
     * @return the product, [ZERO] if [factor] is zero.
     * @throws IllegalArgumentException if [factor] is negative, because a size cannot be negative.
     * @throws ArithmeticException if the product exceeds [Long.MAX_VALUE] bytes. The message names the size, the
     * factor and the largest data size.
     * @since 1.0.0
     */
    public operator fun times(factor: Int): DataSize = times(factor.toLong())

    /**
     * Returns the size in the largest of the units GiB, MiB and KiB that divides it exactly, such as `64 KiB` or
     * `3 GiB`, and in bytes, such as `1536 B` or `0 B`, when none divides it.
     *
     * The text holds only digits, a space and the unit, so it is safe to show and to log.
     *
     * @return the size as text.
     * @since 1.0.0
     */
    override fun toString(): String {
        val unit = UNITS.firstOrNull { (factor, _) -> bytes != 0L && bytes % factor == 0L } ?: return "$bytes B"
        return "${bytes / unit.first} ${unit.second}"
    }

    /**
     * The binary units of data sizes and the empty size.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The number of bytes in a kibibyte, 1024.
         *
         * @since 1.0.0
         */
        public const val KIBIBYTE: Long = 1024L

        /**
         * The number of bytes in a mebibyte, 1024 kibibytes or 1 048 576 bytes.
         *
         * @since 1.0.0
         */
        public const val MEBIBYTE: Long = KIBIBYTE * KIBIBYTE

        /**
         * The number of bytes in a gibibyte, 1024 mebibytes or 1 073 741 824 bytes.
         *
         * @since 1.0.0
         */
        public const val GIBIBYTE: Long = MEBIBYTE * KIBIBYTE

        /**
         * The size of nothing, zero bytes.
         *
         * @since 1.0.0
         */
        public val ZERO: DataSize = DataSize(0L)

        private val UNITS: List<Pair<Long, String>> = listOf(
            GIBIBYTE to "GiB",
            MEBIBYTE to "MiB",
            KIBIBYTE to "KiB",
        )
    }
}
