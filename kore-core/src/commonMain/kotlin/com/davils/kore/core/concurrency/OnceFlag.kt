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

package com.davils.kore.core.concurrency

import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * A flag that is set once and never again, for an action that must happen exactly once, such as closing a resource or
 * releasing a permit, however many threads try.
 *
 * Exactly one call of [trySet] returns `true`, the first one; every other call, concurrent or later, returns `false`.
 * The flag is safe to share between threads and coroutines, and never blocks or suspends.
 *
 * @since 2.1.0
 */
@OptIn(ExperimentalAtomicApi::class)
public class OnceFlag {
    private val flag: AtomicBoolean = AtomicBoolean(false)

    /**
     * Whether the flag was set.
     *
     * @since 2.1.0
     */
    public val isSet: Boolean get() = flag.load()

    /**
     * Sets the flag, and tells whether this call was the one that set it.
     *
     * @return `true` for the first call only.
     * @since 2.1.0
     */
    public fun trySet(): Boolean = flag.compareAndSet(expectedValue = false, newValue = true)
}
