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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll

private const val ANSWER: Int = 42
private const val LONGEST: Int = 24
private const val BOND: String = "007"

private val asciiNumbers: Arb<String> =
    Arb.list(Arb.element("0", "1", "9", "-", "+", " ", "e", "."), 0..LONGEST).map { parts -> parts.joinToString("") }

class IntegersTest : FunSpec({
    context("parseIntOrNull") {
        test("reads plain and negative integers") {
            parseIntOrNull("42") shouldBe ANSWER
            parseIntOrNull("-42") shouldBe -ANSWER
            parseIntOrNull("0") shouldBe 0
            parseIntOrNull("${Int.MAX_VALUE}") shouldBe Int.MAX_VALUE
            parseIntOrNull("${Int.MIN_VALUE}") shouldBe Int.MIN_VALUE
        }

        test("refuses what Kotlin would read but is no plain ASCII integer") {
            listOf("+1", " 1", "1 ", "1_000", "٣", "１", "", "-", "--1").forEach { text ->
                parseIntOrNull(text).shouldBeNull()
            }
        }

        test("refuses an integer that does not fit") {
            parseIntOrNull("${Int.MAX_VALUE.toLong() + 1}").shouldBeNull()
        }

        test("refuses leading zeros only when told to") {
            parseIntOrNull(BOND) shouldBe BOND.toInt()
            parseIntOrNull(text = BOND, allowLeadingZeros = false).shouldBeNull()
            parseIntOrNull(text = "0", allowLeadingZeros = false) shouldBe 0
            parseIntOrNull(text = "-0", allowLeadingZeros = false) shouldBe 0
        }

        test("refuses text beyond the longest numeric text") {
            parseIntOrNull("0".repeat(Limits.MAX_NUMERIC_TEXT_LENGTH + 1)).shouldBeNull()
        }
    }

    context("parseLongOrNull") {
        test("reads every long back from its text") {
            checkAll(Arb.long()) { value -> parseLongOrNull(value.toString()) shouldBe value }
        }

        test("agrees with toLongOrNull on ASCII text without a plus") {
            checkAll(asciiNumbers) { text ->
                val expected = text.takeUnless { candidate -> candidate.startsWith("+") }?.toLongOrNull()

                parseLongOrNull(text) shouldBe expected
            }
        }

        test("refuses leading zeros only when told to") {
            parseLongOrNull(text = BOND, allowLeadingZeros = false).shouldBeNull()
        }
    }
})
