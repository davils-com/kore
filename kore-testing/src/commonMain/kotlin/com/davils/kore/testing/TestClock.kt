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

package com.davils.kore.testing

import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.updateAndFetch
import kotlin.time.AbstractLongTimeSource
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.TimeSource

/**
 * A clock for tests that stands still until the test moves it, and a monotonic time source that moves with it.
 *
 * [now] starts at [start] and moves only by [advanceBy], so code under test that takes a [Clock] and a
 * [TimeSource], such as a cache with an expiry or a scheduler, sees exactly the time the test sets. [timeSource]
 * shares the same elapsed time, so a mark taken from it and the wall time of the clock always agree. The clock is safe
 * to share between threads: advancing it is atomic, and every reader sees the time once it has been advanced.
 *
 * It counts nanoseconds in a [Long], so it covers about 292 years from [start].
 *
 * @param start the time the clock shows before it is advanced; the epoch by default.
 * @since 2.1.0
 */
@OptIn(ExperimentalAtomicApi::class)
public class TestClock(private val start: Instant = Instant.fromEpochMilliseconds(0L)) : Clock {
    private val elapsedNanoseconds: AtomicLong = AtomicLong(0L)

    /**
     * A monotonic time source that moves exactly when this clock is advanced.
     *
     * @since 2.1.0
     */
    public val timeSource: TimeSource.WithComparableMarks = object : AbstractLongTimeSource(DurationUnit.NANOSECONDS) {
        override fun read(): Long = elapsedNanoseconds.load()
    }

    /**
     * Returns the time the clock shows: [start] plus every duration it was advanced by.
     *
     * @return the current time of the clock.
     * @since 2.1.0
     */
    override fun now(): Instant = start + elapsedNanoseconds.load().nanoseconds

    /**
     * Moves the clock and its time source forward by [duration].
     *
     * @param duration how far to move, zero or more and finite.
     * @throws IllegalArgumentException if [duration] is negative or infinite, because a clock for tests never goes
     * back and never stops.
     * @throws ArithmeticException if the clock would pass about 292 years after [start].
     * @since 2.1.0
     */
    public fun advanceBy(duration: Duration) {
        require(!duration.isNegative() && duration.isFinite()) {
            "A test clock moves forward by a finite duration, but was asked to move by $duration"
        }
        val step = duration.inWholeNanoseconds
        elapsedNanoseconds.updateAndFetch { elapsed -> checkedSum(elapsed = elapsed, step = step) }
    }
}

private fun checkedSum(elapsed: Long, step: Long): Long {
    if (step > Long.MAX_VALUE - elapsed) throw ArithmeticException("The test clock cannot move past 292 years")
    return elapsed + step
}
