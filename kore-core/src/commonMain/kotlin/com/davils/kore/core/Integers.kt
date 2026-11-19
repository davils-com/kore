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
import com.davils.kore.core.arguments.conversion.isAsciiInteger

private const val ZERO: Char = '0'
private const val MINUS: String = "-"

/**
 * Reads [text] as an integer that fits an [Int], for the Davils libraries that build on Kore.
 *
 * The text must be ASCII: an optional `-` followed by digits, such as `42` or `-7`. Unlike `String.toIntOrNull` of
 * Kotlin, which also accepts a leading `+` and the digits of other scripts, such as `٣`, everything else is refused:
 * a `+`, white space, digit separators, and every digit outside `0` to `9`. Text longer than
 * [Limits.MAX_NUMERIC_TEXT_LENGTH] is refused before it is read. The function is pure and safe to call from any thread.
 *
 * Marked [DavilsInternal] for the libraries of Davils. The opt-in is a marker for the compiler, not an access
 * boundary.
 *
 * @param text the text to read, which may contain anything.
 * @param allowLeadingZeros whether digits may start with `0`, as in `007`; `true` by default. A single `0` is always
 * accepted.
 * @return the integer, or `null` if [text] is no integer of this form or does not fit an [Int].
 * @since 2.1.0
 */
@DavilsInternal
public fun parseIntOrNull(text: String, allowLeadingZeros: Boolean = true): Int? {
    if (!isStrictInteger(text, allowLeadingZeros)) return null
    return text.toIntOrNull()
}

/**
 * Reads [text] as an integer that fits a [Long], for the Davils libraries that build on Kore.
 *
 * It accepts exactly the text that [parseIntOrNull] accepts, with the range of a [Long].
 *
 * Marked [DavilsInternal] for the libraries of Davils. The opt-in is a marker for the compiler, not an access
 * boundary.
 *
 * @param text the text to read, which may contain anything.
 * @param allowLeadingZeros whether digits may start with `0`, as in `007`; `true` by default. A single `0` is always
 * accepted.
 * @return the integer, or `null` if [text] is no integer of this form or does not fit a [Long].
 * @since 2.1.0
 */
@DavilsInternal
public fun parseLongOrNull(text: String, allowLeadingZeros: Boolean = true): Long? {
    if (!isStrictInteger(text, allowLeadingZeros)) return null
    return text.toLongOrNull()
}

private fun isStrictInteger(text: String, allowLeadingZeros: Boolean): Boolean {
    val digits = text.removePrefix(MINUS)
    val hasLeadingZero = digits.length > 1 && digits.first() == ZERO
    return isAsciiInteger(text) && (allowLeadingZeros || !hasLeadingZero)
}
