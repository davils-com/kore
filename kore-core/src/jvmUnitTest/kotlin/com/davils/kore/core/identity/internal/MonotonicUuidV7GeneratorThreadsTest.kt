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

package com.davils.kore.core.identity.internal

import com.davils.kore.core.identity.UuidV7
import com.davils.kore.core.identity.generator.fixture.FallingClock
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeStrictlyIncreasing
import io.kotest.matchers.collections.shouldHaveSize
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

private const val NOW: Long = 1_763_251_200_000L
private const val THREADS: Int = 8
private const val PER_THREAD: Int = 5_000

class MonotonicUuidV7GeneratorThreadsTest : FunSpec({
    context("minting from several threads at once") {
        test("hands out distinct identifiers, in order per thread, even while the clock goes backwards") {
            val generator = MonotonicUuidV7Generator(FallingClock(NOW))
            val start = CountDownLatch(1)
            val batches = ConcurrentLinkedQueue<List<UuidV7>>()

            val workers = List(THREADS) {
                thread {
                    start.await()
                    batches += List(PER_THREAD) { generator.next() }
                }
            }
            start.countDown()
            workers.forEach(Thread::join)

            batches.flatten().toSet() shouldHaveSize THREADS * PER_THREAD
            batches.forEach { batch -> batch.shouldBeStrictlyIncreasing() }
        }
    }
})
