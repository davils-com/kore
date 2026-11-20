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

package com.davils.kore.core.concurrency.fixture

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

private const val SHARED_HASH: Int = 7

internal class KeyLookups(callers: Int) {
    private val pending: CountDownLatch = CountDownLatch(callers)

    fun newKey(): Any = Key()

    fun shouldAllHappen() {
        pending.shouldOpen()
    }

    private inner class Key {
        private val isLookedUp: AtomicBoolean = AtomicBoolean(false)

        override fun equals(other: Any?): Boolean = other is Key

        override fun hashCode(): Int {
            if (isLookedUp.compareAndSet(false, true)) pending.countDown()
            return SHARED_HASH
        }
    }
}
