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

import com.davils.kore.core.text.fixture.RepeatedText
import com.davils.kore.core.text.fixture.dataClassMembers
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

private const val TEXT: String = "port: 80"
private const val ALPHABET: String = "abcdefghijklmnop"
private const val SECRET: String = "hunter2"
private const val LISTEN: String = "listen"
private const val VALUE_START: Int = 6
private const val CASES: Int = 300
private const val LONGEST: Int = 12
private const val MOST_CUTS: Int = 8
private const val INSIDE: Int = 3
private const val INSERTED: Int = 2
private const val CHUNK: Int = 1 shl 20
private const val CHUNKS_IN_LARGEST_LENGTH: Int = 2047
private const val CHUNKS_ABOVE_BOUND: Int = 1024
private const val LONGEST_EDITED_TEXT: Int = 1_073_741_819

class TextEditingTest : FunSpec({
    context("an edit") {
        test("knows how much it changes the length") {
            TextEdit(TextRange(start = VALUE_START, end = TEXT.length), "8080").delta shouldBe 2
        }

        test("knows whether it inserts or deletes") {
            val insertion = TextEdit(TextRange.at(0), "a")
            val deletion = TextEdit(TextRange(start = 0, end = 1), "")

            insertion.isInsertion shouldBe true
            insertion.isDeletion shouldBe false
            deletion.isInsertion shouldBe false
            deletion.isDeletion shouldBe true
        }

        test("equals an edit of the same range with the same replacement and shares its hash code") {
            val edit = TextEdit(TextRange(start = 0, end = INSIDE), LISTEN)
            val same = TextEdit(TextRange(start = 0, end = INSIDE), LISTEN)

            same shouldBe edit
            same.hashCode() shouldBe edit.hashCode()
            edit shouldNotBe TextEdit(TextRange(start = 0, end = INSIDE), "$LISTEN!")
            edit shouldNotBe TextEdit(TextRange(start = 0, end = INSERTED), LISTEN)
        }

        test("names its range and the length of its replacement, but never the replacement itself") {
            val edit = TextEdit(TextRange(start = VALUE_START, end = TEXT.length), SECRET)

            edit.toString() shouldBe "TextEdit(range=6..8, replacement=7 chars)"
        }

        test("is a plain class without copy or component functions") {
            TextEdit::class.java.dataClassMembers().shouldBeEmpty()
        }
    }

    context("applying edits") {
        test("applies edits in the order of their position") {
            val edits = listOf(
                TextEdit(TextRange(start = VALUE_START, end = TEXT.length), "443"),
                TextEdit(TextRange(start = 0, end = 4), LISTEN),
            )

            TEXT.edited(edits) shouldBe "listen: 443"
        }

        test("keeps two insertions at one offset in their given order") {
            val edits = listOf(TextEdit(TextRange.at(0), "a"), TextEdit(TextRange.at(0), "b"))

            TEXT.edited(edits) shouldBe "ab$TEXT"
        }

        test("applies an insertion and a replacement at the same offset alike in either order") {
            val insertion = TextEdit(TextRange.at(VALUE_START), "x")
            val replacement = TextEdit(TextRange(start = VALUE_START, end = TEXT.length), "443")

            TEXT.edited(listOf(insertion, replacement)) shouldBe "port: x443"
            TEXT.edited(listOf(replacement, insertion)) shouldBe "port: x443"
        }

        test("leaves the text alone without edits") {
            TEXT.edited(emptyList()) shouldBe TEXT
        }

        test("refuses edits that overlap") {
            val edits = listOf(
                TextEdit(TextRange(start = 0, end = VALUE_START), ""),
                TextEdit(TextRange(start = 2, end = TEXT.length), ""),
            )

            shouldThrow<IllegalArgumentException> { TEXT.edited(edits) }
        }

        test("names only the ranges of edits that overlap, never their replacements") {
            val edits = listOf(
                TextEdit(TextRange(start = 0, end = VALUE_START), SECRET),
                TextEdit(TextRange(start = 2, end = TEXT.length), SECRET),
            )

            val refusal = shouldThrow<IllegalArgumentException> { TEXT.edited(edits) }

            refusal.message shouldContain "0..6"
            refusal.message shouldContain "2..8"
            refusal.message shouldNotContain SECRET
        }

        test("refuses an edit beyond the text") {
            val edit = TextEdit(TextRange(start = 0, end = TEXT.length + 1), "")

            shouldThrow<IllegalArgumentException> { TEXT.edited(listOf(edit)) }
        }

        test("names only the range of an edit beyond the text, never its replacement") {
            val edit = TextEdit(TextRange(start = 0, end = TEXT.length + 1), SECRET)

            val refusal = shouldThrow<IllegalArgumentException> { TEXT.edited(listOf(edit)) }

            refusal.message shouldContain "0..9"
            refusal.message shouldNotContain SECRET
        }

        test("refuses edits whose result would not fit in a string instead of overflowing its length") {
            val huge = RepeatedText(length = Int.MAX_VALUE - 1, character = 'a')
            val edit = TextEdit(TextRange.at(0), "ab")

            val refusal = shouldThrow<IllegalArgumentException> { huge.edited(listOf(edit)) }

            refusal.message shouldContain "2147483648"
        }

        test("refuses a result of the largest length with an exception instead of running out of memory") {
            val chunk = "x".repeat(CHUNK)
            val edits = List(CHUNKS_IN_LARGEST_LENGTH) { TextEdit(TextRange.at(0), chunk) } +
                TextEdit(TextRange.at(0), chunk.substring(1))

            val refusal = shouldThrow<IllegalArgumentException> { "".edited(edits) }

            refusal.message shouldContain "${Int.MAX_VALUE}"
        }

        test("refuses a result longer than a string of any content can hold") {
            val chunk = "x".repeat(CHUNK)
            val edits = List(CHUNKS_ABOVE_BOUND) { TextEdit(TextRange.at(0), chunk) }

            val refusal = shouldThrow<IllegalArgumentException> { "".edited(edits) }

            refusal.message shouldContain "$LONGEST_EDITED_TEXT"
        }
    }

    context("mapping offsets") {
        val edits = listOf(TextEdit(TextRange(start = 2, end = 4), "xyz"))

        test("keeps an offset before an edit") {
            edits.mapOffset(1) shouldBe 1
        }

        test("keeps an offset at the start of an edit before the inserted text") {
            edits.mapOffset(2) shouldBe 2
        }

        test("moves an offset inside an edit to the end of its replacement") {
            edits.mapOffset(INSIDE) shouldBe VALUE_START - 1
        }

        test("shifts an offset after an edit by its delta") {
            edits.mapOffset(VALUE_START) shouldBe VALUE_START + 1
        }

        test("follows every edit before the offset") {
            val two = edits + TextEdit(TextRange.at(0), "ab")

            two.mapOffset(VALUE_START) shouldBe VALUE_START + 1 + INSERTED
        }

        test("keeps an offset at an insertion in front of the inserted text, so it points at its first character") {
            val insertion = listOf(TextEdit(TextRange.at(1), "X"))
            val edited = "abc".edited(insertion)
            val mapped = insertion.mapOffset(1)

            edited[mapped] shouldBe 'X'
            edited[mapped + 1] shouldBe 'b'
        }

        test("refuses edits that overlap, like applying them does") {
            val overlapping = edits + TextEdit(TextRange(start = INSIDE, end = VALUE_START), "")

            shouldThrow<IllegalArgumentException> { overlapping.mapOffset(1) }
        }

        test("refuses an offset before the text") {
            shouldThrow<IllegalArgumentException> { edits.mapOffset(-1) }
        }

        test("refuses to move an offset after the last possible offset instead of overflowing") {
            val insertion = listOf(TextEdit(TextRange.at(0), "ab"))

            shouldThrow<IllegalArgumentException> { insertion.mapOffset(Int.MAX_VALUE - 1) }
        }

        test("maps an offset after a replacement onto the same character") {
            checkAll(CASES, Arb.string(0..LONGEST), Arb.int(0..LONGEST)) { replacement, cut ->
                val start = cut.coerceAtMost(TEXT.length - 2)
                val edit = TextEdit(TextRange(start = start, end = TEXT.length - 1), replacement)
                val edited = TEXT.edited(listOf(edit))

                edited[listOf(edit).mapOffset(TEXT.length - 1)] shouldBe TEXT.last()
            }
        }

        test("maps every offset outside the replaced ranges and the insertions onto the same character, in any order") {
            val cutPoints = Arb.list(Arb.int(0..ALPHABET.length), 0..MOST_CUTS)
            checkAll(CASES, cutPoints, Arb.string(0..INSIDE)) { points, replacement ->
                val given = editsBetween(points, replacement)
                val reversed = given.asReversed()
                val edited = ALPHABET.edited(reversed)
                val untouched = ALPHABET.indices.filter { offset -> given.none { edit -> touchesEdit(edit, offset) } }

                ALPHABET.edited(given) shouldBe edited
                untouched.forEach { offset -> edited[reversed.mapOffset(offset)] shouldBe ALPHABET[offset] }
            }
        }

        test("maps the offset of every insertion onto the first character it inserts, whatever the edits") {
            val cutPoints = Arb.list(Arb.int(0..ALPHABET.length), 0..MOST_CUTS)
            checkAll(CASES, cutPoints, Arb.string(1..INSIDE)) { points, replacement ->
                val given = editsBetween(points, replacement)
                val edited = ALPHABET.edited(given)
                val insertions = given.filter { edit -> edit.isInsertion }

                insertions.forEach { edit -> edited[given.mapOffset(edit.range.start)] shouldBe replacement.first() }
            }
        }
    }
})

private fun editsBetween(points: List<Int>, replacement: String): List<TextEdit> {
    val sorted = points.sorted()
    val ranges = sorted.chunked(2).filter { pair -> pair.size == 2 }
    return ranges.map { pair -> TextEdit(TextRange(start = pair[0], end = pair[1]), replacement) }
}

private fun touchesEdit(edit: TextEdit, offset: Int): Boolean {
    val isInsertionOfText = edit.isInsertion && !edit.isDeletion && offset == edit.range.start
    return isInsertionOfText || offset in edit.range
}
