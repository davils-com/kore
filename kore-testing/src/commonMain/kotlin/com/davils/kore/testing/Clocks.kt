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

import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.TimeSource

/**
 * Returns a clock that shows [origin] plus the time this source has measured since the call, such as a clock that
 * follows the virtual time of a coroutine test scheduler.
 *
 * The clock takes a mark of this source once, when it is created, and shows [origin] plus the time elapsed since that
 * mark on every call of `now`.
 *
 * @receiver the time source the clock follows.
 * @param origin the time the clock shows at the moment it is created.
 * @return the clock.
 * @since 2.1.0
 */
public fun TimeSource.asClock(origin: Instant): Clock {
    val mark = markNow()
    return object : Clock {
        override fun now(): Instant = origin + mark.elapsedNow()
    }
}
