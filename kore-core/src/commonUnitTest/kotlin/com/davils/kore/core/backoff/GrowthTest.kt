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
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val DOUBLING: Double = 2.0

class GrowthTest : FunSpec({
    context("an exponential delay") {
        test("grows by the multiplier on every step") {
            grownDelay(initial = 100.milliseconds, multiplier = DOUBLING, step = 0, maximum = 10.seconds) shouldBe
                100.milliseconds
            grownDelay(initial = 100.milliseconds, multiplier = DOUBLING, step = 3, maximum = 10.seconds) shouldBe
                800.milliseconds
        }

        test("never exceeds the maximum") {
            grownDelay(initial = 1.seconds, multiplier = DOUBLING, step = 10, maximum = 30.seconds) shouldBe 30.seconds
        }

        test("gives the maximum for a growth beyond every number") {
            grownDelay(initial = 1.seconds, multiplier = DOUBLING, step = Int.MAX_VALUE, maximum = 5.seconds) shouldBe
                5.seconds
        }

        test("counts a negative step as none") {
            grownDelay(initial = 1.seconds, multiplier = DOUBLING, step = -4, maximum = 5.seconds) shouldBe 1.seconds
        }

        test("stays between the initial delay and the maximum for every step") {
            checkAll(Arb.int()) { step ->
                val delay = grownDelay(initial = 1.seconds, multiplier = DOUBLING, step = step, maximum = 1_000.seconds)

                (delay in 1.seconds..1_000.seconds) shouldBe true
            }
        }
    }
})
