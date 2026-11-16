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
import com.davils.kore.core.concurrency.fixture.ImmediateDispatcher
import com.davils.kore.core.concurrency.fixture.SharedCancellation
import com.davils.kore.core.concurrency.fixture.shouldOpen
import com.davils.kore.core.concurrency.fixture.withSingleThread
import com.davils.kore.core.concurrency.fixture.withThreads
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicReferenceArray
import kotlin.coroutines.cancellation.CancellationException

private const val ANSWER: Int = 42
private const val PATIENCE: Long = 5
private const val HANDLE: String = "open handle"
private const val TENANTS: Int = 3
private const val TIMEOUT_MILLISECONDS: Long = 100
private const val FOREIGN_CANCELLATION: String = "future cancelled"
private const val SIMULATED: String = "simulated"
private const val SHUTDOWN: String = "shutdown"
private const val DISK_GONE: String = "disk gone"

class BlockingTest : FunSpec({
    context("a blocking call that is not cancelled") {
        test("returns its result without being told to stop or discarding it") {
            val cancels = AtomicInteger(0)
            val discarded = AtomicInteger(0)

            val result = withThreads { dispatcher ->
                blocking(
                    dispatcher,
                    onCancel = { cancels.incrementAndGet() },
                    onDiscard = { discarded.incrementAndGet() },
                ) { ANSWER }
            }

            result shouldBe ANSWER
            cancels.get() shouldBe 0
            discarded.get() shouldBe 0
        }

        test("passes on the failure of the call and never tells a call that merely failed to stop") {
            val cancels = AtomicInteger(0)

            val failure = shouldThrow<IOException> {
                withThreads { dispatcher ->
                    blocking(dispatcher, onCancel = { cancels.incrementAndGet() }) { throw IOException(DISK_GONE) }
                }
            }

            failure.message shouldBe DISK_GONE
            cancels.get() shouldBe 0
        }
    }

    context("a blocking call whose caller is cancelled") {
        test("is told to stop while it is still running") {
            val started = CountDownLatch(1)
            val stopped = CountDownLatch(1)

            withThreads { dispatcher ->
                coroutineScope {
                    val call = launch(start = CoroutineStart.UNDISPATCHED) {
                        blocking(dispatcher, onCancel = { stopped.countDown() }) {
                            started.countDown()
                            stopped.await(PATIENCE, TimeUnit.SECONDS)
                        }
                    }
                    started.shouldOpen()
                    call.cancelAndJoin()
                }
            }

            stopped.count shouldBe 0L
        }

        test("is told to stop on the cancelling thread while it occupies the only thread the caller runs on") {
            val started = CountDownLatch(1)
            val stopped = CountDownLatch(1)
            val released = AtomicBoolean(false)
            val stoppedOn = AtomicReference<Thread?>(null)
            val canceller = AtomicReference<Thread?>(null)

            withSingleThread { worker ->
                coroutineScope {
                    val call = launch(worker.dispatcher) {
                        blocking(
                            worker.dispatcher,
                            onCancel = {
                                stoppedOn.set(Thread.currentThread())
                                stopped.countDown()
                            },
                        ) {
                            started.countDown()
                            released.set(stopped.await(PATIENCE, TimeUnit.SECONDS))
                        }
                    }
                    started.shouldOpen()
                    canceller.set(Thread.currentThread())
                    call.cancel()
                    stopped.shouldOpen()
                    call.join()
                }
            }

            released.get() shouldBe true
            stoppedOn.get() shouldBe canceller.get()
        }

        test("is told to stop while it runs on a dispatcher that runs it right away on the thread of the caller") {
            val started = CountDownLatch(1)
            val stopped = CountDownLatch(1)
            val released = AtomicBoolean(false)

            withSingleThread { worker ->
                coroutineScope {
                    val call = launch(worker.dispatcher) {
                        blocking(ImmediateDispatcher, onCancel = { stopped.countDown() }) {
                            started.countDown()
                            released.set(stopped.await(PATIENCE, TimeUnit.SECONDS))
                        }
                    }
                    started.shouldOpen()
                    call.cancel()
                    stopped.shouldOpen()
                    call.join()
                }
            }

            released.get() shouldBe true
        }

        test("is stopped by a timeout of a caller that runs on the dispatcher the call occupies") {
            val stopped = CountDownLatch(1)
            val released = AtomicBoolean(false)

            withSingleThread { worker ->
                withContext(worker.dispatcher) {
                    shouldThrow<TimeoutCancellationException> {
                        withTimeout(TIMEOUT_MILLISECONDS) {
                            blocking(worker.dispatcher, onCancel = { stopped.countDown() }) {
                                released.set(stopped.await(PATIENCE, TimeUnit.SECONDS))
                            }
                        }
                    }
                }
            }

            released.get() shouldBe true
        }

        test("never starts and is never told to stop when the caller is cancelled before the call began") {
            val ran = AtomicBoolean(false)
            val cancels = AtomicInteger(0)
            val caught = AtomicReference<Throwable?>(null)
            val busy = CountDownLatch(1)

            withSingleThread { worker ->
                worker.occupyUntil(busy)
                coroutineScope {
                    val call = launch(start = CoroutineStart.UNDISPATCHED) {
                        val thrown = shouldThrow<CancellationException> {
                            blocking(worker.dispatcher, onCancel = { cancels.incrementAndGet() }) { ran.set(true) }
                        }
                        caught.set(thrown)
                    }
                    call.cancel()
                    busy.countDown()
                    call.join()
                }
            }

            ran.get() shouldBe false
            cancels.get() shouldBe 0
            caught.get().shouldBeInstanceOf<CancellationException>()
        }

        test("hands a result that arrives after it was told to stop to onDiscard") {
            val started = CountDownLatch(1)
            val stopped = CountDownLatch(1)
            val release = CountDownLatch(1)
            val discarded = AtomicReference<String?>(null)
            val caught = AtomicReference<Throwable?>(null)

            withSingleThread { caller ->
                withThreads { dispatcher ->
                    coroutineScope {
                        val call = launch(caller.dispatcher) {
                            val thrown = shouldThrow<CancellationException> {
                                blocking(
                                    dispatcher,
                                    onCancel = { stopped.countDown() },
                                    onDiscard = { handle -> discarded.set(handle) },
                                ) {
                                    started.countDown()
                                    release.await(PATIENCE, TimeUnit.SECONDS)
                                    HANDLE
                                }
                            }
                            caught.set(thrown)
                        }
                        started.shouldOpen()
                        call.cancel()
                        stopped.shouldOpen()
                        release.countDown()
                        call.join()
                    }
                }
            }

            discarded.get() shouldBe HANDLE
            caught.get().shouldBeInstanceOf<CancellationException>()
        }

        test("hands a result that was ready but never delivered to onDiscard without telling the call to stop") {
            val cancels = AtomicInteger(0)
            val discarded = AtomicReference<String?>(null)
            val caught = AtomicReference<Throwable?>(null)
            val resume = CountDownLatch(1)
            val returning = CountDownLatch(1)

            withSingleThread { caller ->
                withSingleThread { worker ->
                    coroutineScope {
                        val call = launch(caller.dispatcher) {
                            val thrown = shouldThrow<CancellationException> {
                                blocking(
                                    worker.dispatcher,
                                    onCancel = { cancels.incrementAndGet() },
                                    onDiscard = { handle -> discarded.set(handle) },
                                ) {
                                    caller.awaitIdle()
                                    caller.occupyUntil(resume)
                                    returning.countDown()
                                    HANDLE
                                }
                            }
                            caught.set(thrown)
                        }
                        returning.shouldOpen()
                        worker.awaitIdle()
                        call.cancel()
                        resume.countDown()
                        call.join()
                    }
                }
            }

            cancels.get() shouldBe 0
            discarded.get() shouldBe HANDLE
            caught.get().shouldBeInstanceOf<CancellationException>()
        }

        test("passes on the failure of a call that threw before its caller was cancelled and never tells it to stop") {
            val cancels = AtomicInteger(0)
            val caught = AtomicReference<Throwable?>(null)
            val resume = CountDownLatch(1)
            val returning = CountDownLatch(1)

            withSingleThread { caller ->
                withSingleThread { worker ->
                    coroutineScope {
                        val call = launch(caller.dispatcher) {
                            val thrown = shouldThrow<IOException> {
                                blocking<Unit>(worker.dispatcher, onCancel = { cancels.incrementAndGet() }) {
                                    caller.awaitIdle()
                                    caller.occupyUntil(resume)
                                    returning.countDown()
                                    throw IOException(DISK_GONE)
                                }
                            }
                            caught.set(thrown)
                        }
                        returning.shouldOpen()
                        worker.awaitIdle()
                        call.cancel()
                        resume.countDown()
                        call.join()
                    }
                }
            }

            cancels.get() shouldBe 0
            caught.get()?.message shouldBe DISK_GONE
        }

        test("keeps the cancellation and attaches the failure of onCancel to it") {
            val started = CountDownLatch(1)
            val release = CountDownLatch(1)
            val caught = AtomicReference<Throwable?>(null)

            withThreads { dispatcher ->
                coroutineScope {
                    val call = launch(start = CoroutineStart.UNDISPATCHED) {
                        val thrown = shouldThrow<CancellationException> {
                            blocking(
                                dispatcher,
                                onCancel = {
                                    release.countDown()
                                    error("abort failed")
                                },
                            ) {
                                started.countDown()
                                release.await(PATIENCE, TimeUnit.SECONDS)
                            }
                        }
                        caught.set(thrown)
                    }
                    started.shouldOpen()
                    call.cancelAndJoin()
                }
            }

            val thrown = caught.get().shouldBeInstanceOf<CancellationException>()
            val suppressed = thrown.suppressed.toList()
            suppressed.shouldHaveSingleElement { failure -> failure is IllegalStateException }
            suppressed.single().message shouldBe "abort failed"
        }

        test("keeps the cancellation and attaches the failure of a call that broke off after it was told to stop") {
            val started = CountDownLatch(1)
            val release = CountDownLatch(1)
            val caught = AtomicReference<Throwable?>(null)

            withThreads { dispatcher ->
                coroutineScope {
                    val call = launch(start = CoroutineStart.UNDISPATCHED) {
                        val thrown = shouldThrow<CancellationException> {
                            blocking(dispatcher, onCancel = { release.countDown() }) {
                                started.countDown()
                                release.await(PATIENCE, TimeUnit.SECONDS)
                                throw IOException("connection aborted")
                            }
                        }
                        caught.set(thrown)
                    }
                    started.shouldOpen()
                    call.cancelAndJoin()
                }
            }

            val thrown = caught.get().shouldBeInstanceOf<CancellationException>()
            thrown.suppressed.map { failure -> failure.message } shouldBe listOf("connection aborted")
        }

        test("survives an onCancel that throws the cancellation it was called for") {
            val started = CountDownLatch(1)
            val release = CountDownLatch(1)
            val reason = CancellationException("caller gave up")
            val caught = AtomicReference<Throwable?>(null)

            withThreads { dispatcher ->
                coroutineScope {
                    val call = launch(start = CoroutineStart.UNDISPATCHED) {
                        val thrown = shouldThrow<CancellationException> {
                            blocking(
                                dispatcher,
                                onCancel = {
                                    release.countDown()
                                    throw reason
                                },
                            ) {
                                started.countDown()
                                release.await(PATIENCE, TimeUnit.SECONDS)
                            }
                        }
                        caught.set(thrown)
                    }
                    started.shouldOpen()
                    call.cancel(reason)
                    call.join()
                }
            }

            caught.get().shouldBeInstanceOf<CancellationException>()
        }

        test("throws a virtual machine error of onCancel unchanged in place of the cancellation") {
            val started = CountDownLatch(1)
            val release = CountDownLatch(1)
            val simulated = OutOfMemoryError(SIMULATED)
            val caught = AtomicReference<Throwable?>(null)

            withThreads { dispatcher ->
                coroutineScope {
                    val call = launch(start = CoroutineStart.UNDISPATCHED) {
                        caught.set(
                            shouldThrow<OutOfMemoryError> {
                                blocking(
                                    dispatcher,
                                    onCancel = {
                                        release.countDown()
                                        throw simulated
                                    },
                                ) {
                                    started.countDown()
                                    release.await(PATIENCE, TimeUnit.SECONDS)
                                }
                            },
                        )
                    }
                    started.shouldOpen()
                    call.cancelAndJoin()
                }
            }

            caught.get() shouldBeSameInstanceAs simulated
            simulated.suppressed.toList().shouldBeEmpty()
        }

        test("throws a virtual machine error of a call that broke off after it was told to stop in its place") {
            val started = CountDownLatch(1)
            val release = CountDownLatch(1)
            val simulated = OutOfMemoryError(SIMULATED)
            val caught = AtomicReference<Throwable?>(null)

            withThreads { dispatcher ->
                coroutineScope {
                    val call = launch(start = CoroutineStart.UNDISPATCHED) {
                        caught.set(
                            shouldThrow<OutOfMemoryError> {
                                blocking(dispatcher, onCancel = { release.countDown() }) {
                                    started.countDown()
                                    release.await(PATIENCE, TimeUnit.SECONDS)
                                    throw simulated
                                }
                            },
                        )
                    }
                    started.shouldOpen()
                    call.cancelAndJoin()
                }
            }

            caught.get() shouldBeSameInstanceAs simulated
        }

        test("attaches nothing when nothing failed") {
            val started = CountDownLatch(1)
            val caught = AtomicReference<Throwable?>(null)

            withThreads { dispatcher ->
                coroutineScope {
                    val call = launch(start = CoroutineStart.UNDISPATCHED) {
                        val release = CountDownLatch(1)
                        val thrown = shouldThrow<CancellationException> {
                            blocking(dispatcher, onCancel = { release.countDown() }) {
                                started.countDown()
                                release.await(PATIENCE, TimeUnit.SECONDS)
                            }
                        }
                        caught.set(thrown)
                    }
                    started.shouldOpen()
                    call.cancelAndJoin()
                }
            }

            val thrown = caught.get().shouldBeInstanceOf<CancellationException>()
            thrown.suppressed.toList().shouldBeEmpty()
        }
    }

    context("blocking calls whose callers are cancelled with one shared cancellation") {
        test("each throw a cancellation of their own that carries only their own failures") {
            val shared = SharedCancellation(SHUTDOWN)
            val started = CountDownLatch(TENANTS)
            val caught = AtomicReferenceArray<Throwable?>(TENANTS)

            withThreads { dispatcher ->
                coroutineScope {
                    val calls = List(TENANTS) { tenant ->
                        launch(start = CoroutineStart.UNDISPATCHED) {
                            val release = CountDownLatch(1)
                            val thrown = shouldThrow<CancellationException> {
                                blocking(
                                    dispatcher,
                                    onCancel = {
                                        release.countDown()
                                        error("secret of tenant $tenant")
                                    },
                                ) {
                                    started.countDown()
                                    release.await(PATIENCE, TimeUnit.SECONDS)
                                }
                            }
                            caught.set(tenant, thrown)
                        }
                    }
                    started.shouldOpen()
                    calls.forEach { call -> call.cancel(shared) }
                    calls.joinAll()
                }
            }

            shared.suppressed.toList().shouldBeEmpty()
            repeat(TENANTS) { tenant ->
                val thrown = caught.get(tenant).shouldBeInstanceOf<CancellationException>()
                thrown shouldNotBeSameInstanceAs shared
                thrown.cause shouldBeSameInstanceAs shared
                thrown.message shouldBe shared.message
                thrown.suppressed.map { failure -> failure.message } shouldBe listOf("secret of tenant $tenant")
            }
        }

        test("hand the shared cancellation on unchanged when nothing failed") {
            val shared = SharedCancellation(SHUTDOWN)
            val started = CountDownLatch(1)
            val caught = AtomicReference<Throwable?>(null)

            withThreads { dispatcher ->
                coroutineScope {
                    val call = launch(start = CoroutineStart.UNDISPATCHED) {
                        val release = CountDownLatch(1)
                        val thrown = shouldThrow<CancellationException> {
                            blocking(dispatcher, onCancel = { release.countDown() }) {
                                started.countDown()
                                release.await(PATIENCE, TimeUnit.SECONDS)
                            }
                        }
                        caught.set(thrown)
                    }
                    started.shouldOpen()
                    call.cancel(shared)
                    call.join()
                }
            }

            caught.get() shouldBeSameInstanceAs shared
            shared.suppressed.toList().shouldBeEmpty()
        }
    }

    context("a blocking call that ends in a cancellation its caller did not cause") {
        test("throws an IllegalStateException that is no cancellation when the call throws a cancellation") {
            val cancels = AtomicInteger(0)

            val failure = shouldThrowExactly<IllegalStateException> {
                withThreads { dispatcher ->
                    blocking(dispatcher, onCancel = { cancels.incrementAndGet() }) {
                        throw CancellationException(FOREIGN_CANCELLATION)
                    }
                }
            }

            failure.cause.shouldBeInstanceOf<CancellationException>()
            failure.cause?.message shouldBe FOREIGN_CANCELLATION
            cancels.get() shouldBe 0
            currentCoroutineContext().isActive shouldBe true
        }

        test("throws an IllegalStateException that is no cancellation when the dispatcher refuses the call") {
            val ran = AtomicBoolean(false)
            val cancels = AtomicInteger(0)

            val failure = shouldThrowExactly<IllegalStateException> {
                withSingleThread { refusing ->
                    refusing.close()
                    blocking(refusing.dispatcher, onCancel = { cancels.incrementAndGet() }) { ran.set(true) }
                }
            }

            failure.cause.shouldBeInstanceOf<CancellationException>()
            ran.get() shouldBe false
            cancels.get() shouldBe 0
            currentCoroutineContext().isActive shouldBe true
        }

        test("reaches the exception handler of a caller that was launched instead of vanishing") {
            val handled = AtomicReference<Throwable?>(null)
            val handler = CoroutineExceptionHandler { _, failure -> handled.set(failure) }

            withThreads { dispatcher ->
                val scope = CoroutineScope(SupervisorJob() + handler)
                val call = scope.launch {
                    blocking(dispatcher, onCancel = {}) { throw CancellationException(FOREIGN_CANCELLATION) }
                }
                call.join()
            }

            handled.get().shouldBeInstanceOf<IllegalStateException>()
        }
    }
})
