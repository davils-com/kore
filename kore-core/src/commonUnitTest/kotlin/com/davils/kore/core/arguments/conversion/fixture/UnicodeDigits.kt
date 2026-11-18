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

package com.davils.kore.core.arguments.conversion.fixture

import com.davils.kore.core.text.internal.textOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map

private const val LAST_ASCII: Int = 0x7F
private const val LONGEST_NUMBER: Int = 12
private const val LAST_BASIC_PLANE: Int = 0xFFFF
private const val FIRST_MATHEMATICAL_DIGIT: Int = 0x1D7CE
private const val LAST_MATHEMATICAL_DIGIT: Int = 0x1D7FF

private val basicPlaneDigits: List<Int> = (LAST_ASCII + 1..LAST_BASIC_PLANE).filter { codePoint ->
    codePoint.toChar().category == CharCategory.DECIMAL_DIGIT_NUMBER
}

private val foreignCodePoints: List<Int> = basicPlaneDigits + (FIRST_MATHEMATICAL_DIGIT..LAST_MATHEMATICAL_DIGIT)

private val foreignDigits: List<String> = foreignCodePoints.map { codePoint -> textOf(codePoint) }

private val foreignDigitLists: Arb<List<String>> = Arb.list(Arb.element(foreignDigits), 1..LONGEST_NUMBER)

internal val numbersInForeignDigits: Arb<String> =
    foreignDigitLists.map { digits -> digits.joinToString(separator = "") }
