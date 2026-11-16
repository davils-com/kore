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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTimedValue

private const val INTERVALS: Int = 20_000
private const val BUDGET_SECONDS: Int = 2
private const val LABEL: String = "rc"

private fun candidate(major: Int): KoreSemanticVersion = KoreSemanticVersion(major, preRelease = listOf(LABEL))

private fun everyOther(first: Int): List<KoreSemanticVersionRange> =
    (first until INTERVALS step 2).map { major -> KoreSemanticVersionRange.exactly(candidate(major)) }

private fun unionOf(parts: List<KoreSemanticVersionRange>): KoreSemanticVersionRange {
    if (parts.size == 1) return parts.single()
    val half = parts.size / 2
    val lower = parts.subList(fromIndex = 0, toIndex = half)
    val upper = parts.subList(fromIndex = half, toIndex = parts.size)
    return unionOf(lower) union unionOf(upper)
}

class KoreSemanticVersionRangeScaleTest : FunSpec({
    context("combining ranges of many intervals") {
        test("takes time that grows only a little faster than the number of their intervals") {
            val even = unionOf(everyOther(0))
            val odd = unionOf(everyOther(1))

            val (combined, elapsed) = measureTimedValue { Pair(first = even union odd, second = even intersect odd) }
            val (every, shared) = combined

            elapsed shouldBeLessThan BUDGET_SECONDS.seconds
            every.intervals.size shouldBe INTERVALS
            every.contains(candidate(1)) shouldBe true
            shared shouldBe KoreSemanticVersionRange.NONE
            every intersect even shouldBe even
        }
    }
})
