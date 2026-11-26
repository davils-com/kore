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

package com.davils.kore.core.random

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll

private const val LARGEST_FRACTION: Double = 1.0 - 1.0 / (1L shl 53)
private const val HALF_BITS: Long = Long.MIN_VALUE
private const val HALF: Double = 0.5

class FractionsTest : FunSpec({
    context("a random fraction") {
        test("stays from zero up to one for every source") {
            checkAll(Arb.long()) { bits ->
                val fraction = Entropy { bits }.nextFraction()

                fraction shouldBeGreaterThanOrEqual 0.0
                fraction shouldBeLessThan 1.0
            }
        }

        test("is built from the most significant 53 bits") {
            Entropy { 0L }.nextFraction() shouldBe 0.0
            Entropy { -1L }.nextFraction() shouldBe LARGEST_FRACTION
            Entropy { HALF_BITS }.nextFraction() shouldBe HALF
        }

        test("passes a failure of the source to the caller") {
            val failing = Entropy { throw IllegalStateException("The source is drained") }

            shouldThrow<IllegalStateException> { failing.nextFraction() }
        }
    }
})
