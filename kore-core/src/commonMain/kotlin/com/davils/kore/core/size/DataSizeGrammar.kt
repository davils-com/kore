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

import com.davils.kore.core.Limits
import com.davils.kore.core.outcome.Outcome
import com.davils.kore.core.text.asciiLowercase
import com.davils.kore.core.text.isAsciiDigit

private const val SPACE: String = " "
private const val ZERO: Char = '0'
private const val NO_NUMBER_REASON: String = "does not start with a number of bytes"
private const val LEADING_ZERO_REASON: String = "has a number with a leading zero"
private const val TOO_LARGE_REASON: String = "is larger than the largest data size of ${Long.MAX_VALUE} B"
private const val UNITS_TEXT: String = "B, KiB, MiB, GiB, TiB, PiB or EiB"
private const val AMBIGUOUS_UNIT_REASON: String =
    "uses a unit that is decimal, abbreviated or in the wrong case; write $UNITS_TEXT"
private const val UNKNOWN_UNIT_REASON: String = "has no unit of $UNITS_TEXT after the number"

private val unitFactors: Map<String, Long> = mapOf(
    "B" to 1L,
    "KiB" to DataSize.KIBIBYTE,
    "MiB" to DataSize.MEBIBYTE,
    "GiB" to DataSize.GIBIBYTE,
    "TiB" to DataSize.TEBIBYTE,
    "PiB" to DataSize.PEBIBYTE,
    "EiB" to DataSize.EXBIBYTE,
)

private val ambiguousUnits: Set<String> = setOf(
    "b", "kib", "mib", "gib", "tib", "pib", "eib", "kb", "mb", "gb", "tb", "pb", "eb", "k", "m", "g", "t", "p", "e",
)

internal fun readDataSize(text: String): Outcome<DataSize, String> {
    if (text.length > Limits.MAX_DATA_SIZE_TEXT_LENGTH) {
        return Outcome.Failure("is longer than ${Limits.MAX_DATA_SIZE_TEXT_LENGTH} characters")
    }
    val digits = text.takeWhile(::isAsciiDigit)
    return readParts(digits = digits, unit = text.substring(digits.length).removePrefix(SPACE))
}

private fun readParts(digits: String, unit: String): Outcome<DataSize, String> {
    val problem = numberProblemOf(digits) ?: unitProblemOf(unit)
    if (problem != null) return Outcome.Failure(problem)
    return sizeOf(digits = digits, factor = unitFactors.getValue(unit))
}

private fun numberProblemOf(digits: String): String? {
    if (digits.isEmpty()) return NO_NUMBER_REASON
    return LEADING_ZERO_REASON.takeIf { digits.length > 1 && digits.first() == ZERO }
}

private fun unitProblemOf(unit: String): String? {
    if (unit in unitFactors) return null
    return AMBIGUOUS_UNIT_REASON.takeIf { asciiLowercase(unit) in ambiguousUnits } ?: UNKNOWN_UNIT_REASON
}

private fun sizeOf(digits: String, factor: Long): Outcome<DataSize, String> {
    val count = digits.toLongOrNull()
    val isFitting = count != null && count <= Long.MAX_VALUE / factor
    if (count == null || !isFitting) return Outcome.Failure(TOO_LARGE_REASON)
    return Outcome.Success(DataSize(count * factor))
}
