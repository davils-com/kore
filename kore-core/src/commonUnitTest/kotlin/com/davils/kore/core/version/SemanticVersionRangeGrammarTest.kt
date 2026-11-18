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

package com.davils.kore.core.version

import com.davils.kore.core.Limits
import com.davils.kore.core.text.fixture.hostileText
import com.davils.kore.core.text.fixture.shouldBeSafeToShow
import com.davils.kore.core.version.fixture.range
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

private const val CASES: Int = 500
private const val FUZZ_CASES: Int = 5_000
private const val LONGEST_FUZZ: Int = 32
private const val RANGE_ALPHABET: String = "0123456789.-+xX*^~<>=|[](), \trc"
private const val MOST: Int = Limits.MAX_RANGE_COMPARATORS
private const val TOO_MANY_MESSAGE: String = "Not a version range: it has more than $MOST comparators"
private const val SPACE: String = " "
private const val ALTERNATIVE_SEPARATOR: String = " || "
private const val LONG_LABEL_LENGTH: Int = 240
private const val CARET_LINES: Int = 4
private const val BETWEEN: String = ">=1.2.3 <2"
private const val CARET: String = "^1.2"

private val rangeLike: Arb<String> = Arb.string(
    0..LONGEST_FUZZ,
    Arb.element(RANGE_ALPHABET.toList()).map { character -> Codepoint(character.code) },
)

private fun comparators(count: Int): String = List(count) { ">=1" }.joinToString(SPACE)

private fun alternatives(count: Int): String {
    val releases = List(count) { index -> "${index + 1}.0.0" }
    return releases.joinToString(ALTERNATIVE_SEPARATOR)
}

private fun intervals(count: Int): String = List(count) { index -> "[${index + 1}]" }.joinToString(",")

class SemanticVersionRangeGrammarTest : FunSpec({
    context("the comparator limit") {
        test("reads as many comparators as the limit allows") {
            SemanticVersionRange.parseOrNull(comparators(MOST)).shouldNotBeNull()
            SemanticVersionRange.parseOrNull(alternatives(MOST)).shouldNotBeNull()
            SemanticVersionRange.parse(intervals(MOST)).intervals.size shouldBe MOST
        }

        test("refuses one comparator more, saying why") {
            shouldThrow<IllegalArgumentException> { range(comparators(MOST + 1)) }.message shouldBe TOO_MANY_MESSAGE
        }

        test("counts the comparators of every alternative together") {
            val half = comparators(MOST / 2)
            val written = "$half$ALTERNATIVE_SEPARATOR$half$ALTERNATIVE_SEPARATOR>=1"

            shouldThrow<IllegalArgumentException> { range(written) }.message shouldBe TOO_MANY_MESSAGE
            shouldThrow<IllegalArgumentException> { range(alternatives(MOST + 1)) }.message shouldBe TOO_MANY_MESSAGE
        }

        test("counts every interval of an interval list") {
            shouldThrow<IllegalArgumentException> { range(intervals(MOST + 1)) }.message shouldBe TOO_MANY_MESSAGE
        }
    }

    context("the length") {
        test("is checked before the text is scanned for anything else") {
            val blank = SPACE.repeat(Limits.MAX_RANGE_LENGTH + 1)

            shouldThrow<IllegalArgumentException> { range(blank) }.message shouldBe
                "Not a version range: it is longer than ${Limits.MAX_RANGE_LENGTH} characters"
        }

        test("of the canonical form is kept, so that a range read from text always reads back from its own text") {
            val label = "a".repeat(LONG_LABEL_LENGTH)
            val written = List(CARET_LINES) { index -> "^${index + 1}.0.0-$label" }.joinToString(ALTERNATIVE_SEPARATOR)

            val failure = shouldThrow<IllegalArgumentException> { range(written) }

            withClue("the text itself fits") { (written.length <= Limits.MAX_RANGE_LENGTH) shouldBe true }
            failure.message shouldBe "Not a version range: it renders to more than $MOST intervals or " +
                "${Limits.MAX_RANGE_LENGTH} characters"
        }
    }

    context("problems") {
        test("are reported by the first one found") {
            shouldThrow<IllegalArgumentException> { range("[1.x,2) [3,4) ||") }.message shouldBe
                "Not a version range: it has a bound that is not a version: it must start with three " +
                "dot-separated numbers, such as 1.4.0"
        }
    }

    context("whitespace") {
        test("spaces and tabs separate the parts of a range") {
            range("\t>=1.2.3\t<2 ") shouldBe range(BETWEEN)
            range("^1.2\t||\t^2") shouldBe range("^1.2 || ^2")
            range("[1.2,\t2.0)\t, [3,4)") shouldBe range("[1.2, 2.0), [3, 4)")
        }

        test("any other whitespace is refused rather than trimmed away") {
            val refused = listOf(
                " $CARET",
                "$CARET\n",
                "\r$CARET",
                ">=1.2.3　<2",
                "[1.2, 2.0)",
                "1.2.3 - 2",
                "$CARET\u000B||\u000C^2",
            )
            for (text in refused) {
                withClue(text.toList().map(Char::code)) { SemanticVersionRange.parseOrNull(text).shouldBeNull() }
            }
        }
    }

    context("operators") {
        test("an operator may stand apart from its version") {
            range(">= 1.2.3 < 2") shouldBe range(BETWEEN)
        }

        test("two operators are never glued into one") {
            for (text in listOf("> = 1.2", "> =1.2", "< = 1.2", "= > 1.2", "~ ^1.2", ">= =1.2")) {
                withClue(text) {
                    shouldThrow<IllegalArgumentException> { range(text) }.message shouldBe
                        "Not a version range: it has an operator with no version after it"
                }
            }
        }
    }

    context("the empty range") {
        test("reads back from its own text") {
            val none = SemanticVersionRange.NONE

            SemanticVersionRange.parse(none.toString()) shouldBe none
            SemanticVersionRange.parse(" []\t") shouldBe none
            val read = SemanticVersionRange.parse("[]")

            read.isEmpty shouldBe true
        }

        test("is only written on its own") {
            for (text in listOf("[ ]", "[] || ^1", "^1 || []", "[],[1]", "[[]]")) {
                withClue(text) { SemanticVersionRange.parseOrNull(text).shouldBeNull() }
            }
        }
    }

    context("text made of the characters of a range") {
        test("is either refused or read to a range that reads back from its own text") {
            var read = 0
            checkAll(FUZZ_CASES, rangeLike) { text ->
                val failure = runCatching { SemanticVersionRange.parse(text) }.exceptionOrNull()
                val range = SemanticVersionRange.parseOrNull(text)

                failure?.shouldBeInstanceOf<IllegalArgumentException>()
                if (range != null) read++
                if (range != null) withClue(text) { SemanticVersionRange.parse(range.toString()) shouldBe range }
            }

            withClue("the generated texts must exercise reading") { (read > 0) shouldBe true }
        }
    }

    context("hostile text") {
        test("is refused with a reason that is safe to show, and never makes reading throw anything else") {
            checkAll(CASES, hostileText) { text ->
                val written = "$CARET || $text"
                val failure = runCatching { SemanticVersionRange.parse(written) }.exceptionOrNull()

                (SemanticVersionRange.parseOrNull(written) == null) shouldBe (failure != null)
                if (failure != null) {
                    failure.shouldBeInstanceOf<IllegalArgumentException>()
                    failure.message.orEmpty().shouldBeSafeToShow()
                }
            }
        }
    }
})
