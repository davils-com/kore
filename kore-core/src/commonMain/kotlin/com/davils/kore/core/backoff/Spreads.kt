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

package com.davils.kore.core.backoff

import com.davils.kore.core.annotation.DavilsInternal
import kotlin.time.Duration

private const val DECORRELATION: Int = 3

/**
 * Spreads a [delay] over the whole range from zero up to the [delay]: full jitter.
 *
 * @param delay the delay before the spread.
 * @param fraction a random fraction from zero up to one, such as [com.davils.kore.core.random.nextFraction]; a value
 * outside that range is clamped into it.
 * @return the spread delay, from zero up to [delay].
 * @since 2.5.0
 */
@DavilsInternal
public fun fullJitter(delay: Duration, fraction: Double): Duration = delay * clamped(fraction)

/**
 * Spreads a [delay] over its upper half: equal jitter, which keeps at least half of the [delay].
 *
 * @param delay the delay before the spread.
 * @param fraction a random fraction from zero up to one; a value outside that range is clamped into it.
 * @return the spread delay, from half of [delay] up to [delay].
 * @since 2.5.0
 */
@DavilsInternal
public fun equalJitter(delay: Duration, fraction: Double): Duration {
    val half = delay / 2
    return half + half * clamped(fraction)
}

/**
 * Spreads a [delay] by [factor] in both directions: proportional jitter, from [delay] times one minus [factor] up to
 * [delay] times one plus [factor].
 *
 * @param delay the delay before the spread.
 * @param factor the share of [delay] the spread may move it, from zero up to one; the caller validates it.
 * @param fraction a random fraction from zero up to one; a value outside that range is clamped into it.
 * @return the spread delay.
 * @since 2.5.0
 */
@DavilsInternal
public fun proportionalJitter(delay: Duration, factor: Double, fraction: Double): Duration =
    delay * (1.0 - factor + 2.0 * factor * clamped(fraction))

/**
 * Draws a decorrelated delay between [initial] and three times the [previous] delay, never longer than [maximum], as
 * the decorrelated jitter of exponential backoff describes it.
 *
 * @param initial the shortest delay, the delay of the first retry.
 * @param previous the delay waited before the last attempt, or zero before the first retry.
 * @param maximum the longest delay.
 * @param fraction a random fraction from zero up to one; a value outside that range is clamped into it.
 * @return the delay, from the shorter of [initial] and [maximum] up to [maximum].
 * @since 2.5.0
 */
@DavilsInternal
public fun decorrelatedJitter(initial: Duration, previous: Duration, maximum: Duration, fraction: Double): Duration {
    val high = (previous * DECORRELATION).coerceAtLeast(initial)
    val delay = initial + (high - initial) * clamped(fraction)
    return delay.coerceAtMost(maximum)
}

private fun clamped(fraction: Double): Double {
    if (fraction.isNaN()) return 0.0
    return fraction.coerceIn(minimumValue = 0.0, maximumValue = 1.0)
}
