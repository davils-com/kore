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

package com.davils.kore.core.text

import com.davils.kore.core.Limits
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.fixture.ESCAPE
import com.davils.kore.core.text.fixture.FORGED_LOG_LINE
import com.davils.kore.core.text.fixture.RIGHT_TO_LEFT_OVERRIDE
import com.davils.kore.core.text.fixture.hostileText
import com.davils.kore.core.text.fixture.shouldBeSafeToShow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll

private const val CASES: Int = 500
private const val SHORT: Int = 8
private const val MARK_ROOM: Int = 24
private const val PRINTABLE: String = "port 8080 äöü 日本"
private const val LAST_CODE_POINT: Int = 0x10FFFF

class SafeTextTest : FunSpec({
    context("text") {
        test("keeps printable text as it is") {
            SafeText.text(PRINTABLE) shouldBe PRINTABLE
        }

        test("writes a line break as an escape, so a value cannot forge a line of a log") {
            SafeText.text(FORGED_LOG_LINE) shouldStartWith "\\n2026-11-16"
        }

        test("shortens a long value to the asked length and marks what it left out") {
            val shown = SafeText.text("a".repeat(SHORT * 2), maxLength = SHORT)

            shown shouldBe "a".repeat(SHORT) + "…(+$SHORT chars)"
        }

        test("shows hostile text safely and within the asked length") {
            checkAll(CASES, hostileText, Arb.int(0..Limits.MAX_MESSAGE_LENGTH)) { value, maxLength ->
                val shown = SafeText.text(value, maxLength)

                shown.shouldBeSafeToShow()
                shown.length shouldBeLessThanOrEqual maxLength + MARK_ROOM
            }
        }

        test("refuses a negative length") {
            shouldThrow<IllegalArgumentException> { SafeText.text(PRINTABLE, maxLength = -1) }
        }

        test("refuses a length above the longest message") {
            shouldThrow<IllegalArgumentException> {
                SafeText.text(PRINTABLE, maxLength = Limits.MAX_MESSAGE_LENGTH + 1)
            }
        }
    }

    context("quoted") {
        test("puts the value in quotes and escapes its quotes and backslashes") {
            SafeText.quoted("""say "hi" \ bye""") shouldBe """"say \"hi\" \\ bye""""
        }

        test("shows hostile text safely inside its quotes") {
            checkAll(CASES, hostileText) { value ->
                val shown = SafeText.quoted(value)

                shown.shouldBeSafeToShow()
                shown shouldStartWith "\""
            }
        }

        test("marks what it left out after the closing quote") {
            SafeText.quoted("b".repeat(SHORT * 2), maxLength = SHORT) shouldEndWith "\"…(+$SHORT chars)"
        }

        test("refuses a length outside the allowed range") {
            shouldThrow<IllegalArgumentException> { SafeText.quoted(PRINTABLE, maxLength = -1) }
            shouldThrow<IllegalArgumentException> {
                SafeText.quoted(PRINTABLE, maxLength = Limits.MAX_MESSAGE_LENGTH + 1)
            }
        }
    }

    context("isSafe") {
        test("accepts a printable code point") {
            SafeText.isSafe('a'.code) shouldBe true
        }

        test("refuses control characters and bidirectional overrides") {
            SafeText.isSafe(ESCAPE.single().code) shouldBe false
            SafeText.isSafe(RIGHT_TO_LEFT_OVERRIDE.single().code) shouldBe false
        }

        test("refuses a value outside the range of code points") {
            SafeText.isSafe(-1) shouldBe false
            SafeText.isSafe(LAST_CODE_POINT + 1) shouldBe false
        }
    }

    context("REDACTED") {
        test("hides a value without revealing its length") {
            SafeText.REDACTED shouldBe "****"
            SafeText.text(SafeText.REDACTED) shouldNotContain "\\"
        }
    }
})
