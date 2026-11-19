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

package com.davils.kore.core.time

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val FACTOR: Double = 2.0
private const val THIRD: Int = 3
private const val HALF: Double = 0.5
private const val ALMOST_ONE: Double = 0.999_999
private const val MANY_ATTEMPTS: Int = 1_000

private val initial: Duration = 100.milliseconds
private val second: Duration = 200.milliseconds
private val third: Duration = 400.milliseconds
private val half: Duration = 50.milliseconds
private val threeQuarters: Duration = 75.milliseconds
private val maximum: Duration = 30.seconds

private fun exponential(jitter: Jitter): Backoff =
    Backoff.exponential(initial = initial, maximum = maximum, factor = FACTOR, jitter = jitter)

class BackoffTest : FunSpec({
    context("an exponential backoff") {
        test("grows by its factor from attempt to attempt") {
            val backoff = exponential(Jitter.None)

            backoff.delayFor(attempt = 1, jitterFraction = 0.0) shouldBe initial
            backoff.delayFor(attempt = 2, jitterFraction = 0.0) shouldBe second
            backoff.delayFor(attempt = THIRD, jitterFraction = 0.0) shouldBe third
        }

        test("never exceeds its maximum, not even at the last attempt") {
            val backoff = exponential(Jitter.None)

            backoff.delayFor(attempt = Int.MAX_VALUE, jitterFraction = 0.0) shouldBe maximum
        }

        test("grows monotonically without jitter") {
            val backoff = exponential(Jitter.None)

            checkAll(Arb.int(1..MANY_ATTEMPTS)) { attempt ->
                val next = backoff.delayFor(attempt = attempt + 1, jitterFraction = 0.0)

                next shouldBeGreaterThanOrEqualTo backoff.delayFor(attempt = attempt, jitterFraction = 0.0)
            }
        }
    }

    context("jitter") {
        test("spreads the full delay from zero or from half of it") {
            val full = exponential(Jitter.Full)
            val equal = exponential(Jitter.Equal)

            full.delayFor(attempt = 1, jitterFraction = 0.0) shouldBe Duration.ZERO
            full.delayFor(attempt = 1, jitterFraction = HALF) shouldBe half
            equal.delayFor(attempt = 1, jitterFraction = 0.0) shouldBe half
            equal.delayFor(attempt = 1, jitterFraction = HALF) shouldBe threeQuarters
        }

        test("keeps every random delay between zero and the maximum") {
            checkAll(Arb.enum<Jitter>(), Arb.int(1..Int.MAX_VALUE)) { jitter, attempt ->
                val delay = exponential(jitter).delayFor(attempt)

                delay shouldBeGreaterThanOrEqualTo Duration.ZERO
                delay shouldBeLessThanOrEqualTo maximum
            }
        }

        test("keeps every given fraction within the spread") {
            checkAll(Arb.double(range = 0.0..ALMOST_ONE, includeNaNs = false)) { fraction ->
                val delay = exponential(Jitter.Equal).delayFor(attempt = 1, jitterFraction = fraction)

                delay shouldBeGreaterThanOrEqualTo half
            }
        }
    }

    context("a fixed backoff") {
        test("waits the same delay before every attempt") {
            val backoff = Backoff.fixed(delay = 1.seconds, jitter = Jitter.None)

            backoff.delayFor(attempt = 1) shouldBe 1.seconds
            backoff.delayFor(attempt = Int.MAX_VALUE) shouldBe 1.seconds
        }

        test("shows its configuration") {
            Backoff.fixed(delay = 1.seconds, jitter = Jitter.Full).toString() shouldBe
                "Backoff(initial=1s, maximum=1s, factor=1.0, jitter=Full)"
        }
    }

    context("refusals") {
        test("refuse an attempt below 1 and a fraction outside its range") {
            val backoff = exponential(Jitter.None)

            shouldThrow<IllegalArgumentException> { backoff.delayFor(attempt = 0) }
            shouldThrow<IllegalArgumentException> { backoff.delayFor(attempt = 1, jitterFraction = 1.0) }
            shouldThrow<IllegalArgumentException> { backoff.delayFor(attempt = 1, jitterFraction = -0.1) }
        }

        test("refuse negative and infinite delays and a factor below 1") {
            shouldThrow<IllegalArgumentException> { Backoff.fixed(delay = (-1).seconds, jitter = Jitter.None) }
            shouldThrow<IllegalArgumentException> { Backoff.fixed(delay = Duration.INFINITE, jitter = Jitter.None) }
            shouldThrow<IllegalArgumentException> {
                Backoff.exponential(initial = maximum, maximum = initial, factor = FACTOR, jitter = Jitter.None)
            }
            shouldThrow<IllegalArgumentException> {
                Backoff.exponential(
                    initial = initial,
                    maximum = Duration.INFINITE,
                    factor = FACTOR,
                    jitter = Jitter.None,
                )
            }
            shouldThrow<IllegalArgumentException> {
                Backoff.exponential(initial = initial, maximum = maximum, factor = HALF, jitter = Jitter.None)
            }
            shouldThrow<IllegalArgumentException> {
                Backoff.exponential(initial = initial, maximum = maximum, factor = Double.NaN, jitter = Jitter.None)
            }
            shouldThrow<IllegalArgumentException> {
                Backoff.exponential(
                    initial = initial,
                    maximum = maximum,
                    factor = Double.POSITIVE_INFINITY,
                    jitter = Jitter.None,
                )
            }
        }
    }
})
