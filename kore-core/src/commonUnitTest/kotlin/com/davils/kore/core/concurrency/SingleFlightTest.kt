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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private const val KEY: String = "entry"
private const val OTHER_KEY: String = "other"
private const val ANSWER: Int = 42

class SingleFlightTest : FunSpec({
    context("concurrent callers for one key") {
        test("share one execution and its result") {
            val flight = SingleFlight<String, Int>()
            val release = CompletableDeferred<Unit>()
            var runs = 0

            coroutineScope {
                val leader = async(start = CoroutineStart.UNDISPATCHED) {
                    flight.run(KEY) {
                        runs += 1
                        release.await()
                        ANSWER
                    }
                }
                val follower = async(start = CoroutineStart.UNDISPATCHED) { flight.run(KEY) { -1 } }
                release.complete(Unit)

                leader.await() shouldBe ANSWER
                follower.await() shouldBe ANSWER
            }

            runs shouldBe 1
        }

        test("share the exception of the execution") {
            val flight = SingleFlight<String, Int>()
            val release = CompletableDeferred<Unit>()
            val failure = IllegalStateException("broken")

            coroutineScope {
                val leader = async(start = CoroutineStart.UNDISPATCHED) {
                    shouldThrow<IllegalStateException> {
                        flight.run(KEY) {
                            release.await()
                            throw failure
                        }
                    }
                }
                val follower = async(start = CoroutineStart.UNDISPATCHED) {
                    shouldThrow<IllegalStateException> { flight.run(KEY) { -1 } }
                }
                release.complete(Unit)

                leader.await() shouldBeSameInstanceAs failure
                follower.await() shouldBeSameInstanceAs failure
            }
        }

        test("let a waiting caller lead again when the leader is cancelled") {
            val flight = SingleFlight<String, Int>()
            val never = CompletableDeferred<Int>()

            coroutineScope {
                val leader = launch(start = CoroutineStart.UNDISPATCHED) { flight.run(KEY) { never.await() } }
                val follower = async(start = CoroutineStart.UNDISPATCHED) { flight.run(KEY) { ANSWER } }
                leader.cancelAndJoin()

                follower.await() shouldBe ANSWER
            }
        }

        test("leave the leader running when a waiting caller is cancelled") {
            val flight = SingleFlight<String, Int>()
            val release = CompletableDeferred<Unit>()

            coroutineScope {
                val leader = async(start = CoroutineStart.UNDISPATCHED) {
                    flight.run(KEY) {
                        release.await()
                        ANSWER
                    }
                }
                val follower = launch(start = CoroutineStart.UNDISPATCHED) { flight.run(KEY) { -1 } }
                follower.cancelAndJoin()
                release.complete(Unit)

                leader.await() shouldBe ANSWER
            }
        }
    }

    context("separate calls") {
        test("run again once an execution has finished") {
            val flight = SingleFlight<String, Int>()

            flight.run(KEY) { 1 } shouldBe 1
            flight.run(KEY) { 2 } shouldBe 2
        }

        test("never wait for another key") {
            val flight = SingleFlight<String, Int>()
            val never = CompletableDeferred<Int>()

            coroutineScope {
                val stuck = launch(start = CoroutineStart.UNDISPATCHED) { flight.run(KEY) { never.await() } }

                flight.run(OTHER_KEY) { ANSWER } shouldBe ANSWER
                stuck.cancelAndJoin()
            }
        }

        test("throw the exception of an execution to its only caller") {
            val flight = SingleFlight<String, Int>()

            shouldThrow<IllegalArgumentException> { flight.run(KEY) { throw IllegalArgumentException("refused") } }
        }
    }
})
