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
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private const val ANSWER: Int = 42

class AdmissionGateTest : FunSpec({
    context("an open gate") {
        test("admits work and counts it until its permit is back") {
            val gate = AdmissionGate()

            val permit = gate.tryEnter().shouldNotBeNull()
            gate.inFlight shouldBe 1
            permit.close()

            gate.inFlight shouldBe 0
            gate.state shouldBe GateState.Open
        }

        test("counts a permit closed twice once") {
            val gate = AdmissionGate()
            val first = gate.tryEnter().shouldNotBeNull()
            gate.tryEnter().shouldNotBeNull()

            first.close()
            first.close()

            gate.inFlight shouldBe 1
        }

        test("runs admitted work and gives the permit back, also when it throws") {
            val gate = AdmissionGate()

            gate.admitted { ANSWER } shouldBe ANSWER
            shouldThrow<IllegalArgumentException> { gate.admitted { throw IllegalArgumentException("broken") } }

            gate.inFlight shouldBe 0
        }
    }

    context("draining") {
        test("stops admitting and keeps the work in flight") {
            val gate = AdmissionGate()
            gate.tryEnter().shouldNotBeNull()

            gate.drain()

            gate.state shouldBe GateState.Draining
            gate.tryEnter().shouldBeNull()
            shouldThrow<IllegalStateException> { gate.admitted { ANSWER } }
            gate.inFlight shouldBe 1
        }

        test("waits until the last permit is back") {
            val gate = AdmissionGate()
            val permit = gate.tryEnter().shouldNotBeNull()
            val idle = CompletableDeferred<Unit>()

            coroutineScope {
                val waiting = async(start = CoroutineStart.UNDISPATCHED) {
                    gate.awaitIdle()
                    idle.complete(Unit)
                }
                gate.drain()
                idle.isCompleted shouldBe false
                permit.close()
                waiting.await()
            }

            idle.isCompleted shouldBe true
        }

        test("does not reopen a closed gate") {
            val gate = AdmissionGate()

            gate.close()
            gate.drain()

            gate.state shouldBe GateState.Closed
        }
    }

    context("closing") {
        test("stops admitting and lets permits in flight come back") {
            val gate = AdmissionGate()
            val permit = gate.tryEnter().shouldNotBeNull()

            gate.close()

            gate.tryEnter().shouldBeNull()
            permit.close()
            gate.inFlight shouldBe 0
            gate.awaitIdle()
        }
    }
})
