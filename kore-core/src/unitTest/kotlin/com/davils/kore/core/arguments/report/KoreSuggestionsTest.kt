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

package com.davils.kore.core.arguments.report

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private const val WORD: String = "abc"
private const val CUTOFF: Int = 5
private const val WORD_LENGTH: Int = 3

class KoreSuggestionsTest : FunSpec({
    context("the distance between two names") {
        test("is the length of the one when the other is empty") {
            boundedDistance(left = WORD, right = "", cutoff = CUTOFF) shouldBe WORD_LENGTH
            boundedDistance(left = "", right = WORD, cutoff = CUTOFF) shouldBe WORD_LENGTH
        }

        test("stops at the cutoff once every edit is too expensive") {
            boundedDistance(left = "--abcdef", right = "--uvwxyz", cutoff = 2) shouldBe 2
        }
    }

    context("the closest name") {
        test("is nothing when no candidate is near enough") {
            closestName("--abc", listOf("--uvwxyz")).shouldBeNull()
        }

        test("is the first of two equally near candidates") {
            closestName("--abcd", listOf("--abce", "--abcf")) shouldBe "--abce"
        }
    }
})
