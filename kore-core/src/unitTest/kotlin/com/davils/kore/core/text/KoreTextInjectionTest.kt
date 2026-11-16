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

import com.davils.kore.core.text.fixture.hostileText
import com.davils.kore.core.text.fixture.shouldBeSafeToShow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll

private const val CASES: Int = 500
private const val FARTHEST: Int = 2_500
private const val WIDEST: Int = 200
private const val SECRET: String = "hunter2"

private val offsets: Arb<Int> = Arb.int(0..FARTHEST)
private val widths: Arb<Int> = Arb.int(1..WIDEST)
private val lineNumbers: Arb<Int> = Arb.int(0..Int.MAX_VALUE)
private val ranges: Arb<KoreTextRange> = Arb.bind(offsets, offsets) { first, second ->
    KoreTextRange(start = minOf(a = first, b = second), end = maxOf(a = first, b = second))
}

class KoreTextInjectionTest : FunSpec({
    context("a snippet of hostile text") {
        test("renders every line safely, wherever its range lies and however wide it is") {
            checkAll(iterations = CASES, genA = hostileText, genB = ranges, genC = widths) { text, range, width ->
                val rendered = snippetIn(text, range, width).render()

                rendered.shouldBeSafeOnEveryLine()
            }
        }

        test("renders on exactly two lines, so the text cannot start a line of its own") {
            checkAll(iterations = CASES, genA = hostileText, genB = ranges, genC = widths) { text, range, width ->
                val rendered = snippetIn(text, range, width).render()

                rendered.lines() shouldHaveSize 2
            }
        }

        test("renders every line safely once it is masked") {
            checkAll(iterations = CASES, genA = hostileText, genB = ranges, genC = widths) { text, range, width ->
                val masked = snippetIn(text, range, width).masked()

                masked.render().shouldBeSafeOnEveryLine()
            }
        }

        test("shows itself safely in its text form") {
            checkAll(iterations = CASES, genA = hostileText, genB = ranges, genC = widths) { text, range, width ->
                val shown = snippetIn(text, range, width).toString()

                shown.shouldBeSafeToShow()
            }
        }
    }

    context("a snippet made by hand from a hostile line") {
        test("renders every line safely on exactly two lines, wherever its highlight lies") {
            checkAll(iterations = CASES, genA = hostileText, genB = ranges, genC = lineNumbers) { line, range, number ->
                val highlight = range.within(line)
                val snippet = KoreTextSnippet(
                    position = KoreTextPosition(line = number, column = highlight.start),
                    line = line,
                    highlight = highlight,
                    isCutBefore = range.start % 2 == 0,
                    isCutAfter = range.end % 2 == 0,
                )

                val rendered = snippet.render()

                rendered.shouldBeSafeOnEveryLine()
                rendered.lines() shouldHaveSize 2
            }
        }
    }

    context("an edit with a hostile replacement") {
        test("shows itself safely and without its replacement") {
            checkAll(CASES, hostileText) { text ->
                val shown = KoreTextEdit(KoreTextRange.at(0), withSecret(text)).toString()

                shown.shouldBeSafeToShow()
                shown shouldNotContain SECRET
            }
        }
    }

    context("a refusal") {
        test("of overlapping edits never echoes their replacements and is safe to show") {
            checkAll(CASES, hostileText) { text ->
                val replacement = withSecret(text)
                val edits = listOf(
                    KoreTextEdit(KoreTextRange(start = 0, end = 2), replacement),
                    KoreTextEdit(KoreTextRange(start = 1, end = 2), replacement),
                )

                val message = shouldThrow<IllegalArgumentException> { "abc".edited(edits) }.message.orEmpty()

                message.shouldBeSafeToShow()
                message shouldNotContain SECRET
            }
        }

        test("of an edit beyond the text never echoes the text or the replacement and is safe to show") {
            checkAll(CASES, hostileText) { text ->
                val source = withSecret(text)
                val edit = KoreTextEdit(KoreTextRange.at(source.length + 1), source)

                val message = shouldThrow<IllegalArgumentException> { source.edited(listOf(edit)) }.message.orEmpty()

                message.shouldBeSafeToShow()
                message shouldNotContain SECRET
            }
        }

        test("of a highlight outside a hostile line never echoes the line and is safe to show") {
            checkAll(CASES, hostileText) { text ->
                val line = withSecret(text)
                val outside = KoreTextRange.at(line.length + 1)

                val message = shouldThrow<IllegalArgumentException> {
                    KoreTextSnippet(
                        position = KoreTextPosition(line = 0, column = 0),
                        line = line,
                        highlight = outside,
                        isCutBefore = false,
                        isCutAfter = false,
                    )
                }.message.orEmpty()

                message.shouldBeSafeToShow()
                message shouldNotContain SECRET
            }
        }

        test("of a range beyond a hostile text never echoes the text and is safe to show") {
            checkAll(CASES, hostileText) { text ->
                val source = withSecret(text)
                val beyond = KoreTextRange.at(source.length + 1)

                val readMessage = shouldThrow<IllegalArgumentException> { beyond.textIn(source) }.message.orEmpty()
                val lines = KoreLineMap.of(source)
                val mapMessage = shouldThrow<IllegalArgumentException> { lines.snippet(beyond) }.message.orEmpty()

                readMessage.shouldBeSafeToShow()
                mapMessage.shouldBeSafeToShow()
                readMessage shouldNotContain SECRET
                mapMessage shouldNotContain SECRET
            }
        }
    }
})

private fun withSecret(text: String): String = "$text$SECRET"

private fun KoreTextRange.within(text: String): KoreTextRange =
    KoreTextRange(start = start.coerceAtMost(text.length), end = end.coerceAtMost(text.length))

private fun snippetIn(text: String, range: KoreTextRange, width: Int): KoreTextSnippet {
    val lines = KoreLineMap.of(text)
    return lines.snippet(range.within(text), width)
}

private fun String.shouldBeSafeOnEveryLine() {
    lines().forEach { line -> line.shouldBeSafeToShow(allowTabs = true) }
}
