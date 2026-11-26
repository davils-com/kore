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
import kotlin.math.pow
import kotlin.time.Duration

/**
 * Returns the delay of an exponential backoff after [step] growths: [initial] times [multiplier] to the power of
 * [step], never longer than [maximum].
 *
 * The growth is computed in floating point, so a large [step] or [multiplier] never overflows: a growth that is not
 * finite, or a delay beyond [maximum], gives [maximum]. A [step] below zero counts as zero. The arguments are not
 * validated; the caller checks its own configuration, such as a positive [initial], a [multiplier] of at least one and
 * a [maximum] of at least [initial], before it asks for a delay.
 *
 * @param initial the delay before any growth.
 * @param multiplier the factor of each growth.
 * @param step the number of growths, the first retry having none.
 * @param maximum the longest delay.
 * @return the delay, from the shorter of [initial] and [maximum] up to [maximum].
 * @since 2.5.0
 */
@DavilsInternal
public fun grownDelay(initial: Duration, multiplier: Double, step: Int, maximum: Duration): Duration {
    val growth = multiplier.pow(step.coerceAtLeast(0))
    if (!growth.isFinite()) return maximum
    val grown = initial * growth
    return minOf(a = grown, b = maximum)
}
