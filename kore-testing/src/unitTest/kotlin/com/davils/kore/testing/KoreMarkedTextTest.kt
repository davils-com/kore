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

import com.davils.kore.core.text.KoreTextRange
import com.davils.kore.testing.fixture.HOSTILE_MARKED_TEXT
import com.davils.kore.testing.fixture.isPlainAscii
import com.davils.kore.testing.fixture.markerSoup
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.property.checkAll

private const val VALUE_START: Int = 6
private const val VALUE_END: Int = 8
private const val CASES: Int = 500
private const val SECRET: String = "hunter2"

private val markers: List<String> =
    listOf(KoreMarkedText.CARET, KoreMarkedText.RANGE_START, KoreMarkedText.RANGE_END)

class KoreMarkedTextTest : FunSpec({
    context("reading markers") {
        test("removes a caret and remembers its offset") {
            val marked = KoreMarkedText.of("port: <caret>80")

            marked.text shouldBe "port: 80"
            marked.caret shouldBe VALUE_START
        }

        test("removes a range and remembers what it covered") {
            val marked = KoreMarkedText.of("port: <range>80</range>")

            marked.text shouldBe "port: 80"
            marked.range shouldBe KoreTextRange(start = VALUE_START, end = VALUE_END)
        }

        test("reads nested ranges in the order of their position") {
            val marked = KoreMarkedText.of("<range>a<range>b</range></range>")

            marked.ranges shouldBe listOf(KoreTextRange(start = 0, end = 2), KoreTextRange(start = 1, end = 2))
        }

        test("leaves a text without markers alone") {
            KoreMarkedText.of(
                "plain",
            ) shouldBe KoreMarkedText(text = "plain", carets = emptyList(), ranges = emptyList())
        }
    }

    context("unbalanced ranges") {
        test("refuses a range that is never closed") {
            shouldThrow<IllegalStateException> { KoreMarkedText.of("<range>a") }
        }

        test("refuses a range end without a start") {
            shouldThrow<IllegalStateException> { KoreMarkedText.of("a</range>") }
        }
    }

    context("markers formed by removing a marker") {
        test("refuses a text whose characters around a removed marker join into another marker") {
            shouldThrow<IllegalStateException> { KoreMarkedText.of("<range<caret>>x</ran<caret>ge>") }
            shouldThrow<IllegalStateException> { KoreMarkedText.of("<car<range></range>et>") }
        }

        test("names neither the text nor the marker it formed when it refuses") {
            val refusal = shouldThrow<IllegalStateException> { KoreMarkedText.of("$SECRET<car<caret>et>") }

            refusal.message.orEmpty() shouldNotContain SECRET
        }

        test("never returns a text that contains a marker, whatever the markers are cut into") {
            checkAll(CASES, markerSoup) { marked ->
                val text = runCatching { KoreMarkedText.of(marked).text }.getOrDefault("")

                markers.forEach { marker -> text shouldNotContain marker }
            }
        }
    }

    context("value semantics") {
        test("equals a marked text with the same text, carets and ranges and shares its hash code") {
            val marked = KoreMarkedText.of("a<caret>b<range>c</range>")
            val last = listOf(KoreTextRange(start = 2, end = 3))
            val same = KoreMarkedText(text = "abc", carets = listOf(1), ranges = last)

            same shouldBe marked
            same.hashCode() shouldBe marked.hashCode()
            same shouldNotBe KoreMarkedText(text = "abc", carets = listOf(2), ranges = last)
        }

        test("never equals something that is not a marked text") {
            val marked: Any = KoreMarkedText.of("abc")

            marked shouldNotBe "abc"
        }

        test("keeps its own copy of the carets and ranges it was given") {
            val carets = mutableListOf(0, 1)
            val ranges = mutableListOf(KoreTextRange(start = 0, end = 1), KoreTextRange(start = 1, end = 2))
            val marked = KoreMarkedText(text = "ab", carets = carets, ranges = ranges)

            carets.clear()
            ranges.clear()

            marked.carets shouldBe listOf(0, 1)
            marked.ranges shouldBe listOf(KoreTextRange(start = 0, end = 1), KoreTextRange(start = 1, end = 2))
        }

        test("cannot be changed through its lists, not even after a cast") {
            val marked = KoreMarkedText.of("<caret>a<caret>b<range>c</range><range>d</range>")

            shouldThrow<UnsupportedOperationException> { (marked.carets as MutableList<Int>).add(0) }
            shouldThrow<UnsupportedOperationException> { (marked.ranges as MutableList<KoreTextRange>).clear() }
        }

        test("names the length of its text, its carets and its ranges, but never the text itself") {
            KoreMarkedText.of("port: <caret><range>80</range>").toString() shouldBe
                "KoreMarkedText(length=8, carets=[6], ranges=[6..8])"
        }

        test("shows itself in plain characters, whatever its text contains") {
            val shown = KoreMarkedText.of(HOSTILE_MARKED_TEXT).toString()

            isPlainAscii(shown) shouldBe true
        }

        test("is a plain class without copy or component functions") {
            val names = KoreMarkedText::class.java.methods.map { method -> method.name }

            names.filter { name -> name.startsWith("copy") || name.startsWith("component") }.shouldBeEmpty()
        }
    }
})
