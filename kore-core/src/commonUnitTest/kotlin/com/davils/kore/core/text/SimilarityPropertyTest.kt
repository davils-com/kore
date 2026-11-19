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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll

private const val LONGEST: Int = 12
private const val UNLIMITED: Int = 64

private val pieces: List<String> = listOf("a", "b", "c", "A", "😀", "é")

private val words: Arb<String> = Arb.list(Arb.element(pieces), 0..LONGEST).map { parts -> parts.joinToString("") }

private fun widthAt(text: String, index: Int): Int {
    if (text[index].isHighSurrogate()) return 2
    return 1
}

private fun codePointsOf(text: String): List<String> = buildList {
    var index = 0
    while (index < text.length) {
        val width = widthAt(text, index)
        add(text.substring(startIndex = index, endIndex = index + width))
        index += width
    }
}

private fun costOf(left: String, right: String): Int {
    if (left == right) return 0
    return 1
}

private fun fullDistance(left: String, right: String): Int {
    val first = codePointsOf(left)
    val second = codePointsOf(right)
    val table = Array(first.size + 1) { row -> IntArray(second.size + 1) { column -> maxOf(a = row, b = column) } }
    for (row in 1..first.size) {
        for (column in 1..second.size) {
            val replaced = table[row - 1][column - 1] + costOf(left = first[row - 1], right = second[column - 1])
            val deleted = table[row - 1][column] + 1
            val inserted = table[row][column - 1] + 1
            table[row][column] = minOf(a = replaced, b = deleted, c = inserted)
        }
    }
    return table[first.size][second.size]
}

class SimilarityPropertyTest : FunSpec({
    context("editDistance") {
        test("agrees with the full table, cut off one past the limit") {
            checkAll(words, words, Arb.int(0..LONGEST)) { left, right, limit ->
                val expected = minOf(a = fullDistance(left = left, right = right), b = limit + 1)

                editDistance(left = left, right = right, limit = limit) shouldBe expected
            }
        }

        test("is zero exactly between equal texts") {
            checkAll(words, words) { left, right ->
                val isZero = editDistance(left = left, right = right, limit = UNLIMITED) == 0

                isZero shouldBe (left == right)
            }
        }

        test("is symmetric") {
            checkAll(words, words) { left, right ->
                val forth = editDistance(left = left, right = right, limit = UNLIMITED)

                forth shouldBe editDistance(left = right, right = left, limit = UNLIMITED)
            }
        }

        test("obeys the triangle inequality") {
            checkAll(words, words, words) { first, second, third ->
                val direct = editDistance(left = first, right = third, limit = UNLIMITED)
                val outward = editDistance(left = first, right = second, limit = UNLIMITED)
                val onward = editDistance(left = second, right = third, limit = UNLIMITED)
                val detour = outward + onward

                direct shouldBeLessThanOrEqual detour
            }
        }
    }
})
