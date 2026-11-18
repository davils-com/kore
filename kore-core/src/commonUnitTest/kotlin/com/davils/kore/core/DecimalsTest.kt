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

package com.davils.kore.core

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.fixture.hostileText
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

private const val CASES: Int = 500
private const val LONGEST_ARBITRARY_TEXT: Int = 80
private const val NEAR_LARGEST: Double = 7.620464301632361e307
private const val TWELVE_AND_A_HALF: Double = 12.5
private const val HALF_A_THOUSANDTH: Double = -5.0E-4
private const val THOUSAND: Double = 1000.0
private const val TWO_HUNDRED: Double = 200.0
private const val HALF: Double = 0.5

class DecimalsTest : FunSpec({
    context("parseDecimalOrNull") {
        test("reads decimal numbers with a fraction and an exponent") {
            parseDecimalOrNull("12.5") shouldBe TWELVE_AND_A_HALF
            parseDecimalOrNull("-0.5e-3") shouldBe HALF_A_THOUSANDTH
            parseDecimalOrNull("1E3") shouldBe THOUSAND
            parseDecimalOrNull("2e+2") shouldBe TWO_HUNDRED
        }

        test("reads a number without digits before or after the point") {
            parseDecimalOrNull(".5") shouldBe HALF
            parseDecimalOrNull("1.") shouldBe 1.0
        }

        test("keeps the sign of a negative zero") {
            val zero = parseDecimalOrNull("-0") ?: Double.NaN

            val reciprocal = 1.0 / zero

            reciprocal shouldBe Double.NEGATIVE_INFINITY
        }

        test("rounds a value near the largest double to the nearest double") {
            parseDecimalOrNull("7.620464301632361e307") shouldBe NEAR_LARGEST
        }

        test("refuses text that is no ASCII decimal number") {
            val refused = listOf(
                "", ".", "-", "+1", " 1", "1 ", "0x10", "Infinity", "-Infinity", "NaN", "1e", "1e+", "--1",
                "1.2.3", "1_000", "١٢", "1e1.5", "e5",
            )

            refused.forEach { text -> parseDecimalOrNull(text).shouldBeNull() }
        }

        test("refuses a value too large for a double") {
            parseDecimalOrNull("1e309").shouldBeNull()
            parseDecimalOrNull("-1e309").shouldBeNull()
        }

        test("reads a value too small for a double as zero") {
            parseDecimalOrNull("1e-400") shouldBe 0.0
        }

        test("reads text up to the longest decimal text and refuses longer text") {
            val longest = "0." + "1".repeat(Limits.MAX_DECIMAL_TEXT_LENGTH - 2)

            parseDecimalOrNull(longest).shouldNotBeNullOrRefused()
            parseDecimalOrNull(longest + "1").shouldBeNull()
        }

        test("returns null or a finite number for any text") {
            checkAll(CASES, Arb.string(0..LONGEST_ARBITRARY_TEXT)) { text ->
                parseDecimalOrNull(text).shouldBeFiniteOrNull()
            }
            checkAll(CASES, hostileText) { text ->
                parseDecimalOrNull(text).shouldBeFiniteOrNull()
            }
        }
    }
})

private fun Double?.shouldNotBeNullOrRefused() {
    (this != null && isFinite()) shouldBe true
}

private fun Double?.shouldBeFiniteOrNull() {
    (this == null || isFinite()) shouldBe true
}
