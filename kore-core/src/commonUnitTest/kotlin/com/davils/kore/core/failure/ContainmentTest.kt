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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

private const val ANSWER: Int = 42

private fun brokenCall(failure: Throwable): Int = throw failure

class ContainmentTest : FunSpec({
    context("contained") {
        test("returns the result of the block") {
            contained { ANSWER }.getOrNull() shouldBe ANSWER
        }

        test("contains an exception as it was thrown") {
            val failure = IllegalStateException("broken")

            contained { brokenCall(failure) }.exceptionOrNull() shouldBeSameInstanceAs failure
        }

        test("contains an error that is no failure of the platform") {
            val failure = NotImplementedError("later")

            contained { brokenCall(failure) }.exceptionOrNull() shouldBeSameInstanceAs failure
        }

        test("passes on every cancellation unchanged") {
            val cancellation = CancellationException("stop")

            val thrown = shouldThrow<CancellationException> { contained { brokenCall(cancellation) } }

            thrown shouldBeSameInstanceAs cancellation
        }
    }

    context("attempted") {
        test("returns the result of the block") {
            attempted { ANSWER }.getOrNull() shouldBe ANSWER
        }

        test("contains an exception as it was thrown") {
            val failure = IllegalArgumentException("refused")

            attempted { brokenCall(failure) }.exceptionOrNull() shouldBeSameInstanceAs failure
        }

        test("contains a cancellation that does not come from its caller") {
            val foreign = CompletableDeferred<Int>()
            foreign.cancel()

            val failure = attempted { foreign.await() }.exceptionOrNull()

            val wrapped = failure.shouldBeInstanceOf<IllegalStateException>()
            wrapped.cause.shouldBeInstanceOf<CancellationException>()
        }

        test("passes on the cancellation of its own coroutine") {
            val never = CompletableDeferred<Int>()
            var outcome: Result<Int>? = null

            coroutineScope {
                val job = launch(start = CoroutineStart.UNDISPATCHED) { outcome = attempted { never.await() } }
                job.cancelAndJoin()
                job.isCancelled shouldBe true
            }

            outcome.shouldBeNull()
        }
    }

    context("rethrowIfFatal") {
        test("returns for an ordinary exception") {
            IllegalStateException("broken").rethrowIfFatal()
        }

        test("throws a cancellation again") {
            val cancellation = CancellationException("stop")

            val thrown = shouldThrow<CancellationException> { cancellation.rethrowIfFatal() }

            thrown shouldBeSameInstanceAs cancellation
        }
    }
})
