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

package com.davils.kore.core.failure

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlin.coroutines.cancellation.CancellationException

private const val FOREIGN_MESSAGE: String =
    "The attempt ended in a cancellation that did not come from the cancellation of its caller"

/**
 * Throws this throwable again if no caller may contain it, and returns normally otherwise.
 *
 * Two kinds of throwables are never contained:
 *
 * - A [CancellationException], because it ends a coroutine and must reach the coroutine that is being cancelled.
 * - A failure of the platform. On the JVM these are a [VirtualMachineError], such as an [OutOfMemoryError] or a
 *   [StackOverflowError], after which the virtual machine cannot be trusted to continue, and an
 *   `InterruptedException`, for which the interrupt flag of the current thread is set again before it is thrown, so
 *   that the thread stays interrupted. On WebAssembly there is no such failure: a trap of the engine, such as an
 *   exhausted stack, ends the program without ever reaching Kotlin code.
 *
 * Every other throwable returns normally, every other [Error] included: a [LinkageError] of a missing or broken
 * class, an [AssertionError] or a [NotImplementedError] is a failure of the code that threw it, and a caller that
 * isolates foreign code is meant to contain it.
 *
 * The throwable is thrown as it is, never wrapped and never changed, and nothing is logged.
 *
 * @throws Throwable this throwable, if it is a [CancellationException] or a failure of the platform.
 * @since 2.1.0
 */
public fun Throwable.rethrowIfFatal() {
    rethrowPlatformFatal(this)
    if (this is CancellationException) throw this
}

/**
 * Runs [block] and returns its result or the throwable it threw as a [Result], except for the throwables that no
 * caller may contain.
 *
 * `contained` is the replacement for [runCatching] wherever foreign code runs, such as a callback, a converter or a
 * rule of a caller: [runCatching] also catches the cancellation of a coroutine and the failures of the platform and
 * hides them in a failed result. `contained` throws them again as [rethrowIfFatal] describes, so a [CancellationException]
 * and, on the JVM, a [VirtualMachineError] and an `InterruptedException` pass through, the latter with the interrupt
 * flag of the thread set again. Every other throwable, every other [Error] included, becomes a failed result, as it
 * was thrown.
 *
 * Every [CancellationException] passes through, whether or not it comes from the cancellation of the caller, because
 * outside a coroutine there is no way to tell. Inside a coroutine, use [attempted], which contains a cancellation
 * that does not belong to its caller.
 *
 * `contained` neither logs nor changes what it contains: a failed result holds the throwable of [block] itself, with
 * its message, its cause and its stack trace, so whoever shows it treats its message as foreign text.
 *
 * @param R the type of the result.
 * @param block the code to run.
 * @return the result of [block], or a failed result with the throwable it threw.
 * @throws CancellationException if [block] threw one.
 * @throws Throwable a failure of the platform that [block] threw, such as an [OutOfMemoryError] on the JVM.
 * @since 2.1.0
 */
public inline fun <R> contained(block: () -> R): Result<R> {
    val result = runCatching(block)
    val failure = result.exceptionOrNull() ?: return result
    return Result.failure(containedFailure(failure, isOwnCancellation = true))
}

/**
 * Runs [block] in a coroutine and returns its result or the throwable it threw as a [Result], passing on only the
 * cancellation of the calling coroutine itself.
 *
 * `attempted` contains what [contained] contains and passes on the failures of the platform the same way. It differs
 * in how it treats a [CancellationException] that [block] throws:
 *
 * - If the calling coroutine is no longer active, the exception belongs to its cancellation and is thrown again as
 *   it is, so the coroutine ends as it should.
 * - If the calling coroutine is still active, the exception comes from somewhere else, such as the `withTimeout` of
 *   a callee, a cancelled `Deferred` that [block] awaited or a `java.util.concurrent.Future` that was cancelled.
 *   Thrown as it is, it would end the calling coroutine silently, as if it had been cancelled, and no handler would
 *   ever see the failure. `attempted` returns a failed result with an [IllegalStateException] instead, which has a
 *   constant message and the cancellation as its cause.
 *
 * [block] may suspend, because `attempted` is inline. Nothing is logged, and every other throwable is kept as it was
 * thrown.
 *
 * @param R the type of the result.
 * @param block the code to run.
 * @return the result of [block], or a failed result with the throwable it threw, or with an [IllegalStateException]
 * whose cause is a cancellation that did not come from the calling coroutine.
 * @throws CancellationException if the calling coroutine is cancelled and [block] threw a cancellation.
 * @throws Throwable a failure of the platform that [block] threw, such as an [OutOfMemoryError] on the JVM.
 * @since 2.1.0
 */
public suspend inline fun <R> attempted(block: () -> R): Result<R> {
    val result = runCatching(block)
    val failure = result.exceptionOrNull() ?: return result
    val isOwnCancellation = !currentCoroutineContext().isActive
    return Result.failure(containedFailure(failure, isOwnCancellation))
}

@PublishedApi
internal fun containedFailure(failure: Throwable, isOwnCancellation: Boolean): Throwable {
    rethrowPlatformFatal(failure)
    if (failure !is CancellationException) return failure
    if (isOwnCancellation) throw failure
    return IllegalStateException(FOREIGN_MESSAGE, failure)
}
