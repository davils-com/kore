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

package com.davils.kore.core

import com.davils.kore.core.annotation.DavilsInternal
import kotlin.math.absoluteValue

private const val HASH_FACTOR: Int = 31
private const val DECIMAL_RADIX: Int = 10
private const val HALF_DIGIT: Int = 5
private const val TWO_DIGITS: Int = 2
private const val ZERO: String = "0"

/**
 * The shortest decimal that reads back as a given finite double, as a sign, significant digits and an exponent.
 *
 * [of] finds the decimal with the fewest significant digits that rounds to the double, and of those the one closest to
 * it, with an even last digit on a tie. That is the decimal the `Number::toString` of ECMAScript and the
 * `Double.toString` of Java 19 and later choose, except that Java writes at least two digits where one is enough,
 * such as `4.9E-324` for [Double.MIN_VALUE], whose shortest decimal is `5E-324`. The result is the same on the JVM and
 * on Wasm, so a library writes a double the same way on both, whatever notation it chooses, plain like XPath or with
 * an exponent like YAML. [toString] and [toPlainString] give two such notations, and both read back as the double
 * through [parseDecimalOrNull].
 *
 * The value of the decimal is `d.ddd × 10^exponent`, where `d.ddd` are the [digits] with a point after the first one.
 * Zero has the digits `0` and the exponent `0`; a negative zero keeps its sign in [isNegative].
 *
 * A decimal is an immutable value and safe to share between threads. Two decimals are equal when their sign, digits
 * and exponent are equal.
 *
 * @property isNegative whether the double is negative or a negative zero.
 * @property digits the significant digits, without leading or trailing zeros, between one and seventeen of them;
 * `0` for zero.
 * @property exponent the power of ten of the first digit, between `-324` and `308`.
 * @since 2.2.0
 */
@DavilsInternal
public class ShortestDecimal private constructor(
    public val isNegative: Boolean,
    public val digits: String,
    public val exponent: Int,
) {
    /**
     * Returns the decimal without an exponent, such as `100`, `1.5`, `0.001` or `-0`, with a point only where a
     * fraction follows.
     *
     * The text is at most 327 characters long, the length of the negative of [Double.MIN_VALUE] written this way.
     *
     * @return the decimal in plain notation, with a leading `-` if [isNegative].
     * @since 2.2.0
     */
    public fun toPlainString(): String {
        val sign = "-".takeIf { isNegative }.orEmpty()
        if (exponent < 0) return sign + "0." + ZERO.repeat(-exponent - 1) + digits
        return sign + plainFromOne()
    }

    /**
     * Returns whether [other] is a decimal with the same sign, digits and exponent.
     *
     * @param other the object to compare with, which may be `null`.
     * @return `true` if [other] is an equal decimal, `false` otherwise.
     * @since 2.2.0
     */
    override fun equals(other: Any?): Boolean {
        if (other !is ShortestDecimal) return false
        return isNegative == other.isNegative && digits == other.digits && exponent == other.exponent
    }

    /**
     * Returns a hash code derived from the sign, the digits and the exponent.
     *
     * @return the hash code.
     * @since 2.2.0
     */
    override fun hashCode(): Int = HASH_FACTOR * (HASH_FACTOR * isNegative.hashCode() + digits.hashCode()) + exponent

    /**
     * Returns the decimal in scientific notation, such as `1E21`, `1.5E-323` or `-0E0`: the first digit, a point and
     * the further digits if there are any, `E` and the exponent.
     *
     * @return the decimal with an exponent, with a leading `-` if [isNegative].
     * @since 2.2.0
     */
    override fun toString(): String {
        val sign = "-".takeIf { isNegative }.orEmpty()
        val fraction = digits.drop(1)
        val point = ".".takeIf { fraction.isNotEmpty() }.orEmpty()
        return "$sign${digits.first()}$point${fraction}E$exponent"
    }

    private fun plainFromOne(): String {
        val pointAt = exponent + 1
        if (pointAt >= digits.length) return digits + ZERO.repeat(pointAt - digits.length)
        return digits.take(pointAt) + "." + digits.drop(pointAt)
    }

    /**
     * Creates the shortest decimal of a finite double.
     *
     * @since 2.2.0
     */
    public companion object {
        /**
         * Returns the shortest decimal that reads back as [value].
         *
         * The work is constant, bounded by the at most 24 characters the platform writes for a double. The function
         * is pure and safe to call from any thread.
         *
         * @param value the double to write.
         * @return the decimal with the fewest significant digits that rounds to [value], the closest of them to
         * [value].
         * @throws IllegalArgumentException if [value] is not a number or infinite.
         * @since 2.2.0
         */
        public fun of(value: Double): ShortestDecimal {
            require(value.isFinite()) { "Only a finite double has a decimal, but the value was $value" }
            val isNegativeZero = value == 0.0 && 1.0 / value < 0.0
            val isNegative = value < 0.0 || isNegativeZero
            val magnitude = value.absoluteValue
            val written = writtenOf(formatDecimal(magnitude), isNegative)
            return shortenedOf(written, magnitude)
        }

        private fun writtenOf(text: String, isNegative: Boolean): ShortestDecimal {
            val exponentAt = text.indexOfFirst { character -> character == 'E' || character == 'e' }
            val mantissaEnd = exponentAt.takeIf { at -> at >= 0 } ?: text.length
            val mantissa = text.take(mantissaEnd)
            val writtenExponent = text.drop(mantissaEnd + 1).removePrefix("+")
            val whole = mantissa.substringBefore('.')
            val fraction = mantissa.substringAfter('.', missingDelimiterValue = "")
            val all = whole + fraction
            val unpadded = all.trimStart('0')
            val significant = unpadded.trimEnd('0')
            if (significant.isEmpty()) return ShortestDecimal(isNegative = isNegative, digits = ZERO, exponent = 0)
            val leadingZeros = all.length - unpadded.length
            val exponent = (writtenExponent.toIntOrNull() ?: 0) + whole.length - 1 - leadingZeros
            return ShortestDecimal(isNegative = isNegative, digits = significant, exponent = exponent)
        }

        private fun shortenedOf(written: ShortestDecimal, magnitude: Double): ShortestDecimal {
            if (written.digits.length != TWO_DIGITS) return written
            val twoDigits = written.digits.toInt()
            val lower = twoDigits / DECIMAL_RADIX
            val upper = lower + 1
            val isUpperNearer = twoDigits % DECIMAL_RADIX >= HALF_DIGIT
            val nearer = oneDigit(written, upper.takeIf { isUpperNearer } ?: lower)
            val farther = oneDigit(written, lower.takeIf { isUpperNearer } ?: upper)
            return listOf(nearer, farther).firstOrNull { candidate -> isValueOf(candidate, magnitude) } ?: written
        }

        private fun oneDigit(written: ShortestDecimal, digit: Int): ShortestDecimal {
            val isCarry = digit == DECIMAL_RADIX
            val carry = 1.takeIf { isCarry } ?: 0
            val shown = 1.takeIf { isCarry } ?: digit
            return ShortestDecimal(
                isNegative = written.isNegative,
                digits = shown.toString(),
                exponent = written.exponent + carry,
            )
        }

        private fun isValueOf(candidate: ShortestDecimal, magnitude: Double): Boolean =
            parseDecimalOrNull("${candidate.digits}e${candidate.exponent}") == magnitude
    }
}
