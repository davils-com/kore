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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll

private const val START: Int = 2
private const val END: Int = 6
private const val LENGTH: Int = 4
private const val SHIFT: Int = 3
private const val FAR: Int = 9
private const val CASES: Int = 500

class TextRangeTest : FunSpec({
    val range = TextRange(start = START, end = END)

    context("construction") {
        test("refuses a start before the text") {
            shouldThrow<IllegalArgumentException> { TextRange(start = -1, end = END) }
        }

        test("refuses an end before the start") {
            shouldThrow<IllegalArgumentException> { TextRange(start = END, end = START) }
        }

        test("creates an empty range at an offset") {
            TextRange.at(START) shouldBe TextRange(start = START, end = START)
        }

        test("creates a range from a start and a length") {
            TextRange.of(start = START, length = LENGTH) shouldBe range
        }

        test("refuses a negative length and says so") {
            val refusal = shouldThrow<IllegalArgumentException> { TextRange.of(start = START, length = -1) }

            refusal.message shouldContain "negative length"
        }

        test("refuses a length that would end after the last possible offset instead of overflowing") {
            val refusal = shouldThrow<IllegalArgumentException> {
                TextRange.of(start = Int.MAX_VALUE, length = 1)
            }

            refusal.message shouldContain "after the last possible offset"
        }

        test("creates a range that ends exactly at the last possible offset") {
            TextRange.of(start = START, length = Int.MAX_VALUE - START).end shouldBe Int.MAX_VALUE
        }
    }

    context("measurement") {
        test("reports its length") {
            range.length shouldBe LENGTH
        }

        test("is empty only without characters") {
            range.isEmpty shouldBe false
            TextRange.at(START).isEmpty shouldBe true
        }
    }

    context("containment") {
        test("contains its start but not its end") {
            (START in range) shouldBe true
            (END in range) shouldBe false
            (START - 1 in range) shouldBe false
        }

        test("touches both of its ends") {
            range.touches(START) shouldBe true
            range.touches(END) shouldBe true
            range.touches(END + 1) shouldBe false
            range.touches(START - 1) shouldBe false
        }

        test("contains a range that lies inside it") {
            (TextRange(start = START + 1, end = END) in range) shouldBe true
            (TextRange(start = START, end = END + 1) in range) shouldBe false
            (TextRange(start = START - 1, end = END) in range) shouldBe false
        }

        test("intersects a range that shares a character") {
            range.intersects(TextRange(start = END - 1, end = FAR)) shouldBe true
            range.intersects(TextRange(start = END, end = FAR)) shouldBe false
            range.intersects(TextRange(start = 0, end = START)) shouldBe false
        }
    }

    context("derivation") {
        test("covers another range") {
            range.covering(TextRange(start = END, end = FAR)) shouldBe TextRange(start = START, end = FAR)
            range.covering(TextRange(start = 0, end = 1)) shouldBe TextRange(start = 0, end = END)
        }

        test("shifts by a delta") {
            range.shiftedBy(SHIFT) shouldBe TextRange(start = START + SHIFT, end = END + SHIFT)
        }

        test("refuses a shift that would end after the last possible offset instead of overflowing") {
            val refusal = shouldThrow<IllegalArgumentException> { range.shiftedBy(Int.MAX_VALUE) }

            refusal.message shouldContain "after the last possible offset"
        }

        test("refuses a shift that would start before the text") {
            val refusal = shouldThrow<IllegalArgumentException> { range.shiftedBy(-FAR) }

            refusal.message shouldContain "before the text"
        }

        test("either shifts by exactly the delta or refuses, whatever the delta") {
            val offsets = Arb.int(0..Int.MAX_VALUE)
            checkAll(iterations = CASES, genA = offsets, genB = offsets, genC = Arb.int()) { first, second, delta ->
                val original = TextRange(start = minOf(a = first, b = second), end = maxOf(a = first, b = second))
                val expectedStart = original.start.toLong() + delta
                val expectedEnd = original.end.toLong() + delta
                val isRepresentable = expectedStart >= 0 && expectedEnd <= Int.MAX_VALUE

                if (isRepresentable) original.shiftedBy(delta).start.toLong() shouldBe expectedStart
                if (!isRepresentable) shouldThrow<IllegalArgumentException> { original.shiftedBy(delta) }
            }
        }

        test("reads its text") {
            range.textIn("a port") shouldBe "port"
        }

        test("refuses to read beyond the end of the text instead of failing with an index error") {
            val refusal = shouldThrow<IllegalArgumentException> { range.textIn("a p") }

            refusal.message shouldContain "length 3"
        }
    }

    context("order") {
        test("sorts by start and then by end") {
            val ranges = listOf(
                TextRange(start = START, end = END),
                TextRange(start = 0, end = FAR),
                TextRange(start = START, end = START),
            )

            ranges.sorted() shouldBe listOf(
                TextRange(start = 0, end = FAR),
                TextRange(start = START, end = START),
                TextRange(start = START, end = END),
            )
        }

        test("renders as start and end") {
            range.toString() shouldBe "2..6"
        }
    }

    context("value semantics") {
        test("equals a range with the same start and end and shares its hash code") {
            val same = TextRange(start = START, end = END)

            same shouldBe range
            same.hashCode() shouldBe range.hashCode()
        }

        test("differs from a range with another start or end") {
            range shouldNotBe TextRange(start = START, end = FAR)
            range shouldNotBe TextRange(start = 0, end = END)
        }
    }
})
