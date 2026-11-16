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

import com.davils.kore.core.text.escape.SafeDisplay
import com.davils.kore.core.text.fixture.CLEAR_SCREEN
import com.davils.kore.core.text.fixture.COMBINING_LONG_SOLIDUS
import com.davils.kore.core.text.fixture.ESCAPE
import com.davils.kore.core.text.fixture.FORGED_LOG_LINE
import com.davils.kore.core.text.fixture.HEBREW_ALEF
import com.davils.kore.core.text.fixture.RIGHT_TO_LEFT_OVERRIDE
import com.davils.kore.core.text.fixture.SAMPLE_VALUE_END
import com.davils.kore.core.text.fixture.SAMPLE_VALUE_START
import com.davils.kore.core.text.fixture.dataClassMembers
import com.davils.kore.core.text.fixture.snippetOf
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll

private const val TENTH_LINE: Int = 9
private const val TAB: String = "\t"
private const val SECRET: String = "hunter"
private const val MASKED_END: Int = 10
private const val CASES: Int = 200
private const val LONGEST_SECRET: Int = 400
private const val PADDING: Int = 300
private const val ESCAPED_INDENT: Int = 16
private const val ESCAPE_WIDTH: Int = 8
private const val HIGH_HALF: String = "\uD83D"
private const val LOW_HALF: String = "\uDE00"
private const val CARRIAGE_RETURN: String = "\r"
private const val TABBED_KEY: String = "key:"
private const val RTL_VALUE_START: Int = 9
private const val RTL_VALUE_END: Int = 13
private const val MOST_PIECES: Int = 12
private const val WIDEST: Int = 10

private val pieces: List<String> =
    listOf("a", "日", "$HIGH_HALF$LOW_HALF", HIGH_HALF, LOW_HALF, "\r\n", "\n", "\t", CARRIAGE_RETURN)

private val texts: Arb<String> = Arb.list(Arb.element(pieces), 0..MOST_PIECES).map { parts -> parts.joinToString("") }

private val prefixes: Arb<String> = texts.map(::withoutFinalCarriageReturn)

private val secretPairs: Arb<Pair<String, String>> = Arb.bind(texts, texts) { first, second -> first to second }

class KoreTextSnippetTest : FunSpec({
    val snippet = snippetOf()

    context("construction") {
        test("refuses a highlight outside the line") {
            shouldThrow<IllegalArgumentException> { snippetOf(line = "port") }
        }

        test("names only the highlight and the length of the line when it refuses, never the line") {
            val refusal = shouldThrow<IllegalArgumentException> { snippetOf(line = SECRET) }

            refusal.message shouldContain "6..12"
            refusal.message shouldContain "length 6"
            refusal.message shouldNotContain SECRET
        }
    }

    context("rendering") {
        test("underlines the highlight below the line") {
            snippet.render() shouldBe rendered("1 | port: eighty", "  |       ^^^^^^")
        }

        test("marks the cut ends of a line") {
            val cut = snippetOf(isCutBefore = true, isCutAfter = true)

            cut.render() shouldBe rendered("1 | ...port: eighty...", "  |          ^^^^^^")
        }

        test("points with a single caret at an empty highlight") {
            val empty = snippetOf(highlight = KoreTextRange.at(SAMPLE_VALUE_END))

            empty.render() shouldBe rendered("1 | port: eighty", "  |             ^")
        }

        test("keeps tabs so that the caret stays aligned") {
            val tabbed = snippetOf(line = "\tport: eighty", highlight = KoreTextRange(start = 1, end = 2))

            tabbed.render() shouldBe rendered("1 | ${TAB}port: eighty", "  | $TAB^")
        }

        test("underlines every character of a highlight that holds a tab, the tab included") {
            val tabbed = snippetOf(line = "a${TAB}b", highlight = KoreTextRange(start = 0, end = 3))

            tabbed.render() shouldBe rendered("""1 | a\tb""", "  | ^^^^")
        }

        test("points at a highlighted tab with carets below its escape") {
            val line = "$TABBED_KEY${TAB}value"
            val tabbed = snippetOf(line = line, highlight = KoreTextRange.of(start = TABBED_KEY.length, length = 1))

            tabbed.render() shouldBe rendered("""1 | key:\tvalue""", "  | ${" ".repeat(TABBED_KEY.length)}^^")
        }

        test("shows a combining mark that starts the line as an escape, so it cannot change the gutter") {
            val line = "${COMBINING_LONG_SOLIDUS}5"
            val combining = snippetOf(line = line, highlight = KoreTextRange(start = 1, end = 2))

            combining.render() shouldBe rendered("""1 | \u{0338}5""", "  | ${" ".repeat(ESCAPE_WIDTH)}^")
        }

        test("shows right-to-left letters as they are and escapes the controls that would override the order") {
            val line = "port = $HEBREW_ALEF 8080$RIGHT_TO_LEFT_OVERRIDE"
            val bidi = snippetOf(line = line, highlight = KoreTextRange(start = RTL_VALUE_START, end = RTL_VALUE_END))

            bidi.render() shouldBe rendered(
                "1 | port = $HEBREW_ALEF 8080\\u{202E}",
                "  | ${" ".repeat(RTL_VALUE_START)}^^^^",
            )
        }

        test("widens the gutter for a line number with more digits") {
            val later = snippetOf(position = KoreTextPosition(line = TENTH_LINE, column = SAMPLE_VALUE_START))

            later.render() shouldBe rendered("10 | port: eighty", "   |       ^^^^^^")
        }

        test("counts the last possible line from one without overflowing") {
            val last = snippetOf(position = KoreTextPosition(line = Int.MAX_VALUE, column = SAMPLE_VALUE_START))

            last.render() shouldBe rendered("2147483648 | port: eighty", "           |       ^^^^^^")
        }

        test("shows a terminal control sequence as escapes and points at the escaped line") {
            val line = "key: ${CLEAR_SCREEN}x"
            val hostile = snippetOf(line = line, highlight = KoreTextRange(start = line.length - 1, end = line.length))

            hostile.render() shouldBe rendered("""1 | key: \u{001B}[2Jx""", "  | ${" ".repeat(ESCAPED_INDENT)}^")
        }

        test("underlines every character of an escaped highlight") {
            val hostile = snippetOf(line = "a${ESCAPE}b", highlight = KoreTextRange(start = 1, end = 2))

            hostile.render() shouldBe rendered("""1 | a\u{001B}b""", "  |  ${"^".repeat(ESCAPE_WIDTH)}")
        }

        test("keeps a forged log line on the line of the snippet") {
            val line = "user=bob$FORGED_LOG_LINE"
            val hostile = snippetOf(line = line, highlight = KoreTextRange(start = 0, end = line.length))

            hostile.render().lines() shouldHaveSize 2
        }
    }

    context("masking") {
        test("hides the highlighted characters behind the one fixed mask") {
            val masked = snippet.masked()

            masked.line shouldBe "port: ${SafeDisplay.REDACTED}"
            masked.highlight shouldBe KoreTextRange(start = SAMPLE_VALUE_START, end = MASKED_END)
        }

        test("does not reveal the length of what it hides") {
            val short = snippetOf(line = "port: 8", highlight = KoreTextRange(start = SAMPLE_VALUE_START, end = 7))

            short.masked().render() shouldBe snippet.masked().render()
        }

        test("hides even an empty highlight, so an empty secret looks like any other") {
            val empty = snippetOf(highlight = KoreTextRange.at(SAMPLE_VALUE_START))

            empty.masked() shouldBe snippet.masked()
        }

        test("leaves out what follows the highlight and marks the line as cut there") {
            val masked = snippetOf(line = "port: eighty # ops").masked()

            masked.render() shouldBe rendered("1 | port: ****...", "  |       ^^^^")
        }

        test("builds the masked copy from the text before the secret alone, even at a width of one") {
            val completingPair = maskedSecret(prefix = "a$HIGH_HALF", secret = "${LOW_HALF}rest", width = 1)
            val plain = maskedSecret(prefix = "a$HIGH_HALF", secret = "rest", width = 1)

            completingPair shouldBe plain
        }

        test("depends only on the text before the secret, whatever the secret and the width") {
            val widths = Arb.int(1..WIDEST)
            checkAll(iterations = CASES, genA = prefixes, genB = secretPairs, genC = widths) { prefix, secrets, width ->
                maskedSecret(prefix, secrets.first, width) shouldBe maskedSecret(prefix, secrets.second, width)
            }
        }

        test("reveals only that a secret starts with a line feed that ends a carriage return before it") {
            val completingBreak = maskedSecret(prefix = "a$CARRIAGE_RETURN", secret = "\nrest", width = WIDEST)
            val plain = maskedSecret(prefix = "a$CARRIAGE_RETURN", secret = "rest", width = WIDEST)

            completingBreak shouldNotBe plain
            completingBreak.render() shouldBe rendered("1 | a****...", "  |  ^^^^")
        }

        test("renders the same for a secret of any length at the end of a long line") {
            val padding = "x".repeat(PADDING)
            val reference = maskedSnippetOfSecret(padding, 1)

            checkAll(CASES, Arb.int(1..LONGEST_SECRET)) { length ->
                maskedSnippetOfSecret(padding, length) shouldBe reference
            }
        }
    }

    context("text form and value semantics") {
        test("shows its line quoted and escaped") {
            val hostile = snippetOf(line = "port: e${ESCAPE}ighty")

            hostile.toString() shouldBe
                """KoreTextSnippet(position=1:7, line="port: e\u{001B}ighty", highlight=6..12, """ +
                "isCutBefore=false, isCutAfter=false)"
        }

        test("equals a snippet with the same parts and shares its hash code") {
            val same = snippetOf()

            same shouldBe snippet
            same.hashCode() shouldBe snippet.hashCode()
            snippet shouldNotBe snippetOf(isCutAfter = true)
            snippet shouldNotBe snippetOf(line = "port: eighty!")
        }

        test("is a plain class without copy or component functions") {
            KoreTextSnippet::class.java.dataClassMembers().shouldBeEmpty()
        }
    }
})

private fun rendered(vararg lines: String): String = lines.joinToString("\n")

private fun maskedSnippetOfSecret(padding: String, length: Int): String {
    val text = "${padding}password: ${"s".repeat(length)}"
    val secret = KoreTextRange.of(start = text.length - length, length = length)
    val snippet = KoreLineMap.of(text).snippet(secret)
    return snippet.masked().render()
}

private fun maskedSecret(prefix: String, secret: String, width: Int): KoreTextSnippet {
    val range = KoreTextRange.of(start = prefix.length, length = secret.length)
    val snippet = KoreLineMap.of(prefix + secret).snippet(range, width)
    return snippet.masked()
}

private fun withoutFinalCarriageReturn(text: String): String {
    if (!text.endsWith(CARRIAGE_RETURN)) return text
    return "${text}a"
}
