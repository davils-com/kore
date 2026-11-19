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
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private const val WORD: String = "abc"
private const val WORD_LENGTH: Int = 3
private const val GENEROUS: Int = 5
private const val FACE: String = "😀"
private const val NONE: Int = 0
private const val TWO: Int = 2
private const val PORT: String = "port"
private const val PORTS: String = "ports"
private const val SORT: String = "sort"

private val loneHighSurrogate: String = Char(0xD800).toString()
private val longestMeasurable: String = "a".repeat(Limits.MAX_EDIT_DISTANCE_LENGTH)
private val longestSuggestible: String = "a".repeat(Limits.MAX_SUGGESTION_TOKEN_LENGTH)

class SimilarityTest : FunSpec({
    context("editDistance") {
        test("is the length of the one text when the other is empty") {
            editDistance(left = WORD, right = "", limit = GENEROUS) shouldBe WORD_LENGTH
            editDistance(left = "", right = WORD, limit = GENEROUS) shouldBe WORD_LENGTH
        }

        test("counts an insertion, a deletion and a replacement as one edit each") {
            editDistance(left = PORT, right = PORTS, limit = GENEROUS) shouldBe 1
            editDistance(left = PORTS, right = PORT, limit = GENEROUS) shouldBe 1
            editDistance(left = PORT, right = SORT, limit = GENEROUS) shouldBe 1
        }

        test("is one past the limit once the texts are further apart") {
            editDistance(left = "--abcdef", right = "--uvwxyz", limit = TWO) shouldBe TWO + 1
            editDistance(left = "a", right = "abcdef", limit = TWO) shouldBe TWO + 1
            editDistance(left = "abc", right = "xyz", limit = NONE) shouldBe 1
        }

        test("counts a character outside the Basic Multilingual Plane once") {
            editDistance(left = "a$FACE", right = "a", limit = GENEROUS) shouldBe 1
            editDistance(left = FACE, right = "b", limit = GENEROUS) shouldBe 1
        }

        test("counts a lone surrogate as a character of its own") {
            editDistance(left = "a$loneHighSurrogate", right = "a", limit = GENEROUS) shouldBe 1
        }

        test("compares case exactly") {
            editDistance(left = "Port", right = PORT, limit = GENEROUS) shouldBe 1
        }

        test("measures texts up to the longest allowed") {
            editDistance(left = longestMeasurable, right = longestMeasurable, limit = NONE) shouldBe NONE
        }

        test("refuses a longer text and a negative limit before measuring") {
            val tooLong = longestMeasurable + "a"

            shouldThrow<IllegalArgumentException> { editDistance(left = tooLong, right = "a", limit = 1) }
            shouldThrow<IllegalArgumentException> { editDistance(left = "a", right = tooLong, limit = 1) }
            shouldThrow<IllegalArgumentException> { editDistance(left = "a", right = "b", limit = -1) }
        }
    }

    context("closestMatch") {
        test("is nothing when no candidate is near enough") {
            closestMatch("--abc", listOf("--uvwxyz")).shouldBeNull()
        }

        test("is the first of two equally near candidates") {
            closestMatch("--abcd", listOf("--abce", "--abcf")) shouldBe "--abce"
        }

        test("prefers the nearer candidate whatever its position") {
            closestMatch("--verbose", listOf("--verb", "--verbos")) shouldBe "--verbos"
        }

        test("allows one edit for every three characters, at most three") {
            closestMatch("ab", listOf("ac")).shouldBeNull()
            closestMatch("abc", listOf("abd")) shouldBe "abd"
            closestMatch("abcdefghijkl", listOf("abcdefghxxxx")).shouldBeNull()
            closestMatch("abcdefghijkl", listOf("abcdefghixxx")) shouldBe "abcdefghixxx"
        }

        test("takes the distance it is given") {
            closestMatch("ab", listOf("ac"), maxDistance = 1) shouldBe "ac"
        }

        test("folds ASCII letters only when told to") {
            closestMatch("PORT", listOf(PORT), maxDistance = NONE).shouldBeNull()
            val folded = closestMatch(token = "PORT", candidates = listOf(PORT), maxDistance = NONE, ignoreCase = true)
            val accented = closestMatch(
                token = "\u00C9t\u00E9",
                candidates = listOf("\u00E9t\u00E9"),
                maxDistance = NONE,
                ignoreCase = true,
            )

            folded shouldBe PORT
            accented.shouldBeNull()
        }

        test("suggests nothing for a token beyond the longest suggestible") {
            closestMatch(longestSuggestible, listOf(longestSuggestible)) shouldBe longestSuggestible
            closestMatch(longestSuggestible + "a", listOf(longestSuggestible)).shouldBeNull()
        }

        test("never suggests a candidate beyond the longest measurable") {
            val tooLong = longestMeasurable + "a"

            closestMatch("a", listOf(tooLong), maxDistance = Limits.MAX_EDIT_DISTANCE_LENGTH).shouldBeNull()
        }

        test("refuses a negative distance") {
            shouldThrow<IllegalArgumentException> { closestMatch("a", listOf("a"), maxDistance = -1) }
        }
    }

    context("closeMatches") {
        test("orders the close candidates by distance and keeps the order of equally close ones") {
            val matches = closeMatches(SORT, listOf(PORTS, "sorts", SORT, "short", PORT), maxCount = GENEROUS)

            matches shouldContainExactly listOf(SORT, "sorts", "short", PORT)
        }

        test("returns each candidate once and at most as many as asked for") {
            val matches = closeMatches(PORT, listOf(PORT, PORT, PORTS, SORT), maxCount = TWO)

            matches shouldContainExactly listOf(PORT, PORTS)
        }

        test("returns nothing when asked for nothing") {
            closeMatches(PORT, listOf(PORT), maxCount = NONE).shouldBeEmpty()
        }

        test("refuses a negative count") {
            shouldThrow<IllegalArgumentException> { closeMatches(PORT, listOf(PORT), maxCount = -1) }
        }
    }
})
