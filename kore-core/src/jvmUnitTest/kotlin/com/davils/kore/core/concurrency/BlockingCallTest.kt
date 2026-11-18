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

@file:OptIn(DavilsInternal::class)

package com.davils.kore.core.concurrency

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.concurrency.fixture.shouldOpen
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.coroutines.cancellation.CancellationException

private const val HANDLE: String = "open handle"
private const val PATIENCE: Long = 5

class BlockingCallTest : FunSpec({
    context("the state of a blocking call") {
        test("starts out pending") {
            BlockingCall(onCancel = {}, onDiscard = {}) { HANDLE }.state shouldBe BlockingState.Pending
        }

        test("moves from pending through running to done when the call returns") {
            val seen = AtomicReference<BlockingState?>(null)
            val call = AtomicReference<BlockingCall<String>?>(null)
            call.set(
                BlockingCall(onCancel = {}, onDiscard = {}) {
                    seen.set(call.get()?.state)
                    HANDLE
                },
            )
            val blockingCall = checkNotNull(call.get())

            blockingCall.perform() shouldBe HANDLE

            seen.get() shouldBe BlockingState.Running
            blockingCall.state shouldBe BlockingState.Done
        }

        test("moves from pending to cancelled without running the call or telling it to stop") {
            val ran = AtomicBoolean(false)
            val cancels = AtomicInteger(0)
            val call = BlockingCall(onCancel = { cancels.incrementAndGet() }, onDiscard = {}) { ran.set(true) }

            call.cancel()

            call.state shouldBe BlockingState.Cancelled
            shouldThrow<CancellationException> { call.perform() }
            ran.get() shouldBe false
            cancels.get() shouldBe 0
        }

        test("moves from running to cancelled, tells the call to stop once and discards its late result") {
            val started = CountDownLatch(1)
            val release = CountDownLatch(1)
            val cancels = AtomicInteger(0)
            val discarded = AtomicReference<String?>(null)
            val outcome = AtomicReference<Result<String>?>(null)
            val call = BlockingCall(
                onCancel = { cancels.incrementAndGet() },
                onDiscard = { handle: String -> discarded.set(handle) },
            ) {
                started.countDown()
                release.await(PATIENCE, TimeUnit.SECONDS)
                HANDLE
            }
            val worker = thread { outcome.set(runCatching { call.perform() }) }
            started.shouldOpen()

            call.cancel()
            call.cancel()
            release.countDown()
            worker.join()

            call.state shouldBe BlockingState.Cancelled
            cancels.get() shouldBe 1
            discarded.get() shouldBe HANDLE
            val failure = outcome.get()?.exceptionOrNull()
            failure.shouldBeInstanceOf<CancellationException>()
        }

        test("moves from running to done when the call fails, so a later cancellation never tells it to stop") {
            val cancels = AtomicInteger(0)
            val call = BlockingCall<String>(onCancel = { cancels.incrementAndGet() }, onDiscard = {}) {
                throw IOException("disk gone")
            }

            shouldThrow<IOException> { call.perform() }
            call.cancel()

            call.state shouldBe BlockingState.Done
            cancels.get() shouldBe 0
        }

        test("stays done when it is cancelled after the call returned") {
            val cancels = AtomicInteger(0)
            val discarded = AtomicInteger(0)
            val call = BlockingCall(
                onCancel = { cancels.incrementAndGet() },
                onDiscard = { _: String -> discarded.incrementAndGet() },
            ) { HANDLE }

            call.perform()
            call.cancel()

            call.state shouldBe BlockingState.Done
            cancels.get() shouldBe 0
            discarded.get() shouldBe 0
        }
    }

    context("failures of the callbacks") {
        test("never let a virtual machine error of onCancel escape into the thread that cancels") {
            val started = CountDownLatch(1)
            val release = CountDownLatch(1)
            val cancels = AtomicInteger(0)
            val call = BlockingCall(
                onCancel = {
                    cancels.incrementAndGet()
                    throw OutOfMemoryError("simulated")
                },
                onDiscard = {},
            ) {
                started.countDown()
                release.await(PATIENCE, TimeUnit.SECONDS)
            }
            val worker = thread { runCatching { call.perform() } }
            started.shouldOpen()

            shouldNotThrowAny { call.cancel() }

            release.countDown()
            worker.join()
            cancels.get() shouldBe 1
        }

        test("never let a virtual machine error of a call that broke off after it was told to stop escape") {
            val started = CountDownLatch(1)
            val release = CountDownLatch(1)
            val outcome = AtomicReference<Result<Unit>?>(null)
            val call = BlockingCall<Unit>(onCancel = { release.countDown() }, onDiscard = {}) {
                started.countDown()
                release.await(PATIENCE, TimeUnit.SECONDS)
                throw OutOfMemoryError("simulated")
            }
            val worker = thread { outcome.set(runCatching { call.perform() }) }
            started.shouldOpen()

            call.cancel()
            worker.join()

            val failure = outcome.get()?.exceptionOrNull()
            failure.shouldBeInstanceOf<CancellationException>()
        }
    }
})
