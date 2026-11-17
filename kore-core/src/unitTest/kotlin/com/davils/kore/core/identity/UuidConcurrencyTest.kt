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

package com.davils.kore.core.identity

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeStrictlyIncreasing
import io.kotest.matchers.collections.shouldHaveSize
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread

private const val THREADS: Int = 8
private const val PER_THREAD: Int = 5_000

class UuidConcurrencyTest : FunSpec({
    context("minting from several threads at once") {
        test("never produces the same version 4 identifier twice") {
            val minted = ConcurrentLinkedQueue<Uuid>()
            val workers = List(THREADS) { thread { repeat(PER_THREAD) { minted += UuidV4.random() } } }
            workers.forEach(Thread::join)

            minted.toSet() shouldHaveSize THREADS * PER_THREAD
        }

        test("never produces the same version 7 identifier twice and keeps each thread in order") {
            val batches = ConcurrentLinkedQueue<List<UuidV7>>()
            val workers = List(THREADS) { thread { batches += List(PER_THREAD) { UuidV7.random() } } }
            workers.forEach(Thread::join)

            batches.flatten().toSet() shouldHaveSize THREADS * PER_THREAD
            batches.forEach { batch -> batch.shouldBeStrictlyIncreasing() }
        }
    }
})
