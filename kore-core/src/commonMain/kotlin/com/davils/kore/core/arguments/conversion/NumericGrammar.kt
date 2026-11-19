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

package com.davils.kore.core.arguments.conversion

import com.davils.kore.core.Limits
import com.davils.kore.core.text.isAsciiDigit

private const val MINUS: Char = '-'
private const val PLUS: Char = '+'
private const val DECIMAL_POINT: Char = '.'
private const val INTEGER_REASON: String = "is not a valid integer"
private const val INTEGER_RANGE_REASON: String = "is too large or too small for an integer"
private const val LONG_REASON: String = "is not a valid long"
private const val LONG_RANGE_REASON: String = "is too large or too small for a long"
private const val DECIMAL_REASON: String = "is not a valid decimal number"
private const val DECIMAL_RANGE_REASON: String = "is too large or too small for a decimal number"

internal fun isAsciiInteger(text: String): Boolean {
    if (text.length > Limits.MAX_NUMERIC_TEXT_LENGTH) return false
    return isDigits(withoutSign(text))
}

internal fun isAsciiDecimal(text: String, maxLength: Int = Limits.MAX_NUMERIC_TEXT_LENGTH): Boolean {
    if (text.length > maxLength) return false
    return isUnsignedDecimal(withoutSign(text))
}

internal fun readInt(raw: String): Int {
    if (!isAsciiInteger(raw)) throw ConversionException(INTEGER_REASON)
    return raw.toIntOrNull() ?: throw ConversionException(INTEGER_RANGE_REASON)
}

internal fun readLong(raw: String): Long {
    if (!isAsciiInteger(raw)) throw ConversionException(LONG_REASON)
    return raw.toLongOrNull() ?: throw ConversionException(LONG_RANGE_REASON)
}

internal fun readDouble(raw: String): Double {
    if (!isAsciiDecimal(raw)) throw ConversionException(DECIMAL_REASON)
    val value = parseDecimal(raw)
    if (!value.isFinite()) throw ConversionException(DECIMAL_RANGE_REASON)
    return value
}

private fun withoutSign(text: String): String = text.removePrefix(MINUS.toString())

private fun isDigits(text: String): Boolean = text.isNotEmpty() && text.all(::isAsciiDigit)

private fun isUnsignedDecimal(unsigned: String): Boolean {
    val exponentAt = unsigned.indexOfFirst { character -> character == 'e' || character == 'E' }
    if (exponentAt < 0) return isMantissa(unsigned)
    return isMantissa(unsigned.substring(startIndex = 0, endIndex = exponentAt)) &&
        isExponent(unsigned.substring(exponentAt + 1))
}

private fun isMantissa(text: String): Boolean {
    val whole = text.substringBefore(DECIMAL_POINT)
    val fraction = text.substringAfter(DECIMAL_POINT, missingDelimiterValue = "")
    val hasDigits = whole.isNotEmpty() || fraction.isNotEmpty()
    return hasDigits && whole.all(::isAsciiDigit) && fraction.all(::isAsciiDigit)
}

private fun isExponent(text: String): Boolean {
    val isSigned = text.startsWith(PLUS) || text.startsWith(MINUS)
    val digits = text.drop(1).takeIf { isSigned } ?: text
    return isDigits(digits)
}
