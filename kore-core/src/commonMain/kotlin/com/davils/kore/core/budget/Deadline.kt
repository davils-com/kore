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

package com.davils.kore.core.budget

import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * A budget of time that started at one moment and runs out [budget] later, such as the grace period of a shutdown or
 * the time a request may take, so that the steps of the work share one end instead of each starting a timeout of its
 * own.
 *
 * The time is read from a monotonic [TimeSource], so a change of the wall clock never moves the end. A deadline with an
 * infinite [budget] never expires. A deadline is immutable and safe to share between threads and coroutines.
 *
 * @property budget the time from the start to the end, zero or more.
 * @since 2.5.0
 */
public class Deadline private constructor(private val start: TimeMark, public val budget: Duration) {
    /**
     * The time left until the end, zero once the deadline has expired and never negative.
     *
     * @since 2.5.0
     */
    public val remaining: Duration get() = (budget - start.elapsedNow()).coerceAtLeast(Duration.ZERO)

    /**
     * Whether no time is left.
     *
     * @since 2.5.0
     */
    public val isExpired: Boolean get() = remaining == Duration.ZERO

    /**
     * Returns the time a step of the work may take: the shorter of [timeout] and [remaining].
     *
     * @param timeout the time the step may take on its own; a negative timeout counts as zero.
     * @return the time for the step, never negative.
     * @since 2.5.0
     */
    public fun within(timeout: Duration): Duration = minOf(a = timeout.coerceAtLeast(Duration.ZERO), b = remaining)

    /**
     * Returns the time left until the end less [margin], such as the time a step needs afterwards to report, never
     * negative.
     *
     * @param margin the time to keep back; a negative margin counts as zero.
     * @return the remaining time less [margin].
     * @since 2.5.0
     */
    public fun remainingLess(margin: Duration): Duration =
        (remaining - margin.coerceAtLeast(Duration.ZERO)).coerceAtLeast(Duration.ZERO)

    /**
     * Shows the budget, never the remaining time, which changes with every read.
     *
     * @return the text.
     * @since 2.5.0
     */
    override fun toString(): String = "Deadline(budget=$budget)"

    /**
     * Starts deadlines.
     *
     * @since 2.5.0
     */
    public companion object {
        /**
         * Starts a deadline that ends [budget] from now.
         *
         * @param budget the time until the end, zero or more; [Duration.INFINITE] never ends.
         * @param timeSource the monotonic source of the time, [TimeSource.Monotonic] unless a test passes its own.
         * @return the deadline.
         * @throws IllegalArgumentException if [budget] is negative.
         * @since 2.5.0
         */
        public fun after(budget: Duration, timeSource: TimeSource = TimeSource.Monotonic): Deadline {
            require(!budget.isNegative()) { "The budget of a deadline must not be negative, but was $budget" }
            return Deadline(start = timeSource.markNow(), budget = budget)
        }
    }
}
