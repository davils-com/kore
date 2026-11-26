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

package com.davils.kore.testing

import com.davils.kore.testing.HostileFragments.COMBINING_LONG_SOLIDUS
import com.davils.kore.testing.HostileFragments.ESCAPE
import com.davils.kore.testing.HostileFragments.FORGED_LOG_LINE
import com.davils.kore.testing.HostileFragments.HEBREW_ALEF
import com.davils.kore.testing.HostileFragments.LONE_HIGH_SURROGATE
import com.davils.kore.testing.HostileFragments.LONE_LOW_SURROGATE
import com.davils.kore.testing.HostileFragments.TAG_LATIN_A
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe

private const val PRINTABLE: String = """port 8080 äöü 日本 😀 "quoted" C:\path"""
private const val ZERO_WIDTH_SPACE: String = "\u200B"
private const val NEXT_LINE: String = "\u0085"
private const val HIGH_SURROGATE_NOTATION: String = "U+D800"

private val safeFragments: Set<String> = setOf(
    "a", "Z", "7", ".", "[", "]", "\"", "\\", " ", "ä", "日", "\uD83D\uDE00", COMBINING_LONG_SOLIDUS, HEBREW_ALEF,
)

class SafeToShowTest : FunSpec({
    context("isSafeToShow") {
        test("accepts printable text of every script") {
            isSafeToShow(PRINTABLE) shouldBe true
            isSafeToShow("$HEBREW_ALEF${COMBINING_LONG_SOLIDUS}a") shouldBe true
        }

        test("refuses a control character, a line break and an invisible character") {
            isSafeToShow("a${ESCAPE}b") shouldBe false
            isSafeToShow(FORGED_LOG_LINE) shouldBe false
            isSafeToShow("sec${ZERO_WIDTH_SPACE}ret") shouldBe false
        }

        test("refuses a tab unless tabs are allowed") {
            isSafeToShow("a\tb") shouldBe false
            isSafeToShow("a\tb", allowTabs = true) shouldBe true
        }
    }

    context("unsafeCodePointsIn") {
        test("names every unsafe code point in the order it appears") {
            unsafeCodePointsIn("a${ESCAPE}b${NEXT_LINE}") shouldContainExactly listOf("U+001B", "U+0085")
        }

        test("names each lone surrogate as one code point") {
            unsafeCodePointsIn(LONE_HIGH_SURROGATE) shouldContainExactly listOf(HIGH_SURROGATE_NOTATION)
            unsafeCodePointsIn(LONE_LOW_SURROGATE) shouldContainExactly listOf("U+DC00")
        }

        test("names a high surrogate as one code point wherever its low half is missing") {
            val twice = LONE_HIGH_SURROGATE + LONE_HIGH_SURROGATE

            unsafeCodePointsIn("${LONE_HIGH_SURROGATE}a") shouldContainExactly listOf(HIGH_SURROGATE_NOTATION)
            unsafeCodePointsIn("a$LONE_HIGH_SURROGATE") shouldContainExactly listOf(HIGH_SURROGATE_NOTATION)
            unsafeCodePointsIn(twice) shouldContainExactly listOf(HIGH_SURROGATE_NOTATION, HIGH_SURROGATE_NOTATION)
        }

        test("names a supplementary tag character as one code point") {
            unsafeCodePointsIn(TAG_LATIN_A) shouldContainExactly listOf("U+E0041")
        }

        test("names a tab only when tabs are not allowed") {
            unsafeCodePointsIn("a\tb") shouldContainExactly listOf("U+0009")
            unsafeCodePointsIn("a\tb", allowTabs = true).shouldBeEmpty()
        }

        test("finds nothing in safe text") {
            unsafeCodePointsIn(PRINTABLE).shouldBeEmpty()
        }

        test("finds something unsafe in every fragment that is not plain text") {
            val hostile = HostileFragments.all.filterNot { fragment -> fragment in safeFragments }

            hostile.forEach { fragment -> unsafeCodePointsIn(fragment).shouldNotBeEmpty() }
        }

        test("finds nothing unsafe in the plain fragments") {
            safeFragments.forEach { fragment -> unsafeCodePointsIn(fragment).shouldBeEmpty() }
        }
    }
})
