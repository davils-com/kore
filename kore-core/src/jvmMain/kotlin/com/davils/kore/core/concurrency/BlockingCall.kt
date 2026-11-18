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

import com.davils.kore.core.annotation.DavilsInternal
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume

private const val SKIPPED_MESSAGE: String = "The blocking call was cancelled before it started"
private const val ABANDONED_MESSAGE: String = "The blocking call finished after its caller was cancelled"
private const val FOREIGN_MESSAGE: String =
    "The blocking call ended in a cancellation that did not come from the cancellation of its caller"

@DavilsInternal
internal class BlockingCall<T>(
    private val onCancel: () -> Unit,
    private val onDiscard: (T) -> Unit,
    private val call: () -> T,
) {
    private val phase: AtomicInteger = AtomicInteger(BlockingState.Pending.ordinal)
    private val produced: AtomicReference<Result<T>?> = AtomicReference(null)
    private val failures: ConcurrentLinkedQueue<Throwable> = ConcurrentLinkedQueue()

    val state: BlockingState get() = BlockingState.entries[phase.get()]

    suspend fun awaitOn(dispatcher: CoroutineDispatcher): T =
        settlingCancellation({ cancellation -> settled(cancellation, !currentCoroutineContext().isActive) }) {
            coroutineScope {
                val running = async(dispatcher, CoroutineStart.LAZY) { perform() }
                startAndAwait(running)
            }
        }

    fun perform(): T {
        if (!transition(from = BlockingState.Pending, to = BlockingState.Running)) {
            throw CancellationException(SKIPPED_MESSAGE)
        }
        val outcome = runCatching(call)
        if (transition(from = BlockingState.Running, to = BlockingState.Done)) return delivered(outcome)
        abandon(outcome)
        throw CancellationException(ABANDONED_MESSAGE)
    }

    fun cancel() {
        if (transition(from = BlockingState.Pending, to = BlockingState.Cancelled)) return
        if (!transition(from = BlockingState.Running, to = BlockingState.Cancelled)) return
        guarded(onCancel)
    }

    private suspend fun startAndAwait(running: Deferred<T>): T {
        suspendCancellableCoroutine<Unit> { waiting ->
            waiting.invokeOnCancellation { cancel() }
            running.invokeOnCompletion { waiting.resume(Unit) }
            running.start()
        }
        return running.await()
    }

    private fun settled(cancellation: CancellationException, isCallerCancelled: Boolean): Throwable {
        discardUnclaimed()
        if (!isCallerCancelled) return IllegalStateException(FOREIGN_MESSAGE, cancellation)
        return failures.firstOrNull { failure -> failure is Error } ?: carrying(cancellation)
    }

    private fun carrying(cancellation: CancellationException): CancellationException {
        val attached = failures.filter { failure -> failure !== cancellation }
        if (attached.isEmpty()) return cancellation
        val own = CancellationException(cancellation.message, cancellation)
        attached.forEach(own::addSuppressed)
        return own
    }

    private fun delivered(outcome: Result<T>): T {
        if (outcome.isSuccess) produced.set(outcome)
        return outcome.getOrThrow()
    }

    private fun abandon(outcome: Result<T>) {
        outcome.onFailure(failures::add)
        outcome.onSuccess(::discard)
    }

    private fun discardUnclaimed() {
        val unclaimed = produced.getAndSet(null) ?: return
        unclaimed.onSuccess(::discard)
    }

    private fun discard(result: T) {
        guarded { onDiscard(result) }
    }

    private fun guarded(action: () -> Unit) {
        runCatching(action).onFailure(failures::add)
    }

    private fun transition(from: BlockingState, to: BlockingState): Boolean =
        phase.compareAndSet(from.ordinal, to.ordinal)
}

private inline fun <R> settlingCancellation(settle: (CancellationException) -> Throwable, block: () -> R): R {
    try {
        return block()
    } catch (cancellation: CancellationException) {
        throw settle(cancellation)
    }
}
