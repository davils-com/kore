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

@file:OptIn(DavilsInternal::class)

package com.davils.kore.core.uri

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.uri.fixture.GROWTH_FACTOR
import com.davils.kore.core.uri.fixture.reference
import com.davils.kore.core.uri.fixture.shouldGrowLinearly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull

private const val SMALL_COUNT: Int = 1_000
private const val LARGE_COUNT: Int = SMALL_COUNT * GROWTH_FACTOR

private fun segments(count: Int): String = "http://a" + "/b".repeat(count)

private fun climbs(count: Int): String = "../".repeat(count) + "g"

private fun detours(count: Int): String = "/" + "b/../".repeat(count / 2) + "g"

private fun encodedDots(count: Int): String = "http://A/" + "%2e/%7E".repeat(count / 2)

class UriGrowthTest : FunSpec({
    context("the work on a URI reference") {
        test("grows linearly with the segments of a path that is read") {
            shouldGrowLinearly(small = segments(SMALL_COUNT), large = segments(LARGE_COUNT)) { text ->
                reference(text)
            }
        }

        test("grows linearly with the dot segments that climb above the base") {
            val base = reference(segments(LARGE_COUNT / 2))

            shouldGrowLinearly(small = climbs(SMALL_COUNT), large = climbs(LARGE_COUNT)) { text ->
                base.resolve(reference(text)).shouldNotBeNull()
            }
        }

        test("grows linearly with dot segments that remove the segment before them") {
            val base = reference("http://a/b")

            shouldGrowLinearly(small = detours(SMALL_COUNT), large = detours(LARGE_COUNT)) { text ->
                base.resolve(reference(text)).shouldNotBeNull()
            }
        }

        test("grows linearly with the percent-encoded octets and dot segments that are normalized") {
            shouldGrowLinearly(small = encodedDots(SMALL_COUNT), large = encodedDots(LARGE_COUNT)) { text ->
                reference(text).normalize()
            }
        }
    }
})
