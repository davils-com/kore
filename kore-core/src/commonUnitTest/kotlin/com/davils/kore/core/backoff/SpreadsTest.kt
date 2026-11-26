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

package com.davils.kore.core.backoff

import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.double
import io.kotest.property.checkAll
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val HALF: Double = 0.5
private const val QUARTER: Double = 0.25

class SpreadsTest : FunSpec({
    context("full jitter") {
        test("spreads from zero up to the delay") {
            fullJitter(delay = 10.seconds, fraction = 0.0) shouldBe Duration.ZERO
            fullJitter(delay = 10.seconds, fraction = HALF) shouldBe 5.seconds
            fullJitter(delay = 10.seconds, fraction = 1.0) shouldBe 10.seconds
        }

        test("clamps a fraction outside the range") {
            fullJitter(delay = 10.seconds, fraction = -1.0) shouldBe Duration.ZERO
            fullJitter(delay = 10.seconds, fraction = 2.0) shouldBe 10.seconds
            fullJitter(delay = 10.seconds, fraction = Double.NaN) shouldBe Duration.ZERO
        }
    }

    context("equal jitter") {
        test("keeps at least half of the delay") {
            equalJitter(delay = 10.seconds, fraction = 0.0) shouldBe 5.seconds
            equalJitter(delay = 10.seconds, fraction = 1.0) shouldBe 10.seconds
        }
    }

    context("proportional jitter") {
        test("moves the delay by the factor in both directions") {
            proportionalJitter(delay = 10.seconds, factor = QUARTER, fraction = 0.0) shouldBe 7.5.seconds
            proportionalJitter(delay = 10.seconds, factor = QUARTER, fraction = HALF) shouldBe 10.seconds
            proportionalJitter(delay = 10.seconds, factor = QUARTER, fraction = 1.0) shouldBe 12.5.seconds
        }
    }

    context("decorrelated jitter") {
        test("draws between the initial delay and three times the previous one") {
            decorrelatedJitter(initial = 1.seconds, previous = 4.seconds, maximum = 60.seconds, fraction = 0.0) shouldBe
                1.seconds
            decorrelatedJitter(initial = 1.seconds, previous = 4.seconds, maximum = 60.seconds, fraction = 1.0) shouldBe
                12.seconds
        }

        test("starts at the initial delay before the first retry") {
            decorrelatedJitter(
                initial = 2.seconds,
                previous = Duration.ZERO,
                maximum = 60.seconds,
                fraction = 1.0,
            ) shouldBe 2.seconds
        }

        test("never exceeds the maximum") {
            checkAll(Arb.double()) { fraction ->
                val delay = decorrelatedJitter(
                    initial = 1.seconds,
                    previous = 100.seconds,
                    maximum = 30.seconds,
                    fraction = fraction,
                )

                (delay in 1.seconds..30.seconds) shouldBe true
            }
        }
    }
})
