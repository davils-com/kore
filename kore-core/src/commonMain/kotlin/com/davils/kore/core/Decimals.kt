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
import com.davils.kore.core.arguments.conversion.isAsciiDecimal
import com.davils.kore.core.arguments.conversion.parseDecimal

/**
 * Reads [text] as a finite decimal number with the same result on every target, for the Davils libraries that build
 * on Kore.
 *
 * The text must be ASCII: an optional `-`, digits with an optional `.`, and an optional exponent of `e` or `E` with an
 * optional sign and digits, such as `-12.5e-3`, `.5` or `1.`. A leading `+`, white space, `Infinity`, `NaN`,
 * hexadecimal, digit separators and the digits of other scripts are refused, and so is text longer than
 * [Limits.MAX_DECIMAL_TEXT_LENGTH], before anything is parsed. The value is rounded to the nearest double, ties to
 * even, on the JVM and on Wasm alike; the `String.toDouble` of Kotlin/Wasm rounds some values near
 * [Double.MAX_VALUE] to the neighbouring double and is therefore not used. The function is pure and safe to call from
 * any thread.
 *
 * @param text the text to read, which may contain anything.
 * @return the nearest double, or `null` if [text] is no decimal number of this form or its value is too large for a
 * double. A value too small for a double becomes zero with the sign of [text].
 * @since 2.1.0
 */
@DavilsInternal
public fun parseDecimalOrNull(text: String): Double? {
    if (!isAsciiDecimal(text, Limits.MAX_DECIMAL_TEXT_LENGTH)) return null
    return parseDecimal(text).takeIf { value -> value.isFinite() }
}
