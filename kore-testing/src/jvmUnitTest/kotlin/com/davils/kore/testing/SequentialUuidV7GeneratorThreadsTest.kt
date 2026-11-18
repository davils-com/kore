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

import com.davils.kore.core.identity.UuidV7
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeStrictlyIncreasing
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.time.Instant

private const val THREADS: Int = 8
private const val PER_THREAD: Int = 2_000
private const val START: Long = 1_000_000L

class SequentialUuidV7GeneratorThreadsTest : FunSpec({
    context("minting from several threads at once") {
        test("hands out every millisecond exactly once and keeps each thread in order") {
            val generator = SequentialUuidV7Generator(Instant.fromEpochMilliseconds(START))
            val begin = CountDownLatch(1)
            val batches = ConcurrentLinkedQueue<List<UuidV7>>()

            val workers = List(THREADS) {
                thread {
                    begin.await()
                    batches += List(PER_THREAD) { generator.next() }
                }
            }
            begin.countDown()
            workers.forEach(Thread::join)

            val milliseconds = batches.flatten().map { uuid -> uuid.epochMilliseconds }
            milliseconds.toSet() shouldHaveSize THREADS * PER_THREAD
            milliseconds.toSet() shouldBe (START until START + THREADS * PER_THREAD).toSet()
            batches.forEach { batch -> batch.shouldBeStrictlyIncreasing() }
            generator.issued shouldBe (THREADS * PER_THREAD).toLong()
        }
    }
})
