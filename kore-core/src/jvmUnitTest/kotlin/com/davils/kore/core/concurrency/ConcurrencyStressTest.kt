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

@file:OptIn(DavilsExperimental::class)

package com.davils.kore.core.concurrency

import com.davils.kore.core.annotation.DavilsExperimental
import com.davils.kore.core.concurrency.fixture.withThreads
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

private const val WORKERS: Int = 10_000
private const val ANSWER: Int = 42
private const val THREADS_OF_THOUGHT: Int = 64

class ConcurrencyStressTest : FunSpec({
    context("an admission gate under contention") {
        test("admits nothing after drain has returned and never counts below zero") {
            val gate = AdmissionGate()
            val admittedAfterDrain = AtomicInteger(0)
            val isDrained = AtomicBoolean(false)

            withThreads { threads ->
                coroutineScope {
                    repeat(WORKERS) {
                        launch(threads) {
                            val isAlreadyDrained = isDrained.get()
                            gate.tryEnter()?.use {
                                if (isAlreadyDrained) admittedAfterDrain.incrementAndGet()
                                gate.inFlight shouldBeGreaterThanOrEqual 1
                                yield()
                            }
                        }
                    }
                    launch(threads) {
                        gate.drain()
                        isDrained.set(true)
                    }
                }
            }
            gate.awaitIdle()

            admittedAfterDrain.get() shouldBe 0
            gate.inFlight shouldBe 0
        }
    }

    context("a single flight under contention") {
        test("runs the block once for callers that wait for the same execution") {
            val flight = SingleFlight<String, Int>()
            val runs = AtomicInteger(0)

            val results = withThreads { threads ->
                coroutineScope {
                    List(THREADS_OF_THOUGHT) {
                        async(threads) {
                            flight.run("key") {
                                runs.incrementAndGet()
                                delay(50.milliseconds)
                                ANSWER
                            }
                        }
                    }.awaitAll()
                }
            }

            results.toSet() shouldBe setOf(ANSWER)
            runs.get() shouldBe 1
        }
    }
})
