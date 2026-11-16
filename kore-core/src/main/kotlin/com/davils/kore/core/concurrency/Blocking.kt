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
import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs the blocking [call] on [dispatcher] and suspends until it finishes, telling the call to stop through
 * [onCancel] as soon as the caller is cancelled while the call runs.
 *
 * The call runs at most once. As long as the caller is not cancelled, `blocking` returns the result of the call or
 * throws its exception, and neither [onCancel] nor [onDiscard] is invoked. Only a [CancellationException] that does
 * not come from the cancellation of the caller is treated differently, whether the call threw it, as
 * [java.util.concurrent.Future.get] does for a cancelled future, or [dispatcher] refused the call, for example after
 * it was closed: `blocking` never throws it as it is, because the caller would take it for its own cancellation and
 * drop the failure silently, but throws an [IllegalStateException] with it as the cause.
 *
 * When the caller is cancelled, `blocking` throws a [CancellationException], and what else happens depends on how far
 * the call got:
 *
 * - Before the call started, it never starts and [onCancel] is not invoked.
 * - While the call runs, [onCancel] is invoked exactly once, at the moment the caller is cancelled and on the thread
 *   that cancels it, such as the thread that calls `Job.cancel` or the one that runs out the time of `withTimeout`.
 *   It does not wait until the caller runs again, so it also reaches a call that occupies every thread the caller
 *   could run on, as when the caller runs on [dispatcher] itself. The caller then waits until the call returns or
 *   throws. A result that arrives after that goes to [onDiscard] on the thread of the call, so a resource such as an
 *   open handle can be released.
 * - After the call returned but before its result reached the caller, the result goes to [onDiscard] in the coroutine
 *   of the caller, and [onCancel] is not invoked.
 * - After the call threw an exception that is no [CancellationException], that exception is passed on in place of
 *   the cancellation and [onCancel] is not invoked, because a call that merely threw is never told to stop.
 *
 * In every case the caller returns only once [dispatcher] has finished or dropped the call, so a cancelled caller
 * still waits for a busy dispatcher. `blocking` never interrupts the thread of the call: a call that ignores
 * [onCancel] keeps the caller waiting until it finishes, so [onCancel] must make it return, for example by closing
 * the socket or the handle it blocks on. [onCancel] runs while the call runs, normally on another thread, so both must
 * be thread-safe with regard to the state they share. It runs inside the cancellation of the caller, on a thread
 * that neither `blocking` nor [dispatcher] chooses, so it must return quickly and must not block or wait for other
 * coroutines. [onDiscard] is invoked at most once.
 *
 * Failures of [onCancel], of [onDiscard] and of a call that broke off after it was told to stop never replace the
 * cancellation and never escape into the thread they happened on. When there are any, `blocking` throws a new
 * [CancellationException] of this call, with the message of the cancellation of the caller, that cancellation as its
 * cause and the failures attached as suppressed exceptions, as they were thrown and without escaping, so whoever logs
 * it treats their messages as foreign text. The cancellation of the caller itself is never changed: callers that are
 * cancelled with one shared exception never see each other's failures, and only a `catch` around `blocking` sees the
 * failures, while the job of the caller and `withTimeout` report the cancellation of the caller as it was. An [Error]
 * among the failures, such as an [OutOfMemoryError], is thrown unchanged instead of any cancellation, and the other
 * failures are dropped.
 *
 * Marked [DavilsInternal] for the libraries of Davils. The opt-in is a marker for the compiler, not an access
 * boundary: the package is exported, so any code on the class path can call it.
 *
 * @param T the type of the result.
 * @param dispatcher where the call runs, one meant for blocking work; Kore starts no threads of its own.
 * @param onCancel tells the running call to stop; invoked at most once, concurrently with the call and on the thread
 * that cancels the caller, so it must be quick and must not block.
 * @param onDiscard releases a result that the caller will never receive; does nothing by default.
 * @param call the blocking work.
 * @return the result of [call].
 * @throws CancellationException if the caller is cancelled before it receives the result, unless the call already
 * threw an exception that is no [CancellationException].
 * @throws IllegalStateException if the call throws a [CancellationException] or [dispatcher] refuses the call while
 * the caller is not cancelled; that [CancellationException] is its cause.
 * @since 1.0.0
 */
@DavilsInternal
public suspend fun <T> blocking(
    dispatcher: CoroutineDispatcher,
    onCancel: () -> Unit,
    onDiscard: (T) -> Unit = {},
    call: () -> T,
): T = BlockingCall(onCancel, onDiscard, call).awaitOn(dispatcher)
