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

import com.davils.kore.core.identity.internal.platformRandomness
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

private const val RANDOM_BITS: Int = 53
private const val SHIFT: Int = Long.SIZE_BITS - RANDOM_BITS
private const val HALF: Double = 0.5
private val unitFraction: Double = 2.0.pow(-RANDOM_BITS)

/**
 * Computes the pause before a retry: a fixed delay, or a delay that grows by a factor with every attempt up to a
 * maximum, spread at random as its [Jitter] says.
 *
 * The delay of attempt `n`, counted from 1, is `initial × factor^(n - 1)`, but never more than the maximum; the
 * jitter then picks a delay between zero, or half of it, and the whole of it. The computation is pure arithmetic in
 * constant time, so even attempt [Int.MAX_VALUE] returns the maximum at once, without overflowing. Delays are rounded
 * down to whole nanoseconds. The random spread comes from the secure random source of the platform, never from a
 * predictable generator; [delayFor] with a fraction gives a caller, such as a test, the same delay every time.
 *
 * A backoff only computes delays: it does not wait, does not decide whether to retry and keeps no state, so it is
 * immutable and safe to share between threads.
 *
 * @since 2.1.0
 */
public class Backoff private constructor(
    private val initial: Duration,
    private val maximum: Duration,
    private val factor: Double,
    private val jitter: Jitter,
) {
    /**
     * Returns the delay before attempt [attempt], spread at random as the [Jitter] of this backoff says.
     *
     * @param attempt the attempt the delay comes before, counted from 1.
     * @return the delay, from zero up to the maximum.
     * @throws IllegalArgumentException if [attempt] is below 1.
     * @since 2.1.0
     */
    public fun delayFor(attempt: Int): Duration = delayFor(attempt = attempt, jitterFraction = randomFraction())

    /**
     * Returns the delay before attempt [attempt], spread by [jitterFraction] instead of at random.
     *
     * A fraction of 0 gives the shortest delay the [Jitter] allows, and a fraction close to 1 the longest; without
     * jitter the fraction is ignored.
     *
     * @param attempt the attempt the delay comes before, counted from 1.
     * @param jitterFraction where the delay lies in the spread, at least 0 and below 1.
     * @return the delay, from zero up to the maximum.
     * @throws IllegalArgumentException if [attempt] is below 1 or [jitterFraction] lies outside its range.
     * @since 2.1.0
     */
    public fun delayFor(attempt: Int, jitterFraction: Double): Duration {
        require(attempt >= 1) { "Attempts are counted from 1, but the attempt was $attempt" }
        require(jitterFraction >= 0.0 && jitterFraction < 1.0) {
            "A jitter fraction is at least 0 and below 1, but was $jitterFraction"
        }
        val growth = factor.pow(attempt - 1)
        val ceiling = maximum.inWholeNanoseconds.toDouble()
        val base = minOf(a = initial.inWholeNanoseconds * growth, b = ceiling)
        return spread(base = base, fraction = jitterFraction).toLong().nanoseconds
    }

    /**
     * Returns the configuration of this backoff, such as `Backoff(initial=100ms, maximum=30s, factor=2.0,
     * jitter=Full)`.
     *
     * @return the configuration as one line of text.
     * @since 2.1.0
     */
    override fun toString(): String = "Backoff(initial=$initial, maximum=$maximum, factor=$factor, jitter=$jitter)"

    private fun spread(base: Double, fraction: Double): Double = when (jitter) {
        Jitter.None -> base
        Jitter.Full -> base * fraction
        Jitter.Equal -> base * HALF + base * HALF * fraction
    }

    /**
     * Creates backoffs.
     *
     * @since 2.1.0
     */
    public companion object {
        /**
         * Returns a backoff that waits [delay] before every attempt, spread by [jitter].
         *
         * @param delay the delay, zero or more and finite.
         * @param jitter how the delay is spread; [Jitter.None] for exactly [delay] every time.
         * @return the backoff.
         * @throws IllegalArgumentException if [delay] is negative or infinite.
         * @since 2.1.0
         */
        public fun fixed(delay: Duration, jitter: Jitter): Backoff =
            exponential(initial = delay, maximum = delay, factor = 1.0, jitter = jitter)

        /**
         * Returns a backoff whose delay starts at [initial] and grows by [factor] with every attempt, up to [maximum].
         *
         * @param initial the delay before the first attempt, zero or more and finite.
         * @param maximum the longest delay, at least [initial] and finite.
         * @param factor how much the delay grows from one attempt to the next, at least 1 and finite.
         * @param jitter how each delay is spread.
         * @return the backoff.
         * @throws IllegalArgumentException if a duration is negative or infinite, [maximum] is below [initial], or
         * [factor] is below 1, infinite or not a number.
         * @since 2.1.0
         */
        public fun exponential(initial: Duration, maximum: Duration, factor: Double, jitter: Jitter): Backoff {
            require(!initial.isNegative() && initial.isFinite()) { "The first delay is zero or more and finite" }
            require(maximum >= initial && maximum.isFinite()) { "The longest delay is finite and at least the first" }
            require(factor >= 1.0 && factor.isFinite()) { "The factor of a backoff is at least 1 and finite" }
            return Backoff(initial = initial, maximum = maximum, factor = factor, jitter = jitter)
        }
    }
}

private fun randomFraction(): Double = (platformRandomness().nextLong() ushr SHIFT) * unitFraction
