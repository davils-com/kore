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

import com.davils.kore.core.limit.KoreLimits
import com.davils.kore.core.text.fixture.RepeatedText
import com.davils.kore.core.text.fixture.UnderReportedText
import com.davils.kore.core.text.fixture.hostileText
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import java.lang.reflect.InvocationTargetException
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

private const val CARRIAGE_RETURN: String = "\r"
private const val LINE_FEED: String = "\n"
private const val MIXED: String = "host: a$CARRIAGE_RETURN${LINE_FEED}port: 80${CARRIAGE_RETURN}name: b${LINE_FEED}last"
private const val SECOND_LINE: Int = 9
private const val THIRD_LINE: Int = 18
private const val FOURTH_LINE: Int = 26
private const val LINES: Int = 4
private const val COLUMN: Int = 6
private const val NARROW: Int = 4
private const val CASES: Int = 300
private const val MOST_PIECES: Int = 400
private const val WIDEST_TEST_WINDOW: Int = 10
private const val SMALLEST_STRICT_WIDTH: Int = 3
private const val LONG_LINE: Int = 1 shl 20
private const val LOOKUPS: Int = 20_000
private const val LOOKUP_SPREAD: Int = 1_000
private const val LOOKUP_BUDGET_SECONDS: Int = 2
private const val EMOJI: String = "😀"
private const val PORT_LINE: String = "port: 80"

private val surrogatePieces: Arb<List<String>> =
    Arb.list(Arb.element("a", EMOJI, "\uD800", "\uDC00", "日", "\n", "\r\n", "\r"), 0..MOST_PIECES)

private val surrogateLines: Arb<String> = surrogatePieces.map { pieces -> pieces.joinToString(separator = "") }

private val offsets: Arb<Int> = Arb.int(0..MOST_PIECES)

private val pairedPieces: Arb<List<String>> = Arb.list(Arb.element("a", "b", EMOJI), 0..MOST_PIECES)

private val pairedLine: Arb<String> = pairedPieces.map { pieces -> pieces.joinToString(separator = "") }

class KoreLineMapTest : FunSpec({
    val lines = KoreLineMap.of(MIXED)

    context("lines") {
        test("ends a line at a line feed, a carriage return and both together") {
            lines.lineCount shouldBe LINES
            lines.lineOf(SECOND_LINE) shouldBe 1
            lines.lineOf(THIRD_LINE) shouldBe 2
            lines.lineOf(FOURTH_LINE) shouldBe LINES - 1
        }

        test("counts an empty text as one line") {
            KoreLineMap.of("").lineCount shouldBe 1
        }

        test("starts a last empty line after a final break") {
            KoreLineMap.of("a\n").lineCount shouldBe 2
        }

        test("places the break of a line on that line") {
            lines.lineOf(SECOND_LINE - 1) shouldBe 0
            lines.lineOf(SECOND_LINE - 2) shouldBe 0
        }

        test("refuses an offset outside the text") {
            shouldThrow<IllegalArgumentException> { lines.lineOf(-1) }
            shouldThrow<IllegalArgumentException> { lines.lineOf(MIXED.length + 1) }
        }

        test("reports the content of a line without its break") {
            lines.lineRange(0) shouldBe KoreTextRange(start = 0, end = SECOND_LINE - 2)
            lines.lineRange(1) shouldBe KoreTextRange(start = SECOND_LINE, end = THIRD_LINE - 1)
            lines.lineRange(2) shouldBe KoreTextRange(start = THIRD_LINE, end = FOURTH_LINE - 1)
            lines.lineRange(LINES - 1) shouldBe KoreTextRange(start = FOURTH_LINE, end = MIXED.length)
        }

        test("refuses a line that does not exist") {
            shouldThrow<IllegalArgumentException> { lines.lineRange(LINES) }
        }

        test("counts a lone line feed as a break of one character") {
            KoreLineMap.of("\nb").lineRange(0) shouldBe KoreTextRange(start = 0, end = 0)
        }

        test("indexes a snapshot, so a later change of the indexed builder cannot corrupt it") {
            val builder = StringBuilder("a\nport: 80")
            val map = KoreLineMap.of(builder)
            builder.setLength(0)
            builder.append("x")

            map.lineRange(1) shouldBe KoreTextRange(start = 2, end = SECOND_LINE + 1)
            map.snippet(KoreTextRange(start = 2, end = SECOND_LINE + 1)).line shouldBe PORT_LINE
        }

        test("refuses a text longer than the limit before it copies or scans it") {
            val huge = RepeatedText(length = KoreLimits.MAX_LINE_MAP_LENGTH + 1, character = '\n')

            val refusal = shouldThrow<IllegalArgumentException> { KoreLineMap.of(huge) }

            refusal.message shouldContain "${KoreLimits.MAX_LINE_MAP_LENGTH}"
        }

        test("refuses a text whose length reports less than its content once it has taken the snapshot") {
            val liar = UnderReportedText("x".repeat(KoreLimits.MAX_LINE_MAP_LENGTH + 1))

            val refusal = shouldThrow<IllegalArgumentException> { KoreLineMap.of(liar) }

            refusal.message shouldContain "${KoreLimits.MAX_LINE_MAP_LENGTH + 1}"
        }

        test("checks every instance, even one built through the constructor the compiler adds for its companion") {
            val constructors = KoreLineMap::class.java.declaredConstructors.toList()
            val parameterTypes = constructors.flatMap { constructor -> constructor.parameterTypes.toList() }
            val synthetic = constructors.single { constructor -> constructor.isSynthetic }
            val huge = "x".repeat(KoreLimits.MAX_LINE_MAP_LENGTH + 1)

            val refusal = shouldThrow<InvocationTargetException> { synthetic.newInstance(huge, null) }

            parameterTypes shouldNotContain IntArray::class.java
            refusal.cause.shouldBeInstanceOf<IllegalArgumentException>()
        }

        test("finds the line of every offset, whatever the text") {
            checkAll(CASES, hostileText) { text ->
                val map = KoreLineMap.of(text)

                for (offset in 0..text.length) {
                    val content = map.lineRange(map.lineOf(offset))
                    content.start shouldBeLessThanOrEqual offset
                }
            }
        }
    }

    context("positions") {
        test("turns an offset into a line and a column") {
            lines.positionOf(SECOND_LINE + COLUMN) shouldBe KoreTextPosition(line = 1, column = COLUMN)
        }

        test("turns a position back into its offset") {
            lines.offsetOf(KoreTextPosition(line = 1, column = COLUMN)) shouldBe SECOND_LINE + COLUMN
        }

        test("clamps a column beyond the end of its line") {
            lines.offsetOf(KoreTextPosition(line = 0, column = MIXED.length)) shouldBe SECOND_LINE - 2
        }

        test("clamps the largest column to the end of a later line instead of overflowing") {
            lines.offsetOf(KoreTextPosition(line = 1, column = Int.MAX_VALUE)) shouldBe THIRD_LINE - 1
            lines.offsetOf(KoreTextPosition(line = LINES - 1, column = Int.MAX_VALUE)) shouldBe MIXED.length
        }

        test("counts a surrogate pair as one code point") {
            val text = "a: 😀b"
            val map = KoreLineMap.of(text)

            map.codePointColumnOf(text.length) shouldBe COLUMN - 1
            map.positionOf(text.length).column shouldBe COLUMN
        }

        test("counts a lone low surrogate as a code point of its own") {
            KoreLineMap.of("\uDE00b").codePointColumnOf(2) shouldBe 2
        }

        test("counts code points like the platform does, whatever the text") {
            checkAll(CASES, surrogateLines) { text ->
                val map = KoreLineMap.of(text)

                for (offset in 0..text.length) {
                    val start = map.lineRange(map.lineOf(offset)).start
                    map.codePointColumnOf(offset) shouldBe text.codePointCount(beginIndex = start, endIndex = offset)
                }
            }
        }

        test("counts code points far into a long line without walking the line from its start each time") {
            val text = "a$EMOJI".repeat(LONG_LINE / EMOJI.length)
            val map = KoreLineMap.of(text)

            val elapsed = measureTime {
                repeat(LOOKUPS) { lookup -> map.codePointColumnOf(text.length - lookup % LOOKUP_SPREAD) }
            }

            elapsed shouldBeLessThan LOOKUP_BUDGET_SECONDS.seconds
            map.codePointColumnOf(text.length) shouldBe text.codePointCount(beginIndex = 0, endIndex = text.length)
        }
    }

    context("snippets") {
        test("shows the whole line when it fits") {
            val snippet = lines.snippet(KoreTextRange(start = SECOND_LINE + COLUMN, end = THIRD_LINE - 1))

            snippet shouldBe KoreTextSnippet(
                position = KoreTextPosition(line = 1, column = COLUMN),
                line = PORT_LINE,
                highlight = KoreTextRange(start = COLUMN, end = COLUMN + 2),
                isCutBefore = false,
                isCutAfter = false,
            )
        }

        test("cuts a long line around the range") {
            val snippet = KoreLineMap.of("abcdefghij").snippet(KoreTextRange(start = COLUMN - 1, end = COLUMN), NARROW)

            snippet.line shouldBe "defg"
            snippet.highlight shouldBe KoreTextRange(start = 2, end = 3)
            snippet.isCutBefore shouldBe true
            snippet.isCutAfter shouldBe true
        }

        test("ends the highlight at the end of the line") {
            val snippet = lines.snippet(KoreTextRange(start = SECOND_LINE + COLUMN, end = MIXED.length))

            snippet.highlight shouldBe KoreTextRange(start = COLUMN, end = COLUMN + 2)
        }

        test("points at the end of a line for a range that starts in its break") {
            val snippet = lines.snippet(KoreTextRange(start = SECOND_LINE - 1, end = SECOND_LINE))

            snippet.highlight shouldBe KoreTextRange(start = SECOND_LINE - 2, end = SECOND_LINE - 2)
        }

        test("refuses a width below one") {
            shouldThrow<IllegalArgumentException> { lines.snippet(KoreTextRange.at(0), width = 0) }
        }

        test("refuses a range that ends beyond the text") {
            shouldThrow<IllegalArgumentException> { lines.snippet(KoreTextRange(start = 0, end = MIXED.length + 1)) }
        }

        test("accepts the largest width on a later line instead of overflowing") {
            val range = KoreTextRange(start = SECOND_LINE + COLUMN, end = THIRD_LINE - 1)

            lines.snippet(range, width = Int.MAX_VALUE).line shouldBe PORT_LINE
        }

        test("never shows more than the widest window, whatever width is asked for") {
            val text = "a".repeat(KoreLimits.MAX_SNIPPET_WIDTH * SMALLEST_STRICT_WIDTH)
            val middle = KoreTextRange.at(KoreLimits.MAX_SNIPPET_WIDTH + 1)
            val snippet = KoreLineMap.of(text).snippet(middle, width = Int.MAX_VALUE)

            snippet.line.length shouldBe KoreLimits.MAX_SNIPPET_WIDTH
        }

        test("never cuts a surrogate pair at the edges of its window, unless the range itself starts inside one") {
            val widths = Arb.int(1..WIDEST_TEST_WINDOW)
            checkAll(iterations = CASES, genA = pairedLine, genB = offsets, genC = widths) { text, at, width ->
                val start = at.coerceAtMost(text.length)
                val snippet = KoreLineMap.of(text).snippet(KoreTextRange.at(start), width)
                val first = snippet.line.firstOrNull() ?: 'a'
                val last = snippet.line.lastOrNull() ?: 'a'
                val isStartInsidePair = start in 1..<text.length && text[start].isLowSurrogate()

                first.isLowSurrogate() shouldBe (isStartInsidePair && width == 1)
                last.isHighSurrogate() shouldBe false
            }
        }

        test("starts the window of a range that starts inside a pair at the range, never at the half before it") {
            val text = "a$EMOJI"
            val snippet = KoreLineMap.of(text).snippet(KoreTextRange.at(2), width = 1)

            snippet.line shouldBe EMOJI.substring(1)
            snippet.isCutBefore shouldBe true
        }

        test("keeps its window within the asked width when that holds more than one pair") {
            val widths = Arb.int(SMALLEST_STRICT_WIDTH..WIDEST_TEST_WINDOW)
            checkAll(iterations = CASES, genA = pairedLine, genB = offsets, genC = widths) { text, at, width ->
                val start = at.coerceAtMost(text.length)
                val snippet = KoreLineMap.of(text).snippet(KoreTextRange.at(start), width)

                snippet.line.length shouldBeLessThanOrEqual width
            }
        }

        test("keeps the highlight inside its line, whatever the text, the range and the width") {
            val widths = Arb.int(1..WIDEST_TEST_WINDOW)
            checkAll(
                iterations = CASES,
                genA = hostileText,
                genB = offsets,
                genC = offsets,
                genD = widths,
            ) { text, first, second, width ->
                val start = minOf(a = first, b = second).coerceAtMost(text.length)
                val end = maxOf(a = first, b = second).coerceAtMost(text.length)
                val snippet = KoreLineMap.of(text).snippet(KoreTextRange(start = start, end = end), width)

                snippet.highlight.end shouldBeLessThanOrEqual snippet.line.length
            }
        }
    }
})
