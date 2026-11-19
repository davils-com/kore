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

package com.davils.kore.core.text

import com.davils.kore.core.Limits
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll

private const val LONGEST: Int = 32
private const val LF: String = "\n"
private const val CR: String = "\r"
private const val NEXT_LINE: String = "\u0085"
private const val LINE_SEPARATOR: String = " "

private val pieces: List<String> = listOf("a", "\n", "\r", "\r\n", "\n\r", " ", NEXT_LINE, LINE_SEPARATOR)

private val texts: Arb<String> = Arb.list(Arb.element(pieces), 0..LONGEST).map { parts -> parts.joinToString("") }

private val oversized: CharSequence = object : CharSequence {
    override val length: Int get() = Limits.MAX_LINE_MAP_LENGTH + 1

    override fun get(index: Int): Char = 'a'

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = "a".repeat(endIndex - startIndex)
}

private fun breakCount(text: String): Int = text.withLineBreaks(LineBreak.Lf).count { character -> character == '\n' }

class LineBreakTest : FunSpec({
    context("firstIn") {
        test("finds the first break of each kind") {
            LineBreak.firstIn("a\nb\r\n") shouldBe LineBreak.Lf
            LineBreak.firstIn("a\r\nb\n") shouldBe LineBreak.CrLf
            LineBreak.firstIn("a\rb\r\n") shouldBe LineBreak.Cr
            LineBreak.firstIn("a\r") shouldBe LineBreak.Cr
        }

        test("finds nothing in a text without breaks") {
            LineBreak.firstIn("one line").shouldBeNull()
            LineBreak.firstIn("a${NEXT_LINE}b${LINE_SEPARATOR}c").shouldBeNull()
        }
    }

    context("withLineBreaks") {
        test("writes every break as the target") {
            val mixed = "a${LF}b${CR}${LF}c${CR}d"

            mixed.withLineBreaks() shouldBe "a${LF}b${LF}c${LF}d"
            mixed.withLineBreaks(LineBreak.CrLf) shouldBe "a${CR}${LF}b${CR}${LF}c${CR}${LF}d"
            mixed.withLineBreaks(LineBreak.Cr) shouldBe "a${CR}b${CR}c${CR}d"
        }

        test("keeps the next line and the separators of Unicode") {
            "a${NEXT_LINE}b$LINE_SEPARATOR".withLineBreaks() shouldBe "a${NEXT_LINE}b$LINE_SEPARATOR"
        }

        test("counts a line feed after a carriage return as part of one break") {
            "\r\n".withLineBreaks() shouldBe "\n"
            "\n\r".withLineBreaks() shouldBe "\n\n"
        }

        test("is idempotent and keeps the number of breaks") {
            checkAll(texts, Arb.enum<LineBreak>()) { text, target ->
                val once = text.withLineBreaks(target)

                once.withLineBreaks(target) shouldBe once
                breakCount(once) shouldBe breakCount(text)
            }
        }

        test("leaves only the target as a break") {
            checkAll(texts, Arb.enum<LineBreak>()) { text, target ->
                val written = text.withLineBreaks(target)
                val firstBreak = LineBreak.firstIn(written)

                if (firstBreak != null) firstBreak shouldBe target
            }
        }

        test("refuses a text longer than the longest indexable before reading it") {
            shouldThrow<IllegalArgumentException> { oversized.withLineBreaks() }
        }
    }
})
